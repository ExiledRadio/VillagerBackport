# Pillage plan: raids, Hero of the Village, pillagers

Status (2026-11-01): **D1 decided: own implementation + Raids Backport bridge. P1, P2 and P3 built
for 2.2.0, and P4 (patrols, outposts)**, headless-tested (P1 against Raids Backport 1.1.5b, the
rest alone). P5 (polish) done: JEI info page for the ominous bottle and the pillager crossbow hidden, COMPATIBILITY and CurseForge description rewritten for raids. All five phases built; none played in a client yet beyond the first P3 check.
**P6 (2026-11-02): the player crossbow** (user reversed the D2 scope line: feature complete as a
standalone). Package `crossbow/`: `ItemCrossbow`, Quick Charge / Multishot / Piercing, piercing
arrow subclasses, a straight-flying rocket, four advancements granted from code. Steps aside for
Future MC / Crossbows Backport (`compat.crossbow=AUTO|OURS|OFF`). Headless-tested; client side
(models, pose, rocket render) not yet seen. `compat.raids` is `AUTO | OURS | RAIDS_BACKPORT | NONE`.
P4 as built: `illager/PatrolSpawner` + `EntityAIPatrol` (1.14 constants, patrol state on
`EntityPillager`), `village/modern/MapGenOutpost` (1.14 grid/roll/biomes/village clearance, driven
from `OutpostGenerator`'s IWorldGenerator since 1.12 has no generator slot), `OutpostStart` (shared
jigsaw `Assembler`, depth 7, 12-block margin), outposts converted by `tools/village-port/convert.py
... pillager_outpost`, chest crossbow added at LootTableLoadEvent from `raids.crossbows`.
Patrols and outposts only run with this mod's raids (Raids Backport has its own).
P3 as built (package `illager/`): `EntityPillager` (AbstractIllager, own crossbow AI
`EntityAICrossbowAttack`, shoots its own arrows; the held crossbow is only displayed and tagged
loaded - `Charged` for Future MC / Crossbows Backport / ours, `isLoaded` for Spartan),
`EntityRavager`, `ItemPillagerCrossbow` (D2: pillager-only, no tab/recipe, drop chance 0),
`ItemOminousBottle` (meta = level, as DD/Raids Backport), riders in `VillageRaid`.
D3 as shipped: captain gives Bad Omen by default; `raids.captainDropsOminousBottle` for current Java.
P2 as built: `raid/VillageRaid` (1.14 Raid), `RaidManager` (per-dimension WorldSavedData),
`RaidVillages` (occupied-POI isVillage from villager claims + bells, door villages as fallback),
`RaiderData` (raid id/wave in ForgeData, any mob), `RaiderGoals`, `RaidEvents`, `ModEffects`,
`OminousBanner`, `/raid`, `disableRaids` game rule, two advancements.

The village half of "Village & Pillage" is done. This plan covers the pillage half: everything in
current Java that involves illagers, with the villagers' side of it. As with the rest of the mod,
the target is current Java behaviour (minecraft.wiki), with the 1.14.4 source as the reference.

## 1. What already exists in the packs

Every 1.12.2 pack mod was scanned for illager, raid and omen classes (RLCraft, Dregora, Dregora
Cleanroom, Revamped, the 1.12.2 test instance).

| Mod | In which pack | What it adds | Why it matters |
|---|---|---|---|
| **Raids Backport** 1.1.5b (smileycorp, modid `raids`) | **Revamped** | Pillager, Ravager, Allay; raids with a data-driven wave table; Bad Omen, Raid Omen, ominous bottles; patrols; outposts; Hero of the Village with discount and gifts; advancements; 1.19 vex model | Already does nearly everything in this plan. **Mixin-heavy**: `EntityVillager`, `MerchantRecipe`, `MerchantRecipeList`, `GuiMerchant`, `SlotMerchantResult`, `EntityAIMoveIndoors`, `WorldServer` |
| **Deeper Depths** 1.1.2a | Revamped, 1.12.2 test | Its own Bad Omen potion and ominous bottle (trial chambers), plus a `RaidsIntegration` that converts its Bad Omen into Raids Backport's Raid Omen | A second Bad Omen source we must accept as well |
| **Crossbows Backport** 1.1.1 (smileycorp) | Revamped | Crossbow item | Crossbow source for pillagers |
| **Future MC** 0.2.21 | all | Crossbow item, and a bell that Raids Backport hooks into | Crossbow source. Its bell is **hidden** by our `KEEP_OURS` policy |
| **Spartan Weaponry** | RLCraft, Dregora | Crossbow item | Crossbow source |
| Vanilla 1.12.2 | — | Vindicator, Evoker, Vex, Illusioner, Witch, woodland mansions | Four of the five raid mobs are already in the game. Pillager and Ravager are the missing two |

