# Compatibility

## Why this mod has no coremod and no Mixin

Changing villager restock behaviour looks like it wants an ASM patch — the logic lives inside
private methods on `EntityVillager`. It doesn't, and the reason is what the rest of a large pack
actually does to villagers.

A scan of all 180 jars in an RLCraft 2.9.3 instance found 30 mods referencing villager classes.
Grouped by what they reach for:

| What they use | Mods | Conflict risk |
|---|---|---|
| `VillagerRegistry` — register professions/careers, add trades | Ice and Fire, Charm, Quark, Waystones, RLTweaker, Placebo, SilentLib, librarianlib, carrotslib, base, RecurrentComplex, SoManyEnchantments | **None.** Their trades land in the same `buyingList` this mod reads. |
| `MerchantRecipe` construction | Ice and Fire, Charm, Quark, VariedCommodities | **None.** We only reset `uses` on recipes that already exist. |
| Mixins into villager classes | RLMixins (`icenfire/ModVillagersMixin`, `vanilla/EntityZombieVillagerMixin`) | **None.** Neither touches the restock or trade-use path. |
| AI goals | Quark (`VillagerPursueEmeralds`) | **None.** We add no AI. |
| Rendering / models only | MoBends, Neat, classyhats, iChunUtil, DynamicSurroundings | **None.** |
| Bundled mapping tables (false positives) | ForgeEndertech, CreativeCore | **None.** High reference counts come from shaded name tables, not real usage. |

The pattern: **every mod that adds villager content does it through `VillagerRegistry`, and nothing
touches the restock path.** That path is unclaimed territory, which is exactly why an event handler
is sufficient — there is nothing to collide with.

A coremod on `EntityVillager` would have been the riskiest possible choice here, because RLMixins
already mixes into villager classes and both would be rewriting the same class.

## What this mod does and does not do

The table above is from the first release, when the mod only fixed restocking. It has grown a long
way since; this is what it touches now.

Does not:
- Subclass or replace `EntityVillager`, or replace any vanilla renderer (badges, outfits and held
  items are layers added to whichever villager renderer is installed)
- Patch bytecode or apply Mixins, anywhere
- Replace vanilla's `ContainerMerchant` (the trade *screen* is swapped, the container is not)
- Run anything in another mod's item code for a mob (pillagers only tag a crossbow as loaded)

Does:
- Register its own blocks (the twelve workstations, lantern), items (banner patterns, the ominous
  bottle, the crossbow and a pillager-only stand-in), enchantments (Quick Charge, Multishot,
  Piercing), entities (pillager, ravager, the crossbow's arrows and rocket), effects (Bad Omen, Raid
  Omen, Hero of the Village), sounds, a career (mason), loot tables and advancements
- Add AI goals to villagers (work, sleep, shelter, gather at the bell, breed on beds, panic, raids,
  hero gifts) and remove the vanilla goals they replace (`EntityAIMoveIndoors`, `EntityAIVillagerMate`,
  `EntityAIPlay`, `EntityAIAvoidEntity`)
- Add raider goals to any mob that joins one of its raids, and swap its fight-back goal for one that
  ignores other raiders
