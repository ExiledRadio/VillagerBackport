# Stream H: village blocks

Branch `v14-village-blocks`. Package `com.exiledradio.villagerbackport.woodset`. Config `villageBlocks`.

## Changelog section

## New: the last blocks 1.14's villages are built from

Villages no longer borrow these from other mods or fall back to 1.12.2 stand-ins:

*   **Stripped logs and stripped wood in all six woods**, with 1.14's textures. Stripped logs face whichever way you place them, like logs. Four stripped logs make three stripped wood. Both make four planks, smelt into charcoal, burn in a furnace and catch fire like logs, keep leaves from decaying, and are `logWood` in the ore dictionary.
*   **Strip logs with an axe**: right-click a log with any axe, modded axes included, and it becomes the stripped log, facing the same way. Bark blocks become stripped wood. You hear 1.14's strip sound, and the axe takes one point of damage. If Future MC or Quark already strips logs in your pack, theirs is left to do it.
*   **Trapdoors, buttons and pressure plates in spruce, birch, jungle, acacia and dark oak**, each crafted from its own planks with 1.14's recipe and drawn with 1.14's trapdoor textures. They work exactly like the oak ones: the same redstone, the same timing, the same sounds. Buttons burn for 100 ticks in a furnace, trapdoors and pressure plates for 300.
*   **Blue ice**, from nine packed ice. Slipperier than packed ice (boats on it are fast), never melts, and only drops with Silk Touch. Mined fastest with a pickaxe.
*   **Sea pickles**: place up to four in one block by right-clicking with more. In water they glow, brighter the more there are; out of water they are dark. They need a block with a top to stand on, smelt into lime dye and compost at 65%.
*   **Villages use them**: taiga villages get real spruce trapdoors and pressure plates, snowy villages real stripped spruce logs and wood and blue ice, jungle buttons, acacia pressure plates and so on, wherever 1.12.2 used a stand-in. If Future MC, Quark, Farmer's Future Delight or Oceanic Expanse provides one of these blocks, villages keep using theirs.
*   Everything is in this mod's creative tab.

## Changed: three vanilla recipes, as in 1.14