"Village and Pillage" (an MCreator mod with Railigers, Vardians, an illager ship) also exists for
1.12.2. It is not in any pack and not worth specific support. The generic raider list in §4 covers
it anyway.

### Two things are already broken in Revamped today

These come from our existing features combined with Raids Backport. Both need fixing whatever else
we decide:

1. **The bell never reveals raiders.** Raids Backport only glows raiders when Future MC's
   `BellTileEntity` rings. Our `futureMcDuplicates=KEEP_OURS` hides that bell, and our bell only
   highlights villagers.
2. **Villagers no longer hide during a raid.** Raids Backport's hiding is a mixin on
   `EntityAIMoveIndoors`. `JobSiteAttacher` (line 177) removes that goal from every villager in
   favour of our own sheltering AI, so the mixin has nothing to run.

And a third that shows up as soon as anyone gets Hero of the Village:

3. **The trade screen shows the wrong price.** Raids Backport stores the hero discount on
   `MerchantRecipe` (a mixed-in `discountedPrice`, saved to NBT) and draws it with a `GuiMerchant`
   mixin. We swap `GuiMerchant` for our own screen, so the player sees the full price and is
   charged the discounted one. Their discount is also worked out from `getItemToBuy().getCount()`,
   which by then already includes our demand surcharge and gossip discount. That is not how 1.14
   stacks them.

## 2. Architecture

**Recommendation: build our own, and step aside cleanly when Raids Backport is installed.**

- The mod has a hard rule that it must run standalone on plain Forge. "Install Raids Backport" can't
  be how we ship raids.
- But Revamped already has Raids Backport and its worlds already hold its raids, pillagers and
  outposts. Running two raid systems at once would give two pillager types, two Bad Omens, two
  discounts and double patrols.
- So this works the way `compat.futureMcDuplicates` does: `compat.raids = AUTO | OURS | RAIDS_BACKPORT`.
  `AUTO` picks Raids Backport when `raids` is loaded. In that mode our illager half (entities,
  spawning, raid engine, patrols, outposts) stays registered but inactive, and our villager half
  drives itself from *their* raid data.
- **Our entities are always registered**, even when inactive, so a world that once had ours never
  loses them. Same rule as the old stall pieces kept registered for old saves.
- No Mixin, no coremod. Same rule as everywhere else on the villager path. Raids Backport can do
  what it does because it owns its mixins. We read its state reflectively, as we already do with
  Future MC.

The seam between the two halves is one interface:

```
RaidView       raidAt(world, pos) -> Raid?   isRaider(entity)   heroLevel(player)
               badOmenLevel(player)          isVictory / isLoss / isActive
  OurRaids             - our own WorldSavedData
  RaidsBackportRaids   - reflection on WorldDataRaids, RaidHandler, RaidsContent potions
```

Everything villagers do (hiding, ringing the bell, celebrating, gifts, discounts, bell glow) goes
through `RaidView`, so it is written once and works under either system.

## 3. Phases

Each phase ships on its own and is useful without the next one. The order goes from what fixes
Revamped now to what makes a plain install complete.

### Phase P1: villager side + Raids Backport bridge (fixes §1)

- **Bell reveals raiders** (current Java: rung during a raid, raiders within 48 blocks glow for 3s
  after a 60-tick delay). Uses `RaidView.isRaider`. The old villager highlight was removed
  (user decision 2026-11-01): the bell only ever reveals raiders.
