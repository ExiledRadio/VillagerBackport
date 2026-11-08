package com.exiledradio.villagerbackport.bamboo;

import net.minecraft.init.Biomes;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerAddIsland;
import net.minecraft.world.gen.layer.GenLayerAddMushroomIsland;
import net.minecraft.world.gen.layer.GenLayerAddSnow;
import net.minecraft.world.gen.layer.GenLayerBiome;
import net.minecraft.world.gen.layer.GenLayerBiomeEdge;
import net.minecraft.world.gen.layer.GenLayerDeepOcean;
import net.minecraft.world.gen.layer.GenLayerEdge;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerIsland;
import net.minecraft.world.gen.layer.GenLayerRareBiome;
import net.minecraft.world.gen.layer.GenLayerRemoveTooMuchOcean;
import net.minecraft.world.gen.layer.GenLayerRiver;
import net.minecraft.world.gen.layer.GenLayerRiverInit;
import net.minecraft.world.gen.layer.GenLayerRiverMix;
import net.minecraft.world.gen.layer.GenLayerShore;
import net.minecraft.world.gen.layer.GenLayerSmooth;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;

/**
 * 1.14.4's overworld biome layers ({@code Layers.getDefaultLayers}), built from 1.12.2's layer classes where
 * they already do what 1.14 does, so a world matches 1.14's seed maps:
 *
 * <ul>
 *   <li>{@link RareBiomeLarge}: 1.14's {@code RareBiomeLargeLayer}, salt 1001, right after the biome layer
 *       and before its two zooms: one jungle cell in ten (256 blocks across) becomes a bamboo jungle.</li>
 *   <li>{@link BiomeEdge}: 1.14's {@code BiomeEdgeLayer} also turns a swamp next to bamboo jungle into jungle
 *       edge.</li>
 *   <li>{@link Hills}: 1.14's {@code RegionHillsLayer} makes bamboo jungle hills of bamboo jungle, and reads a
 *       river noise seeded with the world seed. 1.12.2's hills layer never seeds the two zoom layers of its
 *       river noise ({@code initWorldGenSeed} only reaches its biome parent), so which areas turn into their
 *       rare variants differs from 1.13 on, whose layer contexts all get the seed.</li>
 *   <li>Everything else is vanilla 1.12.2's class with 1.14's salt, which Backport Core's vanilla oracle
 *       ({@code tools/vanilla-oracle}) shows to give 1.14.4's map cell for cell. 1.13's ocean temperatures
 *       are Update Aquatic's, which mixes them in after this.</li>
 * </ul>
 * The bamboo jungle's {@code getBiomeClass()} is the jungle's, so 1.12.2's shore layer and "same biome"
 * checks treat it as 1.14's jungle category does.
 */
final class Layers114 {

    private Layers114() {
    }

    /** @return 1.14.4's {biomes, block-scale biomes, biomes}, seeded */
    static GenLayer[] build(long seed, WorldType type, Biome bamboo, Biome bambooHills, int rarity) {
        GenLayer layer = new GenLayerIsland(1L);
        layer = new GenLayerFuzzyZoom(2000L, layer);
        layer = new GenLayerAddIsland(1L, layer);
        layer = new GenLayerZoom(2001L, layer);
        layer = new GenLayerAddIsland(2L, layer);
        layer = new GenLayerAddIsland(50L, layer);
        layer = new GenLayerAddIsland(70L, layer);
        layer = new GenLayerRemoveTooMuchOcean(2L, layer);
        layer = new GenLayerAddSnow(2L, layer);
        layer = new GenLayerAddIsland(3L, layer);
        layer = new GenLayerEdge(2L, layer, GenLayerEdge.Mode.COOL_WARM);
        layer = new GenLayerEdge(2L, layer, GenLayerEdge.Mode.HEAT_ICE);
        layer = new GenLayerEdge(3L, layer, GenLayerEdge.Mode.SPECIAL);
        layer = new GenLayerZoom(2002L, layer);
        layer = new GenLayerZoom(2003L, layer);
        layer = new GenLayerAddIsland(4L, layer);
        layer = new GenLayerAddMushroomIsland(5L, layer);
        layer = new GenLayerDeepOcean(4L, layer);
        GenLayer land = GenLayerZoom.magnify(1000L, layer, 0);

        int biomeSize = type == WorldType.LARGE_BIOMES ? 6 : 4;
        biomeSize = GenLayer.getModdedBiomeSize(type, biomeSize);
        int riverSize = 4;

        GenLayer riverInit = new GenLayerRiverInit(100L, GenLayerZoom.magnify(1000L, land, 0));
        // WorldType.getBiomeLayer, with 1.14's bamboo layer between the biome layer and its zooms.
        GenLayer biomes = new GenLayerBiome(200L, land, type, null);
        biomes = new RareBiomeLarge(1001L, biomes, bamboo, rarity);
        biomes = GenLayerZoom.magnify(1000L, biomes, 2);
        biomes = new BiomeEdge(new GenLayerBiomeEdge(1000L, biomes), biomes, bamboo);
        GenLayer hills = new Hills(1000L, biomes, GenLayerZoom.magnify(1000L, riverInit, 2), bamboo, bambooHills);
        GenLayer rivers = GenLayerZoom.magnify(1000L, riverInit, 2);
        rivers = GenLayerZoom.magnify(1000L, rivers, riverSize);
        rivers = new GenLayerRiver(1L, rivers);
        rivers = new GenLayerSmooth(1000L, rivers);
        hills = new GenLayerRareBiome(1001L, hills);
        for (int k = 0; k < biomeSize; ++k) {
            hills = new GenLayerZoom(1000 + k, hills);
            if (k == 0) {
                hills = new GenLayerAddIsland(3L, hills);
            }
            if (k == 1 || biomeSize == 1) {
                hills = new GenLayerShore(1000L, hills);
            }
        }
        GenLayer mixed = new GenLayerRiverMix(100L, new GenLayerSmooth(1000L, hills), rivers);
        GenLayer blocks = new GenLayerVoronoiZoom(10L, mixed);
        mixed.initWorldGenSeed(seed);
        blocks.initWorldGenSeed(seed);
        return new GenLayer[]{mixed, blocks, mixed};
    }