The **trapdoor**, **wooden button** and **wooden pressure plate** now take oak planks only, since the other woods make their own. They are renamed **Oak Trapdoor** and **Oak Pressure Plate**, as in 1.14. (Vanilla's wooden button shares its name with the stone button, so it stays "Button".)

## With Future MC

Future MC adds stripped logs and wood, the five wood trapdoors and blue ice too. `compat.futureMcDuplicates` covers them like the rest: by default Future MC's are hidden (no recipe, JEI entry or creative listing; placed ones keep working) and villages build with this mod's. `KEEP_FUTURE_MC` hides this mod's instead. Future MC has no wood buttons, pressure plates or sea pickles, so there is nothing to pair those with.

## Config

*   `villageBlocks.enabled`: everything above. Off, nothing is registered, axes strip nothing, vanilla's recipes are untouched and villages use other mods' blocks or 1.12.2's stand-ins as before. Turning it off in a world that has the blocks deletes them.
*   `villageBlocks.axeStripping`: stripping logs with an axe.

## Design notes (for the integrator)

*   **Stripped logs are `BlockLog` subclasses** with vanilla's `axis` property (`x`/`y`/`z`/`none`), so they are wood to leaf decay, tree fellers and anything checking `instanceof BlockLog`. `axis=none` draws as stripped wood; nothing in survival reaches it. Stripped wood keeps an axis too, as 1.14 and Future MC do, so village `axis` values and Future MC's twin carry across by name.
*   **Unstripped 1.14 wood is not added**: 1.12.2's log `axis=none` is that block already, and stripping it gives stripped wood.
*   **Trapdoors, buttons and plates** subclass the exact vanilla class with the oak one's material, hardness, sound and sensitivity, so redstone behavior is vanilla's own code. Buttons and plates use 1.12.2's planks textures so they match 1.12.2 planks; trapdoors use 1.14's textures.
*   **Sea pickles and water**: 1.12.2 has no waterlogging. The block keeps 1.14's `waterlogged` property name (so structure files and 1.14's live/dead model split fit unchanged) and treats "water directly beside or above, or the block it replaced was water" as in water; it is re-checked whenever a neighbor changes. Limitation: a lone pickle in a one-block water hole, with no water beside or above, goes dark the first time a neighbor changes. The material blocks liquids, so flowing water does not wash pickles away, while the block has no collision box. Bone meal spreading is skipped (1.12.2 has no coral). Lime dye is vanilla 1.12.2's (`dye:10`), so the smelting recipe does not depend on the flora stream.
*   **Stripping and other mods**: the handler listens at `LOW` and ignores cancelled events. With Future MC's stripped logs hidden in favor of these, Future MC's own stripping (if it runs first) still makes its own stripped logs; this mod does not fight it.
*   **Shared files touched**: `ModConfig` (new `villageBlocks` category at the end), `en_us.lang` (config key beside the others, `# --- village-blocks ---` block at the end), `sounds.json` (`item.axe.strip` appended), `village/modern/BackportBlocks.java` (this mod's blocks as the last option for stripped logs/wood, trapdoors, buttons, pressure plates, blue ice, sea pickle), `compat/FutureMc.java` (pairs from `WoodsetContent.futureMcPairs()`).
*   **Tools**: `tools/woodset/generate.py` writes the blockstates, models and lang block from `Wood.java` and copies the 1.14.4 textures and sea pickle models from the 1.14.4 jar; `tools/woodset/check_blockstates.py` checks drawing (below); `tools/woodset/selftest/WoodsetSelfTest.java` is the headless test, kept out of the source tree.

## Tested headlessly

Dedicated dev server (`runServer`, port 25708, fresh `woodtest` world), `WoodsetSelfTest` copied in for the run and removed after: **`SELFTEST DONE pass=1717 fail=0`**. The server booting at all is the proof that no client type leaks into a server-loaded class (`woodset/client/WoodsetClient` is the only client code).

*   **Registration**: 29 blocks, each with its `ItemBlock`, in this mod's creative tab; every valid state survives a meta round trip.
*   **Blockstates**: the self-test writes every block's `getValidStates()` as vanilla's variant string (properties sorted by name, `name=value` joined by commas) to `eclipse/woodset-states.txt`; `python tools/woodset/check_blockstates.py` confirmed **29 blocks, 207 states, 0 errors**: each variant is in the blockstate file, the file lists nothing extra, every model and parent exists (ours or 1.12.2's), every texture an element uses resolves to an existing file, every item model resolves, every block has a lang entry, and the four strip sounds exist. Checked that it fails on a missing variant (fed it one extra `powered=false` trapdoor state: 1 error).
*   **Crafting**: stripped log and wood to 4 planks of their wood, 4 stripped logs to 3 stripped wood, each wood's trapdoor (2), button and pressure plate from its own planks, oak planks still making vanilla's trapdoor, button and plate, and vanilla's three recipes still registered under their names; 9 packed ice to blue ice.
*   **Smelting, fuel, fire, ore dictionary**: stripped logs/wood smelt to charcoal, burn 300, flammability and encouragement 5, in `logWood`; buttons burn 100, trapdoors and plates 300; sea pickle smelts to lime dye.
*   **Stripping** through the server's real right-click path (`PlayerInteractionManager.processRightClickBlock`): all 24 vanilla log states (6 woods x 4 axes, including `axis=none` bark to stripped wood with axis `y`) strip to the right block and axis, `SUCCESS`, axe damage exactly 1; a second click does nothing; a stick does nothing; an already-cancelled event is left alone; `axeStripping=false` does nothing; a golden axe strips; an item reporting tool class `axe` counts as an axe; planks are not strippable.
*   **Logs**: placed on an east face get `axis=x`, are wood and sustain leaves, harvest tool axe, rotate in structures, drop themselves.
*   **Trapdoors, buttons, plates**, each wood beside vanilla's oak ones: trapdoors place facing/half from the click, open and close by hand and by a redstone block, hardness 3; buttons place on a wall, press to 15 weak and strong power, power the block they are on, schedule their release, same tick rate as oak; plates are pressed by an item entity and power the block below.
*   **Blue ice**: slipperiness 0.989, hardness 2.8, drops nothing, silk-harvestable, no random tick, `PACKED_ICE`, pickaxe.
*   **Sea pickles**: placed dry is dark with no collision; right-clicks add up to 4 and the fifth is refused, using one item each; water placed beside lights it to 15, draining darkens it; light 6/9/12/15 by count; boxes no taller than 7/16; removing the support pops all four as 4 items; cannot be placed over air; placed on a pool floor replacing water it is lit (6); a lone pickle replacing the only water is lit.
*   **Villages, every palette**: all 492 structure files loaded through `ModernTemplate`, and each palette entry naming one of these blocks resolved through the real path (`BackportBlocks` then `FutureMc.preferred`): 107 files, 286 entries (spruce trapdoor 151, stripped spruce wood 45, stripped spruce log 40, jungle button 16, stripped oak log 10, acacia pressure plate 7, jungle trapdoor 4, spruce pressure plate 4, blue ice 4, sea pickle 2, stripped acacia log 2, stripped oak wood 1) all became this mod's block with the file's `axis`, `facing`, `half`, `open`, `powered` and `pickles`.
*   **Real villages**: the nearest taiga and snowy villages were located and their chunks generated by the world generator; the taiga village contained 102 spruce trapdoors, the snowy one 113 blue ice, 270 stripped spruce logs and 369 stripped spruce wood.
*   **Future MC**: 18 pairs; with Future MC absent nothing is hidden, and the hidden-items list never names a `minecraft:` item.
*   `./gradlew build --rerun-tasks -PdeployDir=` is clean; the release jar's classes are reobfuscated (SRG names) and carry no self-test.

## Not tested

*   **Anything visual** (needs a client): how each block, item and the sea pickle's live/dead models look in game and in the inventory, the trapdoors' textures and cutout, the button and plate textures, the strip sound actually playing and its subtitle, the arm swing, and the client-side prediction of a strip.
*   **With Future MC, Quark, Farmer's Future Delight or Oceanic Expanse installed** (they crash the FG2 dev env): duplicate hiding of the 18 pairs, villages preferring their blocks, and how this mod's stripping and Future MC's or Quark's coexist on one click.
*   Sea pickle behavior in a live world over time (water flowing in and out, chunk reloads), and boats on blue ice.
