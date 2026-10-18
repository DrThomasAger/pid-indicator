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
| `PidIndicatorPlugin.java` | Reads each tick's attack animations, weapons and distance, and every hitsplat, and hands them to the tracker. Saves the attack book. |
| `PidTracker.java` | Pairs each attack with that fighter's next hit, and works out who went first. Plain Java, no RuneLite classes. |
| `AttackBook.java` | Learns which animations are attacks and which timing rule each follows. Reads and writes `attacks.txt`. |
| `PidIndicatorOverlay.java` | Draws the diamonds and labels. |
| `src/main/resources/.../attacks.txt` | The attack book the plugin ships with. |

## The one idea behind it

Every attack has its own hit delay (OSRS Wiki, [Hit delay](https://oldschool.runescape.wiki/w/Hit_delay)). When the defender was processed first in the tick, the hit lands one tick late. So an attack and its hit tell you who went first. The attack book works out each weapon's delay from the same attacks and hits.

## Adding or fixing a weapon

Usually there's nothing to do, because the plugin learns weapons by itself. To ship a weapon to everyone:

1. Fight with it, then open `attacks.txt` in the plugin's folder inside `.runelite`.
2. Copy its line into `src/main/resources/com/drthomasager/pid/attacks.txt`.
3. Run `./gradlew test` and open a pull request.

To see a weapon's animation id while fighting, turn on **Show animation ids** in the plugin's settings.

## Tests

`src/test/java/com/drthomasager/pid/PidTrackerTest.java` checks the tracker on its own. A new rule or a fix comes with a test that fails without it.

## Style

Tabs and braces on their own lines, like RuneLite itself. No reflection, no network calls, and nothing heavy in the overlay, which runs every frame.
