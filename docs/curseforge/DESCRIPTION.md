# 1.14 Villager Backport

**Modern Minecraft's villagers, on 1.12.2.**

Villagers claim a workstation and take the job that comes with it. They go to work every day, restock twice a day and charge more for the trades you use most. They remember what you have done to them and gossip about it, sleep in beds at night, breed when they have food and a free bed, run when they are hurt, and call an iron golem when frightened.

Ported from decompiled 1.14.4 sources. The pricing formula, restock timings, gossip weights, experience values, golem conditions and sleep pose are the ones 1.14 uses.

Twelve workstation blocks come with it, all functional. No dependencies. Needed on both client and server.

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, questions and release pings.

![The 1.14 trade screen](https://media.forgecdn.net/attachments/1900/425/villagergui-png.png)

***

## Jobs

An unemployed villager looks for a free workstation up to 48 blocks away in any direction, walks to it, and takes the job once it is within 2 blocks, with green sparkles. If it cannot get there within a minute it gives up, shows angry particles and looks for another. Break the workstation and a villager you have never traded with loses the job; one you have traded with keeps it and looks for a new workstation.

Villagers with a job go to work every day. During work hours they keep returning to their workstation and using it, with its sound, whether or not they have anything to restock. Only farmers farm.

![The twelve workstations](https://media.forgecdn.net/attachments/1900/426/workstations-png.png)

| Block | Job | What it does |
| --- | --- | --- |
| <strong>Lectern</strong> |Librarian |Holds a book anyone can read, page by page |
| <strong>Composter</strong> |Farmer |Turns crops into bone meal, hopper in the top, out the bottom |
| <strong>Barrel</strong> |Fisherman |A chest that opens with a block on top |
| <strong>Cartography table</strong> |Cartographer |Copies and zooms maps without eight paper |
| <strong>Fletching table</strong> |Fletcher |Cuts wood the way a stonecutter cuts stone |
| <strong>Smithing table</strong> |Toolsmith |Nothing, same as 1.14 |
| <strong>Loom</strong> |Shepherd |Applies banner patterns for one dye |
| <strong>Blast furnace</strong> |Armorer |Smelts ore and armour at double speed |
| <strong>Smoker</strong> |Butcher |Cooks food at double speed |
| <strong>Grindstone</strong> |Weaponsmith |Disenchants and repairs, returns experience |
| <strong>Stonecutter</strong> |Mason |Cuts stone with no offcuts |
| <strong>Brewing stand / Cauldron</strong> |Cleric / Leatherworker |The two 1.12.2 already had |

The bell is not a job site. Ring it and every villager in range glows through walls for a few seconds.

**Nitwits** are their own thing again: the green-robed villager that never takes a job and wears no level badge. They come from village generation and from curing zombie villagers that were nitwits. Villagers from spawn eggs and babies always start out unemployed.

***

## The blocks work

Each does in 1.12.2 what it does in 1.14, apart from the fletching table, which does nothing in 1.14.

| | |
| --- | --- |
| ![Stonecutter](https://media.forgecdn.net/attachments/1900/423/stonecutter-png.png) | ![Fletching table](https://media.forgecdn.net/attachments/1900/418/fletching-png.png) |
| ![Cartography table](https://media.forgecdn.net/attachments/1900/421/map-png.png) | ![Loom](https://media.forgecdn.net/attachments/1900/420/loom-png.png) |

The fletching table here is a stonecutter for wood. A log becomes planks, planks become stairs and slabs, without the offcuts a crafting grid charges you. That is this mod's own addition, not something 1.14 does.

Both saws read the crafting recipes already installed and work out what can be cut from them, so a pack with Chisel or Quark gets its blocks in both. The composter reads the ore dictionary the same way.

Rerolling a librarian means breaking and replacing its lectern until the book is one you want. The roll is reported in chat as it happens, so you do not have to open the trade screen to check.

![Rolled: Punch II](https://media.forgecdn.net/attachments/1900/419/lecternmessage-png.png)

***

## Restocking

Twice a day, at the villager's own workstation, during work hours. A villager that cannot get to its workstation does not restock. A trade selling out shows green sparkles, so you can see which one ran dry.

Every trade has the stock the matching modern trade has: enchanted tools, weapons and armour sell 3 times between restocks; villagers buy everyday goods like crops and wool 16 times and valuable things like iron, gold and diamonds 12 times. 1.12.2 gives everything 7.

***

## Prices move

Prices respond to demand over days. A trade you sell out every day gets a little dearer each day, with no ceiling short of a full stack, and a trade left alone drifts back down. A new trade takes a few days of heavy use before its price first rises.

Expensive items react four times as strongly: enchanted books, maps, enchanted gear, iron and chainmail armour, shields, stone tools, saddles and leather armour are premium trades, as they are in the real game.

The trade screen crosses out the original price and shows the new one beside it, for rises and discounts alike.

![A trade that has gone up in price](https://media.forgecdn.net/attachments/1900/417/demandpricing-png.png)

***

## Reputation

Every villager keeps its own opinion of every player, and it changes prices for that player only.

| Event | Effect |
| --- | --- |
| <strong>Trade</strong> |+4 per trade, up to a cap of 25 |
| <strong>Cure a zombie villager</strong> |+125, and most of it never fades |
| <strong>Hurt a villager</strong> |−25, for that villager |
| <strong>Kill a villager</strong> |−125, for every villager that saw it happen |

Trading a lot takes about an emerald off. Curing a villager takes six off an ordinary trade and twenty-five off an enchanted book. A killing costs you the same amounts the other way, but only with villagers who could actually see it, not ones behind a wall.

Opinions fade day by day and spread by **gossip**. Villagers swap news when they walk up to each other, at most once a minute, and only part of what they know gets passed on, a little weaker each time. A grudge travels through a village over a day or so, not all at once.

### /forgive

Look at a villager and run `/forgive` to clear its grudges against you, or `/forgive 32` for every villager within 32 blocks. It only clears grudges, so discounts you have earned stay. Operators only.

***

## Zombie villagers

1.12.2 keeps a zombified villager's profession and nothing else. The career is re-rolled and the trades are gone, so the librarian you cure sells different books to the one you lost.

Here the whole villager rides across and back. Trades, level, experience, demand and gossip sit on the zombie while it stands there, and come back when it is cured. A zombie villager you had traded with never despawns, so it is still there to cure.

Curing is the biggest discount in the mod, and most of it never fades. Curing the same villager again adds nothing more, as in the real game.

***

## Levels

| Level | Experience | Badge |
| --- | --- | --- |
| <strong>Novice</strong> |0 |Stone |
| <strong>Apprentice</strong> |10 |Iron |
| <strong>Journeyman</strong> |70 |Gold |
| <strong>Expert</strong> |150 |Emerald |
| <strong>Master</strong> |250 |Diamond |

Trades unlock by level rather than by trade count. A tier-one trade pays the villager 1 or 2 experience, a tier-five trade pays 30. A villager levels up at most once per trading session, after you close the screen. You get 3 to 6 experience per trade, plus 5 on the trade that levels the villager up.

Villagers sell 1.14's trades. Each level offers two trades drawn at random from that level's list, so two farmers can sell different things. Items 1.12.2 does not have, such as the crossbow, campfire and sweet berries, are offered only when a mod that adds them is installed. Set `trades.table` to `LEGACY` for 1.12.2's own trades. A villager you have traded with keeps the trades it has whichever you choose, and keeps unlocking from the list it started on.

Villagers wear 1.14's skins, so biome type, profession robes and the badge for the level all show.

![A villager in 1.14's skin](https://media.forgecdn.net/attachments/1900/424/villager-png.png)

***

## Beds and sleep

A villager claims a bed up to 48 blocks away and sleeps in it from dusk to dawn. Sleeping villagers lie in the bed with a small hitbox on the pillow. Right-click a sleeping villager or its bed to wake it; it lies back down a couple of seconds later if the bed is still free. To take a villager's bed, wake it and sleep there yourself, and it will find another. A villager with no bed waits indoors until morning.

`/village [radius]` draws the village. Every bed gets a box, green where a villager has claimed it and red where it is going spare, with the counts that decide whether the village can grow.

***

## Breeding

Villagers breed on food and beds, not doors, so a modern breeder design built in 1.12.2 works.

A villager is willing when the food it carries adds up to 12: bread is worth 4, carrots, potatoes and beetroot 1 each, in any mix. Both parents need beds of their own, and the baby needs a free bed they can walk to with two empty blocks above it. With no such bed, the attempt fails with angry particles and the food is still eaten. Villagers with jobs do not breed during work hours.

***

## Danger

Villagers panic when they are hurt or when they see a zombie, husk, zombie villager or illager close by. A panicking villager wakes up if it was asleep, runs from the threat, and goes back to what it was doing once it has passed.

## Iron golems

A frightened villager calls for an iron golem, needing three villagers nearby who can help. Villagers chatting call one too, needing five. Any grown villager that has slept in the last 20 minutes can help, and none will while a golem is within 16 blocks, so a village keeps the golems it has rather than filling up with them.

These are the rules modern golem farms are built around.

***

## Villages

Villages are 1.14's villages, built from 1.14's own structure files: plains, desert, savanna, taiga and snowy, laid out street by street the way 1.14 lays them out, with the occasional abandoned zombie village. The workstations are inside the houses where 1.14 puts them, along with the beds, the bell at the meeting point and the villagers. Snowy villages generate on snowy tundra, which 1.12.2 never built villages on.

Blocks 1.12.2 does not have are swapped for the closest it does: stripped logs become logs, blue ice becomes packed ice, smooth sandstone becomes cut sandstone. Lanterns are added as a block of their own, since they light most of the village. Chests carry 1.14's village loot.

Houses sit on the ground they were planned for. 1.14 shapes the terrain around a village as it generates; here the inside of each building is cleared and the ground under it filled in, so a house on a slope is neither buried on one side nor standing on stilts on the other.

1.14 villages need the vanilla overworld generator to plan them. In a world made by another generator, and with `villages.style` set to `LEGACY`, villages are 1.12.2's, and each building gets the workstation its trade needs: a lectern in the library, a smoker in the butcher's, a blast furnace, smithing table and grindstone at the blacksmith's, a brewing stand in the church, a composter on each field, a bell at the well, and one at random in each small house.

A mod that brings its own village generator, such as Village Names, keeps it.

Villages that already generated are retrofitted as you find them. Composters go into crop fields, lecterns into libraries, and a workbench becomes the workstation the building implies.

***

## What is not here

No raids and no pillagers. This backports the villager, and 1.14 shipped a lot besides.

*   Illagers, pillager patrols, outposts, ravagers
*   Bad Omen, and the Hero of the Village discount
*   Wandering traders and their llamas
*   Map locking, which 1.12.2's map data has nowhere to store
*   Gathering at the bell in the afternoon, and babies' play time

1.14's bell exists to make raiders glow. With no raids to warn of, ours highlights villagers.

***

## Config

`config/villagerbackport.cfg`, or the in-game config screen. Nine categories, every setting documented in the file.

![The config screen](https://media.forgecdn.net/attachments/1900/416/config-png.png)

*   `restock`: how many per day, the gap between them, working hours, sounds
*   `pricing`: demand multipliers, gossip, stock, experience, level gating
*   `jobs`: search radius, working distance, whether villagers must be at their workstation to restock, the job-to-block map
*   `workstations`: what each block accepts and produces, extra recipes, compostables
*   `trades`: 1.14's trades or 1.12.2's, and whether untraded villagers switch over
*   `villages`: 1.14's villages or 1.12.2's, and workstations for 1.12.2's houses
*   `structures`: retrofitting workstations into structures that already generate
*   `homes`: beds, sleeping, breeding, golems
*   `display`: trade screen, level badges, villager skins, the refill key

Every part of it switches off. Turn off `homes` and villagers ignore beds. Turn off `jobs` and the workstations are decoration.

***

## Things to know

Existing worlds work. Villagers already in the world take jobs the first time they find a workstation, and villages that already generated get workstations retrofitted.

If your world has villages missing their workstation blocks, set `strictJobSite` to `false` and villagers will restock without one.

Spacebar refills the trade. If it does not, another mod has claimed the key, and Inventory Tweaks binds it by default. The key is configurable.

***

## Requirements

*   Minecraft 1.12.2, Forge 14.23.5.2847 or newer
*   No dependencies
*   Required on the client and the server

## Install

Drop the jar in `mods` on both sides. Delete any older version first, or two copies will crash on startup with a duplicate mod id.

***

Also by me: [Enchantment Recipes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-enchantment-recipes), craft enchanted books from mod materials, and [Villager Tomes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-villager-tomes), teach a librarian the books you find.

Not affiliated with Mojang. Licensed MIT.

Source: [https://github.com/ExiledRadio/VillagerBackport](https://github.com/ExiledRadio/VillagerBackport)
