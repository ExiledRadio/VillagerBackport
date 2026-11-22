# Trade audit against 26.3

Every villager and wandering trader trade of 26.3 (decompiled `VillagerTrades` and `VillagerTradesTagsProvider`, 279
trade keys; `G:/MCDownloads/gd-mech-decomp/out`), checked against this mod's tables (`trade/ModernTrades.java`,
`trader/TraderTrades.java`) and the family mods' `TradeRegistry` registrations on their `main` branches
(2026-11-21). Villager Trade Rebalance is an experiment in 26.3, so it is left out.

Status: **ok** (offered as 26.3 offers it), **ok (owner)** (offered with the owner's item, by name, only while the owner
is installed), **owner registers** (the owner adds it through Backport Core), **price** (offered, at a price from an
older version), **gap** (not offered).

## Villagers

The tables are 1.14's, with later changes applied where noted. Prices, stock and experience match 26.3 except where a
row says otherwise.

| Trade (26.3) | Version | Item owner | Status |
| --- | --- | --- | --- |
| Farmer: wheat, potato, carrot, beetroot, bread, pumpkin, pie, apple, cookie, melon, golden carrot, glistering melon | 1.14 | vanilla | ok |
| Farmer 4: cake | 1.14 | vanilla | price: 1 emerald (1.14); 26.3 asks 3 |
| Farmer 4: suspicious stew (six effects) | 1.14 | this mod (flora) | ok |
| Fisherman 1: cod bucket | 1.14 | Update Aquatic, Future MC | ok (owner) - Update Aquatic's id added |
| Fisherman 2: campfire | 1.14 | this mod, Future MC | ok (owner) |
| Fisherman: string, coal, cod, salmon, tropical fish, pufferfish, enchanted rod, boat for the village type | 1.14 | vanilla | ok |
| Shepherd: wool, dyes, wool and carpet of each color, beds, banners, painting, shears | 1.14 | vanilla; 1.14's four dyes this mod's | ok |
| Fletcher: sticks, arrows, flint, bow, string, feather, tripwire hook, enchanted bow | 1.14 | vanilla | ok |
| Fletcher 3, 5: crossbow, enchanted crossbow | 1.14 | this mod, Future MC, Crossbows | ok (owner) |
| Fletcher 5: tipped arrows | 1.14 | vanilla | ok |
| Librarian 1-4: paper, books, ink sac, writable book, bookshelf (9), glass, clock, compass, enchanted books | 1.14; bookshelf later | vanilla | ok; books 26.1's (`trades.modernEnchantedTrades`) |
| Librarian 2: lantern | 1.14 | this mod, Future MC | ok (owner) |
| Librarian 5: yellow and red candle (3 emeralds) | 26.1 | Deeper Depths, Caves & Cliffs | ok (owner) - Caves & Cliffs' ids added |
| Librarian 5: no name tag | 26.1 | - | ok with Game Drops; without it 1.14's name tag stays where no candle mod is installed |
| Cartographer (biome maps, trial chambers map, banners by biome, mansion map at master) | 1.21.5 | vanilla; trial chambers Tricky Trials | ok (`trades.modernCartographer`); Game Drops turns the sold maps into its explorer map items |
| Cleric: rotten flesh, redstone, gold, lapis, rabbit foot, glowstone, glass bottle, ender pearl, nether wart, bottle o' enchanting | 1.14 | vanilla | ok |
| Cleric 4: turtle scute | 1.14 | Update Aquatic, Oceanic Expanse | ok (owner) - Update Aquatic's `scute` added |
| Armorer, weaponsmith, toolsmith (incl. bell at level 2) | 1.14 | vanilla; bell this mod | ok |
| Mason 1-2, 4-5: clay, brick, stone, chiseled stone bricks, quartz, terracotta, glazed terracotta, quartz blocks | 1.14 | vanilla | ok |
| Mason 3: granite, andesite, diorite, polished stones | 1.14 | vanilla | ok |
| Mason 3: dripstone block (1 emerald for 4, 16 uses) | 1.17 | Caves & Cliffs | ok (owner) - added |
| Butcher: chicken, porkchop, rabbit, stew, coal, cooked meat, mutton, beef | 1.14 | vanilla | ok |
| Butcher 4: dried kelp block | 1.14 | Update Aquatic, Oceanic Expanse | ok (owner) - Update Aquatic's id added |
| Butcher 5: sweet berries | 1.14 | this mod, Future MC | ok (owner) |
| Leatherworker: leather, dyed armor, flint, rabbit hide, saddle | 1.14 | vanilla | ok |
| Leatherworker 4: turtle scute; dyed leather horse armor | 1.14 | Update Aquatic, Oceanic Expanse; this mod | ok (owner) - Update Aquatic's `scute` added |

