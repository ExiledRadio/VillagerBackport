# 1.14 Villager Backport

**Minecraft 1.14's villagers, on 1.12.2.**

Villagers claim a workstation and take the job that comes with it, restock twice a day, charge more for the trade you use most, and remember what you have done to them. They sleep in beds, breed on free beds instead of doors, and call an iron golem when frightened.

Ported from decompiled 1.14.4 sources. The pricing formula, restock timings, gossip weights, experience values, golem conditions and sleep pose are the ones 1.14 uses, not approximations of them.

Twelve workstation blocks come with it, all functional. No dependencies. Needed on both client and server.

Pairs with my other mods: [RLCraft Enchantment Recipes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-enchantment-recipes) and [RLCraft Villager Tomes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-villager-tomes).

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** — bug reports, questions and release pings.

![The 1.14 trade screen](PASTE_URL)

***

## Jobs

A villager claims the nearest unclaimed workstation it can walk to and takes that profession. It has to reach the block — no claiming through a wall, and no claiming across a village it cannot path to. Break the workstation and it loses the job and goes looking for another.

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

The stonecutter and fletching table read the crafting recipes already installed and work out what can be cut from them, so a pack with Chisel or Quark gets its blocks in both without either mod knowing about the other. The composter uses the ore dictionary for the same reason.

The bell is not a job site. Ring it and every villager in range glows through walls for a few seconds, which is how you find the one you were looking for.

![Workstations](PASTE_URL)

***

## Restocking

Twice a day, standing at the workstation, and only between 2000 and 9000 ticks — a villager that cannot get to work does not restock, which is what makes trading halls behave the way they do in 1.14. Two minutes minimum between restocks. The workstation's own sound plays when it happens.

Selling trades stock 16 uses and buying trades 12, matching 1.14 rather than 1.12.2's 7.

***

## Prices move

Every use of a trade raises its demand; every restock that finds it untouched lowers it. The surcharge is the base price times demand times a multiplier, rounded down, and the multiplier is **5%** normally and **20%** for enchanted books and filled maps.

The trade screen strikes out the original price and shows the new one in its place, so you can see which trade you have exhausted.

![Demand pricing](PASTE_URL)

***

## Reputation

Villagers keep a per-player score, decay it daily, and tell it to villagers within 8 blocks.

| Event | Effect |
| --- | --- |
| <strong>Trade</strong> |+2, up to a cap of 25 |
| <strong>Cure a zombie villager</strong> |+125, and most of it never decays |
| <strong>Hurt a villager</strong> |−25 |
| <strong>Kill a villager in view of another</strong> |−125 |

The discount is that score times the same multiplier the surcharge uses. Trading a lot takes an emerald off; curing a villager takes six off an ordinary trade and up to twenty-five off an enchanted book. Killing one in front of the village makes everything more expensive, from everyone who saw it.

***

## Levels

| Level | Experience | Badge |
| --- | --- | --- |
| <strong>Novice</strong> |0 |Stone |
| <strong>Apprentice</strong> |10 |Iron |
| <strong>Journeyman</strong> |70 |Gold |
| <strong>Expert</strong> |150 |Emerald |
| <strong>Master</strong> |250 |Diamond |

Trades unlock by level rather than by trade count. Experience is 1.14's: a tier-one trade pays 1 or 2, a tier-five trade pays 30. Overflow carries into the next level the way player experience does.

Villagers wear 1.14's skins — biome type, profession robes and the badge for their level — so a master weaponsmith is identifiable across the village.

![Villager skins and level badges](PASTE_URL)

***

## Beds

A villager claims a bed as its home, sleeps in it from dusk until shortly after dawn, and wakes if you shove it far enough out. Babies sleep too. Breeding needs a free bed rather than a door, so a 1.14 breeder design built in 1.12.2 works.

`/village [radius]` draws the village: a box round every bed, green where claimed and red where free, and the counts that decide whether it can grow.

![The village command](PASTE_URL)

***

## Iron golems

1.14's conditions, unchanged. A villager counts towards a golem only if it has a job, slept in the last 24000 ticks, worked in the last 36000, and has not seen a golem spawn in the last 600. A panicking villager needs three others nearby; two villagers gossiping need five.

These are the rules 1.14 golem farms are built around, and the sleep requirement is why this waited for beds.

***

## Villages

Generated villages get workshops, market stalls and a meeting point, registered as ordinary weighted village pieces — so vanilla houses, RLCraft's houses and these mix into one village with no patching on either side.

Villages that already generated are retrofitted as you find them: composters in crop fields, lecterns in libraries, workbenches replaced with the workstation the building implies.

***

## Config

`config/villagerbackport.cfg`, or the in-game config screen. Eight categories, every setting documented in the file.

*   **restock** — how many per day, the gap between them, working hours, sounds
*   **pricing** — demand multipliers, gossip range and decay, stock, experience, level gating
*   **jobs** — search radius, working distance, how long before a villager gives up on a site, the job-to-block map
*   **workstations** — what each block accepts and produces, extra recipes, compostables
*   **villages** — which buildings generate and how often
*   **structures** — retrofitting workstations into structures that already generate
*   **homes** — beds, sleeping, breeding, golems
*   **display** — trade screen, level badges, villager skins, the refill key

Every part of it switches off. Turn off `homes` and villagers ignore beds; turn off `jobs` and the workstations are decoration.

***

## Things to know

**Curing a zombie villager keeps everything.** Trades, level, experience and gossip all survive, which they did not in 1.12.2.

**Existing worlds work.** Villagers already in the world take jobs the first time they find a workstation, and villages that already generated get workstations retrofitted.

**Raids and illagers are not here.** 1.14's bell warns of a raid; with no raids to warn of, ours highlights villagers instead. Map locking is missing for a similar reason — 1.12.2's map data has nowhere to store it.

**Spacebar refills the trade.** If it does not, another mod has claimed the key — Inventory Tweaks binds it by default. The key is configurable.

***

## Requirements

*   Minecraft 1.12.2, Forge 14.23.5.2847 or newer
*   No dependencies
*   Required on the client and the server

## Install

Drop the jar in `mods` on both sides. **Delete any older version first** — two copies crash on startup with a duplicate mod id.

***

Unofficial. Not affiliated with Mojang or the RLCraft team. Licensed MIT.

Source: [https://github.com/ExiledRadio/VillagerBackport](https://github.com/ExiledRadio/VillagerBackport)
