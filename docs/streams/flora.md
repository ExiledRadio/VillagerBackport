# Stream F: flowers, dyes, suspicious stew and leather horse armor

Branch `v14-flora`. Package `com.exiledradio.villagerbackport.flora` (client code in `flora/client`),
packet 19, config category `flora`.

## Changelog section

## New: flowers

*   **Cornflower**, **lily of the valley** and **wither rose**, with 1.14's textures. They stand where flowers stand, sit slightly off-center like 1.12.2's own, go in flower pots, and compost at 65%.
*   **Where they grow**: cornflowers in plains, sunflower plains and flower forests; lilies of the valley in forests, birch forests, dark forests (and their hills) and flower forests, as current Java. Other mods' plains and broadleaf forests grow them too, by their biome type. New chunks only.
*   **Bone meal** on grass grows cornflowers in plains, sunflower plains and flower forests, and lilies of the valley in flower forests only, as in current Java.
*   **The wither rose** withers anything that walks through it, except the Wither, wither skeletons and players in creative, and not on Peaceful. It stands on netherrack and soul sand as well as grass and dirt, and smokes. In a pot it's harmless.
*   **When the Wither kills anything** - a mob, a villager, a player - a wither rose is planted where it died, or dropped if it can't be (no room, the wrong ground, or `mobGriefing` off).

## New: dyes

*   **White, black, brown and blue dye**, 1.14's own items. Bone meal or a lily of the valley makes white dye, an ink sac or a wither rose makes black, cocoa beans make brown, lapis lazuli or a cornflower makes blue.
*   **They work everywhere a dye does**: wool, glass, terracotta, beds, banners at the loom and in the grid, dye mixing, firework stars, leather armor, sheep, wolf collars, signs, and every other mod's recipe that asks for a dye by its ore dictionary name. Shepherds buy them.
*   **Nothing about bone meal, ink sacs, cocoa beans or lapis changes.** They're still dyes too, and still everything else they were.

## New: suspicious stew

*   A bowl, a red and a brown mushroom and any small flower make **suspicious stew**: mushroom stew with that flower's effect, at current Java's durations (a cornflower gives 5 seconds of Jump Boost, a lily of the valley 11 of Poison, a wither rose 7 of Wither, and so on). JEI shows a recipe for each flower. It doesn't stack.
*   **Expert farmers** sell it for an emerald, in six flavors, as in 1.14.

## New: brown mooshrooms

*   **Lightning** turns a red mooshroom brown and a brown one red, without hurting it, with 1.14's sound.
*   **Feed a brown mooshroom a small flower**, then milk it with a bowl, for that flower's suspicious stew. Feed it a second one first and it just puffs smoke. Milked again, it gives plain mushroom stew until it eats another flower. Works in creative too.
*   **Sheared**, a brown mooshroom drops brown mushrooms.
*   **Breeding**: calves take their parents' color (one time in 1024 the other one); a red and a brown parent have either.
*   Red mooshrooms are exactly as they were. Brown ones are still 1.12.2's mooshroom underneath, so other mods treat them as mooshrooms.

## New: leather horse armor

*   1.14's **leather horse armor**: 3 armor points, crafted from seven leather in an H. **Dye it** in the crafting grid like leather armor, with any dye; wash the dye off in a cauldron. Horses wear it in its color.
*   **Expert leatherworkers** sell it, dyed, for 6 emeralds, as in 1.14.

## New: the Globe banner pattern

*   1.14's fifth banner pattern, the **Globe**, sold by **master cartographers** for 8 emeralds in place of the stand-in they sold before. Use it at the loom like the other four; it isn't used up. With Future MC installed its Globe is used rather than adding a second one.

## New: dyed sign text

*   **Right-click a sign with any dye** to recolor its text, as in 1.14. The dye is used up only if the color changes. Every player with the mod sees the color.

## Future MC

*   Future MC adds the same flowers, dyes and suspicious stew. Which copy you see follows `compat.futureMcDuplicates`, as the workstations do: with `KEEP_OURS` (the default) Future MC's leave the creative menu and JEI and lose their recipes, and each can be crafted into this mod's one; with `KEEP_FUTURE_MC` it's the other way round, and this mod's flowers stop generating. Brown mooshrooms, leather horse armor, the Globe and dyed signs stay either way - Future MC has none of them.

## Settings

New `flora` section: `enabled` (everything above), `generateFlowers`, `brownMooshrooms`, `dyeableSigns`, `witherKillsPlantRoses`.

## What was tested

Headless, on a dedicated dev server (`runServer`, port 25706, seed 4242, DEFAULT world), by
`flora/FloraSelfTest.java`, which runs only with `-Dvillagerbackport.floraSelfTest=true` (passed through
`JAVA_TOOL_OPTIONS`) and drives everything through Forge's own hooks with a real `EntityPlayerMP`. It
logs one `[FloraSelfTest] PASS`/`FAIL` line per check and stops the server. Result on the final run:
**109 checks, 109 passed.** `./gradlew build --rerun-tasks -PdeployDir=` is clean and the jar is reobfuscated
(SRG names checked in `flora/ItemModernDye.class`).

*   **Registration**: 6 blocks, 10 items, the four ore dictionary names plus `dye`, the Globe pattern
    (hash `glb`) as a special loom pattern (index 35 of 40) with the free list still 34 long, the leather horse armor type
    (3 points), compost chances, stew stack size.