## Wandering trader

This mod's table already has current Java's three pools (buying, uncommon, common).

| Trade (26.3) | Version | Item owner | Status |
| --- | --- | --- | --- |
| Buying: water bottle, water bucket, milk, fermented spider eye, baked potato, hay | after 1.14 | vanilla | ok |
| Packed ice, gunpowder, podzol, six 1.12.2 logs, enchanted iron pickaxe | 1.14 | vanilla | ok |
| Blue ice | 1.14 | Update Aquatic, this mod, Oceanic Expanse, Future MC | ok (owner) - Update Aquatic's id added first |
| Cherry log, cherry sapling | 1.20 | Trails & Tales | ok (owner) - added |
| Mangrove log | 1.19 | Wild Update | ok (owner) - added |
| Mangrove propagule | 1.19 | Wild Update | owner registers |
| Pale oak log, pale oak sapling, pale hanging moss, pale moss block, open eyeblossom | 1.21.4 | Tricky Trials | ok (owner) - added |
| Wildflowers, tall dry grass, firefly bush | 1.21.5 | Tricky Trials | ok (owner) - added |
| Poplar log, poplar sapling, shelf mushroom, golden dandelion, name tag | 26.1-26.3 | Game Drops | owner registers; this mod's own name tag trade now steps aside with Game Drops (it was offered twice) |
| Sulfur spike | 26.2 | Game Drops | ok (owner) - added (Game Drops registers none) |
| Pointed dripstone | 1.17 | Caves & Cliffs | owner registers |
| Small dripleaf, rooted dirt, moss block | 1.17 | Caves & Cliffs | ok (owner) - added |
| Invisibility potion | 1.14 | vanilla | price: 1.14's normal potion; 26.3 sells the long one |
| Kelp, coral blocks, nautilus shell, tropical fish and pufferfish buckets | 1.14 | Update Aquatic, Oceanic Expanse, Future MC | ok (owner) - Update Aquatic's ids added first |
| Sea pickle | 1.14 | Update Aquatic | ok (owner) |
| Slime ball, glowstone | 1.14 | vanilla | price: 1.14's 5 and 3 emeralds; 26.3 asks 4 and 2 |
| Flowers, seeds, saplings, dyes, vines, mushrooms, lily pads, sand, red sand, fern, sugar cane, pumpkin, cactus | 1.14 | vanilla; cornflower and lily of the valley this mod, Future MC | ok |

## Not done: trade tables by installed version

The design the user asked for (2026-11-21): villagers and the trader use the trade table of the newest installed
backport's version - Village & Pillage alone 1.14's, with Caves & Cliffs 1.17's, Wild Update 1.19's, Trails & Tales
1.20's, Tricky Trials 1.21.11's, Game Drops 26.3's - built as data keyed by version, so a later backport adds a row.
Existing villagers keep the offers they have; new levels and new villagers use the current table.

Today the tables are one mix: 1.14's numbers, with the later changes above switched on by setting or by the owner
being installed. The rows marked **price** are the ones a per-version table would change; the owner-gated rows already
appear only from the version whose mod is installed. What the per-version tables still need:

- each version's own data (1.17.1, 1.19.4, 1.20.6, 1.21.11, 26.3 decompiled `VillagerTrades`), diffed into per-version
  rows: prices, stock, experience, which level a trade sits at, removed trades;
- the version from Backport Core (`Presentation` newest installed, or `VanillaRevisions`);
- self-tests per version with that version's mods (or stand-ins) loaded.

## Tests

`-Pselftest=trades26`, with stand-ins for the owners in a dev environment: the run directory files `fake-ua`,
`fake-cc`, `fake-gd` register Update Aquatic's, Caves & Cliffs' and Game Drops' items by name
(`selftest/Fake*.java`). Each wired row above with a stand-in is checked to appear in its pool; Trails & Tales', Wild
Update's and Tricky Trials' rows have no stand-in yet.
