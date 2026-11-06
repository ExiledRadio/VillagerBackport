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