- **Hide from raid** (`HideFromRaidOnBellRingTask` / `FindHidingPlaceDuringRaidTask`). When a bell
  rings during a raid, villagers within reach go to their claimed bed (`HomeClaims`), or else the
  nearest indoor spot, and stay until the raid ends. This is our replacement for the
  `EntityAIMoveIndoors` path we removed. It goes in our sheltering AI, with the bookkeeping on the
  entity tick, not in `shouldExecute` (see the AI-mutex trap).
- **Ring the bell** (`RingBellTask`): a villager at the meeting point rings the bell when it sees a
  raider during a raid.
- **Celebrate + go outside after victory** (`CelebrateRaidVictoryTask`, `GoOutsideAfterRaidTask`):
  jumping, celebration sound, fireworks. Raids Backport launches its fireworks from an
  `EntityVillager` mixin, so in that mode we only do the jumping and the walk outside.
- **Hero of the Village discount** goes through `PriceEngine`'s existing `specialPrice` slot, using
  1.14's formula. It is added to the *base* price inside the clamp, with gossip, exactly as 1.14
  does. In Raids Backport mode we read their potion level and clear their `discountedPrice`
  (reflection, on interact, before the screen opens) so the discount is applied once and shown
  correctly. Fixes §1.3.
- **Hero gifts** (`GiveHeroGiftsTask`): the 1.14 per-profession gift loot tables, Mason included
  (Raids Backport keys gifts on vanilla careers, and Mason is ours). Gifts go through the loot-table
  JSON so packs can edit them. In Raids Backport mode theirs stay on and we only add Mason.
- **Iron golems target raiders**, and `PanicWatch` counts raiders as threats (it already names
  illagers).

### Phase P2: the raid engine (our own)

Port `world/raid/Raid` + `RaidManager` as `WorldSavedData`:

- **Village detection the 1.14 way**: a raid needs an actual village near the player. We use our own
  POIs (claimed beds, bells, `WorkstationIndex`), with `VillageCollection` as a fallback. That way
  it works for our jigsaw villages, RC houses and player-built villages alike. Raids Backport only
  checks door-based `VillageCollection`.
- **Bad Omen → Raid Omen → raid** (current Java, 1.21): a player with Bad Omen entering a village
  gets Raid Omen. When it runs out (30s) the raid starts at that spot. Bad Omen from Deeper Depths'
  potion is accepted as well, by registry name. The raid's level comes from the omen level.
- **Waves** (1.14 `WaveMember` table): easy 3, normal 5, hard 7, plus one bonus wave when the omen
  level is above 1.

  | | w1 | w2 | w3 | w4 | w5 | w6 | w7 |
  |---|---|---|---|---|---|---|---|
  | Pillager | 4 | 3 | 3 | 4 | 4 | 4 | 2 |
  | Vindicator | 0 | 2 | 0 | 1 | 4 | 2 | 5 |
  | Ravager | 0 | 0 | 1 | 0 | 1 | 0 | 2 |
  | Witch | 0 | 0 | 0 | 3 | 0 | 0 | 1 |
  | Evoker | 0 | 0 | 0 | 0 | 1 | 1 | 2 |

  Plus 1.14's per-difficulty bonus rolls and riders (a pillager, vindicator or evoker on a ravager).
  The table is a JSON file so packs can add modded raiders.
- **Raid bar** (`BossInfoServer`: "Raid", "Raid - Victory", "Raid - Defeat", raiders remaining),
  the horn sound per wave, the 48000-tick timeout, defeat when no villagers are left or the village
  is gone.
- **Raiders move to the village** (`MoveTowardsRaidGoal`) and target villagers, players and golems.
  Raiders that wander more than 112 blocks away get pulled back or dropped, as in 1.14.
- **On victory**: Hero of the Village for every player who took part, level = omen level, 48000
  ticks. A `RaidVictoryEvent`/`RaidDefeatEvent` on the Forge bus for other mods.
- `/villagerbackport raid start|stop|status`, and the Hero of the Village and Voluntary Exile
  advancements.

### Phase P3: illager entities (our own)

- **Raider capability**, not a subclass: any `EntityLiving` listed in config can join a raid.
  Vanilla vindicator, evoker, witch and illusioner get it by default. That lets the 1.12.2 mobs
  serve in raids untouched, and lets other mods' illagers join through config.
