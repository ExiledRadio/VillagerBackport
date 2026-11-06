# The backport family

One mod per Minecraft update, each named after it, all built to work together, plus a "Modern MC"
umbrella that requires every one of them. Together they replace Future MC.

## The mods

| Mod | Update | Mod id | State |
| --- | --- | --- | --- |
| **Backport Core** | (library) | `backportcore` | Not started; extract it when Update Aquatic Backport starts |
| **Update Aquatic Backport** | 1.13 | `updateaquaticbackport` | Next |
| **Village & Pillage Backport** | 1.14 | `villagerbackport` (kept from 1.x; never changes) | 2.3.0, unreleased |
| **Buzzy Bees Backport** | 1.15 | `buzzybeesbackport` | Later |
| **Nether Update Backport** | 1.16 | `netherupdatebackport` | Later |
| **Modern MC** | all of them | `modernmc` | Requires all of the above; adds nothing of its own except a front page |

Mod ids are permanent the day a mod is released: they're in every world that uses the mod. Display
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

## Rule 3: Future MC is a guest

Every block a backport shares with Future MC follows Village & Pillage's pattern:

- `compat.futureMcDuplicates`: hide one of each pair (default: keep ours).
- Villages, trades and recipes use whichever copy is kept.
- `/futuremc convert` and the automatic conversion when Future MC is removed cover the mod's blocks too:
  each mod contributes its pairs to Backport Core's table, and the recorded states come from
  `/futuremc dump`.

## What Backport Core holds

Code only, no blocks or items of its own:

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

1. Ship Village & Pillage Backport 2.3.0, still standalone.
2. Start Update Aquatic Backport. Extract Backport Core from Village & Pillage's `migrate/`, `compat/FutureMc`
   and smithing code as the first step, and release Village & Pillage 2.4.0 depending on it.
3. Update Aquatic content, Future MC pairs included. Swimming, crawling and waterlogging need player and
   world mixins: decide between writing them and supporting Aqua Acrobatics and Fluidlogged API before
   starting them.
4. Buzzy Bees, then Nether Update, each adding its Rule 2 integrations with the mods already out.
5. Modern MC, once there are at least two backports to bundle.
