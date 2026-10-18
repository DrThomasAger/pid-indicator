package com.drthomasager.pid;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PidTrackerTest
{
	private static final int WHIP = 1658;

	@Test
	public void nothingSeenIsUnknown()
	{
		assertEquals(PidTracker.Holder.UNKNOWN, new PidTracker().holderAt(100));
	}

	@Test
	public void myHitLandingOnMySwingTickMeansIHavePid()
	{
		PidTracker t = new PidTracker();
		t.observe(PidTracker.Side.ME, 10, WHIP, 1);
		t.hit(PidTracker.Side.ME, 10);
		assertEquals(PidTracker.Holder.ME, t.holderAt(11));
	}

	@Test
	public void myHitLandingATickLateMeansTheyHavePid()
	{
		PidTracker t = new PidTracker();
		t.observe(PidTracker.Side.ME, 10, WHIP, 1);
		t.hit(PidTracker.Side.ME, 11);
		assertEquals(PidTracker.Holder.THEM, t.holderAt(12));
	}

	@Test
	public void theirHitLandingOnTheirSwingTickMeansTheyHavePid()
	{
		PidTracker t = new PidTracker();
		t.observe(PidTracker.Side.THEM, 20, WHIP, 1);
		t.hit(PidTracker.Side.THEM, 20);
		assertEquals(PidTracker.Holder.THEM, t.holderAt(21));
	}

	@Test
	public void evidenceOlderThanFortyTicksIsForgotten()
	{
		PidTracker t = new PidTracker();
		t.observe(PidTracker.Side.ME, 10, WHIP, 1);
		t.hit(PidTracker.Side.ME, 10);
		assertEquals(PidTracker.Holder.UNKNOWN, t.holderAt(51));
	}

	@Test
	public void theMajorityOfTheLastThreeWins()
	{
		PidTracker t = new PidTracker();
		int[][] swings = {{10, 10}, {16, 17}, {22, 22}};
		for (int[] s : swings)
		{
			t.observe(PidTracker.Side.ME, s[0] - 1, -1, 1);
			t.observe(PidTracker.Side.ME, s[0], WHIP, 1);
			t.hit(PidTracker.Side.ME, s[1]);
		}
		assertEquals(PidTracker.Holder.ME, t.holderAt(23));
	}

	@Test
	public void crossbowOnItsFormulaTickMeansTheShooterHasPid()
	{
		PidTracker t = new PidTracker();
		// at a distance of 4 the bolt takes 1 + (3 + 4) / 6 = 2 ticks
		t.observe(PidTracker.Side.ME, 30, PidTracker.CROSSBOW_ANIM, 4);
		t.observe(PidTracker.Side.ME, 32, -1, 4);
		t.hit(PidTracker.Side.ME, 32);
		assertEquals(PidTracker.Holder.ME, t.holderAt(33));
	}

	@Test
	public void crossbowOneTickLateMeansTheOtherHasPid()
	{
		PidTracker t = new PidTracker();
		t.observe(PidTracker.Side.ME, 30, PidTracker.CROSSBOW_ANIM, 4);
		t.observe(PidTracker.Side.ME, 33, -1, 4);
		t.hit(PidTracker.Side.ME, 33);
		assertEquals(PidTracker.Holder.THEM, t.holderAt(34));
	}

	@Test
	public void aBarrageOnItsDelayMeansTheCasterHasPid()
	{
		PidTracker t = new PidTracker();
		// at a distance of 5 a spell takes 1 + (1 + 5) / 3 = 3 ticks
		t.observe(PidTracker.Side.THEM, 40, 1979, 5);
		t.observe(PidTracker.Side.THEM, 43, -1, 5);
		t.hit(PidTracker.Side.THEM, 43);
		assertEquals(PidTracker.Holder.THEM, t.holderAt(44));
	}

	@Test
	public void aThrownHitATickLateMeansTheTargetHasPid()
	{
		PidTracker t = new PidTracker();
		// at a distance of 3 a thrown weapon takes 1 + 3 / 6 = 1 tick
		t.observe(PidTracker.Side.ME, 50, 929, 3);
		t.observe(PidTracker.Side.ME, 52, -1, 3);
		t.hit(PidTracker.Side.ME, 52);
		assertEquals(PidTracker.Holder.THEM, t.holderAt(53));
	}

	@Test
	public void anAnimationItDoesNotKnowSaysNothing()
	{
		PidTracker t = new PidTracker();
		// 829 is eating
		t.observe(PidTracker.Side.ME, 60, 829, 1);
		t.hit(PidTracker.Side.ME, 60);
		assertEquals(PidTracker.Holder.UNKNOWN, t.holderAt(61));
	}
}
