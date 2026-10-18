package com.drthomasager.pid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every attack the plugin knows, learned from the game as you fight.
 *
 * <p>The OSRS Wiki's Hit delay page sorts every attack into a few rules for
 * how many ticks its hit takes to land. Whichever rule an attack follows, its
 * hit lands on that delay when the attacker went first in the tick, and one
 * tick later when the defender did. So each time an animation starts and that
 * fighter's next hit lands, the book notes which rules the gap fits. Once one
 * rule fits nearly every hit, the animation counts as an attack of that kind.
 * Eating, blocking and casting Vengeance never settle on a rule, so they never
 * count. A weapon nobody has seen before is learned the same way after a few
 * hits.
 *
 * <p>The book is a plain text file, one attack per line, which anyone can read
 * or edit. It starts from {@code attacks.txt} in the plugin and adds what each
 * player's own fights teach it.
 */
public class AttackBook
{
	/** The hit delay rules, from the OSRS Wiki's Hit delay page. */
	public enum Kind
	{
		MELEE("melee", "0"),
		BOW("bow", "1 + (3 + distance) / 6"),
		THROWN("thrown", "1 + distance / 6"),
		MAGIC("magic", "1 + (1 + distance) / 3"),
		BALLISTA("ballista", "2 up to 4 tiles, 3 beyond"),
		FIXED1("fixed1", "1"),
		FIXED2("fixed2", "2"),
		FIXED3("fixed3", "3");

		final String word;
		final String rule;

		Kind(String word, String rule)
		{
			this.word = word;
			this.rule = rule;
		}

		/** The hit's delay in ticks at a distance, when the attacker goes first. */
		int delay(int distance)
		{
			switch (this)
			{
				case MELEE:
					return 0;
				case BOW:
					return 1 + (3 + distance) / 6;
				case THROWN:
					return 1 + distance / 6;
				case MAGIC:
					return 1 + (1 + distance) / 3;
				case BALLISTA:
					return distance <= 4 ? 2 : 3;
				case FIXED1:
					return 1;
				case FIXED2:
					return 2;
				default:
					return 3;
			}
		}

		static Kind of(String word)
		{
			for (Kind k : values())
			{
				if (k.word.equalsIgnoreCase(word.trim()))
				{
					return k;
				}
			}
			return null;
		}
	}

	/** Hits seen before an animation can settle on a rule. */
	static final int MIN_SEEN = 8;
	/** The share of hits that must fit the rule. */
	static final double MIN_FIT = 0.75;

	static final class Entry
	{
		final int animation;
		String weapon = "unknown";
		int seen;
		final Map<Kind, Integer> fits = new EnumMap<>(Kind.class);
		/** A rule read from the book file, kept until the hits counted here say otherwise. */
		Kind fromFile;
		int fileFit;
		int fileSeen;

		Entry(int animation)
		{
			this.animation = animation;
		}

		/**
		 * The settled rule, or null while it is still being learned or is not
		 * an attack. Hits from the book file count for the rule written there.
		 */
		Kind kind()
		{
			int total = seen + fileSeen;
			if (total < MIN_SEEN)
			{
				return null;
			}
			Kind best = null;
			int bestFit = -1;
			for (Kind k : Kind.values())
			{
				if (fitOf(k) > bestFit)
				{
					best = k;
					bestFit = fitOf(k);
				}
			}
			return bestFit >= MIN_FIT * total ? best : null;
		}

		int fitOf(Kind k)
		{
			int f = fits.getOrDefault(k, 0);
			return k == fromFile ? f + fileFit : f;
		}
	}

	private final Map<Integer, Entry> entries = new LinkedHashMap<>();
	private boolean changed;

	/** The book that ships with the plugin. */
	public static AttackBook bundled()
	{
		AttackBook b = new AttackBook();
		try (InputStream in = AttackBook.class.getResourceAsStream("attacks.txt"))
		{
			if (in != null)
			{
				b.read(new InputStreamReader(in, StandardCharsets.UTF_8));
			}
		}
		catch (IOException ignored)
		{
		}
		b.changed = false;
		return b;
	}

	/** Reads a book file into this one, line by line. */
	public void read(Reader r) throws IOException
	{
		BufferedReader br = new BufferedReader(r);
		String line;
		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#"))
			{
				continue;
			}
			String[] p = line.split("\\|");
			if (p.length < 4)
			{
				continue;
			}
			try
			{
				int anim = Integer.parseInt(p[0].trim());
				Kind k = Kind.of(p[1]);
				String[] fs = p[3].trim().split("/");
				int fit = Integer.parseInt(fs[0].trim());
				int seen = Integer.parseInt(fs[1].trim());
				if (k == null)
				{
					continue;
				}
				Entry e = entries.computeIfAbsent(anim, Entry::new);
				if (e.fromFile == null || seen > e.fileSeen)
				{
					e.fromFile = k;
					e.fileFit = fit;
					e.fileSeen = seen;
				}
				if (p.length > 4 && !p[4].trim().isEmpty())
				{
					e.weapon = p[4].trim();
				}
			}
			catch (NumberFormatException ignored)
			{
			}
		}
	}

	/**
	 * One animation start and the gap until that fighter's next hit landed,
	 * at the distance when the attack started.
	 */
	public void learn(int animation, String weapon, int distance, int gap)
	{
		if (animation < 0 || distance < 0 || gap < 0)
		{
			return;
		}
		Entry e = entries.computeIfAbsent(animation, Entry::new);
		e.seen++;
		if (weapon != null && !weapon.isEmpty() && !"null".equals(weapon))
		{
			e.weapon = weapon;
		}
		for (Kind k : Kind.values())
		{
			int r = gap - k.delay(distance);
			if (r == 0 || r == 1)
			{
				e.fits.merge(k, 1, Integer::sum);
			}
		}
		changed = true;
	}

	/** The rule this animation follows, or null when it is not a known attack. */
	public Kind kindOf(int animation)
	{
		Entry e = entries.get(animation);
		return e == null ? null : e.kind();
	}

	/** True when anything was learned since the book was last written. */
	public boolean changed()
	{
		return changed;
	}

	/** The whole book as text, the settled attacks first. */
	public String write()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("# PID Indicator attack book\n");
		sb.append("#\n");
		sb.append("# One attack per line: animation | kind | hit delay in ticks | hits that fit / hits seen | weapon\n");
		sb.append("# A hit lands on its delay when the attacker went first in the tick, and one tick later\n");
		sb.append("# when the defender did. Lines marked learning have not settled on a rule yet.\n");
		sb.append("#\n");
		for (boolean settled : new boolean[]{true, false})
		{
			for (Entry e : entries.values())
			{
				Kind k = e.kind();
				if ((k != null) != settled)
				{
					continue;
				}
				Kind shown = k;
				if (shown == null)
				{
					int best = -1;
					for (Kind c : Kind.values())
					{
						if (e.fitOf(c) > best)
						{
							best = e.fitOf(c);
							shown = c;
						}
					}
				}
				int total = e.seen + e.fileSeen;
				String rule = shown == null ? "?" : shown.rule;
				sb.append(String.format("%-6d | %-8s | %-26s | %5d/%-5d | %s%s%n",
					e.animation, shown == null ? "?" : shown.word, rule,
					shown == null ? 0 : e.fitOf(shown), total, e.weapon,
					settled ? "" : "  (learning)"));
			}
		}
		changed = false;
		return sb.toString();
	}
}
