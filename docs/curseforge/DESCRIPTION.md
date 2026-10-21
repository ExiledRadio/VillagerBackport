# 1.14 Villager Backport

**Modern Minecraft's villagers and villages, on 1.12.2.**

Villages generate the way they do in 1.14, from 1.14's own buildings, with the workstations already inside the houses. Villagers take the job that comes with a workstation, sell 1.14's trades and go to work every day, restocking twice a day and charging more for the trades you use most. They remember what you have done to them and gossip about it, sleep in beds at night, breed when they have food and a free bed, run when they are hurt, and call an iron golem when they are frightened.

It is ported from the decompiled 1.14.4 sources, so the village layouts, trade tables, pricing formula, restock timings, gossip weights, experience values, golem conditions and sleep pose are all the ones the real game uses.

**Standalone, and friendly with other backports.** It comes with twelve working workstation blocks, the bell and the lantern, and needs no other mod. Install Future MC, Oceanic Expanse, Deeper Depths or Crossbows Backport alongside it and villagers start trading their items too. It is needed on both the client and the server.

**Recommended companions** (none are required): **Future MC** adds the campfire, crossbow and sweet berries that 1.14 villagers trade, along with the real 1.14 building blocks for villages. **Quark**, **Oceanic Expanse** and **Farmer's Future Delight** add more of those village blocks, and **Crossbows Backport** is another source of crossbows.

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, questions and release pings.

