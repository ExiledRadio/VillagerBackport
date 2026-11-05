# Stream G: building blocks

Branch `v14-building`. Package `com.exiledradio.villagerbackport.building`. Config `building`.

## Changelog section

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
*   **Villages use them**: desert villages get real smooth sandstone, smooth sandstone stairs and slabs and sandstone walls, taiga villages spruce signs, and so on, wherever 1.12.2 used a stand-in before. If Future MC, Quark, Oceanic Expanse or Farmer's Future Delight provides one of these blocks, villages keep using theirs.

## Changed: seven vanilla recipes, as in 1.14

So every new block can be crafted, these change the way 1.14 changed them (`building.vanillaRecipeChanges`, on by default):

*   **Sign**: oak planks only. The other woods make their own sign.
*   **Stone Slab** (1.12.2's, now named **Smooth Stone Slab** as in 1.14): made from smooth stone. Stone makes 1.14's new plain stone slab.
*   **Sandstone and red sandstone slabs**: no longer from cut sandstone, which makes the cut sandstone slab.
*   **Stone brick slab and stairs**: plain stone bricks only. Mossy stone bricks make their own.
*   **Nether brick fence**: four nether brick blocks with two nether bricks up the middle. Six nether brick blocks make the wall.

Renamed to 1.14's names: Smooth Sandstone to **Cut Sandstone**, Smooth Red Sandstone to **Cut Red Sandstone**, Stone Slab to **Smooth Stone Slab**, Sign to **Oak Sign**.

## With Future MC

Future MC adds the twelve walls, the four smooth blocks and the five signs too. `compat.futureMcDuplicates` now covers them like the workstations: by default Future MC's are hidden (no recipe, smelting recipe, JEI entry or creative listing; placed ones keep working) and villages build with this mod's. `KEEP_FUTURE_MC` hides this mod's instead.

## Config

*   `building.enabled`: everything above. Off, nothing is registered, villages keep their stand-ins and vanilla recipes are untouched. Turning it off in a world that has the blocks deletes them.
*   `building.vanillaRecipeChanges`: the seven recipe changes.

---

## For the integrator

### Files outside the package

*   `ModConfig.java`: `Building` category filled in; one paragraph added to `compat.futureMcDuplicates`' comment.
*   `en_us.lang`: the `# --- building ---` section at the end, generated (see below). Also overrides four vanilla names.
*   `compat/FutureMc.java`: pairs come from `SHARED` plus `BuildingContent.futureMcPairs()` (signs are `<wood>_standing_sign` in Future MC); `hiddenItems()` falls back to `Block.getItem` for blocks with no `ItemBlock` (signs); `hideFromCreative()` also clears hidden items' tabs; `removeLateRecipes()` also removes smelting recipes whose result is hidden.
*   `village/modern/BackportBlocks.java`: this mod's blocks appended as the last option to every entry they fit, plus `smooth_sandstone_stairs` and the remaining 1.14 shapes.
*   `block/StonecutterRecipes.java`: `chainOneForOne` after derivation (one step, only through one-for-one whole-block cuts). Affects the fletching table's derived list the same way (log to planks is 1-for-4 so it is not chained).
*   Assets: 268 generated blockstate/model files, 5 sign entity textures and 5 sign item textures from the 1.14.4 jar.

### Generated files

`tools/building/generate.py` reads `BuildingTable.java` and `SignWood.java` as text and writes every blockstate, block model, item model and the lang section. Rerun it after changing the table:

```bash
python tools/building/generate.py
```

### For the flora stream (sign dyeing)

The five wood signs are `TileEntityWoodSign extends TileEntitySign`, so `instanceof TileEntitySign` checks see them. They are drawn by their own renderer, `building/client/TileEntityWoodSignRenderer`, a copy of vanilla's that draws `getFormattedText()` in color 0. If dyeing stores a color in text-component style, it shows on these signs as it does on oak; if it changes vanilla's renderer or stores the color somewhere else, this renderer needs the same change.

## Tested headlessly

Dedicated dev server (`runServer`, port 25707, fresh world), with a self-test that runs on the first tick using a real `EntityPlayerMP` (kept at `tools/building/selftest/BuildingSelfTest.java`, outside the source tree; its Javadoc says how to run it). Result: **495 checks, 0 failures**.

*   Registration: all 72 blocks (4 smooth, 16 slabs + 16 doubles, 14 stairs, 12 walls, 10 signs) and 51 items exist, each block's item is its own.
*   Crafting, through `CraftingManager.findMatchingResult`: every slab, stairs, wall and sign recipe gives the right block and count; oak planks still give vanilla's sign; smooth stone gives vanilla's stone slab; plain sandstone, chiseled red sandstone, plain stone bricks give vanilla's slabs; cut sandstone gives the new cut slab, not vanilla's; the new nether brick fence recipe works; mossy stone bricks give the new mossy slab/stairs (this caught the stone brick and nether brick fence collisions, now fixed).
*   Smelting: stone, sandstone, red sandstone, quartz block to the four smooth blocks; stone bricks to cracked; andesite does not smelt.
*   Stonecutter: every new slab (x2), stairs and wall is offered from its base block; andesite offers polished andesite slabs, stone offers stone brick walls (the chain).
*   Ore dictionary counts.
*   Every one of the 46 blocks with an item placed through its item's `onItemUse`, checked in the world, its drops checked, harvest tool is pickaxe, and broken. Each slab placed twice becomes its double slab, which drops two slabs.
*   Walls join a neighboring new wall and a vanilla cobblestone wall; stairs form a corner.
*   All five signs: placed standing (facing up) and on a wall from the item, the tile is `TileEntityWoodSign` and `TileEntitySign`, the item is used up, the edit screen is opened for the placer, a real `CPacketUpdateSign` through `NetHandlerPlayServer.processUpdateSign` sets the text, it saves as `villagerbackport:wood_sign` with the text, it has an update packet; removing the block underneath pops both signs off as two sign items.
*   Villages: `BackportBlocks.resolve` gives this mod's block for diorite wall, granite stairs, smooth sandstone, smooth sandstone stairs, smooth stone, mossy cobblestone slab (top half), spruce wall sign (facing), sandstone wall, end stone brick wall and the diorite double slab.
*   Booting with `building.enabled=false`: no errors, nothing registered, vanilla recipes unchanged.
*   `build --rerun-tasks -PdeployDir=`: clean, reobfuscated (SRG names in the jar), self-test not in the jar.

## Not tested (needs the client)

*   **Everything visual**: block and item models, textures, slab top halves, stairs corners, wall posts and sides, item models in the inventory, the smooth blocks' textures. The blockstate/model JSON is generated and its parents are vanilla 1.12.2's templates, but no client has loaded it.
*   **The sign renderer**: standing post vs wall, the 1.14 textures, text, rotation in all 16 directions, breaking animation, and the edit screen (which needed a correction for standing signs; see the renderer's Javadoc).
*   Language names in game, including the four vanilla renames (another mod's language file loading later could override them).
*   JEI showing the recipes and the stonecutter page; the creative tab order.
*   A village actually generating with these blocks (only the block resolution was tested, not world generation).
*   With Future MC installed (it crashes the FG2 dev env): pairing, hiding, recipe and smelting removal for the walls, smooth blocks and signs.
*   With VintageFix: the client registration uses explicit per-block state mappers and model locations, never iterates the model registry, but has not been run with it.
