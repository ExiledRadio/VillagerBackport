# 2.7.7

*   **Fixed: a crash ("Ticking player", "Stack can not be invalid!") when a mod adds a furnace recipe with an empty input.** The blast furnace and smoker recipe books skip such recipes now.
*   **Lanterns stand on furniture tables**: MrCrayfish's Furniture Mod and other furniture mods' tables, desks and counters hold a lantern, as any block with a full top does. A lantern goes by the shape of the block's top, as current Java does.

Needs Backport Core 1.4.9 or newer.

# 2.7.6

*   **Recipe books for the blast furnace and smoker**, as in 1.14. The blast furnace's has Blocks and Miscellaneous tabs, the smoker's a Food tab. Campfire recipes unlock as you pick up their food and show a toast with the campfire.
*   **Fixed: on a server, a cat getting up from its owner's bed in the morning crashed the server.** Singleplayer was not affected.
*   **Dynamic Trees**: bamboo jungles grow Dynamic Trees' jungle trees (mostly mega jungle trees, an odd oak), as its own jungles do, and village trees are Dynamic Trees' where it grows the biome's trees. Bamboo jungles had only normal trees, and villages normal trees among dynamic ones. A pack's `config/dynamictrees/worldgen.json` still decides last.
*   **LittleMaidReengaged**: maids use the crossbow as their bow. It counts as an archer's weapon, and a maid loads it as she draws and fires it on her next shot. A pack can still turn it off with a "-" entry in LittleMaidReengaged's trigger config.

Needs Backport Core 1.4.9 or newer.

# 2.7.5

*   **1.14's look and feel pieces moved into Backport Core**, so every backport of 1.14 or later has them, even without this mod: Scroll Sensitivity and Discrete Scrolling, F3 + N, F3 + Esc, the F3 server chunk line, the title screen's Accessibility icon, the Mojang startup logo, 1.14's splashes and vanilla's text as 1.14 words it. Your Scroll Sensitivity and Discrete Scrolling settings carry over.
*   The village panorama stays in this mod.

Needs Backport Core 1.4.7 or newer.

# 2.7.4

*   **Realistic Terrain Generation** (and RTG Unofficial): bamboo jungles now generate in RTG worlds, on RTG's jungle terrain. RTG turned them into plains. Needs Backport Core 1.4.5.
*   **The ominous bottle is now Tricky Trials Backport's**, since 1.21 added it. Village & Pillage alone has 1.14's raids again: killing a captain gives Bad Omen and there is no bottle.
*   **With Tricky Trials Backport, captains drop an ominous bottle instead**, as in 1.21: a pillager captain that isn't in a raid drops a bottle of level I to V, and no captain gives Bad Omen. Drinking the bottle gives Bad Omen, which turns into Raid Omen in a village. The Captains Drop Ominous Bottles setting is gone.
*   **Ominous bottles from older worlds are kept.** With Tricky Trials Backport 1.0.1 they become its bottles, with the same level. Without it they stay in your inventory as plain items that can't be drunk, and they become Tricky Trials' bottles once it is added.
*   **With a 1.21.6 or later backport, books are signed on their own screen**, as in 1.21.6. Cancel goes back to the book and keeps the title. Otherwise books are still signed on their page, as in 1.14.

# 2.7.3

*   **New trades appear while the trade screen is open**, as in 26.3: the trade that earns a level levels the villager there and then, and the new level's trades show up on the open screen at your price. **Trades Update While Open** in Trade Tables turns it off (1.14 waited until the screen was closed).
*   **Enchanted trades as in 26.1**: a librarian's enchanted book can be any enchantment but soul speed, swift sneak and wind burst, and asks for the emeralds before the book; enchanted gear that comes out with no enchantment is no longer sold. Prices are as before. **Today's Enchanted Trades** turns it off.
*   **With Game Drops Backport, master librarians no longer sell name tags** (26.1). They sell yellow and red candles where a mod adds candles, Caves & Cliffs Backport's now included.
*   **Trades sell the family's items**: the wandering trader sells Update Aquatic's kelp, corals, nautilus shells, fish buckets and blue ice, and (each with its mod) cherry, mangrove and pale oak logs, cherry and pale oak saplings, pale hanging moss, pale moss, eyeblossoms, wildflowers, tall dry grass, firefly bushes, small dripleaf, rooted dirt, moss blocks and sulfur spikes, at current prices. Fishermen, butchers, clerics and leatherworkers trade Update Aquatic's cod buckets, dried kelp blocks and scutes, and masons sell Caves & Cliffs' dripstone blocks.
*   **Fixed: with Game Drops Backport the wandering trader could offer name tags twice.**
*   **Villagers and the wandering trader trade as the newest installed backport's version does**: 1.14's trades with Village & Pillage alone, 1.17's with Caves & Cliffs, 1.19's with Wild Update, 1.20's with Trails & Tales, 1.21's with Tricky Trials and 26.3's with Game Drops - prices, stock and which trades exist. With Tricky Trials the trader also buys things, sells logs and has two special offers. Villagers keep the trades they already have.
*   **Fixed: the wandering trader's slime balls and glowstone cost 5 and 3 emeralds** instead of 4 and 2.
*   **The cartography table copies and locks Game Drops Backport's explorer maps**, which come out as the same map; as in 26.3, only plain maps can be zoomed out.
*   **26.1's baby foxes, cats, pandas and villagers** when Game Drops Backport is installed: each with its new model and textures, and baby villagers wear their biome's clothes. They follow Game Drops' Baby Models setting; without Game Drops babies look as before.
*   **Fixed: horse armor, leather included, could go undrawn on horses** while Game Drops Backport was installed.

# 2.7.2

*   **Fixed villagers who never took a job at a workstation they could see.** A villager whose path ended outside the house wall, or that got wedged on a step a few blocks from its workstation, held its claim for good and never looked elsewhere. Villagers now find their way in through the door, and let a claim go after a minute if they can't reach it, as in current Java.

# 2.7.1

*   **Fixed: village blocks that stand in water were placed dry**, such as the fence posts in the savanna fisher cottage's pool. Applies to newly generated villages.
*   **Fixed see-through gaps along the edges of a held crossbow.**

# 2.7.0

*   **1.14's shorter crouch**: sneaking makes you 1.5 blocks tall with your eyes at 1.27, so you fit through a 1.5-block gap, and under a 1.5-block ceiling you stay crouched.
*   **Vindicators break wooden doors** during a raid, on Normal and Hard.
*   **TNT and TNT minecarts drop every block they destroy.** Creepers and other explosions keep their chance.
*   **Leaves drop sticks** now and then, more with Fortune.
*   **Illagers and ravagers leave baby villagers alone**, as since 1.18.
*   **Note blocks play 1.14's six new instruments**: iron xylophone, cow bell, didgeridoo, bit, banjo and pling.
*   **Parrots imitate pillagers and ravagers.**
*   **Mobs no longer spawn only to vanish** the next tick far from every player.
*   **Zombie sieges as in 1.14**: one night in ten, zombies come for a player in any village, however small.
*   **Arrows break chorus flowers**, which drop.
*   **A creeper killed by a stray or wither skeleton drops a music disc.**
*   **Invisibility hides you from guardians, and villagers from zombies and illagers**, until they are close.
*   **A tamed wolf's kills count as its owner's**, so they drop what only player kills drop.
*   **Dead bushes are furnace fuel.**
*   **Horse armor is drawn as in 1.14**: it no longer flashes red when the horse is hurt, and an invisible horse's armor shows.
*   **Torches stand on glass panes and iron bars.**
*   **The book and quill editor as 1.14 rewrote it**: a cursor, text selection, copy, cut and paste, double-click for a word, and 100 pages.
*   **Each of these has a setting** in the new **1.14 Changes** section, on by default.
*   **Cartographers trade as in current Java**: village maps by biome, swamp hut and jungle temple maps, a trial chambers map with Tricky Trials Backport, banners by biome and the woodland mansion map at master. A map is only offered for a structure an installed mod builds. **Today's Cartographer** in Trade Tables turns it off.
*   **Snow covers pillager outposts and villages** in snowy biomes, as in 1.14. Outposts were left bare, and villages had bare spots by their lights.
*   **Suspicious stew from later versions' flowers**: with Trails & Tales Backport, the torchflower gives Night Vision for 5 seconds, crafted or from a brown mooshroom, as in 1.20.
*   **The look and feel follows Backport Core's Look & Feel page**: with **Menus and Buttons** or the whole page turned off, the hotbar scrolls as in 1.12.2, and F3 + Esc pauses without the menu only while **F3 and Debug** is on.
*   **Fixed villagers pacing at the edge of their village** instead of walking to a workstation just outside it, and stalling for 10 seconds when hopping up a block on the way to work.

Needs Backport Core 1.4.0 or newer.

# 2.6.0

**Requires Backport Core 1.3.0 or newer.**

*   **Villages and pillager outposts in Caves & Cliffs Backport's 1.18 worlds**: 1.14's villages instead of 1.12's, where 1.18 puts them, laid out piece for piece as 1.18.2 lays them out for the seed, from 1.18.2's own templates. The ground is shaped around them as in 1.18. `/locate` finds them there. Worlds without Caves & Cliffs Backport keep their villages as before.
*   **Superflat worlds generate only what their preset asks for**: cornflowers, lilies of the valley, sweet berry bushes and bamboo only with `decoration`, 1.14 villages only with `village`, and no pillager outposts at all, as in vanilla.
*   **Fixed baby pandas' heads floating in front of their bodies.** Babies now use 1.14's own baby proportions.
*   **Sounds as current Java plays them**: the bell rings louder and at its three pitches and is heard from far away, the grindstone is half as loud, and a ravager's roar and a fox's screech carry as far as in vanilla (with Backport Core 1.3.1).
*   **Fixed village wells and other buildings in Caves & Cliffs Backport's 1.18 worlds** being cut through by a street. A street under a building now goes into the ground beneath it, as in vanilla, so wells keep their roof, rim and path ring.

# 2.5.1

*   **Cats sit and stand with an empty hand.** Right-clicking your cat with an empty hand gave the order twice, so it sat down and stood straight back up; only a click with fish in hand worked. Now any click the cat has no other use for tells it to sit or stand, as in current Java.
*   **Recipes unlock like vanilla's**, with Backport Core 1.2.0: an iron ingot unlocks the smithing table's recipe, a stick or coal the campfire's, and so on for every recipe of this mod, with the recipe toast. Update Backport Core to get it.
*   **The hotbar scrolls again on Cleanroom.** Cleanroom reports one wheel notch as 1 where Forge reports 120, and the 1.14 scroll code read every notch as 1/120 of a slot, so the selected slot never moved. Forge was not affected.

