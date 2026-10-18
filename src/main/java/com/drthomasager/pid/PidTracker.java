package com.drthomasager.pid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Who has PID in a fight, read from when hits land.
 *
 * <p>A melee hit lands 0 or 1 tick after its attack animation begins. The
 * fighter whose hit lands on the very tick their swing begins was processed
 * first in that tick, which is having PID. Across 147 recorded Last Man
 * Standing fights, when one side's delay was 0 the other side's was 1 (245
 * nearby pairs, 2 exceptions). A crossbow shot adds evidence too, its delay
 * set against the bolt's travel time for the distance.
 *
 * <p>The holder is the side most of the last three pieces of evidence within
 * 40 ticks name, and unknown 40 ticks after the newest. Scored over the
 * recorded fights this names the next melee evidence's holder 88 percent of
 * the time and is known on 90 percent of fight ticks.
 *
 * <p>Pure Java with no RuneLite class, so it can be tested on its own.
 */
public class PidTracker
{
	public enum Side { ME, THEM }

	public enum Holder { ME, THEM, UNKNOWN }

	/*
	 * Every attack says who went first. The OSRS Wiki's hit delay page gives
	 * each attack its own delay, and a hit lands one tick later when the
	 * defender was processed first that tick. The animations below were sorted
	 * into kinds from recorded Last Man Standing fights, each kept only where
	 * its hits land on its kind's delay.
	 */

	/** Melee attacks: the hit's own delay is 0 ticks. */
	static final Set<Integer> MELEE_ANIMS = new HashSet<>(Arrays.asList(
		1658, 440, 1062, 7514, 393, 390, 376, 377, 9471, 7045, 428, 7516, 7644, 2067, 9171, 11140,
		414, 423, 10989, 11222));
	/** Bows and crossbows: 1 + (3 + distance) / 6. */
	static final Set<Integer> BOW_ANIMS = new HashSet<>(Arrays.asList(4230, 426, 9166));
	/** Thrown weapons, chinchompas and the blowpipe: 1 + distance / 6. */
	static final Set<Integer> THROWN_ANIMS = new HashSet<>(Arrays.asList(7218, 929));
	/** Spells: 1 + (1 + distance) / 3. */
	static final Set<Integer> MAGIC_ANIMS = new HashSet<>(Arrays.asList(1979, 1978, 7855, 1161, 710));
	static final int CROSSBOW_ANIM = 4230;

	/** Ticks after the newest evidence that the holder is still trusted. */
	static final int WINDOW = 40;
	private static final int KEEP = 200;

	static final class Obs
	{
		final int tick;
		final int anim;
		final int dist;

		Obs(int tick, int anim, int dist)
		{
			this.tick = tick;
			this.anim = anim;
			this.dist = dist;
		}
	}

	static final class Evidence
	{
		final int tick;
		final Side side;
		final Holder holder;

		Evidence(int tick, Side side, Holder holder)
		{
			this.tick = tick;
			this.side = side;
			this.holder = holder;
		}
	}

	private final List<Obs> mine = new ArrayList<>();
	private final List<Obs> theirs = new ArrayList<>();
	private final List<Integer> dealt = new ArrayList<>();
	private final List<Integer> taken = new ArrayList<>();

	public void reset()
	{
		mine.clear();
		theirs.clear();
		dealt.clear();
		taken.clear();
	}

	/** One reading of a side's animation on a tick, with the distance between the two. */
	public void observe(Side side, int tick, int anim, int dist)
	{
		List<Obs> l = side == Side.ME ? mine : theirs;
		l.add(new Obs(tick, anim, dist));
		trim(l);
	}

	/** A hitsplat on the opponent that is ours (Side.ME), or one on us (Side.THEM). */
	public void hit(Side attacker, int tick)
	{
		List<Integer> l = attacker == Side.ME ? dealt : taken;
		l.add(tick);
		trim(l);
	}

	private static <T> void trim(List<T> l)
	{
		while (l.size() > KEEP)
		{
			l.remove(0);
		}
	}

