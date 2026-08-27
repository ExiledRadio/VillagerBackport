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

Does not:
- Register any block, item, entity, profession, career, or trade
- Subclass or replace `EntityVillager`
- Patch bytecode or apply Mixins
- Fire or cancel `MerchantTradeOffersEvent` (several mods listen to it; we stay off it entirely)
- Run any client-side logic

Does:
- Listen to `LivingEvent.LivingUpdateEvent`, server-side only
- Read two private fields on `EntityVillager` reflectively (`buyingList`, `timeUntilReset`)
- Reset `uses` on recipes via `MerchantRecipe`'s own public NBT round-trip
- Store four longs per villager in the entity's `ForgeData` tag under key `villagerbackport`

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
