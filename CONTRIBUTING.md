# Contributing to PID Indicator

Thank you for helping. Here's everything you need.

## Build, test and run

```
./gradlew build    # builds the jar into build/libs and runs the tests
./gradlew test     # the tests alone
./gradlew run      # starts RuneLite in developer mode with the plugin loaded
```

You need Java 11 or newer. To log in with a Jagex account in the developer client, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) guide.

## How it fits together

| File | What it does |
|---|---|
| `PidIndicatorPlugin.java` | Each tick, reads both fighters' animations and the weapon in their hands, and every hitsplat, and hands them to the tracker. |
| `WeaponCategory.java` | Reads a weapon's category from the game's own item data and gives the hit delay that follows from it. Plain Java. |
| `PidTracker.java` | Pairs each attack with that fighter's next hit, and works out who went first. Plain Java. |
| `PidIndicatorOverlay.java` | Draws the diamonds. |

## The one idea behind it

Every attack has its own hit delay, set by the weapon in the attacker's hands (OSRS Wiki, [Hit delay](https://oldschool.runescape.wiki/w/Hit_delay)). When the defender was processed first in the tick, the hit lands one tick late. So an attack and its hit tell you who went first.

The weapon's kind comes from the game itself. Every item in the game's data carries a category, the same number the combat interface uses to choose a weapon's attack styles. Crossbows are 37, bows 64 and 106, thrown weapons 24, chinchompas 572, ballistae 567, staves 1, and each kind of melee weapon has its own. `WeaponCategory` reads that number from the item config and maps it to a delay. A weapon released tomorrow carries its category from the day it comes out, so there is no list to keep.

## Adding a rule

If a new kind of weapon arrives with a hit delay of its own, add its category to `WeaponCategory.kindOf` and its delay to `WeaponCategory.delay`, with a test in `PidTrackerTest`. The categories are in the game's item data, and RuneLite's `net.runelite.cache` library reads them from your own cache.

## Tests

`src/test/java/com/drthomasager/pid/PidTrackerTest.java` checks the tracker, the delays and the config reader on their own. A new rule or a fix comes with a test that fails without it.

## Style

Tabs and braces on their own lines, like RuneLite itself. No reflection, no network calls, and nothing heavy in the overlay, which runs every frame.
