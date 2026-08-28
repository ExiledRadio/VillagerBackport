# 1.14 Villager Backport

**Minecraft 1.14's villagers, on 1.12.2.**

Villagers claim a workstation and take the job that comes with it. They restock twice a day and charge more for the trade you use most. They remember what you have done to them, sleep in beds at night, breed when a bed is free rather than when doors are, and call an iron golem when frightened.

Ported from decompiled 1.14.4 sources. The pricing formula, restock timings, gossip weights, experience values, golem conditions and sleep pose are the ones 1.14 uses.

Twelve workstation blocks come with it, all functional. No dependencies. Needed on both client and server.

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, questions and release pings.

![The 1.14 trade screen](https://media.forgecdn.net/attachments/1900/425/villagergui-png.png)

***

## Jobs

A villager claims the nearest unclaimed workstation it can walk to and takes that profession. It has to reach the block, so there is no claiming through a wall and none across a village it cannot path to. Break the workstation and it loses the job and goes looking for another.

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

Twice a day, at the workstation, between 2000 and 9000 ticks. A villager that cannot get to work does not restock. Two minutes minimum between restocks, and the workstation's own sound plays when it happens.

Selling trades stock 16 uses and buying trades 12, matching 1.14. 1.12.2 gives 7.

***

## Prices move

Every use of a trade raises its demand. Every restock that finds it untouched lowers it again. The surcharge is the base price times demand times a multiplier, rounded down, and that multiplier is 5% normally, 20% for enchanted books and filled maps.

The trade screen strikes out the original price and shows the new one beside it.

![A trade that has gone up in price](https://media.forgecdn.net/attachments/1900/417/demandpricing-png.png)

***

## Reputation

Villagers keep a per-player score, decay it daily, and pass it to villagers within 8 blocks.

| Event | Effect |
| --- | --- |
| <strong>Trade</strong> |+2, up to a cap of 25 |
| <strong>Cure a zombie villager</strong> |+125, and most of it never decays |
| <strong>Hurt a villager</strong> |−25 |
| <strong>Kill a villager in view of another</strong> |−125 |

The discount is that score times the same multiplier the surcharge uses. Trading a lot takes an emerald off. Curing a villager takes six off an ordinary trade and up to twenty-five off an enchanted book. Kill one where another can see it and prices rise for you with every villager that watched.

***

## Zombie villagers

1.12.2 keeps a zombified villager's profession and nothing else. The career is re-rolled and the trades are gone, so the librarian you cure sells different books to the one you lost.

Here the whole villager rides across and back. Trades, level, experience, demand and gossip sit on the zombie while it stands there, and come back when it is cured.

Curing is the biggest discount in the mod, and the part of it that matters does not decay. Curing the same villager again drops its prices again, up to the five cures 1.14 caps it at.

***

## Levels

| Level | Experience | Badge |
| --- | --- | --- |
| <strong>Novice</strong> |0 |Stone |
| <strong>Apprentice</strong> |10 |Iron |
| <strong>Journeyman</strong> |70 |Gold |
| <strong>Expert</strong> |150 |Emerald |
| <strong>Master</strong> |250 |Diamond |

Trades unlock by level rather than by trade count. Experience follows 1.14: a tier-one trade pays 1 or 2, a tier-five trade pays 30. Overflow carries into the next level the way player experience does.

Villagers wear 1.14's skins, so biome type, profession robes and the badge for the level all show.

![A villager in 1.14's skin](https://media.forgecdn.net/attachments/1900/424/villager-png.png)

***

## Beds

A villager claims a bed as its home and sleeps in it from dusk until shortly after dawn. Shove it far enough out and it wakes up. Babies sleep too. Breeding needs a free bed instead of a door, so a 1.14 breeder design built in 1.12.2 works.

`/village [radius]` draws the village. Every bed gets a box, green where a villager has claimed it and red where it is going spare, with the counts that decide whether the village can grow.

***

## Iron golems

1.14's conditions, unchanged. A villager counts towards a golem only if it has a job, slept in the last 24000 ticks, worked in the last 36000, and has not seen a golem spawn in the last 600. A panicking villager needs three others nearby. Two villagers gossiping need five.

These are the rules 1.14 golem farms are built around.

***

## Villages

Generated villages get workshops, market stalls and a meeting point, registered as ordinary weighted village pieces. Vanilla houses, houses from a structure mod and these all mix into one village with no patching on either side.

![A market stall in a generated village](https://media.forgecdn.net/attachments/1900/422/stalls-png.png)

Villages that already generated are retrofitted as you find them. Composters go into crop fields, lecterns into libraries, and a workbench becomes the workstation the building implies.

***

## What is not here

No raids and no pillagers. This backports the villager, and 1.14 shipped a lot besides.

*   Illagers, pillager patrols, outposts, ravagers
*   Bad Omen, and the Hero of the Village discount
*   Wandering traders and their llamas
*   Map locking, which 1.12.2's map data has nowhere to store
*   The full daily schedule. Villagers work, sleep and breed on 1.14's timings, but they do not gather at the bell at midday, and babies have no play period

1.14's bell exists to make raiders glow. With no raids to warn of, ours highlights villagers.

***

## Config

`config/villagerbackport.cfg`, or the in-game config screen. Eight categories, every setting documented in the file.

![The config screen](https://media.forgecdn.net/attachments/1900/416/config-png.png)

*   `restock`: how many per day, the gap between them, working hours, sounds
*   `pricing`: demand multipliers, gossip range and decay, stock, experience, level gating
*   `jobs`: search radius, working distance, how long before a villager gives up on a site, the job-to-block map
*   `workstations`: what each block accepts and produces, extra recipes, compostables
*   `villages`: which buildings generate and how often
*   `structures`: retrofitting workstations into structures that already generate
*   `homes`: beds, sleeping, breeding, golems
*   `display`: trade screen, level badges, villager skins, the refill key

Every part of it switches off. Turn off `homes` and villagers ignore beds. Turn off `jobs` and the workstations are decoration.

***

## Things to know

Existing worlds work. Villagers already in the world take jobs the first time they find a workstation, and villages that already generated get workstations retrofitted.

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
