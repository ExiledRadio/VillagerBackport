# Stream C: foxes and sweet berries

Branch `v14-foxes`. Package `com.exiledradio.villagerbackport.fox` (client code in `fox/client/`),
entity ID 108. Packet IDs 14-15 are reserved but unused: every fox pose is a synced data flag, as in
1.14, so nothing needs a packet. Config category `foxes`.

## Changelog section

## New: sweet berries

*   **Sweet berry bushes** grow through four stages: a sprig, a bush, a few berries, fully ripe. They grow on random ticks in light 9 or brighter, and bone meal adds a stage.
*   **Pick them, don't break them**: use a bush with a few berries for 1-2 berries, or a ripe one for 2-3, and it goes back to a bare bush that grows again. Breaking a ripe bush drops the same (Fortune adds more); an unripe one drops nothing.
*   **Thorns**: walking through a bush slows you to a crawl, and once it has grown past a sprig every step costs half a heart, with 1.14's scratchy hurt sound and death message. Mobs walk around bushes. Foxes are never slowed or hurt and walk straight through.
*   **Sweet berries** are a quick snack (2 hunger) and the bush's seed: use them on grass, dirt, podzol or farmland to plant one. Composters take them at 30%.
*   **In the world**: bushes grow wild in taigas at 1.14's rates - one patch per chunk in an ordinary taiga, one chunk in twelve in a snowy taiga - and also, rarely, in giant tree taigas, as in current Java. Modded taigas get them too (the Forge biome dictionary's `CONIFEROUS`). New chunks only. Taiga villages plant them in their fields.
*   Butchers buy them at master level, as in 1.14.
*   1.14's textures and sounds, and the ore dictionary names `cropSweetberry` and `listAllberry`.

## New: foxes

*   **Foxes** spawn in taigas in groups of two to four, as in 1.14: red foxes, and white snow foxes in snowy taigas. The third and fourth of a group are cubs. Spawn egg included.
*   **Nocturnal**: by day a fox finds shade and sleeps, curled up with its eyes shut, until something it doesn't trust comes within 12 blocks, night falls, it gets hurt or wet, or a thunderstorm starts. At night it hunts, wanders, and strolls through villages. Its night-time screech carries.
*   **Hunting**: foxes stalk chickens and rabbits, crouch, wiggle, and pounce with a high leap. Pouncing into snow sometimes buries a fox's face in it.
*   **The mouth**: a fox carries one item. One in five spawn holding something (an emerald, an egg, a rabbit's foot or hide, wheat, leather or a feather). It picks up items it walks over, swaps what it holds for food (spitting the old item out), eats the food after a while, and drops whatever it holds when it dies. Foxes also walk to ripe sweet berry bushes and pick them, keeping a berry in the mouth.
*   **Skittish**: foxes run from players who aren't sneaking, from wild wolves and from polar bears. Sneak to get close.
*   **Trust**: breed two foxes with sweet berries and the cub trusts the players who fed its parents. A trusted player doesn't scare it or wake it, and it attacks whatever hurts that player.
*   Foxes take fall damage from 5 blocks rather than 3.
*   1.14's model, textures (awake and asleep, for both kinds) and all eleven sounds.

## Settings (`foxes`)

`enabled` (everything above), `generateBushes`, `taigaPatchesPerChunk` (1), `rarePatchChance` (12, for snowy and giant taigas), `bushBiomeBlacklist`, `spawnWeight` (8), `minGroup` (2), `maxGroup` (4), `extraFoxBiomes`, `foxBiomeBlacklist`.

## With Future MC

Future MC adds sweet berries and bushes too (and, in some versions, a fox). `compat.futureMcDuplicates` - the same setting as for the barrels and looms - decides whose are used:

*   `KEEP_OURS` (default): this mod's bushes generate and its foxes spawn; Future MC's berries are hidden from JEI and the creative menu, and its fox, if it has one, no longer spawns naturally. **Future MC's own bushes keep generating** - turn them off in its config (`berry bush` > `Spawn Rate` 0) to have only these.
*   `KEEP_FUTURE_MC`: this mod's bushes and foxes stay registered (so nothing vanishes from a world) but don't generate or spawn, and its berries are hidden. Trades and villages use Future MC's berries.
*   `SHOW_BOTH`: both.

Foxes eat, breed on and pick either mod's berries in every mode.

## Shared files touched

*   `ModConfig.java`: only inside `Foxes`.
*   `VillagerBackport.init`: one line, `FoxContent.init()` (ore names, composter, spawns, world generator).
*   `compat/FutureMc.hiddenItems()`: one line adding `FoxContent.hiddenItems()` (berries have no block item, so they aren't in the block pairs).
*   `block/Compostables`: a public `register(chance, item)` for content registered after the table is built.
*   `trade/ModernTrades`: the butcher's master trade uses `FoxContent.berries()` instead of only `futuremc:sweet_berries`.
*   `village/modern/VillageFeatures`: taiga village berry fields use `FoxContent.berryBush()` instead of only Future MC's.
*   `en_us.lang` (block `# --- foxes ---`), `sounds.json` (14 events appended at the end).

## Tested headlessly

A temporary self-test (removed before committing) on a dedicated dev server (`runServer`, port 25703), in a single-biome taiga world. Passed, 38 of 38:

*   Registration; butcher trades resolve to this mod's berries; composter chance 0.3.
*   Spawn lists: taiga and giant taiga have foxes, plains doesn't; snowy taiga is classed as snow-fox country.
*   World generation: 79 bushes (all ripe) in 289 chunks around spawn; 10 to 30 foxes spawned with the world across runs, about one in five holding an item.
*   Bush: random-tick growth to ripe, bone meal adds one stage, picking a ripe bush gives 2-3 and resets to stage 1, a stage-1 bush can't be picked, drops when broken (ripe 2-3, unripe none), berries plant on grass and not on stone, path node type (mobs avoid, foxes don't).
*   Thorns: a pig pushed through a bush is hurt; a fox pushed through one is not.
*   NBT: type, trusted UUIDs and sleeping survive a save and load.
*   Fox: picks up a dropped item, swaps a non-food item for food and spits the old one out, eats the food; walks to a ripe bush, picks it and keeps a berry; stalks, crouches, pounces on and kills a chicken; flees an untamed wolf; with a trusted (villager) target hurt by a zombie, attacks the zombie and enters the defending state; two foxes in love have a cub of a parent's type; sleeps by day under a roof, wakes at night.
*   The server boots and runs with the client classes present in the jar (no client type reaches a server class).

## Not tested (needs a client, or the integrator)

*   Everything visual: the model and its poses (sitting, sleeping, crouching wiggle, pounce tilt, face-plant), the held-item placement for adults and cubs, textures, the bush and berry models, eating crumbs, sounds and subtitles.
*   The entity renderer is registered from `ModelRegistryEvent` (`FoxClient`); check a fox renders and isn't the white box.
*   The thorns slowing a real player (it is client-side movement) and the player's bush hurt sound.
*   Trust from a real breeding by a player (the headless test had no real `EntityPlayerMP`, so the cub's trusted UUIDs from love causes are untested; trust itself is tested with a villager).
*   A face-plant was not observed in the test run (it needs a pounce to miss and land in snow).
*   Future MC side by side (it crashes the FG2 dev env): duplicate hiding, `KEEP_FUTURE_MC`, picking Future MC's bushes.
*   Snow foxes in a real snowy taiga (only the biome classification was tested), modded taiga biomes.
*   Night-time village strolling.
