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
		t.observe(PidTracker.Side.ME, 30, 4230, 4);
		t.observe(PidTracker.Side.ME, 32, -1, 4);
		t.hit(PidTracker.Side.ME, 32);
		assertEquals(PidTracker.Holder.ME, t.holderAt(33));
	}

	@Test
	public void crossbowOneTickLateMeansTheOtherHasPid()
	{
		PidTracker t = new PidTracker();
		t.observe(PidTracker.Side.ME, 30, 4230, 4);
		t.observe(PidTracker.Side.ME, 33, -1, 4);
		t.hit(PidTracker.Side.ME, 33);
		assertEquals(PidTracker.Holder.THEM, t.holderAt(34));
	}

	@Test
	public void aSpellOnItsDelayMeansTheCasterHasPid()
	{
		PidTracker t = new PidTracker();
		// at a distance of 5 a spell takes 1 + (1 + 5) / 3 = 3 ticks
		t.observe(PidTracker.Side.THEM, 40, 7855, 5);
		t.observe(PidTracker.Side.THEM, 43, -1, 5);
		t.hit(PidTracker.Side.THEM, 43);
		assertEquals(PidTracker.Holder.THEM, t.holderAt(44));
	}

	@Test
	public void aJavelinHitATickLateMeansTheTargetHasPid()
	{
		PidTracker t = new PidTracker();
		// a javelin takes 1 tick
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

	@Test
	public void aWeaponNobodyListedIsLearnedFromItsHits()
	{
		AttackBook book = new AttackBook();
		PidTracker t = new PidTracker(book);
		int newWeapon = 55555;
		int tick = 100;
		for (int i = 0; i < AttackBook.MIN_SEEN; i++)
		{
			t.observe(PidTracker.Side.ME, tick, newWeapon, 1, "Lotus blade");
			t.hit(PidTracker.Side.ME, tick + (i % 2));
			t.observe(PidTracker.Side.ME, tick + 1, -1, 1);
			tick += 4;
			t.holderAt(tick);
		}
		assertEquals(AttackBook.Kind.MELEE, book.kindOf(newWeapon));
	}

	@Test
	public void eatingNeverSettlesAsAnAttack()
	{
		AttackBook book = new AttackBook();
		PidTracker t = new PidTracker(book);
		int[] gaps = {3, 0, 5, 2, 4, 1, 6, 3, 5, 2};
		int tick = 200;
		for (int g : gaps)
		{
			t.observe(PidTracker.Side.ME, tick, 829, 1);
			t.hit(PidTracker.Side.ME, tick + g);
			t.observe(PidTracker.Side.ME, tick + 1, -1, 1);
			tick += 8;
			t.holderAt(tick);
		}
		assertEquals(null, book.kindOf(829));
	}

	@Test
	public void theBookReadsBackWhatItWrites() throws Exception
	{
		AttackBook book = AttackBook.bundled();
		AttackBook again = new AttackBook();
		again.read(new java.io.StringReader(book.write()));
		assertEquals(AttackBook.Kind.MELEE, again.kindOf(1658));
		assertEquals(AttackBook.Kind.BOW, again.kindOf(4230));
	}
}
