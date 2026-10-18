package com.drthomasager.pid;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

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

	/**
	 * Animations that are not attacks, so a hit landing near one says
	 * nothing: eating, drinking, Vengeance, dying, and the block animations.
	 */
	private static final Set<Integer> NOT_ATTACKS = new HashSet<>(Arrays.asList(
		829, 1327, 8317, 836, 424, 1156, 4177, 420, 388, 7219, 2079, 832, 1659));

	/** A staff swung as a melee weapon rather than cast with. */
	private static final Set<Integer> STAFF_BASH = new HashSet<>(Arrays.asList(
		440, 414, 419, 415, 393, 428, 423));

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PidIndicatorOverlay overlay;

	private final PidTracker tracker = new PidTracker();
	private final WeaponCategory.Cache categories = new WeaponCategory.Cache();

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
	private final int[] lastAnim = {-1, -1};
	private final int[] lastAnimTick = {-9, -9};

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

	/** The category of the weapon in a player's hands, from the game's item data, or -1. */
	private int weaponCategory(Player p)
	{
		PlayerComposition comp = p.getPlayerComposition();
		if (comp == null)
		{
			return -1;
		}
		int id = comp.getEquipmentId(KitType.WEAPON);
		if (id <= 0)
		{
			return -1;
		}
		IndexDataBase configs = client.getIndexConfig();
		if (configs == null)
		{
			return -1;
		}
		return categories.category(id, itemId -> configs.loadData(WeaponCategory.ITEM_ARCHIVE, itemId));
	}

	/**
	 * Reads one player's animation this tick and, when an attack newly
	 * begins, tells the tracker its kind from the weapon in their hands.
	 */
	private void readAttack(Player p, PidTracker.Side side, int tick, int dist)
	{
		int i = side.ordinal();
		int anim = p.getAnimation();
		boolean isNew = anim >= 0 && (anim != lastAnim[i] || tick - lastAnimTick[i] > 1);
		lastAnim[i] = anim;
		lastAnimTick[i] = tick;
		if (!isNew || NOT_ATTACKS.contains(anim))
		{
			return;
		}
		int category = weaponCategory(p);
		WeaponCategory.Kind kind = WeaponCategory.kindOf(category);
		if (kind == WeaponCategory.Kind.MAGIC && STAFF_BASH.contains(anim))
		{
			kind = WeaponCategory.Kind.MELEE;
		}
		tracker.attack(side, tick, kind, dist);
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
			readAttack(me, PidTracker.Side.ME, tick, dist);
			readAttack(live, PidTracker.Side.THEM, tick, dist);
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
}
