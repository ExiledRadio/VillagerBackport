# Creative tabs

Village & Pillage Backport has no creative tab of its own. Each block and item is in the vanilla tab Minecraft 1.19.2 gives it, next to its vanilla neighbor in 1.19.2's order, through Backport Core's creative placement (`creative.CreativePlacement`; the rule and the table are in Backport Core's `docs/CREATIVE_TABS.md`). Items are placed by their vanilla name: each stream's registration calls `CreativePlacement.place` (`ModBlocks`, `ModItems`, `BuildingContent`, `WoodsetContent`, `FloraContent`, `BambooContent`, `CampfireContent`, `CrossbowContent`, `FoxContent`, `IllagerContent`), and `VillagerBackport.preInit` places the spawn eggs. Crossbow enchantment books are in Combat.

"After" and "Before" are the stacks either side of it with this mod and Backport Core alone, from a dev client's `-Dbackportcore.creativeDump` (an item is put after its nearest 1.19.2 neighbor before it, or before the one after it, whichever is of its kind). With other backports installed an item moves to stay by its 1.19.2 neighbor: stripped logs follow the warped stem once the Nether Update adds it, and so on.

## Building Blocks

| Item | After | Before |
|---|---|---|
| `stone_slab` | `iron_block` | `smooth_stone_slab` |
| `cut_sandstone_slab` | `sandstone_slab` | `cobblestone_slab` |
| `stripped_oak_log` | `dark_oak_log` | `stripped_spruce_log` (this mod) |
| `stripped_spruce_log` | `stripped_oak_log` (this mod) | `stripped_birch_log` (this mod) |
| `stripped_birch_log` | `stripped_spruce_log` (this mod) | `stripped_jungle_log` (this mod) |
| `stripped_jungle_log` | `stripped_birch_log` (this mod) | `stripped_acacia_log` (this mod) |
| `stripped_acacia_log` | `stripped_jungle_log` (this mod) | `stripped_dark_oak_log` (this mod) |
| `stripped_dark_oak_log` | `stripped_acacia_log` (this mod) | `stripped_oak_wood` (this mod) |
| `stripped_oak_wood` | `stripped_dark_oak_log` (this mod) | `stripped_spruce_wood` (this mod) |
| `stripped_spruce_wood` | `stripped_oak_wood` (this mod) | `stripped_birch_wood` (this mod) |
| `stripped_birch_wood` | `stripped_spruce_wood` (this mod) | `stripped_jungle_wood` (this mod) |
| `stripped_jungle_wood` | `stripped_birch_wood` (this mod) | `stripped_acacia_wood` (this mod) |
| `stripped_acacia_wood` | `stripped_jungle_wood` (this mod) | `stripped_dark_oak_wood` (this mod) |
| `stripped_dark_oak_wood` | `stripped_acacia_wood` (this mod) | `acacia_stairs` |
| `cut_red_sandstone_slab` | `red_sandstone_slab` | `purpur_block` |
| `smooth_quartz` | `purpur_slab` | `smooth_red_sandstone` (this mod) |
| `smooth_red_sandstone` | `smooth_quartz` (this mod) | `smooth_sandstone` (this mod) |
| `smooth_sandstone` | `smooth_red_sandstone` (this mod) | `smooth_stone` (this mod) |
| `smooth_stone` | `smooth_sandstone` (this mod) | `end_stone_bricks` |
| `blue_ice` | `black_concrete_powder` | `polished_granite_stairs` (this mod) |
| `polished_granite_stairs` | `blue_ice` (this mod) | `smooth_red_sandstone_stairs` (this mod) |
| `smooth_red_sandstone_stairs` | `polished_granite_stairs` (this mod) | `mossy_stone_brick_stairs` (this mod) |
| `mossy_stone_brick_stairs` | `smooth_red_sandstone_stairs` (this mod) | `polished_diorite_stairs` (this mod) |
| `polished_diorite_stairs` | `mossy_stone_brick_stairs` (this mod) | `mossy_cobblestone_stairs` (this mod) |
| `mossy_cobblestone_stairs` | `polished_diorite_stairs` (this mod) | `end_stone_brick_stairs` (this mod) |
| `end_stone_brick_stairs` | `mossy_cobblestone_stairs` (this mod) | `stone_stairs` (this mod) |
| `stone_stairs` | `end_stone_brick_stairs` (this mod) | `smooth_sandstone_stairs` (this mod) |
| `smooth_sandstone_stairs` | `stone_stairs` (this mod) | `smooth_quartz_stairs` (this mod) |
| `smooth_quartz_stairs` | `smooth_sandstone_stairs` (this mod) | `granite_stairs` (this mod) |
| `granite_stairs` | `smooth_quartz_stairs` (this mod) | `andesite_stairs` (this mod) |
| `andesite_stairs` | `granite_stairs` (this mod) | `red_nether_brick_stairs` (this mod) |
| `red_nether_brick_stairs` | `andesite_stairs` (this mod) | `polished_andesite_stairs` (this mod) |
| `polished_andesite_stairs` | `red_nether_brick_stairs` (this mod) | `diorite_stairs` (this mod) |
| `diorite_stairs` | `polished_andesite_stairs` (this mod) | `polished_granite_slab` (this mod) |
| `polished_granite_slab` | `diorite_stairs` (this mod) | `smooth_red_sandstone_slab` (this mod) |
| `smooth_red_sandstone_slab` | `polished_granite_slab` (this mod) | `mossy_stone_brick_slab` (this mod) |
| `mossy_stone_brick_slab` | `smooth_red_sandstone_slab` (this mod) | `polished_diorite_slab` (this mod) |
| `polished_diorite_slab` | `mossy_stone_brick_slab` (this mod) | `mossy_cobblestone_slab` (this mod) |
| `mossy_cobblestone_slab` | `polished_diorite_slab` (this mod) | `end_stone_brick_slab` (this mod) |
| `end_stone_brick_slab` | `mossy_cobblestone_slab` (this mod) | `smooth_sandstone_slab` (this mod) |
| `smooth_sandstone_slab` | `end_stone_brick_slab` (this mod) | `smooth_quartz_slab` (this mod) |
| `smooth_quartz_slab` | `smooth_sandstone_slab` (this mod) | `granite_slab` (this mod) |
| `granite_slab` | `smooth_quartz_slab` (this mod) | `andesite_slab` (this mod) |
| `andesite_slab` | `granite_slab` (this mod) | `red_nether_brick_slab` (this mod) |
| `red_nether_brick_slab` | `andesite_slab` (this mod) | `polished_andesite_slab` (this mod) |
| `polished_andesite_slab` | `red_nether_brick_slab` (this mod) | `diorite_slab` (this mod) |
| `diorite_slab` | `polished_andesite_slab` (this mod) | (end of the tab) |

## Decoration Blocks

| Item | After | Before |
|---|---|---|
| `sea_pickle` | `dead_bush` | `dandelion` |
| `cornflower` | `oxeye_daisy` | `lily_of_the_valley` (this mod) |
| `lily_of_the_valley` | `cornflower` (this mod) | `wither_rose` (this mod) |
| `wither_rose` | `lily_of_the_valley` (this mod) | `brown_mushroom` |
| `bamboo` | `red_mushroom` | `torch` |
| `brick_wall` | `mossy_cobblestone_wall` | `prismarine_wall` (this mod) |
| `prismarine_wall` | `brick_wall` (this mod) | `red_sandstone_wall` (this mod) |
| `red_sandstone_wall` | `prismarine_wall` (this mod) | `mossy_stone_brick_wall` (this mod) |
| `mossy_stone_brick_wall` | `red_sandstone_wall` (this mod) | `granite_wall` (this mod) |
| `granite_wall` | `mossy_stone_brick_wall` (this mod) | `stone_brick_wall` (this mod) |
| `stone_brick_wall` | `granite_wall` (this mod) | `nether_brick_wall` (this mod) |
| `nether_brick_wall` | `stone_brick_wall` (this mod) | `andesite_wall` (this mod) |
| `andesite_wall` | `nether_brick_wall` (this mod) | `red_nether_brick_wall` (this mod) |
| `red_nether_brick_wall` | `andesite_wall` (this mod) | `sandstone_wall` (this mod) |
| `sandstone_wall` | `red_nether_brick_wall` (this mod) | `end_stone_brick_wall` (this mod) |
| `end_stone_brick_wall` | `sandstone_wall` (this mod) | `diorite_wall` (this mod) |
| `diorite_wall` | `end_stone_brick_wall` (this mod) | `anvil` |
| `scaffolding` | `dark_oak_leaves` | `slime_block` |
| `spruce_sign` | `oak_sign` | `birch_sign` (this mod) |
| `birch_sign` | `spruce_sign` (this mod) | `jungle_sign` (this mod) |
| `jungle_sign` | `birch_sign` (this mod) | `acacia_sign` (this mod) |
| `acacia_sign` | `jungle_sign` (this mod) | `dark_oak_sign` (this mod) |
| `dark_oak_sign` | `acacia_sign` (this mod) | `white_bed` |
| `loom` | `end_crystal` | `composter` (this mod) |
| `composter` | `loom` (this mod) | `barrel` (this mod) |
| `barrel` | `composter` (this mod) | `smoker` (this mod) |
| `smoker` | `barrel` (this mod) | `blast_furnace` (this mod) |
| `blast_furnace` | `smoker` (this mod) | `cartography_table` (this mod) |
| `cartography_table` | `blast_furnace` (this mod) | `fletching_table` (this mod) |
| `fletching_table` | `cartography_table` (this mod) | `grindstone` (this mod) |
| `grindstone` | `fletching_table` (this mod) | `smithing_table` (this mod) |
| `smithing_table` | `grindstone` (this mod) | `stonecutter` (this mod) |
| `stonecutter` | `smithing_table` (this mod) | `bell` (this mod) |
| `bell` | `stonecutter` (this mod) | `lantern` (this mod) |
| `lantern` | `bell` (this mod) | `campfire` (this mod) |
| `campfire` | `lantern` (this mod) | (end of the tab) |

## Redstone

| Item | After | Before |
|---|---|---|
| `spruce_pressure_plate` | `oak_pressure_plate` | `birch_pressure_plate` (this mod) |
| `birch_pressure_plate` | `spruce_pressure_plate` (this mod) | `jungle_pressure_plate` (this mod) |
| `jungle_pressure_plate` | `birch_pressure_plate` (this mod) | `acacia_pressure_plate` (this mod) |
| `acacia_pressure_plate` | `jungle_pressure_plate` (this mod) | `dark_oak_pressure_plate` (this mod) |
| `dark_oak_pressure_plate` | `acacia_pressure_plate` (this mod) | `redstone_torch` |
| `spruce_trapdoor` | `oak_trapdoor` | `birch_trapdoor` (this mod) |
| `birch_trapdoor` | `spruce_trapdoor` (this mod) | `jungle_trapdoor` (this mod) |
| `jungle_trapdoor` | `birch_trapdoor` (this mod) | `acacia_trapdoor` (this mod) |
| `acacia_trapdoor` | `jungle_trapdoor` (this mod) | `dark_oak_trapdoor` (this mod) |
| `dark_oak_trapdoor` | `acacia_trapdoor` (this mod) | `oak_fence_gate` |
| `spruce_button` | `oak_button` | `birch_button` (this mod) |
| `birch_button` | `spruce_button` (this mod) | `jungle_button` (this mod) |
| `jungle_button` | `birch_button` (this mod) | `acacia_button` (this mod) |
| `acacia_button` | `jungle_button` (this mod) | `dark_oak_button` (this mod) |
| `dark_oak_button` | `acacia_button` (this mod) | `trapped_chest` |
| `lectern` | `dropper` | `iron_trapdoor` |

## Miscellaneous

| Item | After | Before |
|---|---|---|
| `black_dye` | `red_dye` | `green_dye` |
| `blue_dye` | `purple_dye` | `brown_dye` (this mod) |
| `brown_dye` | `blue_dye` (this mod) | `cyan_dye` |
| `white_dye` | `magenta_dye` | `orange_dye` |
| `cat spawn egg` | `blaze spawn egg` | `cave_spider spawn egg` |
| `fox spawn egg` | `evoker spawn egg` | `ghast spawn egg` |
| `panda spawn egg` | `ocelot spawn egg` | `parrot spawn egg` |
| `pillager spawn egg` | `pig spawn egg` | `polar_bear spawn egg` |
| `ravager spawn egg` | `rabbit spawn egg` | `sheep spawn egg` |
| `trader_llama spawn egg` | `stray spawn egg` | `vex spawn egg` |
| `wandering_trader spawn egg` | `vindicator spawn egg` | `witch spawn egg` |
| `leather_horse_armor` | `diamond_horse_armor` | `chorus_fruit` |
| `flower_banner_pattern` | `music_disc_wait` | `creeper_banner_pattern` (this mod) |
| `creeper_banner_pattern` | `flower_banner_pattern` (this mod) | `skull_banner_pattern` (this mod) |
| `skull_banner_pattern` | `creeper_banner_pattern` (this mod) | `mojang_banner_pattern` (this mod) |
| `mojang_banner_pattern` | `skull_banner_pattern` (this mod) | `globe_banner_pattern` (this mod) |
| `globe_banner_pattern` | `mojang_banner_pattern` (this mod) | (end of the tab) |

## Foodstuffs

| Item | After | Before |
|---|---|---|
| `sweet_berries` | `beetroot_soup` | `ominous_bottle` (this mod) |
| `ominous_bottle` | `sweet_berries` (this mod) | `ominous_bottle` (this mod) |
| `ominous_bottle` | `ominous_bottle` (this mod) | `ominous_bottle` (this mod) |
| `ominous_bottle` | `ominous_bottle` (this mod) | `ominous_bottle` (this mod) |
| `ominous_bottle` | `ominous_bottle` (this mod) | `ominous_bottle` (this mod) |
| `ominous_bottle` | `ominous_bottle` (this mod) | `suspicious_stew` |

## Combat

| Item | After | Before |
|---|---|---|
| `crossbow` | `totem_of_undying` | (end of the tab) |

## Not in the creative menu

As in vanilla 1.19.2, which keeps them out of every tab (they are still in the game): `suspicious_stew`.