*   **Recipes**, through `CraftingManager.findMatchingResult` as a crafting table asks: all seven dye
    recipes; a lone white dye crafting nothing; black dye + wool -> black wool; blue + white dye -> light
    blue dye; brown dye stains glass; nine white dye do not make a bone block; blue dye on a leather
    helmet gives exactly lapis's color; white dye colors a firework star; black dye adds a banner
    pattern; the leather horse armor recipe; dyeing it with a vanilla and a new dye (exact colors); every
    flower's stew recipe (effect and duration) and the plain mushroom stew recipe unchanged.
*   **Eating** every flower's stew: the right effect and duration, and a bowl back.
*   **Mooshrooms**: a real lightning bolt ticked four times turns a red one brown exactly once, with no
    damage or fire; brown survives save/load; feeding a cornflower (consumed), a second flower refused,
    milking gives Jump Boost 5 s stew and forgets the flower, then plain mushroom stew; a red one ignores
    flowers and still gives mushroom stew; a second bolt turns it red; breeding odds over 400 calves each
    (brown x brown, brown x red); shearing with real shears through Forge's `IShearable` path drops 5
    brown mushrooms and leaves a cow, and a red one sheared in the same tick still drops red.
*   **Horses**: a horse accepts dyed leather horse armor in its armor slot, gets 3 armor points, reports
    the leather type, and keeps it through save/load. A filled cauldron washes the dye off and drops a level.
*   **Signs**: red dye (vanilla) and blue dye (new) recolor the text, the same color costs nothing, the
    color is in the update tag a client receives and survives a reload.
*   **Flower pots**: a wither rose goes into a vanilla pot (pot becomes `potted_wither_rose`), comes back
    out to an empty hand leaving an empty pot; a broken potted cornflower drops the pot and the flower.
*   **Wither rose**: stands on netherrack, not on stone; withers a zombie (40 ticks), not a wither
    skeleton; a zombie killed by the Wither leaves a planted rose, one killed on stone drops the rose as an
    item, and other deaths leave nothing.
*   **Animals**: the new brown dye dyes a sheep, the new blue dye a tamed wolf's collar (without making it
    sit). Bone meal in plains grows cornflowers, in flower forests lilies, and in plain forests no lilies.
*   **Trades**: the expert farmer's pool has six stews with effects for 1 emerald, the expert leatherworker
    sells dyed leather horse armor for 6, the master cartographer the Globe for 8, the apprentice shepherd
    buys the new white dye.
*   **Worldgen**: chunks generated around a plains and a forest found by the biome provider contain
    cornflowers and lilies of the valley: 380 cornflowers in 49 plains chunks (about 8 a chunk) and 204
    lilies in 49 forest chunks (about 4 a chunk). The density is a judgment call (1.14 places them by
    noise-driven flower features 1.12.2 lacks); the constants are at the top of `FloraWorldGen`.
*   **Bone meal**: 74 of 400 plains plantings were cornflowers; 25 of 400 flower forest plantings were lilies.
*   **`flora.enabled=false`**: a dedicated server boots and stops cleanly, including loading the test world
    that already had flora blocks in it (with FML's missing-block confirmation answered).
*   **Dedicated server boot**: the server starts and stops cleanly with the client classes in
    `flora/client` present, so no client type leaks into a server-loaded class.

## Not tested (client side, needs the in-game check)

*   How everything looks: flower, potted flower, dye, stew, horse armor and Globe pattern item models and
    textures; the wither rose's smoke.
*   **Brown mooshroom rendering** (`RenderBrownMooshroom`, swapped in from `RenderLivingEvent.Pre`), and that
    the client learns the color from packet 19 when a brown mooshroom comes into view and when lightning
    changes it.
*   **Leather horse armor on a horse** (`LayerLeatherHorseArmor`, the grey texture tinted by the dye). The
    texture is 1.12.2's iron horse armor redrawn in grey, since 1.14's leather texture is laid out for
    1.14's horse model; check it looks right and doesn't z-fight. Also the item's tint in the inventory.
*   **Dyed sign text** drawn by `TileEntityDyedSignRenderer`, which replaces vanilla's sign renderer (undyed
    signs go to vanilla's code). Check it with a resource pack or mod that also touches signs.
*   **Globe on a banner and a shield** (textures in `assets/minecraft/textures/entity/banner|shield/globe.png`)
    and in the loom's pattern slot, and the banner tooltip line ("Red Globe").
*   Mooshroom sounds and subtitles, and the client-side cancel on flower/bowl clicks (the server decides;
    check the held item updates without a flicker).
*   Future MC side by side (it crashes the FG2 dev env): `KEEP_OURS` hiding Future MC's flowers, dyes and
    stew and the conversion recipes; `KEEP_FUTURE_MC` hiding ours and stopping our worldgen; reusing Future
    MC's Globe constant.

## Notes for the integrator

*   `FloraSelfTest` is gated by a system property and does nothing otherwise; delete it before release if
    you'd rather not ship it.
*   Shared files touched: `ModConfig` (Flora category only), `NetworkHandler` (packet 19 and `sendFlora`),
    `ModernTrades` (farmer stews, leatherworker horse armor, cartographer Globe, shepherd buys the new dyes),
    `BannerPatterns` (the Globe joins the special patterns, before Creeper), `Compostables` (a public
    `addItem`), `JeiPlugin` (one loop), `en_us.lang` and `sounds.json` (appended blocks).
*   Wood signs from the building stream: if their tiles extend `TileEntitySign` they can already be dyed; their
    own TESR should draw text in `SignColor.textColor(tile)`.
*   The wandering trader stream can sell `villagerbackport:cornflower`, `lily_of_the_valley`, and the four dyes.
