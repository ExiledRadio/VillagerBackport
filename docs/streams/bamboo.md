# Stream D: bamboo, scaffolding, the bamboo jungle and pandas

Branch `v14-bamboo`. Package `com.exiledradio.villagerbackport.bamboo` (client half in `bamboo/client`).
Entity id 109 (panda). No packets. Config category `bamboo`. Test port 25704.

## Changelog section

## New: bamboo

*   **1.14's bamboo**, grown exactly by 1.14's rules: plant a piece on grass, dirt, podzol, sand, red sand, gravel or mycelium and a shoot comes up, then a stalk that grows a block at a time in light 9 or more, up to somewhere between 12 and 16 blocks tall.
*   The stalk **thickens** once it is three blocks tall, its **leaves** follow the top as it grows (large on the top two blocks, small below them, bare further down), and every block sits slightly off-center, so a grove looks wild rather than planted.
*   **Bone meal** adds one or two blocks at once. **A sword cuts it instantly**; an axe is the right tool otherwise. Break the bottom and the whole stalk comes down.
*   Two bamboo make a **stick**. Bamboo burns in a furnace for a quarter of an item.
*   **In jungles**: a stand turns up every few chunks in ordinary jungles and jungle hills, as in 1.14 (`bamboo.jungleBamboo`).

## New: scaffolding

