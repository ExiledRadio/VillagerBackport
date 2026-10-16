# 2.1.0

Three more things modern villagers do, and a fix.

## New: a villager's day

*   **Villagers gather at the bell** from 15:00 to 17:00, as in 1.14. Grown villagers head for the nearest bell within 48 blocks, drift around near it and walk up to each other to mingle, which is when gossip spreads fastest. Without a bell in reach they carry on wandering.
*   **Babies play** from 09:00 to 12:00 and 16:00 to 18:00: they chase each other around in games of tag and bounce on beds. This replaces 1.12.2's baby play, which had them follow each other at any hour.
*   Both can be turned off in `homes` (`gatherAtBell`, `babiesPlay`), and the gathering times changed.

## New: map locking

Put a map and a glass pane in the cartography table to get a locked copy, as in 1.14. It never changes again, however much the land it shows does, while the map you put in keeps updating. Copies of a locked map are locked too, and a locked map cannot be zoomed or locked again. It says "Locked" under its name. The one difference from 1.14: a locked map does not show your own position marker.

## Fixed

*   **A villager could turn invisible, leaving only its shadow,** when its 1.14 skin could not be loaded. It now falls back to its 1.12.2 skin instead.
*   When that happened, the game tried again on every frame and could fill the log with thousands of warnings a minute. It now tries once.

## Settings

New in `homes`: `gatherAtBell`, `gatherStartTime`, `gatherEndTime` and `babiesPlay`. They are written to your config automatically.

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
