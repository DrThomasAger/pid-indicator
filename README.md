# PID Indicator

A RuneLite plugin that shows who has PID in your fight.

![The four states of the PID diamond](docs/diamonds.png)

A diamond floats over your head and over your opponent's. The one with PID gets a gold diamond with a crown. The one without gets a grey diamond with a cross, and both stay grey until the plugin has seen enough to know. Under each diamond is the number of ticks until PID can be redrawn.

## How it knows

Old School RuneScape processes players in an order each tick, and the player processed first has PID. When both of you attack on the same tick, PID decides whose hit lands first.

A melee hit lands either on the tick its swing begins or one tick later. The fighter whose hit lands on the same tick as their swing was processed first that tick, so they have PID. Across 147 recorded Last Man Standing fights, whenever one fighter's hit landed on the swing tick, the other's landed a tick later (245 nearby pairs, 2 exceptions).

Crossbow shots count too. A bolt takes `1 + (3 + distance) / 6` ticks to land (rounded down). Landing on exactly that tick means the shooter was processed first, and landing one tick later means the other fighter was.

The plugin takes the side named by most of the last three readings within 40 ticks. With no reading for 40 ticks it goes back to unknown, since 40 ticks is the soonest PID can be redrawn. Scored over the recorded fights, it names the holder correctly on 88 percent of the next melee readings, and knows the holder on 90 percent of fight ticks.

## The swap ticks

According to the [OSRS Wiki](https://oldschool.runescape.wiki/w/PID), PID is redrawn at random every 100 to 150 ticks, or every 40 to 60 on PvP worlds. The number under each diamond counts down to that window from the last time the plugin saw PID change hands.

- `swap 44-94t` means the earliest and latest ticks until the next redraw.
- `swap now-30t` means the window is open now and closes within 30 ticks.
- `swap any tick` means the window has passed.
- `PID ? hit to learn` means nothing has been read yet, so land a hit.

## Settings

- **Show outside fights.** Keeps your own diamond over your head in PvP places (the Wilderness, PvP, Last Man Standing, high risk and Deadman worlds) even when you aren't fighting. On by default.
- **Show swap ticks.** Shows the tick counts under the diamonds. On by default.

## What it reads

The plugin only reads what your own client already shows you. That means your own and your opponent's attack animations, the hitsplats on you and the ones you deal, and the distance between you. It sends nothing anywhere and changes nothing in the game.

## Building

```
./gradlew build
```

The tests in `src/test` check the tracker on its own. `./gradlew run` starts RuneLite in developer mode with the plugin loaded.

## Where it comes from

The plugin was written by Dr. Thomas Ager for playing Last Man Standing. It started as part of a set of aids written in Clojure and running inside RuneLite, where it was measured against every recorded fight. This Java version names the same holder as that original on 99.9 percent of 49,334 fight ticks.

## Licence

BSD 2-Clause, the same licence as RuneLite. See [LICENSE](LICENSE).