	/** The ticks an attack animation newly begins. */
	static List<Obs> starts(List<Obs> obs)
	{
		List<Obs> out = new ArrayList<>();
		Obs prev = null;
		for (Obs o : obs)
		{
			boolean isNew = baseDelay(o.anim, 0) >= 0
				&& (prev == null || prev.anim != o.anim || o.tick - prev.tick > 1);
			if (isNew)
			{
				out.add(o);
			}
			prev = o;
		}
		return out;
	}

	/** The crossbow bolt's delay in ticks for a distance: 1 + (3 + distance) / 6, rounded down. */
	static int crossbowDelay(int dist)
	{
		return 1 + (3 + dist) / 6;
	}

	/**
	 * The attack's own hit delay at a distance, when the attacker goes first,
	 * or -1 when the animation is not an attack it knows.
	 */
	static int baseDelay(int anim, int dist)
	{
		if (MELEE_ANIMS.contains(anim))
		{
			return 0;
		}
		if (BOW_ANIMS.contains(anim))
		{
			return crossbowDelay(dist);
		}
		if (THROWN_ANIMS.contains(anim))
		{
			return 1 + dist / 6;
		}
		if (MAGIC_ANIMS.contains(anim))
		{
			return 1 + (1 + dist) / 3;
		}
		return -1;
	}

	static Holder holderFor(Side side, boolean zero)
	{
		return zero == (side == Side.ME) ? Holder.ME : Holder.THEM;
	}

	/**
	 * Each attack start paired with that side's first hit within 6 ticks. On
	 * the attack's own delay the attacker went first, one tick after it the
	 * defender did, and any other gap says nothing.
	 */
	static List<Evidence> evidence(Side side, List<Obs> obs, List<Integer> hits)
	{
		List<Evidence> out = new ArrayList<>();
		for (Obs s : starts(obs))
		{
			for (int h : hits)
			{
				if (h >= s.tick && h <= s.tick + 6)
				{
					int d = MELEE_ANIMS.contains(s.anim) ? 0 : distAt(obs, h);
					if (d < 0)
					{
						d = s.dist;
					}
					if (d >= 0)
					{
						int r = (h - s.tick) - baseDelay(s.anim, d);
						if (r == 0 || r == 1)
						{
							out.add(new Evidence(s.tick, side, holderFor(side, r == 0)));
						}
					}
					break;
				}
			}
		}
		return out;
	}

	/** The distance read on a tick, or -1 when that tick was not read. */
	static int distAt(List<Obs> obs, int tick)
	{
		for (int i = obs.size() - 1; i >= 0; i--)
		{
			if (obs.get(i).tick == tick)
			{
				return obs.get(i).dist;
			}
		}
		return -1;
	}

	/** The newest evidence's tick, or -1. */
	private int newestTick = -1;

	public int newestEvidenceTick()
	{
		return newestTick;
	}

	/** Who has PID at tick now. */
	public Holder holderAt(int now)
	{
		List<Evidence> ev = new ArrayList<>();
		ev.addAll(evidence(Side.ME, mine, dealt));
		ev.addAll(evidence(Side.THEM, theirs, taken));
		ev.removeIf(e -> e.tick > now);
		ev.sort((a, b) -> Integer.compare(a.tick, b.tick));
		newestTick = ev.isEmpty() ? -1 : ev.get(ev.size() - 1).tick;
		List<Evidence> recent = new ArrayList<>();
		for (Evidence e : ev)
		{
			if (now - e.tick <= WINDOW)
			{
				recent.add(e);
			}
		}
		if (recent.isEmpty())
		{
			return Holder.UNKNOWN;
		}
		List<Evidence> last3 = recent.subList(Math.max(0, recent.size() - 3), recent.size());
		int me = 0;
		int them = 0;
		for (Evidence e : last3)
		{
			if (e.holder == Holder.ME)
			{
				me++;
			}
			else
			{
				them++;
			}
		}
		if (me != them)
		{
			return me > them ? Holder.ME : Holder.THEM;
		}
		return last3.get(last3.size() - 1).holder;
	}
}
