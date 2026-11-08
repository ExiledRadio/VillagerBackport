package com.exiledradio.villagerbackport.worldgen;

import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.StructurePlacements;
import com.exiledradio.backportcore.worldgen.StructurePlacements.BiomePoint;

/**
 * 1.14's structure placement, for Backport Core's placement table (the newest installed version's numbers
 * win). Every number is 1.14.4's own, read from its decompiled code: {@code ChunkGeneratorSettings} for the
 * spacings and separations, each feature's {@code getRandomSalt} (or its {@code setLargeFeatureWithSalt}
 * call) for the salts. The villages and outposts that read it are in {@code village/modern}, the biome map in
 * {@code bamboo/Layers114}.
 *
 * <ul>
 *   <li>Villages 32/8, salt 10387312; outposts 32/8, salt 165745296; desert pyramid, igloo, jungle temple, witch
 *       hut 32/8 on salts 14357617, 14357618, 14357619 and 14357620; monument 32/5, salt 10387313, and mansion
 *       80/20, salt 10387319, both triangular; shipwrecks and ocean ruins 16/8 (salts 165745295, 14357621).</li>
 *   <li>Every single-biome check reads the block-scale biome at block (9, 0, 9) of the chunk.</li>
 *   <li>1.14's bamboo jungles get jungle temples; strongholds go in the mushroom field shore too (1.14's "has
 *       stronghold"; the bamboo jungles are above sea level, so 1.12.2's rule has them already).</li>
 * </ul>
 * Backport Core applies the rules every version from 1.13 on shares (temples on their four grids, with
 * {@code /locate Temple} finding any of the four; the monument's squares; the strongholds' and spawn point's
 * biome search; the hills layer's seeding).
 */
public final class Placement114 {

    private Placement114() {
    }

    /** Pre-init. */
    public static void register() {
        StructurePlacements.version("1.14")
                .biomePoint(BiomePoint.BLOCK_9_9)
                .grid(Structures.VILLAGE, 32, 8, 10387312)
                .grid(Structures.PILLAGER_OUTPOST, 32, 8, 165745296)
                .grid(StructurePlacements.DESERT_PYRAMID, 32, 8, 14357617)
                .grid(StructurePlacements.IGLOO, 32, 8, 14357618)
                .grid(StructurePlacements.JUNGLE_PYRAMID, 32, 8, 14357619)
                .grid(StructurePlacements.SWAMP_HUT, 32, 8, 14357620)
                .grid(StructurePlacements.OCEAN_MONUMENT, 32, 5, 10387313, true)
                .grid(StructurePlacements.MANSION, 80, 20, 10387319, true)
                .grid(Structures.SHIPWRECK, 16, 8, 165745295)
                .grid(Structures.OCEAN_RUIN, 16, 8, 14357621)
                .structureBiome(StructurePlacements.JUNGLE_PYRAMID, "bamboo_jungle")
                .structureBiome(StructurePlacements.JUNGLE_PYRAMID, "bamboo_jungle_hills")
                .strongholdBiome("minecraft:mushroom_island_shore", true);
    }
}
