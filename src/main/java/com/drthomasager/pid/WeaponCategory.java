package com.drthomasager.pid;

import java.util.HashMap;
import java.util.Map;

/**
 * Each weapon's category, read from the game's own item data, and the hit
 * delay rule that follows from it.
 *
 * <p>The game keeps every item's category in its item config (opcode 94),
 * the same number the combat styles interface uses to decide which attack
 * styles a weapon offers. Crossbows are 37, bows 64 and 106, thrown weapons
 * 24, chinchompas 572, ballistae 567, staves 1, and every melee weapon has a
 * category of its own. A weapon released tomorrow carries its category from
 * the day it comes out, so nothing here needs updating for it.
 *
 * <p>The OSRS Wiki's Hit delay page gives each kind its delay. Melee lands
 * on the tick. Bows and crossbows take 1 + (3 + distance) / 6 ticks. Thrown
 * weapons, chinchompas and the blowpipe take 1 + distance / 6. Ballistae
 * take 2 up to four tiles and 3 beyond. Spells take 1 + (1 + distance) / 3.
 * When the defender was processed first in the tick, the hit lands one tick
 * later than that, which is what the tracker reads PID from.
 */
public final class WeaponCategory
{
	public enum Kind { MELEE, BOW, THROWN, BALLISTA, MAGIC }

	/** The item config archive inside the config index. */
	static final int ITEM_ARCHIVE = 10;
	/** The opcode that carries the category. */
	static final int CATEGORY_OPCODE = 94;

	static final int CAT_STAFF = 1;
	static final int CAT_THROWN = 24;
	static final int CAT_CROSSBOW = 37;
	static final int CAT_BOW = 64;
	static final int CAT_CRYSTAL_BOW = 106;
	static final int CAT_BALLISTA = 567;
	static final int CAT_CHINCHOMPA = 572;
	static final int CAT_SALAMANDER = 586;
	static final int CAT_NATURES_REPRISAL = 975;

	private WeaponCategory()
	{
	}

	/** The kind a weapon category attacks as, taking a staff as magic. */
	public static Kind kindOf(int category)
	{
		switch (category)
		{
			case CAT_CROSSBOW:
			case CAT_BOW:
			case CAT_CRYSTAL_BOW:
				return Kind.BOW;
			case CAT_THROWN:
			case CAT_CHINCHOMPA:
			case CAT_SALAMANDER:
				return Kind.THROWN;
			case CAT_BALLISTA:
				return Kind.BALLISTA;
			case CAT_STAFF:
			case CAT_NATURES_REPRISAL:
				return Kind.MAGIC;
			default:
				return Kind.MELEE;
		}
	}

	/** The hit's delay in ticks at a distance, when the attacker goes first. */
	public static int delay(Kind k, int distance)
	{
		switch (k)
		{
			case BOW:
				return 1 + (3 + distance) / 6;
			case THROWN:
				return 1 + distance / 6;
			case BALLISTA:
				return distance <= 4 ? 2 : 3;
			case MAGIC:
				return 1 + (1 + distance) / 3;
			default:
				return 0;
		}
	}

	/**
	 * The category in an item config, 0 when the config carries none (as the
	 * game itself reads it), or -1 when the config cannot be read. Walks the config's opcodes the way the game writes them
	 * (net.runelite.cache ItemLoader) until it meets opcode 94.
	 */
	public static int categoryOf(byte[] config)
	{
		if (config == null)
		{
			return -1;
		}
		Buf b = new Buf(config);
		int category = 0;
		try
		{
			while (b.pos < config.length)
			{
				int op = b.u8();
				if (op == 0)
				{
					return category;
				}
				if (op == CATEGORY_OPCODE)
				{
					// read to the end, since a config can carry it twice and the
					// game keeps the last (the boxing gloves, 0 then 65)
					category = b.u16();
				}
				else if (!skip(op, b))
				{
					return -1;
				}
			}
		}
		catch (IndexOutOfBoundsException e)
		{
			return -1;
		}
		return category;
	}

	/** Steps over one opcode's values. False on an opcode it does not know. */
	private static boolean skip(int op, Buf b)
	{
		if (op >= 30 && op < 40)
		{
			b.str();
			return true;
		}
		if (op >= 100 && op < 110)
		{
			b.pos += 4;
			return true;
		}
		switch (op)
		{
			case 1: case 4: case 5: case 6: case 7: case 8: case 24: case 26:
			case 78: case 79: case 90: case 91: case 92: case 93: case 95: case 97: case 98: case 99:
			case 110: case 111: case 112: case 139: case 140: case 148: case 149:
			case 75:
				b.pos += 2;
				return true;
			case 2: case 3: case 9:
				b.str();
				return true;
			case 13: case 14: case 27: case 42: case 113: case 114: case 115:
				b.pos += 1;
				return true;
			case 11: case 15: case 16: case 65: case 160: case 251:
				return true;
			case 12: case 44: case 46: case 47: case 49: case 50: case 51: case 52: case 53: case 54:
				b.pos += 4;
				return true;
			case 23: case 25:
				b.pos += 3;
				return true;
			case 40: case 41:
			{
				int n = b.u8();
				b.pos += 4 * n;
				return true;
			}
			case 43:
			{
				b.u8();
				while (true)
				{
					int sub = b.u8();
					if (sub == 0)
					{
						return true;
					}
					b.str();
				}
			}
			case 45: case 48:
				b.pos += 5;
				return true;
			case 161:
			{
				int n = b.u16();
				b.pos += 2 * n;
				return true;
			}
			case 200:
				b.pos += 2;
				b.str();
				return true;
			case 201:
				b.pos += 13;
				b.str();
				return true;
			case 202:
				b.pos += 15;
				b.str();
				return true;
			case 249:
			{
				int n = b.u8();
				for (int i = 0; i < n; i++)
				{
					int type = b.u8();
					b.pos += 3;
					if (type == 1)
					{
						b.str();
					}
					else
					{
						b.pos += type == 2 ? 8 : 4;
					}
				}
				return true;
			}
			default:
				return false;
		}
	}

	private static final class Buf
	{
		final byte[] d;
		int pos;

		Buf(byte[] d)
		{
			this.d = d;
		}

		int u8()
		{
			return d[pos++] & 0xff;
		}

		int u16()
		{
			int v = ((d[pos] & 0xff) << 8) | (d[pos + 1] & 0xff);
			pos += 2;
			return v;
		}

		void str()
		{
			while (d[pos++] != 0)
			{
				// to the end of the string
			}
		}
	}

	/** Categories already read, by item id, so each config is parsed once. */
	public static final class Cache
	{
		private final Map<Integer, Integer> seen = new HashMap<>();

		/** The category of an item, reading it through load when not seen before. */
		public int category(int itemId, java.util.function.IntFunction<byte[]> load)
		{
			Integer c = seen.get(itemId);
			if (c == null)
			{
				c = categoryOf(load.apply(itemId));
				seen.put(itemId, c);
			}
			return c;
		}
	}
}