# 2.5.0

**Now requires Backport Core 1.1.0.** Install it beside this mod. Fluidlogged API comes through Backport Core and only adds waterlogging. Without it, blocks hold no water and a water bucket puts out a campfire.

*   **The new settings screen** from Backport Core. Every setting has a plain name and a one-line description, in all 18 languages.

*   **Vanilla's creative tabs.** The mod has no creative tab of its own any more: every block and item is in the tab vanilla puts it in, next to its vanilla neighbor, as 1.19.2 lists them (the last version with these tabs). Stripped logs follow the logs, the new walls the cobblestone wall, the workstations, bell, lantern and campfire the rest of the decoration blocks, each spawn egg the others in alphabetical order, and the crossbow's enchanted books are in Combat. With other update backports installed, each item still sits by its vanilla neighbor. As in vanilla, the ominous banner and suspicious stew are not in the creative menu (`/give` still gives them).

*   **Today's `/locate`**, now Backport Core's: `/locate structure minecraft:village_plains` (or `village_desert`, `village_savanna`, `village_snowy`, `village_taiga`), `/locate structure minecraft:pillager_outpost`, and `/locate structure #minecraft:village` for the nearest village of any type, which names the type it found. Results are today's: the same village today's game would pick for a seed, at its chunk's corner. Tab completion lists every installed update backport's structures. The plain 1.12.2 name `Village` is no longer listed, since 1.14's villages have types, but `/locate Village` still works. With 1.12.2-style villages, or on terrain 1.14 villages can't be planned on, `#minecraft:village` still finds the world's own villages.

*   **1.14's look and feel.** With Backport Core 1.1.0 the game looks the way 1.14 did, not only plays like it. When another update backport is installed too, the newest update's look wins, as in vanilla.
    *   **The title screen** shows 1.14's village panorama.
    *   **Splashes**: 1.14's sixteen new ones ("Rainbow turtle?", "Ping the human!" and more, and now and then "YOUR NAME IS YOU"), and the five it removed are gone.
    *   **The world-loading screen** shows 1.14's grid of chunks being generated, colored by stage, with the percentage.
    *   **Accessibility Settings** and **Mouse Settings** screens, as in 1.14 (built by Backport Core). Mouse Settings has 1.14's **Scroll Sensitivity** and **Discrete Scrolling**, and the hotbar scrolls by them: a touchpad no longer flicks through several slots at once.
    *   **F3 + N** in Survival switches to Creative (cheats on), and **F3 + Esc** pauses the game without opening the pause menu, as since 1.14.1.
    *   "Chat Opacity" is called **Chat Text Opacity**, as in 1.14, in every language.

*   **Waterlogging, as in current Java.** Every block of this mod that current Java waterlogs now holds water: lanterns (standing and hanging), campfires, scaffolding, the new stairs, walls and half slabs (not double slabs), the new signs and wood trapdoors. Place one into a water source and it keeps the water; a bucket fills or empties it. Bells, grindstones, lecterns, composters, barrels and the other workstations, buttons and pressure plates hold no water, as in vanilla.
    *   **Campfires**: water reaching a lit campfire puts it out, with the hiss and the burst of smoke. A campfire holding water can't be lit by flint and steel, a fire charge or a burning arrow, and one placed into water is placed out. A water bucket now waterlogs a campfire instead of only putting it out.

*   **Sea pickles moved to Update Aquatic Backport.** They are a 1.13 ocean block, so they belong with the update they came from. This mod no longer adds them: no item, recipe or creative tab entry. With Update Aquatic Backport installed:
    *   The wandering trader sells its sea pickles, as in current Java.
    *   The desert house that has sea pickles in 1.14 places its two. Without it, that stand is left bare, as the house was before.
*   **Existing worlds keep every sea pickle.** Sea pickles placed by 2.3.0 or 2.4.0, wet or dry, and pickle items in chests, inventories and on the ground are all kept.
    *   **With Update Aquatic Backport installed**, they become its sea pickles as the world loads, with the same number in each block and the same water. Ones saved as being in water get real water once, when their chunk first loads.
    *   **Without it**, they stay as they are, still glowing in water, and become its sea pickles the day you add it. Until then they can't be placed or crafted, and breaking one gives its pickles back.

*   **Seed maps: worlds match 1.14.** A new world generated with this mod and Update Aquatic Backport matches what 1.14 generates for the same seed, so seed map sites set to 1.14 show its biomes and structures where they really are. Checked against Minecraft 1.14.4's own code over six seeds: every biome in a 2,048-block square and every structure start in an 8,192-block square came out the same.
    *   **Bamboo jungles** are placed by 1.14's own rule: one jungle area in ten, chosen as 1.14 chooses it, with bamboo jungle hills and jungle edges where 1.14 puts them. In a world type this mod doesn't know, or one another mod has changed, they are still placed by the old filter.
    *   **Villages and pillager outposts** check the biome at the block 1.14 does, so they start in the same chunks.
    *   **Temples** each have their own grid, as since 1.13, and jungle temples also generate in bamboo jungles.
    *   **Strongholds and the world spawn** search for their biome as 1.14 does (with Backport Core 1.1.0, which now holds that part for every backport), and strongholds can be in mushroom field shores.
    *   Without Update Aquatic Backport, the oceans have no temperatures and no shipwrecks or ocean ruins; everything else matches 1.14.
    *   With a backport of a later update installed as well, the world matches that update instead: its numbers win.
*   **Villagers find their way home.** They now search for a path as far as 1.14 lets them, instead of 1.12.2's short limit that gave up partway across a village. A path that stops short is walked anyway and asked again from there, as in 1.14. Villagers no longer lose their bed at night just because it was across the village, and no longer stand at the village edge until morning.
    *   **Bedtime works as in current Java.** Villagers open the door on their path as they reach it, every time, and leave it open for another villager coming through instead of shutting it in their face. They no longer walk onto carpet under a low ceiling they can't fit beneath, and a door opening near them no longer sends them back to wherever they last wandered. In a test village, every villager with a reachable bed was asleep within 25 seconds of dusk, three nights running.
    *   Beds and workstations in a newly generated village are seen at once. Before, a villager that looked before the houses were built could miss them for ten minutes.
*   **Book roll messages can be turned off per player.** Hover over a "Rolled:" line in chat to see how, and click it to stop them for you alone. `/rollmessages on` turns them back on (any player can use it), and the setting is kept through death. `display.announceEnchantedBooks` still turns them off for everyone.
*   **TradeStages Legacy**: its rules now lock this mod's trades by career and trade level, not only by item.
*   **Village Names**: villagers without a job keep 1.14's empty trade list. Village Names used to fill it with a 1.12.2 job's trades every time the villager loaded.

# 2.4.0

**Now requires Backport Core 1.0.0.** Backport Core is a new library holding the code and textures every update backport shares, so the next backports can build on the same pieces. Install it beside this mod. Nothing in your world changes: every block, item, setting and saved village carries over.

*   **1.14's textures moved to Backport Core**, with their switch. It is now `textures.modernTextures` in `config/backportcore.cfg`, and in Backport Core's config screen. If you had turned it off in this mod's config, that choice is carried over the first time you start the game. The old line in `villagerbackport.cfg` does nothing any more.
*   Mods can now add villager and wandering trader trades through Backport Core, and netherite upgrades to the smithing table.
*   **Seventeen more languages**: Spanish (Spain and Latin America, including Argentina, Uruguay and Venezuela), Brazilian Portuguese, French, German, Russian, Simplified and Traditional Chinese, Japanese, Korean, Italian, Polish, Turkish, Ukrainian and Dutch. Block, item, mob, effect, advancement and subtitle names use Minecraft's own translations, so they match the names you already know. The config screen's descriptions are translated too.

# 2.3.0

The rest of 1.14. The wandering trader and its llamas, cats, foxes, pandas, bamboo and scaffolding, the campfire, sweet berries, the new flowers and dyes, suspicious stew, brown mooshrooms, every new slab, stair and wall, the new signs, stripped logs, wood trapdoors, buttons and pressure plates, blue ice and sea pickles. Villages are now built from 1.14's own blocks, all of them in this mod.

**Renamed to Village & Pillage Backport**, since it now backports all of 1.14's Village & Pillage update. Only the name changes: the mod id is still `villagerbackport`, so worlds and configs carry over untouched.

## New: the wandering trader

*   **The wandering trader** turns up now and then with two **trader llamas** on leads. Once a day the game tries to send one to a random player in the overworld: a 25% chance the first time, 50% the next, then 75% until one comes, each with a further one in ten roll, as in 1.14 and current Java. It arrives within 48 blocks of the player, or of the nearest **bell** within 48 blocks of them, walks there, and stays within 16 blocks of it.
*   **Current Java's trades**: nine per trader. Two things it buys (hay bales, water bottles, baked potatoes, milk, water buckets, fermented spider eyes), two specials (packed ice, gunpowder, logs, podzol, a potion of invisibility, an enchanted iron pickaxe and blue ice) and five everyday goods: flowers, seeds, saplings, dyes, mushrooms, vines, lily pads, sand, glowstone, cactus, slime balls, pumpkins and name tags. Sea pickles, cornflowers and lilies of the valley too, and kelp, coral blocks, fish buckets and nautilus shells when another mod adds them.
*   **No levels and no restocking**: what it has is all it has. It trades on the 1.14-style trade screen, with no level or experience bar, and drops experience orbs as villagers do.
*   **Invisible at night**: at dusk it drinks a potion of invisibility, and at dawn a bucket of milk, with 1.14's sounds.
*   **It leaves after 40 minutes**, counted only while it's loaded and nobody is trading with it. Its llamas go with it. Traders from spawn eggs never leave.
*   It flees zombies, pillagers, vindicators, evokers, illusioners and vexes, and panics when hit. Zombies, pillagers, vindicators, evokers, illusioners and ravagers go after it as they go after villagers.
*   **Trader llamas** wear the trader's blue and gold decor (a carpet still replaces it), can't be ridden while the trader holds the lead, and spit at whatever attacks the trader. Take one with your own lead and it's yours to keep.
*   1.14's models, textures and sounds, and spawn eggs for both.
*   `trader.enabled` turns the whole thing off. `trader.naturalSpawning`, `spawnDelay`, the three chance settings, `spawnRadius`, `despawnTicks`, `llamas` and `oneAtATime` (no second trader while the last may still be around) tune it, and the **`doTraderSpawning` game rule** switches it off per world, as in current Java. `/wanderingtrader status`, `spawn` and `timer` for operators.

## New: cats

Cats and ocelots are two animals, as in 1.14. Old worlds' tamed ocelots become cats.

