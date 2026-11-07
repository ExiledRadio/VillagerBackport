# The backport family

One mod per Minecraft update, each named after it, all built to work together, plus a "Modern MC"
umbrella that requires every one of them. Together they replace Future MC.

## The mods

| Mod | Update | Mod id | State |
| --- | --- | --- | --- |
| **Backport Core** | (library) | `backportcore` | Not started; extract it when Update Aquatic Backport starts |
| **Update Aquatic Backport** | 1.13 | `updateaquaticbackport` | Next |
| **Village & Pillage Backport** | 1.14 | `villagerbackport` (kept from 1.x; never changes) | 2.3.0 released 2026-11-06 |
| **Buzzy Bees Backport** | 1.15 | `buzzybeesbackport` | Later |
| **Nether Update Backport** | 1.16 | `netherupdatebackport` | Later |
| **Caves & Cliffs Backport** | 1.17 and 1.18 | `cavesandcliffsbackport` | Later |
| **Wild Update Backport** | 1.19 | `wildupdatebackport` | Later |
| **Trails & Tales Backport** | 1.20 | `trailsandtalesbackport` | Later |
| **Tricky Trials Backport** | 1.21 | `trickytrialsbackport` | Later |
| **Modern MC** | all of them | `modernmc` | Requires all of the above; adds nothing of its own except a front page |

Icons for every one of them are in `docs/curseforge/family-icons`, drawn by
`docs/curseforge/icon-options/family_icons.py` in the shared frame. Mod ids are permanent the day a mod is released: they're in every world that uses the mod. Display
names can change any time.

## Rule 1: the mod that ships a block first owns it

Some blocks belong to one update but are needed by another. Village & Pillage Backport adds 1.13's
stripped logs and wood, blue ice, sea pickles, and wood trapdoors, buttons and pressure plates,
because 1.14's villages are built from them. They stay there.

When a later mod needs a block another backport owns:

- **The owner is installed**: register nothing; use the owner's block. For example, Update Aquatic
  Backport's axe stripping turns logs into Village & Pillage's stripped logs.
- **The owner isn't installed**: register its own copy, built from the same code with the same states
  and metadata, under its own mod id.
- **The owner is added to a world later**: the owner remaps the other mod's copies onto its own blocks
  as the world loads, using Forge's missing-mappings event and the same chunk conversion the Future MC
  migration uses (`migrate/` in Village & Pillage). Because the states match exactly, this is a rename,
  not a conversion, and nobody's build moves.

Backport Core keeps the table of who owns what, so every mod asks the same question the same way:
"is X installed? then use its block".

## Rule 2: every new backport adds compatibility with the older ones

When a later update changes something an earlier one added, the later mod's feature works with the
earlier mod's block. For example:

- Nether Update Backport's netherite upgrades through Village & Pillage's smithing table. The smithing
  table already reads netherite recipes from any mod, so this needs only a registration.
- Buzzy Bees Backport's iron golem repair works on Village & Pillage's golems and villages.
- Update Aquatic Backport's waterlogging covers Village & Pillage's lanterns, campfires and sea pickles.

None of these may *require* the other mod. Each mod checks `Loader.isModLoaded` and adds the integration
when the other is there. Each mod's own tests cover both cases: installed alone, and installed with the
others.

## Rule 3: other backport mods are never advertised

The family is the backport; descriptions, changelogs and in-game text never recommend or name another
backport mod (Future MC, Raids Backport and the like). The code still copes quietly when one is
installed:

- duplicate blocks are hidden, ours kept by default (`compat.futureMcDuplicates`);
- villages, trades and recipes use our blocks first;
- a world that had Future MC converts to ours as it loads, and `/futuremc convert` does it on demand.
  Each mod contributes its pairs to Backport Core's table; the recorded states come from `/futuremc dump`.

## What Backport Core holds

Code and shared assets, no blocks or items of its own:

- **Vanilla's new look**: the texture packs from Village & Pillage (1.14's Texture Update, 1.15's glint,
  the classic fallback) move here, with the `modernTextures` switch. Every later update's vanilla
  retextures and resound go in the same packs, so they apply whichever backports are installed - even
  Update Aquatic alone, the one update that changed almost no existing textures.
- **Waterlogging**: a 1.13 feature every later block wants (lanterns, campfires, sea pickles, chains,
  scaffolding). The framework lives in core so any backport's blocks can be waterlogged; Update Aquatic
  provides the water mechanics.
- **Terrain adapters for structures**: the code that plans villages on vanilla, superflat, OTG, Biomes
  O' Plenty and RTG terrain. Every later structure needs it too: shipwrecks, ocean ruins and buried
  treasure (1.13), bee nests (1.15), bastions and ruined portals (1.16).
- **Trade registry**: later updates add villager and wandering trader trades (kelp, coral and fish
  buckets from 1.13, honey from 1.15), so trades are registered through core rather than hard-coded.

- **Ownership table**: which mod owns each shared block, and the lookup every mod uses (Rule 1).
- **Migration**: missing-mapping remaps, chunk conversion with a per-chunk generation flag, content
  moving, stand-in items for one-to-many renames. Moved out of Village & Pillage's `migrate/`.
- **Future MC**: duplicate hiding, the `/futuremc` command, the recorded state table, extended per mod.
- **Smithing upgrades**: the registry the smithing table reads, so any backport (or other mod) can add
  netherite-style upgrades.
- **Shared helpers**: config category conventions, blockstate checks, the ore-dictionary names the
  backports agree on.

Each backport depends on Backport Core, the way Sophisticated Backpacks Legacy depends on Sophisticated
Core Legacy. Village & Pillage keeps working on its own copy of this code until the library exists;
the move is done once Update Aquatic Backport needs it.

## Order of work

1. Ship Village & Pillage Backport 2.3.0, still standalone, textures included.
2. Start Update Aquatic Backport. Extract Backport Core from Village & Pillage's `migrate/`,
   `compat/FutureMc`, smithing, terrain-adapter, trade and texture code as the first step, and release
   Village & Pillage 2.4.0 depending on it.
3. Update Aquatic content, Future MC pairs included. Swimming, crawling and waterlogging need player and
   world mixins: decide between writing them and supporting Aqua Acrobatics and Fluidlogged API before
   starting them.
4. Buzzy Bees, then Nether Update, each adding its Rule 2 integrations with the mods already out.
5. Modern MC, once there are at least two backports to bundle.
