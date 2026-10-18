package com.drthomasager.pid;

import com.google.inject.Provides;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;

@Slf4j
@PluginDescriptor(
	name = "PID Indicator",
	description = "Shows who goes first in your fight (PID), read from when hits land",
	tags = {"pid", "pvp", "pk", "lms", "combat", "tick"},
	internalName = "pid-indicator"
)
public class PidIndicatorPlugin extends Plugin
{
	/** An opponent fought within this long stays marked while on screen. */
	private static final long LINGER_MS = 30_000;
	/** How often a changed attack book is written to disk, in ticks. */
	private static final int SAVE_EVERY = 100;
	private static final String BOOK_FILE = "attacks.txt";

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PidIndicatorOverlay overlay;

	@Inject
	private ScheduledExecutorService executor;

	private PidTracker tracker;

	@Getter
	private PidTracker.Holder holder = PidTracker.Holder.UNKNOWN;
	@Getter
	private int sinceChange;
	@Getter
	private Player opponent;

	private String opponentName;
	private long opponentSeenAt;
	private PidTracker.Holder lastKnown;
	private int changeTick = -1;

	@Override
	protected void startUp()
	{
		AttackBook book = AttackBook.bundled();
		try
		{
			Filepath f = bookFile();
			if (f.exists())
			{
				try (Reader r = f.openReader())
				{
					book.read(r);
				}
			}
		}
		catch (IOException e)
		{
			log.debug("attack book not read", e);
		}
		tracker = new PidTracker(book);
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		saveBook();
		holder = PidTracker.Holder.UNKNOWN;
		opponent = null;
		opponentName = null;
		lastKnown = null;
		changeTick = -1;
	}

	@Provides
	PidIndicatorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PidIndicatorConfig.class);
	}

	private Filepath bookFile() throws IOException
	{
		return getPluginDirectory().join(BOOK_FILE);
	}

	/** Writes the attack book off the client thread, when something was learned. */
	private void saveBook()
	{
		if (tracker == null || !tracker.book().changed())
		{
			return;
		}
		String text = tracker.book().write();
		executor.execute(() ->
		{
			try
			{
				bookFile().write(text);
			}
			catch (IOException e)
			{
				log.debug("attack book not written", e);
			}
		});
	}

	private String weaponOf(Player p)
	{
		PlayerComposition comp = p.getPlayerComposition();
		if (comp == null)
		{
			return null;
		}
		int id = comp.getEquipmentId(KitType.WEAPON);
		if (id <= 0)
		{
			return "Unarmed";
		}
		ItemComposition item = client.getItemDefinition(id);
		return item == null ? null : item.getName();
	}

	private Player findOpponent(Player me)
	{
		Actor t = me.getInteracting();
		if (t instanceof Player)
		{
			return (Player) t;
		}
		for (Player p : client.getTopLevelWorldView().players())
		{
			if (p != null && p != me && p.getInteracting() == me)
			{
				return p;
			}
		}
		return null;
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		Player me = client.getLocalPlayer();
		if (me == null || tracker == null)
		{
			return;
		}
		int tick = client.getTickCount();
		long now = System.currentTimeMillis();
		Player live = findOpponent(me);
		if (live != null && live.getName() != null)
		{
			if (!live.getName().equals(opponentName))
			{
				tracker.reset();
				lastKnown = null;
				changeTick = tick;
				opponentName = live.getName();
			}
			opponentSeenAt = now;
			int dist = me.getWorldLocation().distanceTo(live.getWorldLocation());
			tracker.observe(PidTracker.Side.ME, tick, me.getAnimation(), dist, weaponOf(me));
			tracker.observe(PidTracker.Side.THEM, tick, live.getAnimation(), dist, weaponOf(live));
			opponent = live;
		}
		else if (opponentName != null && now - opponentSeenAt < LINGER_MS)
		{
			opponent = null;
			for (Player p : client.getTopLevelWorldView().players())
			{
				if (p != null && opponentName.equals(p.getName()))
				{
					opponent = p;
					break;
				}
			}
		}
		else
		{
			opponent = null;
		}

		holder = opponent == null ? PidTracker.Holder.UNKNOWN : tracker.holderAt(tick);
		if (holder != PidTracker.Holder.UNKNOWN && holder != lastKnown)
		{
			lastKnown = holder;
			changeTick = tick;
		}
		sinceChange = changeTick < 0 ? 0 : Math.max(0, tick - changeTick);
		if (tick % SAVE_EVERY == 0)
		{
			saveBook();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		Player me = client.getLocalPlayer();
		if (me == null || opponentName == null || tracker == null)
		{
			return;
		}
		int tick = client.getTickCount();
		Actor a = e.getActor();
		if (a == me)
		{
			tracker.hit(PidTracker.Side.THEM, tick);
		}
		else if (a instanceof Player && opponentName.equals(a.getName()) && e.getHitsplat().isMine())
		{
			tracker.hit(PidTracker.Side.ME, tick);
		}
	}

	/** Ticks until PID can be drawn again: the wiki gives 100 to 150, and 40 to 60 on PvP worlds. */
	int[] redrawWindow()
	{
		return client.getWorldType().contains(WorldType.PVP) ? new int[]{40, 60} : new int[]{100, 150};
	}

	boolean inPvpPlace()
	{
		return client.getTopLevelWorldView().isInstance()
			|| client.getWorldType().contains(WorldType.PVP)
			|| client.getWorldType().contains(WorldType.LAST_MAN_STANDING)
			|| client.getWorldType().contains(WorldType.HIGH_RISK)
			|| client.getWorldType().contains(WorldType.DEADMAN)
			|| client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) > 0;
	}
}