*   **Cats**, with 1.14's model, sounds and all **eleven coats**: tabby, tuxedo, red, Siamese, British shorthair, calico, Persian, ragdoll, white, Jellie and black. Black cats belong to witches, or turn up under a full moon.
*   **Taming**: stray cats run from you, but creep after you if you're holding raw cod or salmon, meowing for it. Feed one and, one time in three, it's yours, and sits.
*   **Tamed cats** follow you, sit or stand when you use them, heal on fish and breed on fish. A kitten takes one parent's coat, and its owner. Their **collar** starts red; use a dye on it to change it.
*   They sit on **chests** (which then won't open), lit **furnaces** and the foot of **beds**, and lie down on beds.
*   **Morning gifts**: a cat curls up at the foot of the bed while you sleep, and in the morning has seven chances in ten of having brought you something: string, feathers, rabbit hide or foot, raw chicken or rotten flesh - or a phantom membrane, if a mod adds one.
*   **Creepers keep away from cats**, as from ocelots. With a mod that adds **phantoms**, a phantom breaks off its attack when a cat is within 16 blocks, and the cat hisses at it.
*   **Where they live**: villages have cats from the start (1.14's own cat spots in each village), and more strays turn up in a village with more than four beds villagers sleep in, up to five cats. A black cat comes to a **witch hut** that has none, and stays.
*   **A Complete Catalogue**: tame one of every coat.
*   Spawn egg in the creative tab.

## Changed: ocelots

*   **Ocelots can't be tamed any more**, as in 1.14. Feed one raw cod or salmon instead and, one time in three, it comes to **trust** you: it stops running from players and won't despawn. A trusting ocelot breeds on fish.
*   Ocelots have 1.14's own calls and death sounds.
*   **Tamed ocelots in old worlds become cats** when they load, keeping their owner, name, age and sitting: the tuxedo, red and Siamese ocelots become those cats, as 1.14's world upgrade did.

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

## Settings: berries and foxes

`enabled` (everything above), `generateBushes`, `taigaPatchesPerChunk` (1), `rarePatchChance` (12, for snowy and giant taigas), `bushBiomeBlacklist`, `spawnWeight` (8), `minGroup` (2), `maxGroup` (4), `extraFoxBiomes`, `foxBiomeBlacklist`.

## New: bamboo

*   **1.14's bamboo**, grown exactly by 1.14's rules: plant a piece on grass, dirt, podzol, sand, red sand, gravel or mycelium and a shoot comes up, then a stalk that grows a block at a time in light 9 or more, up to somewhere between 12 and 16 blocks tall.
*   The stalk **thickens** once it is three blocks tall, its **leaves** follow the top as it grows (large on the top two blocks, small below them, bare further down), and every block sits slightly off-center, so a grove looks wild rather than planted.
*   **Bone meal** adds one or two blocks at once. **A sword cuts it instantly**; an axe is the right tool otherwise. Break the bottom and the whole stalk comes down.
*   Two bamboo make a **stick**. Bamboo burns in a furnace for a quarter of an item.
*   **In jungles**: a stand turns up every few chunks in ordinary jungles and jungle hills, as in 1.14 (`bamboo.jungleBamboo`).

## New: scaffolding

*   **Crafted** from six bamboo and a string, six at a time. Breaks instantly and burns like bamboo.
*   **It holds itself up**, as in 1.14: a scaffold can stand on the ground, on more scaffolding, or reach out sideways from a supported column - up to six blocks. Further than that it won't place. Knock out its support and everything leaning on it comes down as items; take out the bottom of a tower and the whole tower falls.
*   A scaffold hanging out from a column shows its bottom frame, and you can stand on that frame.
*   **Moving through it**: walk straight through the sides, stand on top, **jump to climb**, **sneak to go down** - at 1.14's climbing speed, with no fall damage. The scaffolding only answers the questions the game already asks every block, and doesn't touch how players move, so it works alongside mods that change swimming and climbing.
*   **Placing it the 1.14 way**: use scaffolding on the top of a scaffold to extend outward in the direction you face, or on its side to build the column up to its top. Sneak to place it against the clicked face instead.
*   Not 1.14: the selection outline is always the whole block (1.14 shows the frame unless you're holding scaffolding).

## New: the bamboo jungle

*   **Bamboo jungle and bamboo jungle hills**: jungles full of bamboo, with patches of podzol under it, big jungle trees, parrots, ocelots - and pandas.
*   As in 1.14 they don't have a climate of their own: about one jungle area in ten is a bamboo jungle instead (`bamboo.bambooJungleRarity`), and its hills are bamboo jungle hills. Works with the default world type, large biomes, amplified, and biome mods that use vanilla's jungle. Not in Open Terrain Generator worlds, which choose their biomes themselves.
*   Registered with Forge's biome dictionary as jungle, hot, wet and dense (hills too for the hills), so other mods treat them as jungles.
*   `bamboo.bambooJungle` turns them off. New chunks only.

## New: pandas

*   **1.14's pandas**, with the model, textures and sounds. They live in bamboo jungles (spawn weight 80), and now and then one turns up in an ordinary jungle (weight 1). Spawn egg in the creative tab.
*   **Seven personalities**, from 1.14's two-gene inheritance: **lazy** pandas dawdle and lie on their backs, **worried** ones keep away from players and monsters and cower in thunderstorms, **playful** ones roll over all the time, **aggressive** ones keep fighting whatever hit them and join in when another panda is hit, **weak** ones have half the health and their cubs sneeze more, and **brown** pandas are brown. Brown and weak are recessive: a panda only shows them with both genes, so two ordinary-looking pandas can have a brown cub.
*   They **eat bamboo**, and **cake** if they find it: they pick it up off the ground, sit down and munch. Hand one bamboo and it sits and eats it.
*   **Breeding** with bamboo needs bamboo growing nearby. Without it the panda shakes its head and bleats instead.
*   **Cubs** roll about and **sneeze** - startling every grown panda nearby into a jump, and once in a while leaving a slime ball.
*   Drops a bamboo when killed.

## Settings: bamboo, scaffolding and pandas

*   `enabled` - everything above. Off, nothing is registered.
*   `bambooJungle`, `bambooJungleRarity` (10)
*   `jungleBamboo`
*   `pandaJungleWeight` (1), `pandaBambooJungleWeight` (80)

## New: the campfire

*   **1.14's campfire**, with its recipe (three sticks, coal or charcoal, and three logs of any wood). It gives light level 15, crackles and sends up 1.14's big, slow smoke.
*   **Cooks four foods at once**, no fuel: right-click with raw meat, raw fish or a potato to lay it on the logs. Each one takes 30 seconds and pops off cooked. It cooks any furnace recipe that turns a food into a food, so other mods' raw foods work too; `campfire.extraFoods` and `campfire.excludedFoods` change the list, and `campfire.cookTime` the time.
*   **A signal fire**: put a hay bale under it and the smoke climbs about 24 blocks instead of about 10.
*   **It burns**: 1 damage every half second to anything standing in it, sneaking or not, as in current Java. Fire Resistance, Frost Walker and fire-immune mobs are safe. It never sets anything on fire, so mobs that die in it drop raw meat. Mobs walk around a lit one.
*   **Put it out** with a shovel (not from underneath), a water bucket, or a splash or lingering water bottle. **Light it** with flint and steel, a fire charge, a burning arrow, or a blaze's fireball (with `mobGriefing` on). Food on it stays put while it's out, and carries on cooking when it's relit.
*   Breaking it drops two charcoal, or the campfire with Silk Touch, plus whatever was cooking.
*   **Fishermen sell it**, and **taiga villages** have theirs again: five of the taiga houses and decorations place a lit campfire, which was an empty spot before.
*   `campfire.enabled` turns it off entirely.

Hay bales already cut fall damage by 80% in 1.12.2, exactly as in 1.14, so there was nothing to add there.

## New: flowers

*   **Cornflower**, **lily of the valley** and **wither rose**, with 1.14's textures. They stand where flowers stand, sit slightly off-center like 1.12.2's own, go in flower pots, and compost at 65%.
*   **Where they grow**: cornflowers in plains, sunflower plains and flower forests; lilies of the valley in forests, birch forests, dark forests (and their hills) and flower forests, as current Java. Other mods' plains and broadleaf forests grow them too, by their biome type. New chunks only.
*   **Bone meal** on grass grows cornflowers in plains, sunflower plains and flower forests, and lilies of the valley in flower forests only, as in current Java.
*   **The wither rose** withers anything that walks through it, except the Wither, wither skeletons and players in creative, and not on Peaceful. It stands on netherrack and soul sand as well as grass and dirt, and smokes. In a pot it's harmless.
*   **When the Wither kills anything** - a mob, a villager, a player - a wither rose is planted where it died, or dropped if it can't be (no room, the wrong ground, or `mobGriefing` off).

## New: dyes

*   **White, black, brown and blue dye**, 1.14's own items. Bone meal or a lily of the valley makes white dye, an ink sac or a wither rose makes black, cocoa beans make brown, lapis lazuli or a cornflower makes blue.
*   **They work everywhere a dye does**: wool, glass, terracotta, beds, banners at the loom and in the grid, dye mixing, firework stars, leather armor, sheep, wolf collars, signs, and every other mod's recipe that asks for a dye by its ore dictionary name. Shepherds buy them.
*   **Nothing about bone meal, ink sacs, cocoa beans or lapis changes.** They're still dyes too, and still everything else they were.

## New: suspicious stew

*   A bowl, a red and a brown mushroom and any small flower make **suspicious stew**: mushroom stew with that flower's effect, at current Java's durations (a cornflower gives 5 seconds of Jump Boost, a lily of the valley 11 of Poison, a wither rose 7 of Wither, and so on). JEI shows a recipe for each flower. It doesn't stack.
*   **Expert farmers** sell it for an emerald, in six flavors, as in 1.14.

## New: brown mooshrooms

*   **Lightning** turns a red mooshroom brown and a brown one red, without hurting it, with 1.14's sound.
*   **Feed a brown mooshroom a small flower**, then milk it with a bowl, for that flower's suspicious stew. Feed it a second one first and it just puffs smoke. Milked again, it gives plain mushroom stew until it eats another flower. Works in creative too.
*   **Sheared**, a brown mooshroom drops brown mushrooms.
*   **Breeding**: calves take their parents' color (one time in 1024 the other one); a red and a brown parent have either.
*   Red mooshrooms are exactly as they were. Brown ones are still 1.12.2's mooshroom underneath, so other mods treat them as mooshrooms.

## New: leather horse armor

*   1.14's **leather horse armor**: 3 armor points, crafted from seven leather in an H. **Dye it** in the crafting grid like leather armor, with any dye; wash the dye off in a cauldron. Horses wear it in its color.
*   **Expert leatherworkers** sell it, dyed, for 6 emeralds, as in 1.14.

## New: the Globe banner pattern

*   1.14's fifth banner pattern, the **Globe**, sold by **master cartographers** for 8 emeralds in place of the stand-in they sold before. Use it at the loom like the other four; it isn't used up.

## New: dyed sign text

*   **Right-click a sign with any dye** to recolor its text, as in 1.14. The dye is used up only if the color changes. Every player with the mod sees the color.

## Settings: flowers, dyes and stew

New `flora` section: `enabled` (everything above), `generateFlowers`, `brownMooshrooms`, `dyeableSigns`, `witherKillsPlantRoses`.

## New: 1.14's building blocks

*   **Every slab, stairs and wall 1.14 added**, 42 in all, with 1.14's names and recipes:
    *   **Slabs**: stone, granite, polished granite, diorite, polished diorite, andesite, polished andesite, mossy cobblestone, mossy stone brick, smooth sandstone, cut sandstone, smooth red sandstone, cut red sandstone, smooth quartz, red nether brick and end stone brick.
    *   **Stairs**: stone, granite, polished granite, diorite, polished diorite, andesite, polished andesite, mossy cobblestone, mossy stone brick, smooth sandstone, smooth red sandstone, smooth quartz, red nether brick and end stone brick.
    *   **Walls**: brick, granite, diorite, andesite, prismarine, stone brick, mossy stone brick, sandstone, red sandstone, nether brick, red nether brick and end stone brick. They join each other, vanilla's cobblestone walls and other mods' walls.
*   **Smooth stone, smooth sandstone, smooth red sandstone and smooth quartz** as blocks of their own, made by smelting stone, sandstone, red sandstone and quartz blocks, as in 1.14. 1.12.2 only had them as the hidden doubled form of a slab.
*   **Signs in spruce, birch, jungle, acacia and dark oak**, each with 1.14's own texture, crafted from its planks. They stand or hang and are edited like an oak sign.
*   **The stonecutter cuts all of them**: one block into one stairs or wall, or two slabs. It also cuts straight through a one-for-one step, as 1.14's does: stone into stone brick slabs, stairs and walls, andesite into polished andesite slabs and stairs, sandstone into cut sandstone slabs. This works for other mods' blocks too.
*   **Smelting stone bricks** gives cracked stone bricks, as in 1.14.
*   Everything is in this mod's creative tab, and in the ore dictionary as `slab`, `stair`, `wall` and `sign`; smooth stone as `stoneSmooth`, the smooth sandstones as `sandstone`, smooth quartz as `blockQuartz`.
*   **Villages use them**: desert villages get real smooth sandstone, smooth sandstone stairs and slabs and sandstone walls, taiga villages spruce signs, and so on, wherever 1.12.2 used a stand-in before.

## Changed: seven vanilla recipes, as in 1.14

So every new block can be crafted, these change the way 1.14 changed them (`building.vanillaRecipeChanges`, on by default):

*   **Sign**: oak planks only. The other woods make their own sign.
*   **Stone Slab** (1.12.2's, now named **Smooth Stone Slab** as in 1.14): made from smooth stone. Stone makes 1.14's new plain stone slab.
*   **Sandstone and red sandstone slabs**: no longer from cut sandstone, which makes the cut sandstone slab.
*   **Stone brick slab and stairs**: plain stone bricks only. Mossy stone bricks make their own.
*   **Nether brick fence**: four nether brick blocks with two nether bricks up the middle. Six nether brick blocks make the wall.

Renamed to 1.14's names: Smooth Sandstone to **Cut Sandstone**, Smooth Red Sandstone to **Cut Red Sandstone**, Stone Slab to **Smooth Stone Slab**, Sign to **Oak Sign**.

## New: the last blocks 1.14's villages are built from

Villages no longer borrow these from other mods or fall back to 1.12.2 stand-ins:

*   **Stripped logs and stripped wood in all six woods**, with 1.14's textures. Stripped logs face whichever way you place them, like logs. Four stripped logs make three stripped wood. Both make four planks, smelt into charcoal, burn in a furnace and catch fire like logs, keep leaves from decaying, and are `logWood` in the ore dictionary.
*   **Strip logs with an axe**: right-click a log with any axe, modded axes included, and it becomes the stripped log, facing the same way. Bark blocks become stripped wood. You hear 1.14's strip sound, and the axe takes one point of damage. If another mod already strips logs in your pack, it's left to do it.
*   **Trapdoors, buttons and pressure plates in spruce, birch, jungle, acacia and dark oak**, each crafted from its own planks with 1.14's recipe and drawn with 1.14's trapdoor textures. They work exactly like the oak ones: the same redstone, the same timing, the same sounds. Buttons burn for 100 ticks in a furnace, trapdoors and pressure plates for 300.
*   **Blue ice**, from nine packed ice. Slipperier than packed ice (boats on it are fast), never melts, and only drops with Silk Touch. Mined fastest with a pickaxe.
*   **Sea pickles**: place up to four in one block by right-clicking with more. In water they glow, brighter the more there are; out of water they are dark. They need a block with a top to stand on, smelt into lime dye and compost at 65%.
*   **Villages use them**: taiga villages get real spruce trapdoors and pressure plates, snowy villages real stripped spruce logs and wood and blue ice, jungle buttons, acacia pressure plates and so on, wherever 1.12.2 used a stand-in.
*   Everything is in this mod's creative tab.

## Changed: three vanilla recipes, as in 1.14

The **trapdoor**, **wooden button** and **wooden pressure plate** now take oak planks only, since the other woods make their own. They are renamed **Oak Trapdoor** and **Oak Pressure Plate**, as in 1.14. (Vanilla's wooden button shares its name with the stone button, so it stays "Button".)

## New: 1.14's textures

*   **Vanilla's blocks, items and mobs get the textures 1.14's Texture Update gave them** - about 620 of them - plus 1.15's enchantment glint. Turn off `textures.modernTextures` to keep 1.12.2's; changing it in game reloads resources as F3+T does.
*   Resource packs you pick, and other mods that change a vanilla texture on purpose, still win. Villagers, horses and water keep 1.12.2's textures either way: 1.14's don't fit 1.12.2's models or colors.
*   Client only; a server ignores the setting.

## Changed: the grindstone hangs on walls and ceilings

*   As in 1.14, the grindstone goes on the floor, the ceiling or a wall, depending on which way you're looking as you place it. Grindstones already placed stay on the floor. The savanna weaponsmith's wall-mounted grindstone is now built as in 1.14.

# 2.2.0

Raids, pillagers, ravagers, patrols, pillager outposts and the crossbow. Carry Bad Omen into a village and, 30 seconds later, a raid comes: waves of illagers, a raid bar, a horn, and Hero of the Village for whoever beats it. Villagers ring the bell, hide and celebrate as in 1.14. Works with **Raids Backport** too.

## New: raids

*   **Bad Omen** in a village turns into **Raid Omen** for 30 seconds, and the raid starts where you were when it runs out, as in current Java. Turn off `raids.raidOmen` for 1.14's rule, where the raid starts the moment you walk in.
*   **1.14's waves**: three on easy, five on normal, seven on hard, plus a bonus wave when the omen was above level I. 1.14's mix of raiders per wave, with the extra raiders it adds on harder difficulties, and sharper axes on the later waves of a strong raid.
*   **The raid bar** fills up while the raid gathers, then counts the raiders' health down, says how many are left when only a couple remain, and shows Victory or Defeat at the end. **The horn** sounds from the direction each wave arrives from.
*   Raiders head for the village, go from house to house looking for villagers, and jump about cheering if they win. Raiders met on the way join in. They never despawn mid-raid, and pick up where they left off after a restart.
*   **Captains**: each wave has one, wearing the ominous banner. Kill a captain away from a raid and you get Bad Omen, one level higher each time, up to V.
*   **The ominous banner** is 1.14's, named in gold, with its pattern list hidden. It's in this mod's creative tab too.
*   A raid needs a village: beds villagers sleep in, workstations they work at, a bell they meet at, or a 1.12.2 village of doors. It's lost when the village is gone and times out after 40 minutes.
*   `/raid start [level]`, `/raid stop` and `/raid status` for operators, and the `disableRaids` game rule to switch raids off in a world.
*   Two advancements: **Hero of the Village** and **Voluntary Exile**.

## New: pillagers and ravagers

*   **Pillagers**: 1.14's crossbow illager. They close to eight blocks, load for just over a second, hold their aim for one or two seconds and fire, with 1.14's speed and spread. On the later waves of a strong raid some carry a multishot crossbow that loads faster.
*   **Their crossbow** is this mod's own (see below), or Future MC's, Crossbows Backport's or Spartan Weaponry's when that one is in use (`raids.crossbows`). It shows loaded and unloaded as the pillager works it, and drops now and then, enchantments and all.
*   **Ravagers**: 100 health, a 12-damage bite that throws you back, and 1.14's shield rule: block the bite with a shield and half the time it is stunned for two seconds, then roars, hurting and scattering everything nearby that isn't an illager. It walks through leaves and tears them down, and drops a saddle.
*   **Riders**: ravagers on normal's last wave carry a pillager; from hard's last wave they carry an evoker or a vindicator, as in 1.14.
*   Both have 1.14's models, textures and sounds, and spawn eggs. Neither spawns on its own.
*   **Ominous bottle**: turn on `raids.captainDropsOminousBottle` and captains drop a bottle of random level instead of giving Bad Omen straight away, as in current Java. Drink it for Bad Omen. With Deeper Depths installed, its bottle is dropped instead.

## New: the crossbow

*   **1.14's crossbow**, with its recipe (sticks, string, an iron ingot and a tripwire hook). Hold use to draw it, let go to load, use again to fire. It stays loaded until you fire it, even put away.
*   **Arrows of any kind**, from anywhere in your inventory, or a **firework rocket** held in your other hand. Rockets fly straight and explode on whatever they hit, damaging everything nearby.
*   **Quick Charge** (I-III) loads it faster, **Multishot** fires three arrows for one, and **Piercing** (I-IV) sends arrows through that many mobs to hit the next. Multishot and Piercing don't go together. Unbreaking and Mending work; the bow's enchantments don't. From the enchanting table, anvils and books, with current Java's odds.
*   Arrows fly at 1.14's speed and always crit, for 6 to 11 damage. 465 durability, as in current Java.
*   Fletchers sell it, enchanted at expert, and outpost chests hold one.
*   Four advancements: **Ol' Betsy**, **Who's the Pillager Now?**, **Two Birds, One Arrow** (with a mod that adds phantoms) and **Arbalistic**.
*   In first person it moves as in 1.14: turned across you while it draws, resting at your side once drawn, and swung in to aim down the middle of the screen once loaded. Seen by others, holding it loaded raises both arms to aim, the nearest pose 1.12.2 has to 1.14's.
*   **With Future MC or Crossbows Backport installed, this mod adds no crossbow**, so a pack never has two: theirs is used by pillagers, fletchers and outposts instead. `compat.crossbow` changes that: `AUTO`, `OURS` (always) or `OFF` (never).

## New: patrols and pillager outposts

*   **Patrols**: from the fifth day, by day, now and then a band of pillagers appears 24 to 48 blocks from a player who isn't in a village, as in 1.14. A captain carrying the ominous banner leads it across the country, the rest following. Kill the captain away from a raid for Bad Omen - which is now how a raid is meant to start. Patrols stay as long as a player is within 128 blocks.
*   **Pillager outposts**: 1.14's own outposts - the dark oak watchtower with its banners and chest at the top, sometimes overgrown, and around it tents, log piles, targets and cages holding an iron golem. They're placed by 1.14's rules: plains, desert, savanna, taiga and snowy tundra (and other mods' biomes like them), never within ten chunks of a village. New chunks only.
*   **Outposts stay manned**: pillagers spawn in and around them day and night, as in 1.14.
*   **The chest**: 1.14's loot, crossbow included.
*   `/locate pillager_outpost` finds the nearest.

## New: the bell reveals raiders

Ring a bell with raiders within 32 blocks and it resonates, and two seconds later every raider within 48 blocks glows for three seconds, as in 1.14. Raiders are pillagers, vindicators, evokers, illusioners, ravagers and witches, whichever mod adds them.

**Changed:** the bell no longer highlights villagers. It only reveals raiders, as in 1.14, and the `workstations.bellHighlightsVillagers`, `bellRadius` and `bellGlowTicks` settings are gone.

## New: villagers hide

*   **When a bell rings**, villagers within 32 blocks run to their bed, or the nearest bed, and stay inside for 15 seconds.
*   **During a raid**, they gather at the bell and ring it until the raiders arrive, then go home and stay there. After a victory they come out, jump about and set off fireworks.
*   A villager that can't reach a bed hides under the nearest roof.
*   Villagers run from pillagers at 15 blocks and ravagers at 12, as in 1.14.

## New: Hero of the Village

*   **Discount**: 30% off every trade, plus 6.25% a level, taken from the base price and shown on the trade screen with the struck-out original. Stacks with reputation, as in 1.14.
*   **Gifts**: villagers walk up to a hero now and then and throw them something: 1.14's gift for each profession, a poppy from a baby, wheat seeds from a villager without a job. Each profession's gift is a loot table you can change.

## Raids Backport

With **Raids Backport** installed, its raids are used instead of this mod's, villagers live through them, and three things that went wrong with the two together are fixed:

*   Ringing this mod's bell now reveals its raiders. Before, only Future MC's bell did, and this mod hides that one.
*   Villagers hide during its raids again. Before, this mod had removed the goal its hiding works through.
*   The hero discount is counted once and shown on the trade screen. Before, it was charged but not shown, and worked out from a price that already included demand.

Its own gifts are left as they are; this mod only adds gifts for the mason.

## Settings

New `raids` section: `bellRevealsRaiders`, `hideOnBell`, `raidBehaviour`, `heroDiscount`, `heroGifts`, `raidOmen`, `easyWaves`/`normalWaves`/`hardWaves`, `crossbows`, `captainDropsOminousBottle`, `patrols`, `outposts`, `outpostsInModdedBiomes`, which mob plays each raider (`pillagers`, `vindicators`, `evokers`, `witches`, `ravagers`) and `extraRaiders` (more mobs to count as raiders). New `compat.raids` picks whose raids there are: `AUTO` (Raids Backport's when installed, otherwise this mod's), `OURS`, `RAIDS_BACKPORT` or `NONE`. New `compat.crossbow` decides whether this mod adds the crossbow: `AUTO` (unless Future MC or Crossbows Backport is installed), `OURS` or `OFF`.

# 2.1.4

1.14 villages in Biomes O' Plenty's world type, rank badges and 1.14 outfits for other mods' villagers, safe curing in modded packs, `/librarian` and `/discount`, 1.14's work sounds, a setting for farming with mob griefing off, and villagers keeping their outfits.

## New: 1.14 villages in Biomes O' Plenty worlds

Worlds made with **Biomes O' Plenty**'s own world type now get 1.14 villages. BOP builds its terrain its own way, so until now this mod couldn't plan villages on it and those worlds got 1.12.2's villages.

*   They're planned on BOP's real terrain, so houses sit on the ground as they do in any other world.
*   Each village takes its style from the biome's tags: snowy biomes get snowy villages, sandy ones desert villages, savanna savanna and coniferous forests taiga; the rest get plains villages. Villages only appear in the biomes BOP allows them in.
*   `/locate village_plains` and the other village types work in BOP worlds too.
*   Villages already in your world stay as they are.

## New: other mods' villagers

*   **Other mods' villagers now wear the rank badge.** A villager from another mod's profession keeps its own texture, since it's a whole 1.12.2 villager with no way to tell its clothes apart, but now shows the 1.14 rank badge for its level like everyone else.
*   **Dress them in 1.14 clothes if you'd rather.** The new `display.modOutfits` setting maps another mod's profession, or a single career, to the closest 1.14 outfit, for example `somemod:chef=butcher`. A mapped villager is dressed exactly like a vanilla one: biome clothing, the outfit, its hat and the badge.
*   **Other mods' villagers no longer lose their profession.** A villager that hadn't traded yet was made unemployed the first time this mod saw it, to find its job at a workstation as in 1.14. No workstation offers another mod's career, so Forestry's apiarists, Immersive Engineering's engineers and the like turned unemployed for good, whether a village, that mod's own houses or a `/summon` made them. Careers no workstation offers now keep their job and restock without one.
*   **Other mods' villagers earned too little experience.** Each trade's experience follows the level it unlocked at, and this mod could only guess that level for another mod's trades, from how its trades are registered. Those guesses often came out as novice, so later trades paid 1 or 2 experience instead of up to 30 and those villagers levelled up very slowly. A villager now gets its first trades from this mod, which records where each level's trades end, as it already did for vanilla villagers. That also stops villagers made with `/summon` starting on their second level's trades and getting them twice.
*   **Careers other mods add to vanilla professions** now wear that profession's 1.14 outfit (the farmer's, librarian's, cleric's, toolsmith's or butcher's) instead of plain biome clothing, unless `display.modOutfits` says otherwise.

## New: farming with `mobGriefing` off

Villagers only harvest, replant and pick up food and seeds while the `mobGriefing` gamerule is on, just as in vanilla. Worlds that turn it off to stop creeper damage end up with farmers standing idle beside ripe fields and villagers ignoring the bread you throw them.

Turn on `jobs.ignoreMobGriefing` and villagers farm and pick up food and seeds anyway. Everything else `mobGriefing` controls stays off, including villagers trampling farmland. It's off by default, so nothing changes unless you turn it on.

## New: curing is safe in modded packs

Zombifying and curing villagers for the discount no longer risks anything in packs that make mobs stronger. A zombie villager that was one of your villagers is never made an **Infernal Mobs** elite, a **Scaling Health** blight or a **Champion**, and gets no Scaling Health bonus health, so an exploding elite can no longer appear in the middle of your trading hall.

In Dregora, the cured villager is also never turned into a **Sussyberian** or **Mentalberian**, which refuse to trade. Until now every cured librarian had a one-in-ten chance of coming back as one. Berians that were made this way before are turned back into the librarians they were, with their trades, the next time their chunk loads. Berians that generated as berians are Dregora's own and are left alone.

All of this is the `compat.convertedStayOrdinary` setting, on by default.

## New: `/librarian` and `/discount`

Two operator commands for putting back what a bad cure took:

*   `/librarian <enchantment> [level] [villager level] [price] [cured]` spawns a librarian where you stand, selling that enchanted book. For example, `/librarian strengthened_vitality 3`. Press Tab for the enchantment names. The price defaults to 1.14's price for the book, and `cured` gives you the cure discount with it.
*   `/discount [radius]` gives you the cure discount from the villager you're looking at, or from every villager within the radius. It's exactly what a cure gives, so using it again changes nothing.

## Fixed

*   **Curing villagers that stood side by side mixed up their trades.** When several villagers a block apart were cured together, one could come back with its neighbour's trades and level, and the neighbour came back with a fresh set. Each cured villager now gets back its own trades, level and experience, however close together they stand. The same goes for villagers zombified together.
*   **Villagers lost their trades through a cure with `pricing.gossipEnabled` off.** Their trades are now kept whether gossip is on or not.
*   **Villagers changed clothes when they walked into another biome.** A villager now keeps the biome outfit it was given when it spawned, as in 1.14: a desert villager led into a taiga stays a desert villager. A baby takes the biome it's born in half the time, otherwise one of its parents' outfits, and a cured zombie villager wears what it wore before it was infected. Villagers already in your world keep the outfit for the biome they're standing in when you first load the world with this version.
*   **Villagers at a smithing table, composter, blast furnace or fletching table made a generic wood or stone knock while working.** Every profession now makes 1.14's own work sound, with 1.14's sound files: the armorer's blast furnace crackles, the butcher's smoker hisses, the farmer's composter fills, the fisherman's barrel opens, the fletcher whittles, the leatherworker dyes in the cauldron, the toolsmith hammers, and the shepherd now uses the loom's finishing sound, as in 1.14.
*   **The left arm's texture was the wrong way round.** 1.14 draws the left sleeve mirrored, so the outfit's detail sits on the outside of both arms, as it should.

***

# 2.1.3

1.14 villages in superflat worlds.

## New: 1.14 villages in superflat worlds

Superflat worlds now get 1.14 villages, in any preset that has villages turned on (Classic Flat does). Superflat builds its villages differently from every other world type, so until now it was left with 1.12.2's villages.

*   Villages keep the preset's own size and spacing settings, and appear in the same biomes as before: plains, desert, savanna, taiga, and now snowy villages in a snowy preset.
*   `/locate village_plains` and the other village types work in superflat worlds too.
*   Villages already in your world stay as they are.

***

# 2.1.2

1.14 villages in Realistic Terrain Generation worlds.

## New: 1.14 villages in Realistic Terrain Generation worlds

Worlds made with **Realistic Terrain Generation**'s world type now get 1.14 villages, with the original RTG or with its maintained fork **Realistic Terrain Generation Unofficial**. Until now this mod couldn't plan a village on RTG's terrain, so those worlds were left with 1.12.2's villages.

*   They are planned on RTG's real terrain, so houses sit properly on its hills.
*   `/locate village_plains` and the other village types work in RTG worlds too.
*   Villages already in your world stay as they are.

***

# 2.1.1

Fixes for playing alongside Future MC.

## Fixed

*   **Villages were still built with this mod's workstations, bells and lanterns when `compat.futureMcDuplicates` was set to `KEEP_FUTURE_MC`.** Both 1.14 villages and the workstations added to 1.12.2's houses now use Future MC's blocks in that case, keeping how each block faces and whether lanterns and bells hang.
*   **The smithing table ignored Future MC's smithing recipes, including ones added with CraftTweaker, unless a netherite ingot existed.** With Future MC's netherite turned off and no other netherite mod, the table found nothing to do, so it never opened. It now reads Future MC's recipes either way and takes them exactly as written, so a recipe can now make any item, in any amount.

Without Future MC or a netherite mod there is still nothing to upgrade, so the smithing table still does nothing, just as in 1.14.

***

# 2.1.0

A big one. The smithing table now upgrades diamond gear to netherite, farmers work the way they do in modern Minecraft, and villagers gather at the bell, play, wear their hats and shake their heads at you. 1.14 villages now generate in RLCraft, RLCraft Dregora and Caves & Cliffs worlds, with waystones and other mods' buildings mixed in, and they are built from your backport mods' real 1.14 blocks wherever those mods provide them.

## Before you update an existing world

*   **Nothing has been removed.** Your settings carry over, and the new ones are added to your config automatically.
*   **Village changes only apply to new chunks.** Villages that have already generated keep the blocks they were built with, so in a pregenerated world only the land beyond the pregenerated area gets the new villages.
*   **Farmers now farm only during work hours, and only the fields around their composter.** A field with no farmer's composter nearby will no longer be tended. Put a composter beside it, or turn off `jobs.modernFarming` to farm the old way.
*   **If you have Future MC, this mod's smithing table now replaces Future MC's**, just as the other shared blocks already did. Future MC's table loses its crafting recipe and its JEI page, but any you have already placed keep working. If you would rather keep Future MC's, set `compat.futureMcDuplicates` to `KEEP_FUTURE_MC`.

## New: netherite upgrades at the smithing table

With a netherite mod installed, the smithing table works as it does in 1.16: put in a diamond item and a netherite ingot, and take out the netherite version. Enchantments, names and damage all carry over, which none of the netherite mods' own tables or crafting recipes manage. Without a netherite mod the table does nothing, just as in 1.14.

*   **Works with most 1.12.2 netherite mods.** The table reads the upgrades straight from each mod: Future MC, Spartan Netherite (every Spartan weapon and shield), Netherite Mod, Complete 1.16 Nether Overhaul, Netherite Stuff, NetherUpdate, NetheritePlusMOD [ESP], Netherites But 1.12.2, Nether+ and others.
*   **Running two netherite mods?** Each mod's ingot makes that mod's own gear.
*   **JEI** shows every upgrade.
*   **Every netherite ingot gets the `ingotNetherite` ore dictionary name**, so mods that look for it, such as Spartan Netherite, work with whichever netherite mod you have.
*   **It replaces Future MC's smithing table** when both are installed. Because it reads Future MC's smithing recipes, including any added with CraftTweaker, it does everything Future MC's table did and handles every other netherite mod's gear as well.
*   **Settings** in `workstations`: `netheriteUpgrades` turns the feature off. `netheriteNameMatching` stops the table guessing upgrades from item names, which it does for mods that only craft their netherite gear from scratch. `removeCraftingUpgrades` removes the netherite mods' own crafting-grid upgrades, making the table the only way to upgrade, as in 1.16.

## New: farmers work like modern farmers

*   **They tend their own fields, during work hours.** A farmer works the farmland within 10 blocks of its composter from 08:00 to 15:00, as in 1.14. In 1.12.2 it farmed whatever field it happened to be near, all night included. Set the reach with `jobs.farmRadius`.
*   **They keep harvesting** as long as they have room for the crop, instead of stopping once they have "enough" food like 1.12.2's farmers did, and they look for work more often.
*   **They bake bread** whenever they visit their composter, turning 3 wheat into 1 loaf, up to 3 loaves a visit. Bread is the food villagers share to breed, so a village with a farmer and a wheat field keeps growing.
*   **They compost their spare seeds**, as farmers have done since 1.16. A farmer keeps 10 of each kind for replanting and puts the rest in its composter. Once the composter is full, the farmer empties it on its next visit and leaves the bone meal on the ground for you.
*   **Settings** in `jobs`: `modernFarming` (turn it off to farm as in 1.12.2), `farmRadius`, `farmersBakeBread` and `farmersCompost`.

## New: a villager's day

*   **Villagers gather at the bell** from 15:00 to 17:00, as in 1.14. Adults head for the nearest bell within 48 blocks, mill about around it and stop to chat with each other, which is when gossip spreads fastest. With no bell in reach, they simply carry on with their day.
*   **Babies play** from 09:00 to 12:00 and again from 16:00 to 18:00, chasing each other in games of tag and bouncing on beds. This replaces 1.12.2's version, where babies trailed after each other at any hour.
*   **Villagers shake their heads** when you try to trade with one that has nothing to sell (a baby, a nitwit or a villager without a job), just as they do in 1.14. Every click gets its own "no", so holding the button down keeps them shaking.
*   You can turn off the gathering and the playing in `homes` (`gatherAtBell`, `babiesPlay`), and change the gathering hours there too.

## New: map locking

Put a map and a glass pane in the cartography table to get a locked copy, as in 1.14. The copy never changes again, however much the land it shows does, while the original keeps updating. Copies of a locked map are locked too, a locked map can't be zoomed or locked again, and it says "Locked" under its name. The only difference from 1.14 is that a locked map doesn't show your own position marker.

## New: hanging bells

The bell now has all four of 1.14's mountings: on a floor stand, hanging from a ceiling, hanging from a wall, or slung between two walls. As in 1.14, the face you place it against decides which, and 1.14 villages hang their bells from beams wherever 1.14 does. Bells you have already placed stay on their stands.

## New: crop sounds

Crops now sound the way they do in 1.14. Planting seeds makes a soft rustle where 1.12.2 was silent, and breaking or harvesting a crop gives a crunch instead of the grass sound. Farmer villagers make the planting sound too as they replant their fields.

Wheat, carrots, potatoes, beetroot and other mods' crops all get the new sounds. Pumpkin and melon stems keep their wooden sound but gain the planting sound, and nether wart gets sounds of its own. Turn them off with `sounds.cropSounds`.

## New: 1.14 villages in Open Terrain Generator worlds (RLCraft Dregora)

Worlds made by **Open Terrain Generator**, RLCraft Dregora's among them, now get 1.14 villages. Until now this mod couldn't plan a village on OTG's terrain, so those worlds were left with 1.12.2's villages.

*   Villages appear wherever the world's own settings put them, with the same spacing and only in the biomes its preset allows. Each biome's village style becomes the matching 1.14 type: sandstone becomes desert, savanna stays savanna, taiga stays taiga, and wood becomes plains. In snowy biomes, wood and taiga villages become 1.14's snowy villages.
*   They are planned on OTG's real terrain, so houses sit properly on its hills.
*   `/locate village_plains` and the other village types work in these worlds.
*   Villages already in your world stay as they are, and anywhere a 1.14 village can't be laid out, OTG builds its own village instead.
*   Waystones, Recurrent Complex, Bountiful and other mods' village buildings join these villages just as they do anywhere else.
*   Dregora's own structures don't know where villages are, so now and then one may overlap a village, as they already can with OTG's villages.
*   With `villages.style` set to `LEGACY`, OTG worlds keep OTG's own villages.

## New: Caves & Cliffs worlds

1.14 villages now generate properly in the 1.18-style worlds of **Farmer's Future Delight** (its Caves & Cliffs world type) and **Caves Not Cliffs [Backported]**. Villages are planned on these worlds' real 1.18 terrain rather than on 1.12.2's, so houses sit on the ground across their hills and mountains. Villages can now be built anywhere from y -64 to 319, and `/locate village_plains` and the rest work here too.

Villagers in these worlds also find workstations, beds and bells below y 0 at their real height. Before, the mod looked for them in the wrong place.

In any other world generator that builds its own terrain, which this mod can't plan villages on, villages now stay 1.12.2's (with their workstations) instead of generating at the wrong height.

## New: other mods' village buildings in 1.14 villages

Mods that add buildings to villages now add them to 1.14 villages as well. Until now a 1.14 village was built only from 1.14's own buildings, so in RLCraft it had no waystone and none of RLCraft's village houses.

*   **Waystones:** the village waystone goes by the town centre, near the bell, whenever Waystones rolls one for that village. RLCraft sets that to every village.
*   **Recurrent Complex**, and through it **RLCraft's** village buildings: the inn, library, blacksmith, farmhouse, vineyard, Three Towers and the rest, along with Recurrent Complex's own buildings such as its marketplace.
*   **Any other mod** that adds village buildings the standard way.

These buildings go on free plots beside the streets, but only where the ground is dry, fairly level and close to the street's height. A village gets about as many as a 1.12.2 village of the same size would, and never more than 10 (the waystone doesn't count towards that). Each one is the mod's own building, built exactly as it would be in a 1.12.2 village, chests, loot and villagers included.

Settings in `villages`: `modPieces` turns the feature off, `maxModPieces` sets the most a village can get, `waystoneNearBell` lets the waystone go anywhere in the village, and `modPieceExclusions` leaves out a whole mod (`reccomplex`) or a single building (`reccomplex:P_VillageInn`). The first time a 1.14 village generates, the log lists every building on offer under those names.

## New: real 1.14 blocks from your backport mods

1.14 villages are now built with the real 1.14 block whenever a backport mod you have installed adds it, instead of 1.12.2's closest match. Without these mods, villages look exactly as they did before.

*   **Future MC:** stripped logs and wood, smooth sandstone, smooth stone, blue ice, sandstone and diorite walls, spruce trapdoors and signs, and campfires.
*   **Quark:** smooth sandstone slabs, diorite and granite stairs and slabs, sandstone walls, and spruce, jungle and acacia trapdoors, buttons and pressure plates.
*   **Oceanic Expanse:** blue ice, which is also mixed into the ice piles beside snowy villages as in 1.14, and sea pickles.
*   **Farmer's Future Delight:** stripped logs and wood.

With Oceanic Expanse installed, Future MC leaves out its stripped logs and blue ice unless you turn off the `Oceanic Expanse` option in Future MC's own config. Villages then take their stripped logs from Farmer's Future Delight if you have it, and their blue ice from Oceanic Expanse.

Turn off `villages.backportBlocks` to keep 1.12.2's blocks even with these mods installed.

## Fixed

*   **RLCraft, and other packs that run Recurrent Complex with its decoration hacks on, still generated 1.12.2 villages.** Recurrent Complex wrapped the village generator before this mod could replace it, so the replacement never happened. These packs now get 1.14 villages, and `/locate` finds every village type. In a world whose generator builds terrain this mod can't plan villages on, `/locate` now tells you so and names the generator.
*   **Villagers were missing their hats.** 1.14's outfits draw every hat, from the farmer's straw hat to the desert villager's head wrap, on a part of the head the 1.12.2 villager model doesn't have, so none of them ever showed. Villagers now wear them, and a profession's hat covers the biome's as in 1.14, so a snowy farmer wears just the straw hat rather than both.
*   **1.14 villages didn't show up on Antique Atlas maps**, because the atlas only recognises 1.12.2's village buildings. 1.14 villages are now drawn with the closest atlas tile for each building (houses, the library, smithy, church, fields, the town centre's well and the lamp posts), and get a village marker when the atlas is set to add them. Villages you have already found appear the next time you load the world.
*   **A villager could turn invisible, leaving only its shadow,** if its 1.14 skin failed to load. It now falls back to its 1.12.2 skin instead. The game also used to retry on every frame when this happened, which could fill the log with thousands of warnings a minute; it now tries just once.
*   **JEI Villagers showed 1.12.2's trades** instead of the 1.14 trades villagers actually sell. It now lists the 1.14 trades, plus any from backport mods you have installed. With `trades.table` set to `LEGACY` it lists 1.12.2's trades, as before.

## Settings

All new, and added to your config automatically:

*   `homes`: `gatherAtBell`, `gatherStartTime`, `gatherEndTime`, `babiesPlay`
*   `villages`: `backportBlocks`, `modPieces`, `maxModPieces`, `waystoneNearBell`, `modPieceExclusions`
*   `jobs`: `modernFarming`, `farmRadius`, `farmersBakeBread`, `farmersCompost`
*   `workstations`: `netheriteUpgrades`, `netheriteNameMatching`, `removeCraftingUpgrades`
*   `sounds` (a new section): `cropSounds`

`compat.futureMcDuplicates` now covers the smithing table too.

***

# 2.0.0

The biggest update yet: 1.14's villages and 1.14's trades. The mod still needs nothing else to run, and now also uses items from other backport mods when you have them.

Shoutout to **Rxpev** and their **VillagerFix** mod, which got Village Names' 1.14 houses building with this mod's workstations and pushed me to stop being lazy and finish structure generation properly. 2.0.0's villages exist because of it.

## Before you update an existing world

*   **New chunks get the new villages.** Villages you have already found keep their layout. Where explored land meets new land you can see an old 1.12.2-style village next to a new one.
*   **The market stalls, workshops and bell squares are gone** from new villages. Ones already built stay where they are.
*   **Villagers nobody has traded with get the new trades.** Villagers you have traded with, even once, keep every trade they have. See "Trades" below.
*   **Two config sections are gone:** `structures`, and the workshop and stall settings in `villages`. Minecraft does not remove old settings from your config file; they do nothing now and can be deleted.

## New: 1.14's villages

Villages generate the way they do in 1.14, from 1.14's own buildings: plains, desert, savanna, taiga and snowy villages, laid out street by street, with farms, animal pens, lamp posts, decorations, an iron golem and the occasional abandoned zombie village.

*   **Workstations are inside the houses**, where 1.14 puts them. A village has its librarian's lectern, its butcher's smoker and its armourer's blast furnace from the start, alongside the beds, the bell at the meeting point and unemployed villagers waiting for work.
*   **Snowy villages** now generate on snowy tundra.
*   **Village chests** have 1.14's loot.
*   **New block: the lantern.** Eight iron nuggets around a torch. It stands on a block or hangs under one, and it lights most of a 1.14 village.
*   Blocks 1.12.2 does not have are swapped for the closest it does: stripped logs become logs, blue ice becomes packed ice, smooth sandstone becomes cut sandstone.
*   Houses sit on the ground they were planned for. The terrain inside each building is cleared and the ground under it filled in, so a house on a slope is neither half buried nor standing on stilts.
*   Streets that cross water become plank bridges, in the village's own wood, as in the real game.
*   1.14 villages need the vanilla overworld generator to plan them. In a world made by another generator, or with `villages.style` set to `LEGACY`, villages are 1.12.2's, and each building gets the workstation its trade needs: a lectern in the library, a smoker in the butcher's, a blast furnace, smithing table and grindstone at the blacksmith's, a brewing stand in the church, a composter on each field, a bell at the well and one at random in each small house. Turn that off with `legacyHouseWorkstations`.

## New: 1.14's trades

Villagers sell what they sell in 1.14, at 1.14's prices and stock. Each level offers two trades drawn at random from that level's list, so two farmers can sell different things.

*   A few trades follow current Minecraft rather than 1.14: the librarian's bookshelf is 9 emeralds, and a master librarian sells candles when a mod adds them.
*   **Villagers you have traded with keep their trades.** Anything they have unlocked stays, and they keep unlocking 1.12.2's trades as they level. Villagers nobody has traded with switch to the new trades the next time they are near a player. Turn that off with `trades.rebuildUntradedVillagers`.
*   Prefer the old trades? Set `trades.table` to `LEGACY`.
*   The cartographer sells one of the four banner patterns this mod adds in place of 1.14's globe pattern, which 1.12.2 has no pattern for.

## New: works with other backports

Nothing here needs another mod. When one is installed, its items join the trades where the real game has them, and leave nothing behind when it is not:

| Mod | Adds to trades |
| --- | --- |
| Future MC | Crossbow, campfire, bucket of cod, sweet berries, suspicious stew |
| Crossbows Backport | Crossbow |
| Oceanic Expanse | Turtle scute, dried kelp block |
| Deeper Depths | Red and yellow candles, for the master librarian |

Without them the trade is simply left out of the draw, and the master librarian sells a name tag, as in 1.14. A butcher and leatherworker can come up short at the top levels without these mods.

A mod that brings its own village generator, such as Village Names, keeps its villages.

## New: `/locate` answers like 1.15

`/locate` says how far away the structure is and gives the coordinates in green brackets. Click them to put a teleport command in your chat box. It also finds one kind of village at a time: `/locate village_plains`, `village_desert`, `village_savanna`, `village_taiga` or `village_snowy`, including villages not generated yet. Everything `/locate` found before it still finds, and names are no longer case-sensitive.

## Removed

*   **Workstation swapping.** The mod no longer turns crafting tables, furnaces, cauldrons, bookshelves or trapdoor-framed tables into workstations, or drops composters into crop fields. 1.14 never did this: its workstations come from the village buildings, and now so do this mod's.
*   **Market stalls, workshops and bell squares** in new villages. 1.14 never had them.

## Settings

New: `trades` (`table`, `rebuildUntradedVillagers`) and `villages` (`style`, `legacyHouseWorkstations`). Both are written to your config automatically. Gone: the `structures` section and the workshop, stall and meeting point settings in `villages`.

***

# 1.2.1

**New:** villagers hold up their trades, as in the real game. Hold an item a villager buys while standing near it, and it holds up what it would give you for it: hold wheat near a farmer that buys wheat and it holds up an emerald. If it has several trades for that item it cycles through them. They don't do it while asleep or trading, and the item they hold up is never dropped. Can be turned off with `showTradeItems` in the config.

Fixed villagers crowding into corners during the day. A villager whose workstation was on the other side of a fence, for example one kept in a pen, walked to the nearest point it could reach and stood pressed against the fence all day, with every villager whose workstation was the same way piling in beside it. Now a villager only sets off if it can actually get to its workstation or bed. If it can't for a full minute, it gives that workstation or bed up, as in the real game. One you've traded with keeps its job but can't restock until it can reach a workstation again.

Fixed villagers getting stuck inside composters. A farmer could step onto the rim of its composter, drop into the hole and never find a way out. Composters are now solid to villagers, and any villager already stuck in one is lifted back out. The inside of a composter also fills up as compost builds, as in the real game, so a full composter is solid to stand on.

Fixed villagers vanishing when they took a farmer's job at a composter. Giving the new farmer its farming task crashed that villager's update, and some packs (RLCraft's RLTweaker among them) deal with that by deleting the villager. Without such a mod it would crash the game instead.

Fixed baby villagers sometimes growing up as nitwits. About one bred villager in six became a nitwit, which never happens in the real game. Bred villagers now always grow up unemployed and look for a job.

Nitwits that came from breeding in 1.2.0 stay nitwits. To turn one back into a normal villager, stand next to it and run `/entitydata @e[type=villager,c=1] {ForgeData:{villagerbackport:{Nitwit:0b}}}` (needs cheats or operator).

***

# 1.2.0

A big one. Villagers now behave much more like they do in current Minecraft (Java Edition): how they price things, how they talk about you, when they work and sleep, and when they call iron golems. It's grouped by topic below.

**Check your config:** a few settings now have new defaults. Minecraft doesn't change values already saved in your config file, so if you're updating an existing install, see "Settings" at the bottom.

## Prices

- **Killing a villager no longer makes the whole village overcharge you forever.** One killing used to add about 25 emeralds to every trade in the village. Now a villager who saw it charges about 7 more on everyday trades, and the story gets weaker each time it's passed on.
- **Enchanted book trades now change price in emeralds, not books.** A librarian who disliked you used to ask for 8 books instead of 1. The trade screen also shows these trades emeralds first.
- **Expensive items react more strongly to demand and reputation.** Enchanted books, maps, enchanted gear, iron and chainmail armour, shields, stone tools, the iron axe, the diamond hoe, saddles and leather armour all count as premium trades: four times as sensitive to demand and to how a villager feels about you. The enchanted iron sword and enchanted bow are ordinary trades.
- **Prices build up over several days.** A trade you sell out every day gets a little dearer each day, and one you leave alone drifts back down. Before, prices quietly reset every morning.
- **New trades take a few days of heavy use before their price first goes up**, as in the real game. Trades already in your world aren't affected.
- **No price ceiling from demand.** A trade in constant demand keeps getting dearer until it costs a full stack. A 1-emerald trade used to stop at 7.
- **Curing the same villager again no longer stacks discounts.** Both cure bonuses are capped at what one cure gives.

## Stock and restocking

- **Each trade has its own stock.** Enchanted tools, weapons and armour sell 3 times between restocks instead of 12. Villagers buy everyday goods like crops and wool 16 times, and valuable things like iron, gold and diamonds 12 times. Bread and wool sell 16 times. Trades added by other mods keep the old rule.
- **Villagers only restock at their own workstation.** A librarian with no lectern it can reach no longer restocks at all.
- **No more free restock at dawn.** A villager's day now starts when work hours begin.
- **A trade selling out shows green sparkles** on the villager, so you can see which one just ran dry.

## Reputation and gossip

- **Only villagers who actually see a killing hold it against you.** Villagers behind a wall or on another floor no longer count as witnesses.
- **Gossip travels like it does in the real game.** Villagers only swap news when they walk right up to each other, at most once a minute each, and only part of what they know gets passed on. Before, a grudge reached the whole village within minutes.
- **Every trade earns you a little goodwill** (+4 each, up to about 1 emerald off), including trades that give the villager no experience and trades where you closed the screen straight away. Buying several at once with shift-click counts each one.
- **Old opinions fade out a day sooner.**
- **New command: `/forgive`.** Look at a villager and run it to clear its grudges against you, or use `/forgive 32` for every villager within 32 blocks. Operators only. It only clears grudges, so discounts you've earned stay. Handy for villages stuck with the old inflated prices.

## Trading screen

- **Clicking a trade always loads it.** If another trade's items were already in the slots, clicking a different trade used to do nothing. Now it swaps them.
- **Clicking a trade uses renamed and enchanted items too,** the same items the villager would accept anyway.
- **Leftover items go back into your inventory when you close the screen** instead of being dropped on the ground. Anything that doesn't fit still drops.
- **Changed prices are drawn like the real game:** the item once with the old price crossed out, and the new price beside it. This now also shows for discounts, not just price rises.

## Levels

- **At most one level-up per trading session.** A villager with enough experience for two levels takes one, and the next waits until you trade with it again.
- **Trading gives you 3 to 6 experience per trade, plus 5 on the trade that levels the villager up**, as in the real game. The extra 5 used to come at random.

## Jobs and workstations

- **Villagers go to work every day.** During work hours each villager with a job keeps going back to its workstation and using it, with its work sound, whether or not it has anything to restock.
- **Taking a job works like the real game.** An unemployed villager that spots a free workstation heads for it and takes the job once it's within 2 blocks, with green sparkles. If it can't get there within a minute it gives up and shows angry particles. Villagers don't look for work while asleep.
- **Villagers look for workstations and beds up to 48 blocks away in every direction.** A workstation two floors up or in a basement used to go unnoticed.
- **A villager that can't reach its workstation or bed for a full minute gives it up** and looks for another, even one it has used before. It never used to give up a bed at all.
- **Only farmers farm.** Fishermen, shepherds and fletchers used to tend crops too, and a villager that became a farmer never started farming until you reloaded the world.

## Sleeping and beds

- **Sleeping villagers have a small hitbox on the pillow.** You can no longer hit them by punching two blocks above the bed, a block placed there no longer suffocates them, and a villager under a low ceiling is no longer drawn pitch black.
- **You can wake a sleeping villager** by right-clicking it or its bed. It gets up, and lies back down a couple of seconds later if the bed is still free. Clicking a sleeping villager no longer opens trading.
- **To take a villager's bed, wake it and sleep in the bed yourself.** A villager won't lie down in a bed someone else is in, and one that finds someone else sleeping in its bed goes and finds another.
- **Villagers without a bed wait indoors at night**, under a roof, until morning. If one spots a free bed, it goes and claims it.
- **Villagers wake up at dawn** (a moment earlier than before) and **wake up when attacked**, or when a zombie or illager comes into sight. They go back to bed once the danger has passed.

## Running away

- **Villagers run when they're hurt.** Hitting a villager, or anything else hurting it, makes it run away from whoever did it. It used to just keep walking.
- **Villagers run from the right monsters at the right distances:** zombies, husks and zombie villagers within 8 blocks, vindicators within 10, evokers and illusioners within 12, vexes within 8, and only ones they can see. Zombie pigmen no longer scare them.

## Breeding

- **Villagers need food to breed, the way the real game counts it.** Bread is worth 4, and carrots, potatoes and beetroot 1 each. A villager carrying 12 or more in total is willing, in any mix. Before, it took a single stack of 3 bread or 12 carrots or potatoes, beetroot didn't count, and trading could make a villager willing with no food at all.
- **Breeding uses up the food even if it fails**, and a failed attempt shows angry particles.
- **Both parents need beds of their own, and the baby needs a free bed the parents can walk to**, within 48 blocks and with two empty blocks above it. The baby gets that bed. Walled-off or blocked beds no longer count.
- **Villagers with jobs don't breed during work hours.** Nitwits and unemployed villagers can breed any time they're awake.

## Iron golems

- **Villages no longer fill up with iron golems.** Villagers won't call for a golem while one is within 16 blocks. A village could get a new one every 30 seconds before.
- **Villagers only panic at real threats:** zombies (including husks and zombie villagers) and illagers they can actually see, or being hurt. Any monster within 8 blocks used to count, even one in a cave under the floor.
- **Any grown villager that has slept in the last 20 minutes counts towards an iron golem**, as in the real game. A job, or having been to work, is no longer needed, so nitwits and unemployed villagers help too.
- **Villagers chatting can call a golem** even when none of them has an opinion about anyone.

## Nitwits

- **Nitwits are back.** A villager that arrives as a nitwit, from village generation or from curing a zombie villager that was one, stays one. It wears the green robe, never takes a job, never counts towards an iron golem, and has no level gem on its belt. Before, nitwits were treated as unemployed and took the first workstation they found.
- **Villagers from spawn eggs, or from `/summon` without data, always start unemployed**, and babies are always born unemployed.
- Villagers already in your world aren't changed.

## Small touches

- **Villager clothing follows the real game's biome list for vanilla biomes.** The Nether and stony shores now give plains clothes. Modded biomes still pick the closest look from their temperature and rainfall, since the real game would make them all plains.
- **Right-clicking a villager with nothing to trade gets a "no"** — nitwits, unemployed villagers and babies. Nothing happened before.
- **A witch made by lightning striking a villager no longer despawns.**
- **A zombie villager you had traded with before it was zombified no longer despawns**, so it's still there for you to cure.

## Bug fixes

- **Items no longer vanish when you switch trades with a nearly full inventory.** Whatever didn't fit back in your inventory used to be deleted. It now stays in the trade slot.
- **Baby villagers no longer vanish.** The "a baby needs a free bed" rule was being applied to every baby villager, so spawn eggs, cured baby zombie villagers and babies in a chunk you came back to could fail to appear. It now only applies to births.

## Settings

New defaults. An existing config file keeps its old values, so change these by hand if you want the new behaviour:

- `strictJobSite` is now `true`: villagers only restock at their own workstation. Set it to `false` if your world has villages missing their workstation blocks.
- `maxDemand` is now `0`, meaning no ceiling (was `128`).
- `gossipRange` is now `2.24` (was `8`).
- `sleepEndTime` is now `10` (was `100`).

Removed: `searchHeight` and `abandonUnreachedTicks` no longer do anything and can be deleted from your config.

***

# 1.1.2

Breaking a bed with a villager asleep in it now wakes the villager up. Before, it stayed lying down in mid-air, turned the wrong way, and never got up.

***

# 1.1.1

Fixes 1.1.0 crashing the game on startup. The 1.1.0 file was built wrong and would not load outside my own test setup. Nothing else has changed, so everything listed under 1.1.0 is in this one.

***

# 1.1.0

Works with Future MC now. Before, installing both gave you two of every workstation, and villagers ignored Future MC's.

Villagers take jobs at either mod's workstations, so a fisherman will use whichever barrel is closer.

Only one of each shows up in JEI and crafting. My blocks are the ones kept by default. To keep Future MC's instead, set `futureMcDuplicates` to `KEEP_FUTURE_MC` in the config, or `SHOW_BOTH` to hide nothing. Anything you already placed keeps working either way.

Future MC's smithing table is always the one kept, since it does netherite upgrades and mine is decorative.

You can also give a profession more than one workstation. Add an extra `modid:block=career` line under `workstations` for each block.

***

# 1.0.2

Fixes this mod's language file never loading, which showed up as raw keys on screen: block names reading `tile.villagerbackport.stonecutter.name`, a barrel titled `container.villagerbackport.barrel`, and in 1.0.1 the creative tab labelled `itemGroup.villagerbackport`.

The jar shipped without a `pack.mcmeta`, so Forge substituted one declaring pack format 2. Minecraft treats a pack of that vintage as old enough to need a compatibility adapter, and that adapter rewrites every `lang/en_us.lang` request to `lang/en_US.lang`, which is not what the file is called. Declaring the format 1.12.2 actually uses stops the rewrite. Textures and models were never affected, which is what made it so hard to spot.

***

# 1.0.1

Everything the mod adds now has a creative tab of its own, the twelve workstations and the four banner patterns together. They were spread between Decorations and Miscellaneous before.

***

# 1.0.0

First release. Backports 1.14.4's village mechanics to 1.12.2, ported from decompiled 1.14.4 sources.

Jobs: twelve working workstation blocks. Lectern, composter, barrel, cartography table, fletching table, smithing table, loom, blast furnace, smoker, grindstone, stonecutter, bell. Villagers claim one they can reach, take its profession, and lose the job if it is broken. The fletching table is a stonecutter for wood, which is this mod's own addition.

Restocking: twice a day, at the workstation, during working hours, on 1.14's timings and with the workstation's own sound.

Prices: supply and demand, at 5% normally and 20% for enchanted books and maps, with the original price struck out on the trade screen.

Reputation: trading, curing, hurting and killing all move a per-player score that decays and spreads between villagers. Curing keeps a discount that never fades.

Zombie villagers: conversion carries the trades, level, experience and gossip across, and curing hands them back. Curing again drops prices again.

Levels: Novice through Master, unlocking trades by level, with 1.14's experience values and 1.14's villager skins and badges.

Beds: claimed as homes, slept in at night, and used for breeding instead of doors. Iron golems spawn on 1.14's conditions, so 1.14 farm designs work.

Villages: workshops, stalls and a meeting point generate in new villages, and existing ones are retrofitted with workstations as you find them.

Screen: 1.14's trade screen, with the scrollable trade list, level badge, experience bar and spacebar refill.

`/village` draws the village's beds and the counts that decide whether it can grow. Everything is configurable in eight categories, and every part of it can be switched off.