![The 1.14 trade screen](https://media.forgecdn.net/attachments/1900/425/villagergui-png.png)

***

## Villages

Villages are 1.14's villages: plains, desert, savanna, taiga and snowy, built from 1.14's own structure files and laid out street by street the way 1.14 lays them out. You will find farms, animal pens, lamp posts, a meeting point with its bell, an iron golem, unemployed villagers waiting for work and, now and then, an abandoned zombie village. Snowy villages generate on snowy tundra, where 1.12.2 never built villages at all.

The workstations sit inside the houses where 1.14 puts them, so a village has its librarian's lectern and its butcher's smoker from the start. As in the real game, workstations come only from the buildings; nothing else in the world is turned into one.

*   Blocks 1.12.2 doesn't have are swapped for the closest ones it does: stripped logs become logs, blue ice becomes packed ice and smooth sandstone becomes cut sandstone. When Future MC, Quark, Oceanic Expanse or Farmer's Future Delight adds the real block, the village is built with that instead. Lanterns are a block of their own.
*   Village chests carry 1.14's loot.
*   Houses sit on the ground they were planned for. The terrain inside each building is cleared and the ground beneath it filled in, so a house on a slope is neither half buried nor standing on stilts.
*   Streets that cross water become plank bridges in the village's own wood, as in the real game.
*   Mods that add buildings to villages add them to 1.14 villages too. In RLCraft that means the waystone by the bell, and RLCraft's inns, libraries, blacksmiths and farmhouses on plots beside the streets.

1.14 villages need a world generator whose terrain they can plan on: vanilla's, Open Terrain Generator's (RLCraft Dregora), Realistic Terrain Generation's, Farmer's Future Delight's Caves & Cliffs world type, or Caves Not Cliffs. In a world made by any other generator, or with `villages.style` set to `LEGACY`, villages are 1.12.2's, and each building gets the workstation its trade needs: a lectern in the library, a smoker in the butcher's, a blast furnace, smithing table and grindstone at the blacksmith's, a brewing stand in the church, a composter on each field, a bell at the well, and one at random in each small house.

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
| <strong>Blast furnace</strong> |Armorer |Smelts ore and armour at double speed |
| <strong>Smoker</strong> |Butcher |Cooks food at double speed |
| <strong>Grindstone</strong> |Weaponsmith |Disenchants and repairs, returns experience |
| <strong>Stonecutter</strong> |Mason |Cuts stone with no offcuts |
| <strong>Brewing stand / Cauldron</strong> |Cleric / Leatherworker |The two 1.12.2 already had |

The bell isn't a job site. It is the village's meeting point, where villagers gather in the afternoon, and ringing it makes every villager in range glow through walls for a few seconds. Like 1.14's, it can stand on a floor stand, hang from a ceiling or a wall, or be slung between two walls. The lantern isn't a job site either: it lights the village, standing on a block or hanging beneath one.

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

Villagers wear 1.14's skins, so their biome, their profession's outfit and hat, and the badge for their level all show.

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

Every trade has the stock the real game gives it. Enchanted tools, weapons and armour sell 3 times between restocks, while villagers buy everyday goods like crops and wool 16 times and valuable things like iron, gold and diamonds 12 times. 1.12.2 gives everything 7.

***

## Prices move

Prices respond to demand over several days. A trade you sell out every day gets a little dearer each day, with no ceiling short of a full stack, while a trade you leave alone drifts back down. A new trade takes a few days of heavy use before its price first rises.

Expensive items react four times as strongly. Enchanted books, maps, enchanted gear, iron and chainmail armour, shields, stone tools, saddles and leather armour are all premium trades, as they are in the real game.

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

Curing is the biggest discount in the mod, and most of it never fades. Curing the same villager again adds nothing more, as in the real game.

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

Villagers panic when they are hurt or when they see a zombie, husk, zombie villager or illager close by. A panicking villager wakes up if it was asleep, runs from the threat, and goes back to what it was doing once the danger has passed.

A frightened villager calls for an iron golem, which takes three villagers nearby to help. Villagers chatting together can call one too, which takes five. Any adult villager that has slept in the last 20 minutes can help, and none will while a golem is within 16 blocks, so a village keeps the golems it has rather than filling up with them. These are the rules modern golem farms are built around.

***

## Works with other mods

Nothing here needs another mod. When one of these is installed, its items join the trades where the real game has them:

| Mod | Adds to trades |
| --- | --- |
| <strong>Future MC</strong> |Crossbow, campfire, bucket of cod, sweet berries, suspicious stew |
| <strong>Crossbows Backport</strong> |Crossbow |
| <strong>Oceanic Expanse</strong> |Turtle scute, dried kelp block |
| <strong>Deeper Depths</strong> |Red and yellow candles, sold by the master librarian |

Without them, those trades are simply left out of the draw, and the master librarian sells a name tag as in 1.14. Butchers and leatherworkers have fewer trades at the top levels without these mods.

**Future MC** adds many of the same blocks. Both sets give villagers jobs, so a fisherman will take either barrel, and one of each pair is hidden from the creative menu, JEI and crafting so you aren't shown two of everything. Choose which set to keep with `compat.futureMcDuplicates` (`KEEP_OURS`, `KEEP_FUTURE_MC` or `SHOW_BOTH`); villages are built with whichever set you keep. That includes the smithing table, and since this mod's reads Future MC's smithing recipes as well as every other netherite mod's, keeping it loses you nothing.

**Future MC, Quark, Oceanic Expanse and Farmer's Future Delight** also give 1.14 villages their real blocks: stripped logs, smooth sandstone, blue ice, the new walls, stairs and slabs, and spruce, jungle and acacia trapdoors, buttons, pressure plates and signs. Without them, villages use 1.12.2's closest blocks.

**Netherite mods:** with almost any 1.12.2 netherite mod installed (Future MC, Spartan Netherite, Netherite Mod, Complete 1.16 Nether Overhaul, Netherite Stuff, NetherUpdate, NetheritePlusMOD, Netherites But 1.12.2, Nether+ and more), the smithing table upgrades diamond gear to that mod's netherite gear, keeping its enchantments and damage. Each mod's ingot makes that mod's own gear, and every netherite ingot gets the `ingotNetherite` ore name.

**Open Terrain Generator**, and with it **RLCraft Dregora**, gets 1.14 villages planned on its real terrain, in the biomes and at the spacing its preset sets.

**Realistic Terrain Generation**'s world type gets 1.14 villages planned on its real terrain too.

**Farmer's Future Delight's** Caves & Cliffs world type and **Caves Not Cliffs** both get 1.14 villages built on their 1.18-style terrain, anywhere from y -64 to 319.

**Waystones, Recurrent Complex and RLCraft's village buildings** appear in 1.14 villages too: the waystone by the bell, and the mods' houses, inns and libraries on plots beside the streets. Any mod that adds village buildings the standard way is included, and `villages.modPieceExclusions` leaves out any you don't want.

**Antique Atlas** draws 1.14 villages on its maps, building by building.

**Village Names**, and any other mod that brings its own village generator, keep their own villages.

**JEI Villagers** lists the 1.14 trades villagers actually sell, including any from the backport mods above.

***

## Commands

*   `/locate <structure>` gives the distance and the coordinates in green brackets; click them to put a teleport command in your chat box. It takes every structure 1.12.2 can find, plus `village_plains`, `village_desert`, `village_savanna`, `village_taiga` and `village_snowy`.
*   `/village [radius]` draws the village. Every bed gets a box, green where a villager has claimed it and red where it is going spare, along with the counts that decide whether the village can grow.
*   `/forgive` clears a villager's grudges against you, and `/forgive 32` does the same for every villager within 32 blocks. Discounts you have earned stay. Operators only.

***

## What is not here

No raids and no pillagers. This backports the villager and the village, and 1.14 shipped a lot besides.

*   Illagers, pillager patrols, outposts and ravagers
*   Bad Omen, and the Hero of the Village discount
*   Wandering traders and their llamas

In 1.14 the bell exists to make raiders glow. With no raids to warn of, ours highlights villagers instead.

***

## Config

Everything lives in `config/villagerbackport.cfg`, or the in-game config screen: ten categories, with every setting documented in the file.

![The config screen](https://media.forgecdn.net/attachments/1900/416/config-png.png)

*   `villages`: 1.14's villages or 1.12.2's, real 1.14 blocks from backport mods, other mods' buildings, and workstations for 1.12.2's houses
*   `trades`: 1.14's trades or 1.12.2's, and whether untraded villagers switch over
*   `restock`: how many per day, the gap between them, working hours, sounds
*   `pricing`: demand multipliers, gossip, stock, experience, level gating
*   `jobs`: search radius, working distance, whether villagers must be at their workstation to restock, the job-to-block map, and modern farming
*   `workstations`: what each block accepts and produces, extra recipes, compostables, netherite upgrades
*   `homes`: beds, sleeping, breeding, golems, gathering at the bell, babies playing
*   `display`: trade screen, level badges, villager skins, the refill key
*   `compat`: Future MC's duplicate blocks
*   `sounds`: 1.14's crop sounds

Every part of it switches off. Turn off `homes` and villagers ignore beds; turn off `jobs` and the workstations are just decoration.

***

## Things to know

Existing worlds work. Villages generate the new way in chunks you haven't explored yet, so where old land meets new you may see a 1.12.2 village beside a 1.14 one. Villagers already in the world take jobs the first time they find a workstation. Villages that generated before this mod was installed have no workstations of their own, so place some and the villagers will find them.

Updating from 1.x: villagers you have traded with keep every trade they have, while ones nobody has traded with switch to 1.14's trades. The old `structures` section and the stall and workshop settings in your config do nothing now and can be deleted.

If your world has villages missing their workstation blocks, set `strictJobSite` to `false` and villagers will restock without one.

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
