# 1.14 Villager Backport

**All of 1.14 Village & Pillage on 1.12.2: villagers, villages, raids, and everything else the update added.**

Villages generate the way they do in 1.14, from 1.14's own buildings, with the workstations already inside the houses. Villagers take the job that comes with a workstation, sell 1.14's trades and go to work every day, restocking twice a day and charging more for the trades you use most. They remember what you have done to them and gossip about it, sleep in beds at night, breed when they have food and a free bed, run when they are hurt, and call an iron golem when they are frightened. And the pillage half of Village & Pillage: pillagers, ravagers, patrols, outposts, Bad Omen and raids, with Hero of the Village for whoever saves the village. And the rest of the update: the wandering trader, cats, foxes, pandas, bamboo and scaffolding, the campfire, sweet berries, the new flowers and dyes, suspicious stew, and every new slab, stair, wall, sign and wood block.

It is ported from the decompiled 1.14.4 sources, so the village layouts, trade tables, pricing formula, restock timings, gossip weights, experience values, golem conditions, sleep pose, raid waves and outpost layouts are all the ones the real game uses.

**Standalone, and friendly with other backports.** Everything 1.14 added is in this one mod, and it needs no other. Install Future MC, Oceanic Expanse or Deeper Depths alongside it and villagers start trading their items too. Where Future MC adds the same block or item, only one of the two is shown (see Works with other mods). It is needed on both the client and the server.

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, questions and release pings.