*   **Crafted** from six bamboo and a string, six at a time. Breaks instantly and burns like bamboo.
*   **It holds itself up**, as in 1.14: a scaffold can stand on the ground, on more scaffolding, or reach out sideways from a supported column - up to six blocks. Further than that it won't place. Knock out its support and everything leaning on it comes down as items; take out the bottom of a tower and the whole tower falls.
*   A scaffold hanging out from a column shows its bottom frame, and you can stand on that frame.
*   **Moving through it**: walk straight through the sides, stand on top, **jump to climb**, **sneak to go down** - at 1.14's climbing speed, with no fall damage. This works with Aqua Acrobatics: the scaffolding only answers the questions the game already asks every block, and doesn't touch how players move.
*   **Placing it the 1.14 way**: use scaffolding on the top of a scaffold to extend outward in the direction you face, or on its side to build the column up to its top. Sneak to place it against the clicked face instead.
*   Not 1.14: the selection outline is always the whole block (1.14 shows the frame unless you're holding scaffolding).

## New: the bamboo jungle

*   **Bamboo jungle and bamboo jungle hills**: jungles full of bamboo, with patches of podzol under it, big jungle trees, parrots, ocelots - and pandas.
*   As in 1.14 they don't have a climate of their own: about one jungle area in ten is a bamboo jungle instead (`bamboo.bambooJungleRarity`), and its hills are bamboo jungle hills. Works with the default world type, large biomes, amplified, and biome mods that use vanilla's jungle. Not in Open Terrain Generator worlds, which choose their biomes themselves.
*   Registered with Forge's biome dictionary as jungle, hot, wet and dense (hills too for the hills), so other mods treat them as jungles.
*   `bamboo.bambooJungle` turns them off. New chunks only.

## New: pandas

*   **1.14's pandas**, with the model, textures and sounds. They live in bamboo jungles (spawn weight 80), and now and then one turns up in an ordinary jungle (weight 1). Spawn egg in the creative tab.
*   **Seven personalities**, from 1.14's two-gene inheritance: **lazy** pandas dawdle and lie on their backs, **worried** ones keep away from players and monsters and cower in thunderstorms, **playful** ones roll over all the time, **aggressive** ones keep fighting whatever hit them and join in when another panda is hit, **weak** ones have half the health and their cubs sneeze more, and **brown** pandas are brown. Brown and weak are recessive: a panda only shows them with both genes, so two ordinary-looking pandas can have a brown cub.
*   They **eat bamboo**, and **cake** if they find it: they pick it up off the ground, sit down and munch. Hand one bamboo and it sits and eats it.
*   **Breeding** with bamboo needs bamboo growing nearby. Without it the panda shakes its head and bleats instead.
*   **Cubs** roll about and **sneeze** - startling every grown panda nearby into a jump, and once in a while leaving a slime ball.
*   Drops a bamboo when killed.

## Works with Future MC

Future MC adds bamboo, scaffolding and pandas too. Which mod's bamboo and scaffolding stay visible follows `compat.futureMcDuplicates`, as it does for the workstations: the hidden pair loses its recipes and its place in JEI and the creative menu, but anything already placed still works. With this mod's kept, Future MC's pandas stop spawning; with Future MC's kept, this mod's pandas only live in bamboo jungles. Pandas eat and breed on either mod's bamboo. Future MC already puts its bamboo in jungles, so this mod's jungle bamboo stands aside while it's installed.

## New settings (`bamboo`)

*   `enabled` - everything above. Off, nothing is registered.
*   `bambooJungle`, `bambooJungleRarity` (10)
*   `jungleBamboo`
*   `pandaJungleWeight` (1), `pandaBambooJungleWeight` (80)

## Tested headlessly

On a dedicated dev server (`runServer`, port 25704, default world type, seed 12345), driven by a
temporary in-server harness that force-loads its test area (kept out of the commit; a copy is in the
group's notes folder as `stream-D-bamboo-BambooSelfTest.java.txt`). Final run: **49 of 49 passed**,
no exceptions in the log. The server booting at all proves no client-only class is loaded on a
dedicated server.

* Bamboo: registration, fuel (50 ticks each), the stick and scaffolding (x6) recipes, sword breaks it
  instantly (1.0 vs 0.0067 by hand), soils (sand, gravel, podzol yes; stone, farmland no), refuses
  water; placing the item makes a shoot, then thin stalk, and the shoot turns into stalk.
* Bone meal grows a shoot to 12-16 tall, stops at stage 1, leaves large/large/small/none from the
  top, thick above, thin at the base. Real random ticks (forced chunk, `randomTickSpeed` 60) grow a
  shoot into a stalk. Breaking the base drops the whole stalk as items (one earlier run counted one
  item short of 13 - looks like an item rolling out of the counting box; the final run counted all).
* Scaffolding: placed on stone at distance 0; clicking a side builds the column up; clicking the top
  extends six out with distances 1..6 and the bottom frame, never a 7th; removing the support
  collapses all 10 into items; a distance-7 scaffold falls as a falling block and lands.
* Scaffolding movement, with armor stands (same code path as players): lands and stands on top of a
  column; sneaking sinks through the column to the ground at 0.150 blocks a tick with no fall
  distance; jumping inside climbs the column.
* Pandas: two in love beside a bamboo stalk breed (brown x brown cub); two in love with no bamboo
  near do not; a panda picks up bamboo off the ground. Genes over 2000 cubs: brown x brown gives
  brown 94.5% (rest = 1/32 mutations), two weak carriers give weak 25%; recessive alone shows normal;
  weak = 10 health, lazy = 0.07 speed.
* Biomes: both registered, dictionary types, pandas weight 80 + parrots in the bamboo jungle, pandas
  weight 1 in jungle. Biome maps: default world type 9.9% of jungle is bamboo jungle (jungle 1411,
  hills 570, bamboo 151, bamboo hills 67 samples); large biomes 15%; the live world's own map has it.
* Generation: 100 chunks of a generated bamboo jungle held 21,253 bamboo blocks, 3,576 podzol, and 6
  pandas and 5-6 parrots spawned at generation; 100 chunks of ordinary jungle held 125 bamboo blocks
  (about a dozen stands) and no podzol.

Raw results of the final run:

```
PASS bamboo registered :: Block{villagerbackport:bamboo}
PASS bamboo fuel 50 :: 50
PASS scaffolding fuel 50 :: 50
PASS stick recipe ::
PASS scaffolding recipe x6 ::
PASS sword breaks bamboo instantly :: sword=1.0 hand=0.006666667
PASS bamboo on sand ::
PASS bamboo on gravel ::
PASS bamboo on podzol ::
PASS bamboo not on stone ::
PASS bamboo not on farmland ::
PASS bone meal grows 12..16 :: height=12 0n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1s0 1l0 1l1
PASS leaves large,large,small,none from top :: 0n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1s0 1l0 1l1
PASS top stopped growing (stage 1) or 16 :: villagerbackport:bamboo[age=1,leaves=large,stage=1]
PASS upper stalk thick :: 0n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1s0 1l0 1l1
PASS bottom stays thin :: 0n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1n0 1s0 1l0 1l1
PASS item on grass plants a shoot :: villagerbackport:bamboo_sapling
PASS item on shoot adds thin stalk :: villagerbackport:bamboo[age=0,leaves=none,stage=0]
PASS shoot under stalk turns into stalk :: villagerbackport:bamboo[age=0,leaves=none,stage=0]
PASS cannot plant into water ::
PASS bamboo jungle registered :: com.exiledradio.villagerbackport.bamboo.BiomeBambooJungle@237f45a6
PASS dictionary types ::
PASS pandas weight 80 in bamboo jungle, parrots too ::
PASS pandas weight 1 in jungle ::
PASS default world: bamboo jungle is ~10% of jungle :: jungle=1411 jungleHills=570 bamboo=151 bambooHills=67 share=0.099
PASS largeBiomes world: bamboo jungle is ~10% of jungle :: jungle=1125 jungleHills=433 bamboo=201 bambooHills=81 share=0.153
PASS this world's biome map has bamboo jungle :: bamboo=268 jungle=1530
PASS scaffold placed on stone, distance 0 :: villagerbackport:scaffolding[bottom=false,distance=0]
PASS clicking the side builds the column up :: villagerbackport:scaffolding[bottom=false,distance=0]
PASS top-click extends 6 out, distances 1..6 bottom, never 7 :: 1b 2b 3b 4b 5b 6b - -
PASS unsupported structure collapses and drops :: gone=true items=10
PASS distance-7 scaffold falls as a block and lands :: villagerbackport:scaffolding[bottom=false,distance=0] falling=0
PASS stands on top of a column :: y=204.0 want 204
PASS sneaking sinks through the column to the ground :: y=200.0 from 204.0
PASS sinking speed capped at 0.15/tick, no fall distance :: maxSpeed=0.1500000029193984 maxFall=0.0
PASS jumping inside climbs the column :: y=204.4952008770059 from 200.0
PASS brown x brown cubs are brown (bar mutation) :: 1889/2000
PASS two weak carriers give ~1/4 weak cubs :: 479/2000
PASS recessive main gene alone shows normal :: NORMAL
PASS weak panda has 10 health :: 10.0
PASS lazy panda speed 0.07 ::
PASS breaking the base drops the whole stalk :: before=12 left=0 items=12 all=1xtile.villagerbackport.scaffolding@0@-199,200,174 1xtile.villagerbackport.scaffolding@0@-197,200,174 2xtile.villagerbackport.scaffolding@0@-198,200,173 3xtile.villagerbackport.scaffolding@0@-196,200,172 2xtile.villagerbackport.scaffolding@0@-194,203,174 1xtile.villagerbackport.scaffolding@0@-193,200,174 1xtile.villagerbackport.bamboo@0@-202,200,205 3xtile.villagerbackport.bamboo@0@-201,200,205 3xtile.villagerbackport.bamboo@0@-203,200,203 1xtile.villagerbackport.bamboo@0@-203,200,204 3xtile.villagerbackport.bamboo@0@-201,200,202 1xtile.villagerbackport.bamboo@0@-204,200,203
PASS random ticks grow a shoot into a stalk :: height=6 0n0 1n0 1n0 1s0 1l0 1l0
PASS pandas breed beside bamboo :: cubs=1 BROWN/BROWN
PASS pandas do not breed without bamboo :: cubs=0 pandas=3
PASS panda picked up the bamboo (holding or ate it) :: held=1xtile.air@0 itemsLeft=0 sitting=false eating=false
PASS bamboo jungle chunks carry bamboo and podzol :: bambooBlocks=21253 podzol=3576 pandas=6 parrots=5
PASS pandas spawned in the bamboo jungle at generation :: pandas=6 parrots=5
PASS ordinary jungle has some bamboo, no podzol from bamboo :: bambooBlocks=125 podzol=0 pandas=0
```

## Not tested (client side, for the in-game check)

*   How anything looks: bamboo models (thickness, leaves, the off-center nudge, the four stalk variants), the shoot, scaffolding (stable and bottom frames), item icons, the panda model and its poses (sitting, eating, lying on its back, the roll somersault, cowering, head-shaking, the sneeze), the held bamboo, the seven textures, cub proportions.
*   Sounds and subtitles (bamboo, shoot, scaffolding and all panda sounds).
*   **Scaffolding with a real player**: the headless test used armor stands, which share the code path (collision per entity, the ladder check, jumping and sneaking), but player movement runs on the client. Check: walking through the sides, standing on top, jump to climb, sneak to descend (and no fall damage), stepping off a column edge while sneaking, the bottom frame, placing by clicking top and side, sneak-placing, and all of it with Aqua Acrobatics installed.
*   Placing and breaking by hand: bamboo with a sword vs an axe vs a hand, the outline of bamboo and scaffolding.
*   Pandas following a player holding bamboo; handing bamboo to a panda; flipping a lazy panda back over with a click.
*   Future MC side by side (the dev environment can't load Future MC): JEI/creative hiding, recipe removal, panda spawn removal.
*   Biome colors of the bamboo jungle (it should look like a jungle), and the biome name in F3.

## Shared files touched

`ModConfig` (Bamboo category only), `client/ClientSetup` (one line: `BambooClient.init()`), `compat/JeiPlugin` (one loop: hide `BambooFutureMc.hiddenItems()`), `recipes/_factories.json` (the `bamboo` condition), `en_us.lang` and `sounds.json` (appended blocks).