    /** 1.14.4's {@code RareBiomeLargeLayer}: {@code nextRandom(10) == 0 && jungle ? bamboo jungle : value}. */
    static final class RareBiomeLarge extends GenLayer {

        private final int jungle = Biome.getIdForBiome(Biomes.JUNGLE);
        private final int bamboo;
        private final int rarity;

        RareBiomeLarge(long salt, GenLayer parent, Biome bamboo, int rarity) {
            super(salt);
            this.parent = parent;
            this.bamboo = Biome.getIdForBiome(bamboo);
            this.rarity = Math.max(1, rarity);
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int[] in = this.parent.getInts(areaX, areaY, areaWidth, areaHeight);
            int[] out = IntCache.getIntCache(areaWidth * areaHeight);
            for (int z = 0; z < areaHeight; z++) {
                for (int x = 0; x < areaWidth; x++) {
                    initChunkSeed(areaX + x, areaY + z);
                    int v = in[x + z * areaWidth];
                    out[x + z * areaWidth] = nextInt(this.rarity) == 0 && v == this.jungle ? this.bamboo : v;
                }
            }
            return out;
        }
    }

    /**
     * 1.12.2's biome edge layer, plus 1.14.4's one bamboo rule: a swamp with a bamboo jungle beside it (and no
     * desert or snow, which make it plains first) becomes jungle edge, as next to a jungle. The edge layer
     * draws no random numbers, so its answer can be corrected afterwards.
     */
    static final class BiomeEdge extends GenLayer {

        private final GenLayer edge;
        private final int swamp = Biome.getIdForBiome(Biomes.SWAMPLAND);
        private final int jungleEdge = Biome.getIdForBiome(Biomes.JUNGLE_EDGE);
        private final int bamboo;

        BiomeEdge(GenLayer edge, GenLayer input, Biome bamboo) {
            super(1000L);
            this.edge = edge;
            this.parent = input;
            this.bamboo = Biome.getIdForBiome(bamboo);
        }

        @Override
        public void initWorldGenSeed(long seed) {
            super.initWorldGenSeed(seed);
            this.edge.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int[] edged = this.edge.getInts(areaX, areaY, areaWidth, areaHeight).clone();
            int w = areaWidth + 2;
            int[] in = this.parent.getInts(areaX - 1, areaY - 1, w, areaHeight + 2);
            int[] out = IntCache.getIntCache(areaWidth * areaHeight);
            for (int z = 0; z < areaHeight; z++) {
                for (int x = 0; x < areaWidth; x++) {
                    int v = edged[x + z * areaWidth];
                    if (v == this.swamp && in[x + 1 + (z + 1) * w] == this.swamp
                            && (in[x + 1 + z * w] == this.bamboo || in[x + 2 + (z + 1) * w] == this.bamboo
                            || in[x + (z + 1) * w] == this.bamboo || in[x + 1 + (z + 2) * w] == this.bamboo)) {
                        v = this.jungleEdge;
                    }
                    out[x + z * areaWidth] = v;
                }
            }
            return out;
        }
    }

    /**
     * 1.12.2's {@code GenLayerHills} as 1.14.4's {@code RegionHillsLayer} runs it: bamboo jungle hills from
     * bamboo jungle, and a river noise seeded with the world seed.
     */
    static final class Hills extends GenLayer {

        private final GenLayer riverLayer;
        private final Biome bamboo;
        private final Biome bambooHills;

        Hills(long salt, GenLayer parent, GenLayer riverLayer, Biome bamboo, Biome bambooHills) {
            super(salt);
            this.parent = parent;
            this.riverLayer = riverLayer;
            this.bamboo = bamboo;
            this.bambooHills = bambooHills;
        }