![The 1.14 trade screen](https://media.forgecdn.net/attachments/1900/425/villagergui-png.png)

***

## Villages

Villages are 1.14's villages: plains, desert, savanna, taiga and snowy, built from 1.14's own structure files and laid out street by street the way 1.14 lays them out. You will find farms, animal pens, lamp posts, a meeting point with its bell, an iron golem, unemployed villagers waiting for work and, now and then, an abandoned zombie village. Snowy villages generate on snowy tundra, where 1.12.2 never built villages at all.

The workstations sit inside the houses where 1.14 puts them, so a village has its librarian's lectern and its butcher's smoker from the start. As in the real game, workstations come only from the buildings; nothing else in the world is turned into one.

*   Villages are built from the real 1.14 blocks, all of which this mod adds: stripped logs and wood, smooth sandstone, blue ice, the new walls, stairs and slabs, wood trapdoors, buttons, pressure plates and signs, campfires, lanterns and sweet berry bushes. When Future MC, Quark, Oceanic Expanse or Farmer's Future Delight provides one of them, the village uses theirs.
*   Villages have cats from the start, and taiga villages light their campfires.
*   Village chests carry 1.14's loot.
*   Houses sit on the ground they were planned for. The terrain inside each building is cleared and the ground beneath it filled in, so a house on a slope is neither half buried nor standing on stilts.
*   Streets that cross water become plank bridges in the village's own wood, as in the real game.
*   Mods that add buildings to villages add them to 1.14 villages too. In RLCraft that means the waystone by the bell, and RLCraft's inns, libraries, blacksmiths and farmhouses on plots beside the streets.

1.14 villages need a world generator whose terrain they can plan on: vanilla's, superflat, Biomes O' Plenty's, Open Terrain Generator's (RLCraft Dregora), Realistic Terrain Generation's, Farmer's Future Delight's Caves & Cliffs world type, or Caves Not Cliffs. In a world made by any other generator, or with `villages.style` set to `LEGACY`, villages are 1.12.2's, and each building gets the workstation its trade needs: a lectern in the library, a smoker in the butcher's, a blast furnace, smithing table and grindstone at the blacksmith's, a brewing stand in the church, a composter on each field, a bell at the well, and one at random in each small house.

`/locate village_plains`, `village_desert`, `village_savanna`, `village_taiga` or `village_snowy` finds the nearest village of that kind.

***

## Jobs

An unemployed villager looks for a free workstation up to 48 blocks away in any direction, walks over to it and, once within 2 blocks, takes the job in a burst of green sparkles. If it can't get there within a minute, it gives up with angry particles and looks for another. Break the workstation and a villager you have never traded with loses its job; one you have traded with keeps the job and looks for a new workstation.

Villagers with a job go to work every day. During work hours they keep returning to their workstation and using it, with its sound, whether or not they have anything to restock. Farmers are the only ones who farm, and they do it like modern farmers: during work hours, on the fields around their composter, baking their wheat into bread and composting their spare seeds. Crops make 1.14's planting and harvesting sounds, for you and for them.

![The twelve workstations](https://media.forgecdn.net/attachments/1900/426/workstations-png.png)

| Block | Job | What it does |
| --- | --- | --- |
| <strong>Lectern</strong> |Librarian |Holds a book anyone can read, page by page |
| <strong>Composter</strong> |Farmer |Turns crops into bone meal, hopper in the top, out the bottom |
| <strong>Barrel</strong> |Fisherman |A chest that opens with a block on top |
| <strong>Cartography table</strong> |Cartographer |Copies and zooms maps without eight paper, and locks them with a glass pane |
| <strong>Fletching table</strong> |Fletcher |Cuts wood the way a stonecutter cuts stone |
| <strong>Smithing table</strong> |Toolsmith |Upgrades diamond gear to netherite, as in 1.16, when a netherite mod is installed, and runs Future MC's smithing recipes, CraftTweaker's included. Otherwise nothing, as in 1.14 |
| <strong>Loom</strong> |Shepherd |Applies banner patterns for one dye |
| <strong>Blast furnace</strong> |Armorer |Smelts ore and armor at double speed |
| <strong>Smoker</strong> |Butcher |Cooks food at double speed |
| <strong>Grindstone</strong> |Weaponsmith |Disenchants and repairs, returns experience |
| <strong>Stonecutter</strong> |Mason |Cuts stone with no offcuts |
| <strong>Brewing stand / Cauldron</strong> |Cleric / Leatherworker |The two 1.12.2 already had |

The bell isn't a job site. It is the village's meeting point, where villagers gather in the afternoon. Ringing it sends villagers within earshot home and makes any raiders nearby glow through walls, as in 1.14. Like 1.14's, it can stand on a floor stand, hang from a ceiling or a wall, or be slung between two walls. The lantern isn't a job site either: it lights the village, standing on a block or hanging beneath one.

**Nitwits** are their own thing again: the green-robed villager that never takes a job and wears no level badge. They come from village generation and from curing zombie villagers that were nitwits. Villagers from spawn eggs, and babies, always start out unemployed. Try to trade with a nitwit, a baby or a jobless villager and it shakes its head at you, as in 1.14.

***

## Trades and levels

Villagers sell 1.14's trades, at 1.14's prices and stock. Each level offers two trades drawn at random from that level's list, so two farmers can sell different things. A few follow current Minecraft rather than 1.14: the librarian's bookshelf costs 9 emeralds, and a master librarian sells candles when a mod adds them.

| Level | Experience | Badge |
| --- | --- | --- |
| <strong>Novice</strong> |0 |Stone |
| <strong>Apprentice</strong> |10 |Iron |
| <strong>Journeyman</strong> |70 |Gold |
| <strong>Expert</strong> |150 |Emerald |
| <strong>Master</strong> |250 |Diamond |

Trades unlock by level rather than by how many trades you have made. A tier-one trade earns the villager 1 or 2 experience, while a tier-five trade earns 30. A villager levels up at most once per trading session, after you close the screen. You get 3 to 6 experience per trade, plus 5 more on the trade that levels the villager up.

Prefer 1.12.2's own trades? Set `trades.table` to `LEGACY`. Whichever you choose, a villager you have traded with keeps the trades it has and keeps unlocking from the list it started on.

Villagers wear 1.14's skins, so their biome, their profession's outfit and hat, and the badge for their level all show. Other mods' villagers keep their own textures with the rank badge drawn on, or can be dressed in the closest 1.14 outfit with `display.modOutfits`.

![A villager in 1.14's skin](https://media.forgecdn.net/attachments/1900/424/villager-png.png)

***

## The blocks work

Each block does in 1.12.2 what it does in 1.14, apart from the fletching table, which does nothing in 1.14.

| | |
| --- | --- |
| ![Stonecutter](https://media.forgecdn.net/attachments/1900/423/stonecutter-png.png) | ![Fletching table](https://media.forgecdn.net/attachments/1900/418/fletching-png.png) |
| ![Cartography table](https://media.forgecdn.net/attachments/1900/421/map-png.png) | ![Loom](https://media.forgecdn.net/attachments/1900/420/loom-png.png) |

Here the fletching table is a stonecutter for wood. A log becomes planks, and planks become stairs and slabs, without the offcuts a crafting grid charges you. That part is this mod's own addition, not something 1.14 does.

Both saws read the crafting recipes already installed and work out what can be cut from them, so a pack with Chisel or Quark gets its blocks in both. The composter reads the ore dictionary in the same way.

**Locking a map** works as in 1.14: put a map and a glass pane in the cartography table and you get a locked copy that never changes again, however much the land it shows does. The map you put in keeps updating as before, and copies of a locked map are locked too. The one difference from 1.14 is that a locked map doesn't show your own position marker.

Rerolling a librarian means breaking and replacing its lectern until the book is one you want. Each roll is announced in chat as it happens, so you don't have to open the trade screen to check.

![Rolled: Punch II](https://media.forgecdn.net/attachments/1900/419/lecternmessage-png.png)

***

## Restocking

Villagers restock twice a day, at their own workstation, during work hours. A villager that can't get to its workstation doesn't restock. When a trade sells out, green sparkles show you which one ran dry.

Every trade has the stock the real game gives it. Enchanted tools, weapons and armor sell 3 times between restocks, while villagers buy everyday goods like crops and wool 16 times and valuable things like iron, gold and diamonds 12 times. 1.12.2 gives everything 7.

***

## Prices move

Prices respond to demand over several days. A trade you sell out every day gets a little dearer each day, with no ceiling short of a full stack, while a trade you leave alone drifts back down. A new trade takes a few days of heavy use before its price first rises.

Expensive items react four times as strongly. Enchanted books, maps, enchanted gear, iron and chainmail armor, shields, stone tools, saddles and leather armor are all premium trades, as they are in the real game.

The trade screen crosses out the original price and shows the new one beside it, for rises and discounts alike.

![A trade that has gone up in price](https://media.forgecdn.net/attachments/1900/417/demandpricing-png.png)

***

## Reputation

Every villager keeps its own opinion of every player, and that opinion changes prices for that player only.

| Event | Effect |
| --- | --- |
| <strong>Trade</strong> |+4 per trade, up to a cap of 25 |
| <strong>Cure a zombie villager</strong> |+125, and most of it never fades |
| <strong>Hurt a villager</strong> |−25, for that villager |
| <strong>Kill a villager</strong> |−125, for every villager that saw it happen |

Trading a lot takes about an emerald off. Curing a villager takes six off an ordinary trade and twenty-five off an enchanted book. A killing costs you the same amounts the other way, but only with villagers who could actually see it, not ones behind a wall.

Opinions fade day by day and spread through **gossip**. Villagers swap news when they walk up to each other, at most once a minute, and only part of what they know gets passed on, a little weaker each time. A grudge travels through a village over a day or so, not all at once.

***

## Zombie villagers

1.12.2 keeps a zombified villager's profession and nothing else. Its career is re-rolled and its trades are gone, so the librarian you cure sells different books from the one you lost.

Here the whole villager carries across and back. Its trades, level, experience, demand and gossip stay with the zombie and return when it is cured. A zombie villager you had traded with never despawns, so it is still there for you to cure.

Curing is the biggest discount in the mod, and most of it never fades. Curing the same villager again adds nothing more, as in the real game. A row of villagers cured together each get back their own trades, however close together they stand.

In packs that make mobs stronger, a zombie villager that was one of your villagers stays an ordinary zombie villager: never an **Infernal Mobs** elite, a **Scaling Health** blight or a **Champion**. In Dregora, a cured librarian never comes back as a **Sussyberian** or **Mentalberian**. Turn this off with `compat.convertedStayOrdinary`.

***

## A villager's day

Villagers keep 1.14's daily schedule. Work runs from 08:00 to 15:00. From 15:00 to 17:00 adults **gather at the bell**: they head for the nearest bell within 48 blocks, mill about around it and stop to chat with each other, which is when gossip gets round the village fastest. At dusk they go to bed.

**Babies play** from 09:00 to 12:00 and again from 16:00 to 18:00, chasing each other around the village in games of tag and bouncing on beds.

## Beds and sleep

A villager claims a bed up to 48 blocks away and sleeps in it from dusk to dawn, lying in the bed with a small hitbox on the pillow. Right-click a sleeping villager or its bed to wake it, and it lies back down a couple of seconds later if the bed is still free. To take a villager's bed, wake it and sleep there yourself, and it will find another. A villager with no bed waits indoors until morning.

## Breeding

Villagers breed on food and beds, not doors, so modern breeder designs work in 1.12.2.

A villager is willing once the food it carries adds up to 12: bread is worth 4, and carrots, potatoes and beetroot 1 each, in any mix. Both parents need beds of their own, and the baby needs a free bed they can walk to, with two empty blocks above it. Without one, the attempt fails with angry particles and the food is still eaten. Villagers with jobs don't breed during work hours.

## Danger and iron golems

Villagers panic when they are hurt or when they see a zombie, husk, zombie villager, illager, pillager or ravager close by. A panicking villager wakes up if it was asleep, runs from the threat, and goes back to what it was doing once the danger has passed.

A frightened villager calls for an iron golem, which takes three villagers nearby to help. Villagers chatting together can call one too, which takes five. Any adult villager that has slept in the last 20 minutes can help, and none will while a golem is within 16 blocks, so a village keeps the golems it has rather than filling up with them. These are the rules modern golem farms are built around.

***

## Raids

Carry Bad Omen into a village and the village knows: 30 seconds later the horn sounds and a raid begins. 1.14's waves follow - three on easy, five on normal, seven on hard, and one more if the omen was strong - of pillagers, vindicators, evokers, witches and ravagers, some riding the ravagers. A bar at the top of the screen shows the raid gathering, then the raiders' strength, and Victory or Defeat at the end.

*   **The villagers know what to do.** Before the first wave they gather at the bell and ring it; when the raiders arrive they run home and stay there; when it's over they come out and celebrate with fireworks.
*   **The bell gives raiders away.** Ring it with raiders nearby and they glow through the walls for a moment. Villagers within earshot run home whenever it rings.
*   **Hero of the Village** goes to everyone who killed a raider in a raid that was won: 30% off every trade, more for a stronger raid, shown on the trade screen. Villagers come up and throw heroes gifts - bread from the farmer, a book from the librarian, armor from the armorer.
*   **Pillagers** load their crossbow, take aim and fire as they do in 1.14, and sometimes drop it.
*   **The crossbow** is yours too: 1.14's recipe, Quick Charge, Multishot and Piercing, arrows or firework rockets, and the four crossbow advancements. With Future MC or Crossbows Backport installed this mod adds none and uses theirs instead (`compat.crossbow`).
*   **Ravagers** bite hard, throw you back, and charge through leaves. Block a bite with a shield and you may stun one; it roars when it recovers.
*   **Patrols** roam the land from the fifth day: a captain with the ominous banner and a few pillagers. Kill the captain away from a raid and you have Bad Omen.
*   **Pillager outposts** stand in plains, deserts, savannas, taigas and snowy tundra: 1.14's own watchtowers, tents and iron golem cages, with pillagers spawning around them day and night and loot at the top of the tower.
*   **The ominous banner** captains wear is 1.14's, and it's in the creative tab too.
*   **The ominous bottle** is there for those who prefer current Java's rule, where captains drop a bottle rather than giving Bad Omen outright (`raids.captainDropsOminousBottle`).

Already using **Raids Backport**? This mod uses its raids instead of its own, and fixes what went wrong with the two together: our bell reveals its raiders, villagers hide from its raids, and its Hero of the Village discount is counted once and shown on the trade screen.

***

## The rest of 1.14

*   **The wandering trader** turns up now and then with two trader llamas, near you or at the nearest bell. It sells current Java's nine trades, never restocks, drinks invisibility at dusk and milk at dawn, and leaves after 40 minutes. Its llamas spit at whatever attacks it. The `doTraderSpawning` game rule switches it off.
*   **Cats**, in all eleven coats, separate from ocelots as in 1.14. Tame strays with raw fish. Tamed cats sit on chests, furnaces and beds, keep creepers away, and bring you gifts in the morning. Villages have their own cats, and a black cat lives at each witch hut. Ocelots now learn to trust you instead of being tamed, and tamed ocelots in old worlds turn into cats.
*   **Foxes**, red and snowy, in taigas. They sleep by day, hunt chickens and rabbits by night with a high pounce, and carry one item in their mouth. Breed them with sweet berries and the cub trusts you.
*   **Sweet berry bushes** grow wild in taigas. Pick them without breaking the bush, and mind the thorns.
*   **Bamboo**, grown by 1.14's rules, in its own **bamboo jungle**. **Scaffolding** holds itself up as in 1.14: jump to climb it, sneak to climb down. It works with Aqua Acrobatics.
*   **Pandas**, with all seven personalities and 1.14's gene inheritance. They eat bamboo and cake, and cubs sneeze.
*   **The campfire** cooks four foods at once without fuel and burns whatever stands in it. Put a hay bale under it for a signal fire. Fishermen sell it.
*   **Cornflowers, lilies of the valley and wither roses**, which also make **white, black, brown and blue dye**. Anything the Wither kills leaves a wither rose.
*   **Suspicious stew**, **brown mooshrooms**, **leather horse armor**, the **Globe** banner pattern, and **sign text you can dye**.
*   **Every new slab, stair and wall** (42 of them), the **smooth stone** blocks, and **signs in every wood**. The stonecutter cuts all of them.
*   **Stripped logs and wood**, and an axe to strip them. **Trapdoors, buttons and pressure plates in every wood**. **Blue ice** and **sea pickles**.

Each of these has its own config category, and each one switches off.

***

## Works with other mods

Nothing here needs another mod. When one of these is installed, its items join the trades where the real game has them:

| Mod | Adds to trades |
| --- | --- |
| <strong>Future MC</strong> |Bucket of cod |
| <strong>Oceanic Expanse</strong> |Turtle scute, dried kelp block |
| <strong>Deeper Depths</strong> |Red and yellow candles, sold by the master librarian |

Without them, those trades are simply left out of the draw, and the master librarian sells a name tag as in 1.14. Butchers and leatherworkers have fewer trades at the top levels without these mods. The fletcher's crossbow is this mod's own, or Future MC's or Crossbows Backport's when one of those is installed.

**Future MC** adds many of the same blocks and items: the workstations, bamboo and scaffolding, the campfire, sweet berries, the flowers and dyes, suspicious stew, the walls, signs, stripped logs, wood trapdoors and blue ice. Both sets work, so a fisherman will take either barrel and a panda will eat either bamboo, but one of each pair is hidden from the creative menu, JEI and crafting so you aren't shown two of everything. Choose which set to keep with `compat.futureMcDuplicates` (`KEEP_OURS`, `KEEP_FUTURE_MC` or `SHOW_BOTH`); villages are built with whichever set you keep. With this mod's kept, Future MC's pandas stop spawning. That includes the smithing table, and since this mod's reads Future MC's smithing recipes as well as every other netherite mod's, keeping it loses you nothing.

**Quark, Oceanic Expanse and Farmer's Future Delight**: when one of them provides a block 1.14 villages are built from, villages use theirs.

**Switching from Future MC**: `/futuremc convert` turns the Future MC blocks around you into this mod's while Future MC is still installed, keeping every block's facing and contents, and converting the items in chests and inventories nearby. If you just remove Future MC, a world that had it converts itself as it loads instead of losing those blocks.

**Aqua Acrobatics**: scaffolding climbs as it should, since it doesn't touch how players move.

**Netherite mods:** with almost any 1.12.2 netherite mod installed (Future MC, Spartan Netherite, Netherite Mod, Complete 1.16 Nether Overhaul, Netherite Stuff, NetherUpdate, NetheritePlusMOD, Netherites But 1.12.2, Nether+ and more), the smithing table upgrades diamond gear to that mod's netherite gear, keeping its enchantments and damage. Each mod's ingot makes that mod's own gear, and every netherite ingot gets the `ingotNetherite` ore name.

**Open Terrain Generator**, and with it **RLCraft Dregora**, gets 1.14 villages planned on its real terrain, in the biomes and at the spacing its preset sets.

**Biomes O' Plenty**'s world type gets 1.14 villages planned on its real terrain, styled by each biome's tags.

**Realistic Terrain Generation**'s world type, original or **Realistic Terrain Generation Unofficial**, gets 1.14 villages planned on its real terrain too.

**Farmer's Future Delight's** Caves & Cliffs world type and **Caves Not Cliffs** both get 1.14 villages built on their 1.18-style terrain, anywhere from y -64 to 319.

**Waystones, Recurrent Complex and RLCraft's village buildings** appear in 1.14 villages too: the waystone by the bell, and the mods' houses, inns and libraries on plots beside the streets. Any mod that adds village buildings the standard way is included, and `villages.modPieceExclusions` leaves out any you don't want.

**Antique Atlas** draws 1.14 villages on its maps, building by building.

**Village Names**, and any other mod that brings its own village generator, keep their own villages.

**JEI Villagers** lists the 1.14 trades villagers actually sell, including any from the backport mods above.

**Raids Backport** runs the raids when it is installed; see Raids above. **Deeper Depths'** Bad Omen and ominous bottles start this mod's raids too. Pillagers and ravagers from any mod count as raiders: they glow when the bell rings and villagers run from them.

***

## Commands

*   `/locate <structure>` gives the distance and the coordinates in green brackets; click them to put a teleport command in your chat box. It takes every structure 1.12.2 can find, plus `village_plains`, `village_desert`, `village_savanna`, `village_taiga` and `village_snowy`.
*   `/village [radius]` draws the village. Every bed gets a box, green where a villager has claimed it and red where it is going spare, along with the counts that decide whether the village can grow.
*   `/forgive` clears a villager's grudges against you, and `/forgive 32` does the same for every villager within 32 blocks. Discounts you have earned stay. Operators only.
*   `/librarian <enchantment> [level] [villager level] [price] [cured]` spawns a librarian selling that enchanted book, for putting back one you lost: `/librarian strengthened_vitality 3`. Add `cured` to get the cure discount with it. Operators only.
*   `/discount` gives you the cure discount from the villager you're looking at, and `/discount 8` from every villager within 8 blocks. Operators only.
*   `/raid start [level]` starts a raid on the village you're in, `/raid stop` ends it, `/raid status` lists the raids under way, and `/raid patrol` sends a patrol out near you. Operators only. The `disableRaids` game rule turns raids off in a world.
*   `/locate pillager_outpost` finds the nearest outpost.
*   `/futuremc preview [radius]` and `/futuremc convert [radius]` turn Future MC's blocks and items around you into this mod's, keeping their facing and contents. `/futuremc check` lists anything that wouldn't carry across exactly. Operators only.
*   `/wanderingtrader status` shows the trader countdown and chance, `/wanderingtrader spawn` sends one to you now, and `/wanderingtrader timer` sets the countdown. Operators only.

***

## What is not here

*   Allays, which came to outposts later
*   What 1.13 and 1.15 added: swimming, the ocean content, phantoms, and bees

***

## Config

Everything lives in `config/villagerbackport.cfg`, or the in-game config screen: nineteen categories, with every setting documented in the file.

![The config screen](https://media.forgecdn.net/attachments/1900/416/config-png.png)

*   `villages`: 1.14's villages or 1.12.2's, real 1.14 blocks from backport mods, other mods' buildings, and workstations for 1.12.2's houses
*   `trades`: 1.14's trades or 1.12.2's, and whether untraded villagers switch over
*   `restock`: how many per day, the gap between them, working hours, sounds
*   `pricing`: demand multipliers, gossip, stock, experience, level gating
*   `jobs`: search radius, working distance, whether villagers must be at their workstation to restock, the job-to-block map, modern farming, and farming with mobGriefing off
*   `workstations`: what each block accepts and produces, extra recipes, compostables, netherite upgrades
*   `homes`: beds, sleeping, breeding, golems, gathering at the bell, babies playing
*   `raids`: the bell and raiders, hiding, the hero discount and gifts, Raid Omen, wave counts, which mob plays each raider, crossbows, patrols, outposts
*   `display`: trade screen, level badges, villager skins, the refill key
*   `compat`: Future MC's duplicate blocks, whose raids there are (this mod's or Raids Backport's), and whose crossbow
*   `sounds`: 1.14's crop sounds
*   `trader`, `cats`, `foxes`, `bamboo`, `campfire`, `flora`, `building`, `villageBlocks`: one for each of the rest of 1.14's content, each with an `enabled` switch

Every part of it switches off. Turn off `homes` and villagers ignore beds; turn off `jobs` and the workstations are just decoration; set `compat.raids` to `NONE` and there are no raids.

***

## Things to know

Existing worlds work. Villages generate the new way in chunks you haven't explored yet, so where old land meets new you may see a 1.12.2 village beside a 1.14 one. Villagers already in the world take jobs the first time they find a workstation. Villages that generated before this mod was installed have no workstations of their own, so place some and the villagers will find them.

Updating from 1.x: villagers you have traded with keep every trade they have, while ones nobody has traded with switch to 1.14's trades. The old `structures` section and the stall and workshop settings in your config do nothing now and can be deleted.

Villagers only farm, replant and pick up food and seeds while the `mobGriefing` gamerule is on, as in vanilla. If yours is off to stop creeper damage, turn on `jobs.ignoreMobGriefing` and they'll farm and pick up food anyway, while still not trampling farmland.

If your world has villages missing their workstation blocks, set `strictJobSite` to `false` and villagers will restock without one.

Patrols and outposts, like villages, only appear in chunks you haven't explored yet; raids work anywhere there is a village. The same goes for the new flowers, sweet berry bushes, bamboo, bamboo jungles, and foxes and pandas that spawn with the world.

As in 1.14, the trapdoor, wooden button, wooden pressure plate and sign take oak planks only, and a few slab and stair recipes take only one kind of stone, since the other woods and stones make their own. Turn off `building.vanillaRecipeChanges` to keep 1.12.2's recipes for the sign, slabs, stairs and nether brick fence.

Spacebar refills the trade. If it doesn't, another mod has claimed the key (Inventory Tweaks binds it by default). The key is configurable.

***

## Requirements

*   Minecraft 1.12.2, Forge 14.23.5.2847 or newer
*   No dependencies
*   Required on the client and the server

## Install

Drop the jar in `mods` on both sides. Delete any older version first, or the two copies will crash on startup with a duplicate mod id.

***

Also by me: [Enchantment Recipes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-enchantment-recipes), which lets you craft enchanted books from mod materials, and [Villager Tomes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-villager-tomes), which lets you teach a librarian the books you find.

Not affiliated with Mojang. Licensed MIT.

Source: [https://github.com/ExiledRadio/VillagerBackport](https://github.com/ExiledRadio/VillagerBackport)