- Generate 1.14 villages (by swapping `MapGenVillage` through `InitMapGenEvent`, only when it is
  exactly vanilla's) and pillager outposts (from an `IWorldGenerator`)
- Read and write villager fields reflectively (`buyingList`, `timeUntilReset`, `careerId`,
  `careerLevel`) and keep its own data in each villager's `ForgeData` under `villagerbackport`
- Listen to `MerchantTradeOffersEvent` at `LOWEST`, only to take Raids Backport's hero discount
  off when this mod's pricing is in charge (see below)

## Second instance: RLCraft Dregora

The Dregora instance (Forge **2860**, a different and newer mod set) was scanned separately. Same
conclusion — everything reaches villagers through `VillagerRegistry` and `MerchantRecipe`
construction. Ice and Fire 2.0.9 is the heaviest user at 114 `VillagerRegistry` references.

Two additions not present in the other instance, both **Phase 3 concerns only**:

- **FermiumMixins** — `vanilla/ContainerMerchant_DropsMixin`. Patches item-drop behaviour on the
  vanilla merchant container. No interaction with restocking or pricing. **Constraint for the GUI
  phase: extend `ContainerMerchant` rather than writing a fresh container**, or that mixin stops
  applying and their fix is silently lost for our trades.
- **EagleMixins** — 7 `EntityVillager` / 4 `VillagerRegistry` references, no villager-named mixin
  classes; appears to be registry use rather than a patch.

Phase 1 was confirmed live on this instance: `Villager restocking active (max 2/day, 2400 tick
cooldown)` at 17:39:03, with zero exceptions naming `com.exiledradio.villagerbackport`. That log line
only prints when both reflective field lookups resolve, so it doubles as proof the SRG names are
correct against a reobfuscated client — and against Forge **2860** while compiled against **2847**,
confirming the binary-compatibility assumption in the gradle.properties ceiling note.

## Why the trade screen is an overlay

*Superseded: since 0.4.0 the screen itself is replaced by a 1.14-style one, handed the same
`ContainerMerchant` vanilla opened, so the container - and FermiumMixins' patch to it - is still
untouched. Kept for the reasoning.*

1.14 shows villager level as part of a redesigned merchant screen. Rebuilding that would mean a
custom `Container` and `GuiScreen` opened in place of vanilla's — the one part of this feature with
real compatibility cost, since FermiumMixins patches `ContainerMerchant` and other mods decorate the
vanilla screen.

Instead the level and XP bar are drawn on top of the vanilla screen via
`GuiScreenEvent.DrawScreenEvent.Post`, in the strip *above* the window so it cannot collide with
anything vanilla or another mod draws inside. Vanilla's container and screen are completely
untouched.

**Accepted tradeoff:** trades cannot be visually locked behind a level, since that needs container
changes. Dropped deliberately rather than taking the conflict.

## Why demand surcharges are not persisted

Prices have to physically live in `MerchantRecipe.getItemToBuy()` — 1.12.2 has no hook between "the
recipe" and "the price shown and charged". But a villager's recipes are saved to its NBT, so a
surcharge left in place becomes part of the world save: turning pricing off would freeze prices, and
uninstalling the mod would leave them inflated permanently with nothing left to undo them.

So the surcharge exists only while a player is actually trading. It is written on interact — Forge's
interact hook fires at `EntityPlayer.interactOn` line 1305, before `processInitialInteract` at line
1310 opens the screen — and removed on the next poll once `getCustomer()` is null. At rest, which is
when a villager is saved, recipes hold base prices. Demand itself is stored separately in ForgeData
and the price is recomputed on each open.

Removal happens on the poll rather than a GUI-close event so it also covers disconnecting mid-trade
and chunks unloading with the screen open. The residual window is a world save taken while a player
has the screen open; the next poll corrects it.

## Interaction with vanilla's own restock

Vanilla's 40-tick reset is left running. It does something worth keeping — it levels the villager's
career up and adds the next tier of trades — and this mod is fixing the case where it *never fires*,
not replacing it. `VillagerAccess.isVanillaResetPending()` checks `timeUntilReset` and this mod
stands down while vanilla's reset is armed, so the two never restock the same villager on one tick.

## Interaction with RLCraft Villager Tomes

Villager Tomes adds player-taught enchanted book trades to the same `buyingList`. Those trades
restock on the same rules as everything else with no special handling — worth testing together,
since Tomes trades are intended to be permanent and the interaction with a use cap is worth
confirming behaves the way you want.

## Raids

This mod runs raids of its own (`compat.raids=AUTO` without Raids Backport, or `OURS`). Raiders
are any mob: membership is stored in the mob's Forge data (`RaidId`, `RaidWave`, `RaidLeader`
under the `villagerbackport` tag) and three goals are added at priorities 3-5 that do nothing
outside a raid, so a mod's own illager keeps its own AI. Which mob fills each of 1.14's five
raider slots is a config list of registry names, first installed wins.

This mod's crossbow (`compat.crossbow`) is only registered when neither Future MC (`futuremc`) nor
Crossbows Backport (`crossbows`) is loaded, by default. Its recipe and its four advancements carry
the `villagerbackport:crossbow` condition (named in both `recipes/_factories.json` and
`advancements/_factories.json`, since Forge clears conditions before loading each), so they go
with it. It stores its load in 1.14's `Charged` / `ChargedProjectiles` tags. Piercing needs arrows
that do not stop at their first hit, so vanilla's arrow, tipped arrow and spectral arrow are fired
as this mod's subclasses of them; another mod's arrow is made by its own item and does not pierce.
Rockets are an entity of this mod's, as 1.12.2's rocket can only rise.

