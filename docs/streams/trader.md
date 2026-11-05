# Stream A: wandering trader

Branch `v14-trader`. Package `com.exiledradio.villagerbackport.trader`, entity ids 105 (trader) and
106 (trader llama), no packets (ids 10-11 left unused), config category `trader`.

## Changelog section

## New: the wandering trader

*   **The wandering trader** turns up now and then with two **trader llamas** on leads. Once a day the game tries to send one to a random player in the overworld: a 25% chance the first time, 50% the next, then 75% until one comes, each with a further one in ten roll, as in 1.14 and current Java. It arrives within 48 blocks of the player, or of the nearest **bell** within 48 blocks of them, walks there, and stays within 16 blocks of it.
*   **Current Java's trades**: nine per trader. Two things it buys (hay bales, water bottles, baked potatoes, milk, water buckets, fermented spider eyes), two specials (packed ice, gunpowder, logs, podzol, a potion of invisibility, an enchanted iron pickaxe, and blue ice when a mod adds it) and five everyday goods: flowers, seeds, saplings, dyes, mushrooms, vines, lily pads, sand, glowstone, cactus, slime balls, pumpkins and name tags. Kelp, sea pickles, coral blocks, fish buckets, cornflowers, lilies of the valley and nautilus shells join in when Oceanic Expanse, Future MC or this mod's flowers add them.
*   **No levels and no restocking**: what it has is all it has. It trades on the 1.14-style trade screen, with no level or experience bar, and drops experience orbs as villagers do.
*   **Invisible at night**: at dusk it drinks a potion of invisibility, and at dawn a bucket of milk, with 1.14's sounds.
*   **It leaves after 40 minutes**, counted only while it's loaded and nobody is trading with it. Its llamas go with it. Traders from spawn eggs never leave.
*   It flees zombies, pillagers, vindicators, evokers, illusioners and vexes, and panics when hit. Zombies, pillagers, vindicators, evokers, illusioners and ravagers go after it as they go after villagers.
*   **Trader llamas** wear the trader's blue and gold decor (a carpet still replaces it), can't be ridden while the trader holds the lead, and spit at whatever attacks the trader. Take one with your own lead and it's yours to keep.
*   1.14's models, textures and sounds, and spawn eggs for both.
*   `trader.enabled` turns the whole thing off. `trader.naturalSpawning`, `spawnDelay`, the three chance settings, `spawnRadius`, `despawnTicks`, `llamas` and `oneAtATime` (no second trader while the last may still be around) tune it, and the **`doTraderSpawning` game rule** switches it off per world, as in current Java. `/wanderingtrader status`, `spawn` and `timer` for operators (plus `awake` and `selftest` for testing on a server with nobody on it).

## Shared files touched

- `ModConfig.java`: only inside `Trader` (fields after `enabled`).
- `VillagerBackport.java`: one guarded line registering `TraderCommand` in `serverStarted`.
- `en_us.lang`: a `# --- trader ---` block at the end.
- `sounds.json`: ten `entity.wandering_trader.*` events appended at the end (registered as
  `villagerbackport:entity.wandering_trader.*` from `TraderContent`, not `ModSounds`).
- Nothing in `ClientSetup`: renderers register from `trader/client/TraderClient`, a client-only
  `@Mod.EventBusSubscriber` on `ModelRegistryEvent`.

## For the integrator

- Item names guessed for other streams, looked up by name and simply absent if wrong:
  `villagerbackport:cornflower`, `villagerbackport:lily_of_the_valley` (stream F) and
  `villagerbackport:white_dye` / `black_dye` / `brown_dye` / `blue_dye` (stream F, falling back to bone
  meal, ink sac, cocoa beans and lapis). If stream F named them differently, change the strings in
  `TraderTrades`.
- Future MC 0.2.21 has no wandering trader, so there is no step-aside mode; its cornflower, lily of
  the valley, fish buckets, nautilus shell and blue ice are used as trade items when present.
- Not added to the JEI trade lists (`compat/JeiTradeLists`); `TraderTrades.examples(Random)` returns
  one example of every available trade if you want it there.
- The `villager_trade` advancement trigger is not fired for trader trades: 1.12.2's trigger takes an
  `EntityVillager`.

## Tested headlessly

Dedicated dev server (`runServer`, port 25701, superflat world with villages), driven from the
console, over five boots of the same world. `/wanderingtrader selftest` passed every step:

- **Spawn**: trader placed within 48 blocks, heading for a village bell rather than the requested
  position (the bell preference working), despawn clock at 48000, **2 trader llamas leashed** to it.
- **Trades**: 9 offers every time (2 bought, 2 special, 5 ordinary), different per trader.
- **Trading** through a real `EntityPlayerMP` (on a connection to nowhere, never in the world):
  right-click opens a `ContainerMerchant` with the trader as merchant and the player as customer; a
  sale bought until stock ran out (6 of 6, 4 of 4) and the trade crossed out with no restock; a
  buying trade (milk, fermented spider eye, water bucket) paid emeralds; the despawn clock does not
  move while trading; closing the screen releases the customer.
- **Llamas defend**: hurting the trader as a zombie makes both llamas target the zombie. (This caught
  a bug: a hit on the trader's first tick was ignored. Fixed.)
- **Hunters**: zombies and pillagers joining the world get a target task for the trader.

Watched over time with `/wanderingtrader status` (and `awake`, which keeps mobs ticking with no
player on):

- Walks toward its target; drinks an invisibility potion at night (`holding=Potion of Invisibility`,
  then `invisible=true`) and milk by day (`invisible=false`).
- A summoned pillager, then a zombie, both targeted it, and it fled each of them (60 blocks).
- Setting `DespawnDelay` to 40: trader and both llamas gone, no leads or other items dropped.
- Spawner: the countdown drops by 1200 a minute and survives restarts; a failed roll raised the
  chance 25 to 50% and it stayed at 50% across a restart; an attempt with no players online reset
  it to 25%, as 1.14 does. `doTraderSpawning` is added to the world as `true`.
- Offers, uses, the despawn clock, the wander target and the 16-block home all survive a restart
  (the home needed saving by hand; 1.12.2 does not save it).
- Dedicated server boots with no client classes loaded; build clean, jar reobfuscated (SRG names).

## Not tested (client side, needs the in-game check)

- The trader's model, hood and brim, and texture; the potion or milk held in front of its arms while
  drinking; it actually turning invisible on screen.
- Trader llama decor over each of the four coats, and a carpet replacing it; the leads drawn
  between llamas and trader; spit visuals.
- The trade screen opening on the 1.14-style screen with the title "Wandering Trader" and no
  experience bar; crossed-out trades after use.
- Sounds and subtitles (ambient, trading haggle, yes/no, drinking, disappearing/reappearing, hurt,
  death); spawn egg colors and names.
- A natural spawn next to a real player (the selftest runs the same placement code against a
  position, since a dedicated server with nobody on it has no player to pick).
