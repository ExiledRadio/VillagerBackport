# Mechanics: 1.14's changes to things 1.12.2 already has

From the family audit of 2026-11-16 (`family-notes/MECHANICS_AUDIT_2026-11-16.md`, owner V&P). Code in
`mechanics/` (and `trade/ModernTrades` for the cartographer); settings in `ModConfig.mechanics` (section
"1.14 Changes") and `ModConfig.trades.modernCartographer`, all on by default; self-tests in
`selftest/MechanicsSelfTest` (`-Pselftest=mechanics`). V&P still has no coremod: the three changes that need a patch
are Backport Core 1.4.0's (P33 to P35), switched on from here.

| Change | Version | How | Setting |
|---|---|---|---|
| Shorter crouch: 1.5 tall, eyes 1.27, 1.5-block gaps, held crouched, drawn crouching, sneak speed | 1.14 (19w12a, 19w14a) | Core's pose engine (`PoseRules.shortCrouch`, P33 for the eyes). The server's setting is sent to each client (`CrouchSync`), so both sides size the player alike | `shortCrouch` |
| Vindicators break wooden doors in a raid, Normal and Hard, 240 ticks | 1.14 (18w47a) | `EntityAIRaidBreakDoor`; the path runs through wooden doors while in a raid | `vindicatorsBreakDoors` |
| TNT and TNT minecarts drop every block | 1.14 (19w11a) | The drop chance is raised to 1 in `HarvestDropsEvent` for the blocks a TNT explosion breaks (removing them early would change the blast's damage) | `tntDropsAll` |
| Leaves drop 1 or 2 sticks: 2%, 2.22%, 2.5%, 3.33%, 10% by Fortune; not sheared or Silk Touch | 1.14 (18w43a) | `HarvestDropsEvent`, vanilla's six leaves only (other mods' leaves keep their own drops) | `leavesDropSticks` |
| Cartographer of 1.21.5: maps by biome, swamp hut and jungle temple maps, trial chambers map, banners by biome, mansion map at master | 1.21.5 (25w07a) | `ModernTrades`; structures through Core's `/locate` registry, so a map is offered only for a structure an installed mod builds. Map icons from Core (P34). Levels now draw again when an entry gives no trade, as today's `addOffersFromItemListings` | `trades.modernCartographer` |
| Illagers and ravagers don't target baby villagers | 1.18 (21w37a, 1.18-pre8) | Target task selectors (`Targeting`), the ravager's own task | `illagersSpareBabies` |
| Note block instruments: iron block, soul sand, pumpkin, emerald block, hay bale, glowstone | 1.14 (19w09a) | The server cancels the note's block event and plays 1.14's sound and particle | `noteBlockInstruments` |
| Parrots imitate pillagers and ravagers | 1.14 (19w11a) | `EntityParrot.registerMimicSound`. Pandas left out: 1.15 removed that | `parrotsImitateRaiders` |
| Spawns that would despawn at once are refused (over 128 blocks from every player) | 1.14 | `LivingSpawnEvent.CheckSpawn` | `refuseInstantDespawns` |
| 1.14 zombie sieges: any village (bed, workstation, bell, or 1.12.2's doors), midnight one night in ten, 20 zombies 32 blocks out | 1.14 | `ModernSiege` replaces the overworld's `VillageSiege` | `modernSieges` |
| Arrows break chorus flowers, which drop | 1.14 (18w43a) | `ProjectileImpactEvent.Arrow` (snowballs and eggs are 1.16's) | `arrowsBreakChorusFlowers` |
| A creeper killed by a stray or wither skeleton drops a disc | 1.14 | `LivingDropsEvent` | `anySkeletonDisc` |
| Invisibility: guardians, and zombies and illagers targeting villagers, notice invisible targets only up to 7% of their range (more with armor; at least 2 blocks) | 1.14 (19w08a) | Target task selectors (`Targeting.visibility`, 1.14's `getVisibilityPercent`). 1.12.2 already does this for players found through `getNearestAttackablePlayer` | `invisibilityHides` |
| A tamed wolf's kill counts as its owner's | 1.14 (19w11a) | `LivingDeathEvent` sets the attacking player | `wolfKillsCount` |
| Dead bush is fuel, 100 ticks | 1.14 (18w43a) | `FurnaceFuelBurnTimeEvent` | `deadBushFuel` |
| Horse armor drawn as a layer: no red flash, visible on an invisible horse | 1.14 (19w08a) | Iron, gold and diamond leave the baked horse texture; `LayerLeatherHorseArmor` draws them (client, restart) | `horseArmorLayer` |
| Torches on glass panes and iron bars | 1.14.3-pre3 | Core's P35 (`BlockRules.torchesOnPanes`) | `torchesOnPanes` |
| Book editor: cursor, selection, Ctrl+A/C/X/V, word and page clicks, Page Up/Down, 100 pages | 1.14 (18w43a) | `GuiBookEditor` in place of vanilla's unsigned book screen; same `MC|BEdit`/`MC|BSign` messages | `modernBookEditor` |

## Checked and left alone

*   **Redstone dust on glass**: 1.12.2 already allows it (glass's top is a solid face, which `BlockRedstoneWire.canPlaceBlockAt`
    accepts); 1.13 lost it and 1.14 restored it. The self-test checks it stays so.
*   **Leather horse armor** and its dyeing: already in V&P (flora).
*   **Horse armor values** (leather 3, iron 5, gold 7, diamond 11) and horse armor not stacking: 1.12.2's already.
*   **1.14's horse armor textures**: drawn for 1.14's horse model, not 1.12.2's; the layer keeps 1.12.2's textures.
*   **Parrots imitating pandas**: 1.14 only; 1.15 removed it.
*   **Withers not seeing invisible players** (also 19w08a): not on the audit's list, and the wither's skull targeting
    bypasses target tasks; not done.

## Limits

*   A wolf's kill counts for its owner when the wolf lands the killing blow. 1.14 also credits the owner for a mob a wolf
    hurt that dies of something else within 5 seconds.
*   Sticks come from vanilla's leaves only; other mods' leaves keep their own drops.
*   The book editor and horse armor layer are client-side: `gradlew runClient -Pselftest=client` checks them
    (`selftest/ClientMechanicsTest`). The crouch on a real client (camera easing, the 1.5 gap, the held crouch, sneak
    speed, drawing) is Backport Core's `runClient -Pselftest` (`ClientCrouchTests`).
