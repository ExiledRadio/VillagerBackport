# Stream E: campfire

## Changelog section

## New: the campfire

*   **1.14's campfire**, with its recipe (three sticks, coal or charcoal, and three logs of any wood). It gives light level 15, crackles and sends up 1.14's big, slow smoke.
*   **Cooks four foods at once**, no fuel: right-click with raw meat, raw fish or a potato to lay it on the logs. Each one takes 30 seconds and pops off cooked. It cooks any furnace recipe that turns a food into a food, so other mods' raw foods work too; `campfire.extraFoods` and `campfire.excludedFoods` change the list, and `campfire.cookTime` the time.
*   **A signal fire**: put a hay bale under it and the smoke climbs about 24 blocks instead of about 10.
*   **It burns**: 1 damage every half second to anything standing in it, sneaking or not, as in current Java. Fire Resistance, Frost Walker and fire-immune mobs are safe. It never sets anything on fire, so mobs that die in it drop raw meat. Mobs walk around a lit one.
*   **Put it out** with a shovel (not from underneath), a water bucket, or a splash or lingering water bottle. **Light it** with flint and steel, a fire charge, a burning arrow, or a blaze's fireball (with `mobGriefing` on). Food on it stays put while it's out, and carries on cooking when it's relit.
*   Breaking it drops two charcoal, or the campfire with Silk Touch, plus whatever was cooking.
*   **Fishermen sell it**, and **taiga villages** have theirs again: five of the taiga houses and decorations place a lit campfire, which was an empty spot without Future MC.
*   **With Future MC**, its campfire and this one are a duplicated pair under `compat.futureMcDuplicates`, like the barrel and the lantern: one is hidden from the creative menu and JEI, loses its recipe, and villages build with the other.
*   `campfire.enabled` turns it off entirely.

Hay bales already cut fall damage by 80% in 1.12.2, exactly as in 1.14, so there was nothing to add there.

## Integrator notes

*   Shared files touched: `ModConfig.Campfire` (own category only), `NetworkHandler` (registers packet 18 and adds a `channel()` getter), `FutureMc.SHARED` (`{"campfire", null}`), `ModernTrades` (fisherman: `villagerbackport:campfire` before `futuremc:campfire`, the lantern's pattern), `BackportBlocks` (new `OWN` table for blocks this mod adds itself, consulted even with `villages.backportBlocks` off), `recipes/_factories.json` (`campfire` condition), `en_us.lang` and `sounds.json` (appended).
*   Departures from the brief, following minecraft.wiki: **sneaking does not protect** (19w04a removed that before 1.14 shipped); **food goes on unlit campfires too** and is kept when it goes out (current Java; 1.14.4 threw it off); the **hay-bale fall cut was not added** because vanilla 1.12.2's `BlockHay.onFallenUpon` already does `fall(distance, 0.2F)` (adding it again cut damage twice, which the test caught).
*   Packet 18 exists only to draw the 20-plume smoke burst when water douses a campfire; 1.12.2 has no world event for it.
*   Docs to update when folding in: `DESCRIPTION.md` and the 2.x compatibility tables still say Future MC is needed for the campfire.
*   The temporary test harness (`CampfireSelfTest`, `CampfireTemplateProbe`, `campfire_test.sh`) is in the shared notes folder under `campfire-test-harness/`, not in the repo.

## Tested headlessly (dedicated dev server, `runServer`, port 25705, 41/41 checks)

*   Registration: block, item, tile entity and sound register; the reobfuscated jar has SRG names; the dedicated server loads and ticks campfires without touching any client class.
*   Recipe rule: raw beef, potato and raw salmon (fish:1) accepted with the right results; chorus fruit, iron ore and pufferfish refused.
*   Placement: placed lit, light level 15, tile entity present, facing from the placer.
*   Food: one item per right-click, held stack shrinks, cobblestone refused, the four slots fill and a fifth is refused.
*   Cooking: beef and three potatoes pop off cooked after 30 s and the slots empty.
*   Extinguish and relight: shovel from below does nothing, shovel from above puts it out and takes 1 durability; food kept with its progress (100 ticks), which then counted back to 0 at 2 a tick while out; flint and steel relights (1 durability); water bucket puts out and leaves an empty bucket; fire charge relights and is used up.
*   Projectiles: a burning arrow dropped onto an unlit campfire lights it; a splash water bottle landing on a lit one puts it out.
*   Signal fire: `signal_fire` true on hay at placement, false when the hay is replaced, true again when it comes back.
*   Contact damage: a zombie boxed in on a lit campfire went from 20 to about 10 health in 5 s without catching fire; a zombie pigman beside it was untouched.
*   Drops: breaking it drops exactly two charcoal plus the raw chicken that was on it.
*   Hay bale: a zombie falling 20 blocks onto hay took 4 (20 - ceil(17 x 0.2)), one falling 21 onto stone took 18.
*   Villages: every `minecraft:campfire` in the five taiga templates resolves to `villagerbackport:campfire` with the template's facing, `lit` and `signal_fire`.
*   `campfire.enabled=false`: boots clean, nothing registered, no recipe errors (on a world that had campfires, FML asks to confirm the missing ids, as for any removed block).

## Not tested (needs a client, for the in-game check)

*   The models (lit and unlit, all four facings), the animated fire and lit-log textures, and the item icon.
*   The food drawn lying on the logs (`TileEntityCampfireRenderer`) and its rotation per slot.
*   Smoke: plume look, height (about 10 blocks, about 24 over hay), stopping under a block, the 20-plume burst on dousing, the small smoke from cooking food, lava sparks.
*   The crackle sound and its subtitle; the extinguish hiss.
*   Right-clicking by a real player (tested with a `FakePlayer` calling the block directly), including creative mode keeping the food and the bucket.
*   With Future MC installed (it crashes the FG2 dev env): which campfire is hidden, recipe removal, villages swapping to Future MC's under `KEEP_FUTURE_MC`.
*   A small fireball from a blaze or a dispenser lighting it; a lingering water bottle putting it out.
*   An actual taiga village generating with campfires in it (template resolution was tested, not world generation).
