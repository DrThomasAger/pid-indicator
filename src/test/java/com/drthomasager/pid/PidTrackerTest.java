package com.drthomasager.pid;

import static org.junit.Assert.assertEquals;
import com.drthomasager.pid.PidTracker.Holder;
import com.drthomasager.pid.PidTracker.Side;
import com.drthomasager.pid.WeaponCategory.Kind;
import org.junit.Test;

public class PidTrackerTest
{
	@Test
	public void nothingSeenIsUnknown()
	{
		assertEquals(Holder.UNKNOWN, new PidTracker().holderAt(100));
	}

	@Test
	public void myMeleeHitLandingOnMySwingTickMeansIHavePid()
	{
		PidTracker t = new PidTracker();
		t.attack(Side.ME, 10, Kind.MELEE, 1);
		t.hit(Side.ME, 10);
		assertEquals(Holder.ME, t.holderAt(11));
	}

	@Test
	public void myMeleeHitLandingATickLateMeansTheyHavePid()
	{
		PidTracker t = new PidTracker();
		t.attack(Side.ME, 10, Kind.MELEE, 1);
		t.hit(Side.ME, 11);
		assertEquals(Holder.THEM, t.holderAt(12));
	}

	@Test
	public void theirMeleeHitLandingOnTheirSwingTickMeansTheyHavePid()
	{
		PidTracker t = new PidTracker();
		t.attack(Side.THEM, 20, Kind.MELEE, 1);
		t.hit(Side.THEM, 20);
		assertEquals(Holder.THEM, t.holderAt(21));
	}

	@Test
	public void evidenceOlderThanFortyTicksIsForgotten()
	{
		PidTracker t = new PidTracker();
		t.attack(Side.ME, 10, Kind.MELEE, 1);
		t.hit(Side.ME, 10);
		assertEquals(Holder.UNKNOWN, t.holderAt(51));
	}

	@Test
	public void theMajorityOfTheLastThreeWins()
	{
		PidTracker t = new PidTracker();
		int[][] swings = {{10, 10}, {16, 17}, {22, 22}};
		for (int[] s : swings)
		{
			t.attack(Side.ME, s[0], Kind.MELEE, 1);
			t.hit(Side.ME, s[1]);
		}
		assertEquals(Holder.ME, t.holderAt(23));
	}

	@Test
	public void aCrossbowBoltOnItsDelayMeansTheShooterHasPid()
	{
		PidTracker t = new PidTracker();
		// at a distance of 4 a bolt takes 1 + (3 + 4) / 6 = 2 ticks
		t.attack(Side.ME, 30, Kind.BOW, 4);
		t.hit(Side.ME, 32);
		assertEquals(Holder.ME, t.holderAt(33));
	}

	@Test
	public void aCrossbowBoltOneTickLateMeansTheOtherHasPid()
	{
		PidTracker t = new PidTracker();
		t.attack(Side.ME, 30, Kind.BOW, 4);
		t.hit(Side.ME, 33);
		assertEquals(Holder.THEM, t.holderAt(34));
	}

	@Test
	public void aSpellOnItsDelayMeansTheCasterHasPid()
	{
		PidTracker t = new PidTracker();
		// at a distance of 5 a spell takes 1 + (1 + 5) / 3 = 3 ticks
		t.attack(Side.THEM, 40, Kind.MAGIC, 5);
		t.hit(Side.THEM, 43);
		assertEquals(Holder.THEM, t.holderAt(44));
	}

	@Test
	public void aThrownHitATickLateMeansTheTargetHasPid()
	{
		PidTracker t = new PidTracker();
		// at a distance of 3 a knife takes 1 + 3 / 6 = 1 tick
		t.attack(Side.ME, 50, Kind.THROWN, 3);
		t.hit(Side.ME, 52);
		assertEquals(Holder.THEM, t.holderAt(53));
	}

	@Test
	public void aHitFarOutsideAnyDelaySaysNothing()
	{
		PidTracker t = new PidTracker();
		t.attack(Side.ME, 60, Kind.MELEE, 1);
		t.hit(Side.ME, 64);
		assertEquals(Holder.UNKNOWN, t.holderAt(65));
	}

	@Test
	public void theHitDelaysMatchTheWiki()
	{
		assertEquals(0, WeaponCategory.delay(Kind.MELEE, 1));
		assertEquals(1, WeaponCategory.delay(Kind.BOW, 1));
		assertEquals(2, WeaponCategory.delay(Kind.BOW, 3));
		assertEquals(1, WeaponCategory.delay(Kind.THROWN, 5));
		assertEquals(2, WeaponCategory.delay(Kind.THROWN, 6));
		assertEquals(2, WeaponCategory.delay(Kind.BALLISTA, 4));
		assertEquals(3, WeaponCategory.delay(Kind.BALLISTA, 5));
		assertEquals(1, WeaponCategory.delay(Kind.MAGIC, 1));
		assertEquals(2, WeaponCategory.delay(Kind.MAGIC, 2));
	}

	@Test
	public void categoriesMapToTheirKinds()
	{
		assertEquals(Kind.BOW, WeaponCategory.kindOf(37));
		assertEquals(Kind.BOW, WeaponCategory.kindOf(64));
		assertEquals(Kind.THROWN, WeaponCategory.kindOf(24));
		assertEquals(Kind.THROWN, WeaponCategory.kindOf(572));
		assertEquals(Kind.BALLISTA, WeaponCategory.kindOf(567));
		assertEquals(Kind.MAGIC, WeaponCategory.kindOf(1));
		assertEquals(Kind.MELEE, WeaponCategory.kindOf(150));
		assertEquals(Kind.MELEE, WeaponCategory.kindOf(-1));
	}

	@Test
	public void theCategoryIsReadFromAnItemConfig()
	{
		// name, then category 37, then the end
		byte[] config = {2, 'L', 'o', 't', 'u', 's', 0, 94, 0, 37, 0};
		assertEquals(37, WeaponCategory.categoryOf(config));
		byte[] none = {2, 'L', 'o', 't', 'u', 's', 0, 0};
		assertEquals(0, WeaponCategory.categoryOf(none));
		assertEquals(-1, WeaponCategory.categoryOf(null));
	}
}
