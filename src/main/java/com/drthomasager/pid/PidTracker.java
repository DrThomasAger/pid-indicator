package com.drthomasager.pid;

import java.util.ArrayList;
import java.util.List;

/**
 * Who has PID in a fight, read from when hits land.
 *
 * <p>Every attack has its own hit delay, and a hit lands one tick later when
 * the defender was processed first in the tick (OSRS Wiki, Hit delay). So each
 * attack, paired with that fighter's next hit, says who went first. Which
 * animations are attacks, and which rule each follows, comes from the
 * {@link AttackBook}, which learns them from the same pairs.
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
		final int anim;
		final int dist;
		final String weapon;
		boolean learned;

		Start(Side side, int tick, int anim, int dist, String weapon)
		{
			this.side = side;
			this.tick = tick;
			this.anim = anim;
			this.dist = dist;
			this.weapon = weapon;
		}
	}

	private final AttackBook book;
	private final List<Start> starts = new ArrayList<>();
	private final List<int[]> hits = new ArrayList<>();
	private final int[] lastAnim = {-1, -1};
	private final int[] lastTick = {-9, -9};

	public PidTracker()
	{
		this(AttackBook.bundled());
	}

	public PidTracker(AttackBook book)
	{
		this.book = book;
	}

	public AttackBook book()
	{
		return book;
	}

	public void reset()
	{
		starts.clear();
		hits.clear();
		lastAnim[0] = lastAnim[1] = -1;
		lastTick[0] = lastTick[1] = -9;
	}

	/** One reading of a side's animation on a tick, with the distance between the two. */
	public void observe(Side side, int tick, int anim, int dist)
	{
		observe(side, tick, anim, dist, null);
	}

	/** The same, with the name of the weapon in that side's hands. */
	public void observe(Side side, int tick, int anim, int dist, String weapon)
	{
		int i = side.ordinal();
		boolean isNew = anim >= 0 && (anim != lastAnim[i] || tick - lastTick[i] > 1);
		lastAnim[i] = anim;
		lastTick[i] = tick;
		if (isNew)
		{
			starts.add(new Start(side, tick, anim, dist, weapon));
			while (starts.size() > KEEP)
			{
				starts.remove(0);
			}
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

	/** Who has PID at tick now. Also teaches the book from attacks that have finished. */
	public Holder holderAt(int now)
	{
		List<Holder> recent = new ArrayList<>();
		for (Start s : starts)
		{
			if (s.tick > now)
			{
				continue;
			}
			int g = gap(s);
			if (!s.learned && (g >= 0 || now - s.tick > MAX_GAP))
			{
				s.learned = true;
				if (g >= 0)
				{
					book.learn(s.anim, s.weapon, s.dist, g);
				}
			}
			if (g < 0 || now - s.tick > WINDOW || s.dist < 0)
			{
				continue;
			}
			AttackBook.Kind k = book.kindOf(s.anim);
			if (k == null)
			{
				continue;
			}
			int r = g - k.delay(s.dist);
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
