# Finishing 1.14 — parallel work plan

Goal: everything Java 1.14 (Village & Pillage) added that this mod doesn't have yet, built by several
agents at once, each in its own git worktree, merged here one at a time. Release target **2.3.0**.
Behavior follows minecraft.wiki (current Java), not literal 1.14.4 code, as for the rest of the mod.

## Where this fits: one mod per update

| Mod | Covers | Status |
| --- | --- | --- |
| **Village & Pillage Backport** (this mod, mod id stays `villagerbackport`) | all of 1.14 | this plan |
| Buzzy Bees Backport | 1.15: bees, honey, hives, iron golem cracks and repair | later |
| Update Aquatic Backport | 1.13: oceans, ocean mobs, trident, coral, swimming, waterlogging | later |
| **Modern MC** | an umbrella that requires all of them | when two or more exist |

- The mod id never changes; only the display name does. Renaming the id breaks every world.
- Each backport knows the others and steps aside or hides duplicates, as this mod already does for
  Future MC (`compat.futureMcDuplicates`) and the crossbow (`compat.crossbow`).
- Shared code (step-aside helpers, duplicate hiding, config plumbing) moves into a small core library
  when the second mod starts, as Sophisticated Core Legacy was split out of Backpacks Legacy. Not now.
- Modern MC is a mod with required dependencies, not a merged jar: one set of jars, two audiences,
  no duplicate mod ids.
- Player pose changes (sneaking at 1.5 blocks, crawling) need mixins into the player and are shared with
  1.13 swimming, so they wait for the Update Aquatic work. Not in this plan.

## Workstreams

Each stream owns one new package and the IDs below. None depends on another.

| # | Stream | Content | Package | Entity IDs | Packet IDs |
| --- | --- | --- | --- | --- | --- |
| A | **Wandering trader** | Wandering trader and trader llamas: spawning near players (1.14 timing, 48-block radius, bell preference), their trade table, invisibility potion at dusk and milk at dawn, despawn timer, llamas leashed to it, spawn eggs | `trader/` | 105-106 | 10-11 |
| B | **Cats** | Cats split from ocelots: 11 variants, taming with raw fish, sitting, morning gifts, spawning in villages (per bed count) and witch huts (black cat), creepers and phantoms fleeing them; ocelots become untameable with the trust mechanic; *A Complete Catalogue* advancement | `cat/` | 107 | 12-13 |
| C | **Foxes and berries** | Sweet berry bush (growth, damage, harvest, taiga and giant tree taiga generation), sweet berries item and compost value; foxes: red and snow types, sleeping by day, pouncing, carrying an item in the mouth, trust after breeding, eating berries | `fox/` | 108 | 14-15 |
| D | **Bamboo and pandas** | Bamboo (growth, saplings, leaves stages, fuel, sticks), scaffolding (placement rules, collapse, climbing, dropping through), bamboo jungle biome and its hills, bamboo in jungles; pandas with their 7 personalities, eating bamboo and cake, rolling, sneezing slime balls | `bamboo/` | 109 | 16-17 |
| E | **Campfire** | Campfire: four cooking slots, smoke signal over a hay bale, contact damage, lit and unlit, shovel and water put it out, flint and steel or fire arrows light it, smoke particles; hay bale cutting fall damage by 80% | `campfire/` | — | 18 |
| F | **Flowers, dyes and stew** | Cornflower, lily of the valley, wither rose (and a wither kill planting one); white, black, brown and blue dye items with 1.14's recipes; suspicious stew and brown mooshrooms; leather horse armor (dyeable, sold by leatherworkers); the Globe banner pattern; dyeing sign text | `flora/` | — | 19 |
| G | **Building blocks** | 1.14's stone slab, stair and wall sets (about 40 blocks: stone, andesite, diorite, granite and their polished forms, mossy stone brick and cobble, smooth and cut sandstone, red sandstone, smooth quartz, red nether brick, end stone brick, brick, prismarine, nether brick walls), smooth stone, signs in the five other woods; stonecutter recipes for all of them; Future MC duplicate hiding | `building/` | — | — |

Stream sizes differ: D and G are the largest, E the smallest.

## Rules every stream follows

**Shared files.** Every stream has to touch a few files that all the others touch too. To keep merges
mechanical:

- **`ModConfig.java`** has an empty category per stream (`trader`, `cats`, `foxes`, `bamboo`,
  `campfire`, `flora`, `building`), added before work starts. A stream only edits inside its own
  category class. Category classes extend `Object` (a subclass crashes 1.12 at startup).
- **`en_us.lang`**, **`sounds.json`**: append keys in a block headed `# --- <stream> ---` at the end.
  Merge conflicts there are expected and are the integrator's job.
- **Registration**: each stream registers its own blocks, items, entities and sounds from its own
  `@Mod.EventBusSubscriber` class in its package, not in `ModSounds` or `IllagerContent`.
- **Lists the mod already keeps** (`ModernTrades`, Future MC duplicate lists, JEI, creative tab,
  `CHANGELOG.md`): small additions are fine; the integrator resolves overlaps.
- **Docs**: each stream writes its changelog section in `docs/streams/<stream>.md`; the integrator
  folds them into `CHANGELOG.md` and `DESCRIPTION.md`.

**Build and test.**

- Always build with `-PdeployDir=` (empty). Without it, `build` copies the jar into the user's
  RLCraft Dregora instance.
- Test on a dedicated dev server (`runServer`) in the worktree's own run dir, never
  `cleanRunServer` (it wipes worlds and configs); rerun with `--rerun-tasks`. Use server port
  `25700 + stream number` (A=25701 … G=25707). Kill only servers you started, by PID.
- Future MC and Deeper Depths crash the FG2 dev env; test with ore-name stand-ins.
- Client-only types never appear in a method signature of a class the server loads; put them in a
  `client/` class.
- Assets (textures, sounds, models) come from the 1.14.4 client jar at
  `G:/Sort this shit/Minecraft/Install/versions/1.14.4/1.14.4.jar`; behaviour numbers from its
  bytecode with Mojang's 1.14.4 mappings when the wiki is unclear.
- No mixins and no coremod.

**Done means**: content registered and working in a headless dev-server test, `build` clean, a stream
doc listing what was tested and what wasn't (anything client-side), and a commit on the stream's
branch. Nothing pushed, nothing merged into the main branch.

## Integration (this session)

1. Prep commit before any stream starts: the seven empty config categories and this plan.
2. Merge streams one at a time as they finish, resolving lang, sounds and list conflicts.
3. After each merge: build, boot a dedicated server, and give the user a jar for the in-game check.
4. When all seven are in: fold the stream docs into the changelog and description, bump to 2.3.0,
   rename the display name, commit.
