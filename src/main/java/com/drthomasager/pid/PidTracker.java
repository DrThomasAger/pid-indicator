package com.drthomasager.pid;

import java.util.ArrayList;
import java.util.List;

/**
 * Who has PID in a fight, read from when hits land.
 *
 * <p>Every attack has its own hit delay, set by the weapon in the attacker's
 * hands (OSRS Wiki, Hit delay), and a hit lands one tick later when the
 * defender was processed first in the tick. So each attack, paired with that
 * fighter's next hit, says who went first.
 *
 * <p>The holder is the side most of the last three readings within 40 ticks
 * name, and unknown 40 ticks after the newest, the soonest PID can be redrawn.
 *
 * <p>Plain Java with no RuneLite class, so it can be tested on its own.
 */
public class PidTracker
{
	public enum Side { ME, THEM }

	public enum Holder { ME, THEM, UNKNOWN }

	/** Ticks after the newest reading that the holder is still trusted. */
	static final int WINDOW = 40;
	/** The longest hit delay looked for after an attack starts. */
	static final int MAX_GAP = 6;
	private static final int KEEP = 200;

	static final class Start
	{
		final Side side;
		final int tick;
		final WeaponCategory.Kind kind;
		final int dist;

		Start(Side side, int tick, WeaponCategory.Kind kind, int dist)
		{
			this.side = side;
			this.tick = tick;
			this.kind = kind;
			this.dist = dist;
		}
	}

	private final List<Start> starts = new ArrayList<>();
	private final List<int[]> hits = new ArrayList<>();

	public void reset()
	{
		starts.clear();
		hits.clear();
	}

	/** An attack of this kind beginning on this tick, at this distance between the two. */
	public void attack(Side side, int tick, WeaponCategory.Kind kind, int dist)
	{
		starts.add(new Start(side, tick, kind, dist));
		while (starts.size() > KEEP)
		{
			starts.remove(0);
		}
	}

	/** A hit that side landed: on the opponent for Side.ME, on us for Side.THEM. */
	public void hit(Side attacker, int tick)
	{
		hits.add(new int[]{attacker.ordinal(), tick});
		while (hits.size() > KEEP)
		{
			hits.remove(0);
		}
	}

	/** Ticks from the start to that side's first hit within MAX_GAP, or -1. */
	private int gap(Start s)
	{
		for (int[] h : hits)
		{
			if (h[0] == s.side.ordinal() && h[1] >= s.tick && h[1] <= s.tick + MAX_GAP)
			{
				return h[1] - s.tick;
			}
		}
		return -1;
	}

	/** Who has PID at tick now. */
	public Holder holderAt(int now)
	{
		List<Holder> recent = new ArrayList<>();
		for (Start s : starts)
		{
			if (s.tick > now || now - s.tick > WINDOW || s.dist < 0)
			{
				continue;
			}
			int g = gap(s);
			if (g < 0)
			{
				continue;
			}
			int r = g - WeaponCategory.delay(s.kind, s.dist);
			if (r == 0 || r == 1)
			{
				boolean attackerFirst = r == 0;
				recent.add(attackerFirst == (s.side == Side.ME) ? Holder.ME : Holder.THEM);
			}
		}
		if (recent.isEmpty())
		{
			return Holder.UNKNOWN;
		}
		List<Holder> last3 = recent.subList(Math.max(0, recent.size() - 3), recent.size());
		int me = 0;
		int them = 0;
		for (Holder h : last3)
		{
			if (h == Holder.ME)
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
		return last3.get(last3.size() - 1);
	}
}
