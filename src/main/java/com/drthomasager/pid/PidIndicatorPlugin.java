package com.drthomasager.pid;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "PID Indicator",
	description = "Shows who has PID in your fight, read from when hits land, with the ticks until it can swap",
	tags = {"pid", "pvp", "pk", "lms", "combat", "tick"}
)
public class PidIndicatorPlugin extends Plugin
{
	/** An opponent fought within this long stays marked while on screen. */
	private static final long LINGER_MS = 30_000;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PidIndicatorOverlay overlay;

	private final PidTracker tracker = new PidTracker();

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
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		tracker.reset();
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
		if (me == null)
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
			tracker.observe(PidTracker.Side.ME, tick, me.getAnimation(), dist);
			tracker.observe(PidTracker.Side.THEM, tick, live.getAnimation(), dist);
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
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		Player me = client.getLocalPlayer();
		if (me == null || opponentName == null)
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