        @Override
        public void initWorldGenSeed(long seed) {
            super.initWorldGenSeed(seed);
            this.riverLayer.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int[] aint = this.parent.getInts(areaX - 1, areaY - 1, areaWidth + 2, areaHeight + 2);
            int[] aint1 = this.riverLayer.getInts(areaX - 1, areaY - 1, areaWidth + 2, areaHeight + 2);
            int[] aint2 = IntCache.getIntCache(areaWidth * areaHeight);
            for (int i = 0; i < areaHeight; ++i) {
                for (int j = 0; j < areaWidth; ++j) {
                    this.initChunkSeed(j + areaX, i + areaY);
                    int k = aint[j + 1 + (i + 1) * (areaWidth + 2)];
                    int l = aint1[j + 1 + (i + 1) * (areaWidth + 2)];
                    boolean flag = (l - 2) % 29 == 0;
                    Biome biome = Biome.getBiomeForId(k);
                    boolean flag1 = biome != null && biome.isMutation();
                    if (k != 0 && l >= 2 && (l - 2) % 29 == 1 && !flag1) {
                        Biome biome3 = Biome.getMutationForBiome(biome);
                        aint2[j + i * areaWidth] = biome3 == null ? k : Biome.getIdForBiome(biome3);
                    } else if (this.nextInt(3) != 0 && !flag) {
                        aint2[j + i * areaWidth] = k;
                    } else {
                        Biome biome1 = biome;
                        if (biome == Biomes.DESERT) {
                            biome1 = Biomes.DESERT_HILLS;
                        } else if (biome == Biomes.FOREST) {
                            biome1 = Biomes.FOREST_HILLS;
                        } else if (biome == Biomes.BIRCH_FOREST) {
                            biome1 = Biomes.BIRCH_FOREST_HILLS;
                        } else if (biome == Biomes.ROOFED_FOREST) {
                            biome1 = Biomes.PLAINS;
                        } else if (biome == Biomes.TAIGA) {
                            biome1 = Biomes.TAIGA_HILLS;
                        } else if (biome == Biomes.REDWOOD_TAIGA) {
                            biome1 = Biomes.REDWOOD_TAIGA_HILLS;
                        } else if (biome == Biomes.COLD_TAIGA) {
                            biome1 = Biomes.COLD_TAIGA_HILLS;
                        } else if (biome == Biomes.PLAINS) {
                            biome1 = this.nextInt(3) == 0 ? Biomes.FOREST_HILLS : Biomes.FOREST;
                        } else if (biome == Biomes.ICE_PLAINS) {
                            biome1 = Biomes.ICE_MOUNTAINS;
                        } else if (biome == Biomes.JUNGLE) {
                            biome1 = Biomes.JUNGLE_HILLS;
                        } else if (biome == this.bamboo) {
                            biome1 = this.bambooHills;
                        } else if (biome == Biomes.OCEAN) {
                            biome1 = Biomes.DEEP_OCEAN;
                        } else if (biome == Biomes.EXTREME_HILLS) {
                            biome1 = Biomes.EXTREME_HILLS_WITH_TREES;
                        } else if (biome == Biomes.SAVANNA) {
                            biome1 = Biomes.SAVANNA_PLATEAU;
                        } else if (biomesEqualOrMesaPlateau(k, Biome.getIdForBiome(Biomes.MESA_ROCK))) {
                            biome1 = Biomes.MESA;
                        } else if (biome == Biomes.DEEP_OCEAN && this.nextInt(3) == 0) {
                            biome1 = this.nextInt(2) == 0 ? Biomes.PLAINS : Biomes.FOREST;
                        }
                        int j2 = Biome.getIdForBiome(biome1);
                        if (flag && j2 != k) {
                            Biome biome2 = Biome.getMutationForBiome(biome1);
                            j2 = biome2 == null ? k : Biome.getIdForBiome(biome2);
                        }
                        if (j2 == k) {
                            aint2[j + i * areaWidth] = k;
                        } else {
                            int i2 = 0;
                            if (biomesEqualOrMesaPlateau(aint[j + 1 + i * (areaWidth + 2)], k)) {
                                ++i2;
                            }
                            if (biomesEqualOrMesaPlateau(aint[j + 2 + (i + 1) * (areaWidth + 2)], k)) {
                                ++i2;
                            }
                            if (biomesEqualOrMesaPlateau(aint[j + (i + 1) * (areaWidth + 2)], k)) {
                                ++i2;
                            }
                            if (biomesEqualOrMesaPlateau(aint[j + 1 + (i + 2) * (areaWidth + 2)], k)) {
                                ++i2;
                            }
                            aint2[j + i * areaWidth] = i2 >= 3 ? j2 : k;
                        }
                    }
                }
            }
            return aint2;
        }
    }
}