- **Pillager**: an `AbstractIllager` subclass on vanilla's `ModelIllager`, with 1.14's textures and
  crossbow pose. Crossbow attack AI from `RangedCrossbowAttackGoal`. **The crossbow comes from
  whichever mod supplies one** (Future MC, Crossbows Backport, Spartan Weaponry), through a small
  per-mod adapter for its charged/loaded NBT. See decision D2 for when none is installed.
- **Ravager**: 1.14 `RavagerModel` and texture; roars and knocks back when stunned by a shield
  block; breaks leaves and crops; can carry an illager.
- **Patrol captains + ominous banner**: 1.14's banner pattern set. Every pattern it uses already
  exists in 1.12.2's `BannerPattern` enum. Killing a captain gives Bad Omen in the 1.14 rule; in
  current Java it drops an ominous bottle instead (config, default current Java).
- **Ominous bottle**, only when Deeper Depths isn't installed. If it is, we use Deeper Depths' bottle.
- Entity ids under `villagerbackport:` (`pillager`, `ravager`). Never removed, never renamed.

### Phase P4: patrols and outposts

- **Patrols** (`PatrolSpawner`, exact 1.14 constants): checked every 12000–13200 ticks, from world
  day 5, daytime only, 1-in-5 chance; 24–48 blocks from a random player, never in a village; a
  captain plus 1–4 members on higher local difficulty. Patrol leaders walk the group over long
  distances (`PatrollerEntity`).
- **Pillager outposts**: in 1.14 these are jigsaw structures, so they go through the same
  `tools/village-port` converter and jigsaw assembler the 1.14 villages already use. Biomes: plains,
  desert, savanna, taiga, snowy tundra, plus the Forge biome dictionary for modded equivalents.
  Seed modifier 165745296, 1-in-5 roll, kept away from villages. Includes the iron golem cage,
  loot tables, and continuous pillager spawning inside the bounding box. OTG/Dregora works through
  the same OTG path as villages.
- Allay is not included (it's an outpost cage mob from 1.19, not a pillager). It can be added later.

### Phase P5: polish

JEI info pages, the full config docs, `COMPATIBILITY.md` (its "registers nothing / no AI" table is
long out of date), and the CurseForge page.

## 4. Compatibility rules

| With | Rule |
|---|---|
| Raids Backport | `compat.raids=AUTO` defers to it. Our villager half reads its raids. Our entities stay registered but never spawn. Their discount is cleared and re-applied through ours |
| Deeper Depths | Its Bad Omen counts as Bad Omen. Its ominous bottle replaces ours |
| Future MC / Crossbows Backport / Spartan | Crossbow sources for pillagers. We add no crossbow item for players (scope line) |
| Infernal Mobs / Champions / Scaling Health | Raiders are ordinary mobs to them. Optional opt-out list using the same flags as the villager opt-outs |
| FermiumMixins `ContainerMerchant` | Untouched. The discount still lives only in the price, never the container |
| Tektopia | Not in any pack. Not supported |

## 5. Testing

- Dedicated dev server (`runServer`) for every phase. Dedicated servers catch `@SideOnly` and
  client-class leaks. Tick-entity reset hook for playerless tests, as for the farmers.
- Two test instances: **plain** (our mod alone, the standalone claim) and **Revamped mod list**
  (Raids Backport + Deeper Depths + Crossbows Backport + Future MC), with `AUTO` checked to pick
  theirs and our P1 fixes verified against their raids.
- The Revamped deployed config needs the new keys added by hand where categories already exist.

## 6. Decisions needed

- **D1: architecture.** Recommended: our own implementation plus the Raids Backport bridge (above).
  The alternative is bridge only (P1 alone, Raids Backport recommended for raids), which is much
  less work but means a plain install has no raids.
- **D2: pillagers with no crossbow mod installed.** Recommended: a pillager-only crossbow item that
  renders and fires but isn't in the creative tab, has no recipe, and drops as nothing (or arrows).
  That keeps the scope line ("no crossbow for players") without unarmed pillagers. Alternative:
  pillagers fall back to a bow.
- **D3: rule set.** Recommended: current Java (Raid Omen, ominous bottles), with 1.14's
  captain-gives-Bad-Omen behind a config switch, following the standing wiki-parity rule.