Pillagers carry the first crossbow installed from `raids.crossbows` (this mod's, Future MC,
Crossbows Backport, Spartan Weaponry) but never use it: the pillager times its load and fires its
own arrows, and only sets the crossbow's loaded tag (`Charged`, or Spartan's `isLoaded`) so its
model shows an arrow. No other crossbow mod's item code runs for a mob; this mod's crossbow makes
the arrow, so its Piercing applies. With no crossbow at all (`compat.crossbow=OFF` and no mod),
pillagers hold a pillager-only stand-in with no recipe, no creative tab and a drop chance of 0.

Bad Omen from Deeper Depths (`deeperdepths:bad_omen`) and Raids Backport (`raids:bad_omen`) is
accepted as well as this mod's. Deeper Depths only converts its own Bad Omen when Raids Backport
is installed, so without it the two do not both act on one omen.

## Raids Backport

Raids Backport (smileycorp, modid `raids`) adds pillagers, ravagers and raids to 1.12.2. With
`compat.raids=AUTO` (the default) this mod lives through its raids rather than adding raids of its
own. It reads Raids Backport's state by reflection, so neither mod needs the other, and if a later
version moves something the bridge logs one error and villagers simply ignore raids.

Three things Raids Backport does would otherwise break against this mod. Each is handled here, with
no Mixin:

| Raids Backport does | Why it breaks here | What this mod does instead |
|---|---|---|
| Glows raiders when **Future MC's** bell rings | `compat.futureMcDuplicates=KEEP_OURS` hides that bell | Our bell reveals raiders itself, on 1.14's timing (resonance after 5 ticks, glow 40 later, 48 blocks) |
| Hides villagers through a mixin on `EntityAIMoveIndoors` | We remove that goal for our sleep AI | `raid.EntityAIRaidResponse`: hide, gather and ring the bell, celebrate |
| Stores the hero discount on `MerchantRecipe` and draws it on vanilla's `GuiMerchant` | We replace the trade screen, so the discount was charged but not shown, and it was taken from a price already including demand and reputation | Hero of the Village is a term in our `specialPrice`, from the base price as 1.14 has it. Raids Backport's discount is set to 0 in `MerchantTradeOffersEvent` at `LOWEST`, only while `pricing.demandPricing` is on |

Gifts: Raids Backport's villagers give their own. This mod only gives gifts for careers it has
nothing for, which is the mason.

Raiders are recognised by registry name (`pillager`, `ravager`, `vindicator`/`vindication_illager`,
`evoker`/`evocation_illager`, `illusioner`/`illusion_illager`, `witch`) whichever mod they come
from, plus `raids.extraRaiders`. Villagers flee pillagers at 15 blocks and ravagers at 12, as in
1.14. This replaces Raids Backport's own `EntityAIAvoidEntity`, which our panic AI removes along
with vanilla's.

Tested on a headless server with Raids Backport 1.1.5b, Crossbows Backport 1.1.1, Future MC 0.2.21
and MixinBooter 11.18. Raids Backport needs Atlas Lib, and with only MixinBooter it failed to start
(MixinExtras could not be found by Atlas Lib's loader). Adding Red Core, as RLCraft Revamped has,
fixed it.

## Failure mode

If either reflective field lookup fails — a future coremod renames something, a Forge build moves a
field — `VillagerAccess.isAvailable()` returns false, one error line is logged, and every villager
check short-circuits. The pack loads and plays normally with vanilla 1.12.2 behaviour. The mod
never throws at runtime.

## SRG names used

Verified against `mcp_stable/39` `fields.csv`:

| MCP | SRG | Class |
|---|---|---|
| `buyingList` | `field_70963_i` | `EntityVillager` |
| `timeUntilReset` | `field_70961_j` | `EntityVillager` |

For later phases: `careerLevel` = `field_175562_bw`, `careerId` = `field_175563_bv`,
`toolUses` = `field_77400_d`, `maxTradeUses` = `field_82786_e`.
