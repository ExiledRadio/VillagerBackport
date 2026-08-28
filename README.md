# 1.14 Villager Backport

Brings 1.14's villages to Minecraft 1.12.2. Villagers take jobs from workstations, restock twice a
day, price their trades by supply and demand, and remember who you are. They claim beds, sleep at
night, breed on free beds rather than doors, and call an iron golem when frightened.

Ported from decompiled 1.14.4 sources, not from memory — the pricing formula, the restock rules, the
gossip weights and decay rates, the golem spawn conditions and the sleep pose are the ones 1.14 uses.

## Building

```
gradlew build
```

Output lands in `build/libs`. Requires JDK 8.

`forge_version` in `gradle.properties` **must stay at 14.23.5.2847** — it is the last 1.12.2 Forge
build that publishes a `-userdev.jar`, which is the only format the ForgeGradle 2.3 fork here can
consume. The mod runs fine on later Forge builds; only the build toolchain is pinned.

Set `deployDir` in `~/.gradle/gradle.properties` to have `gradlew deployToInstance` copy the jar
straight into a test instance.

## What is in it

* **Twelve workstation blocks** — lectern, barrel, composter, cartography table, fletching table,
  smithing table, loom, blast furnace, smoker, grindstone, stonecutter, bell. All functional, not
  decorative.
* **Employment** — villagers claim a nearby unclaimed workstation they can path to, take its
  profession, work at it during working hours, and lose the job if the block goes.
* **Restocking** — twice a day, at the workstation, on 1.14's timing rules.
* **Supply and demand** — prices move with how heavily a trade is used, shown on the trade screen
  with the struck-out original price.
* **Gossip and reputation** — trading, curing, hurting and killing villagers all move a per-player
  score that decays and spreads between villagers.
* **Beds** — claimed as homes, slept in at night, used for breeding and for iron golem eligibility.
* **Iron golems** — spawned on 1.14's rules, by panic or by gossip.
* **1.14 trade screen** — scrollable trade list, level badge, experience bar, spacebar refill.

Server-side where it can be. Clients without the mod can join, but will not see the workstation
blocks or the new trade screen.

## Configuration

`config/villagerbackport.cfg`, eight categories, every setting documented in the file:
`restock`, `pricing`, `jobs`, `workstations`, `villages`, `structures`, `homes`, `display`.

## Licence

MIT. Not affiliated with Mojang.
