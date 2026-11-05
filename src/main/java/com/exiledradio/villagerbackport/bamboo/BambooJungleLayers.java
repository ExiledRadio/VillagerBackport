package com.exiledradio.villagerbackport.bamboo;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.init.Biomes;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Turns some jungles into bamboo jungles, the way 1.14 places them.
 *
 * <h2>How 1.14 does it</h2>
 * The bamboo jungle has no climate of its own. Right after the biome layer picks a jungle for an
 * area - at the scale where one cell is 256 blocks across - one jungle cell in ten becomes a bamboo
 * jungle. The later layers then treat it like a jungle: the hills layer makes some of it bamboo
 * jungle hills, rivers cut through it, and its edges are smoothed as the map is zoomed in.
 *
 * <h2>How this does it</h2>
 * Inserting a layer into the middle of 1.12's chain would mean rebuilding the chain, which would
 * undo every other mod's changes to it and could only work for world types this mod knows about.
 * Instead the finished map is filtered. Alongside it runs a second, private map of its own: one cell
 * in ten (the {@code bambooJungleRarity} setting) marked at the same 256-block scale, then zoomed in
 * with vanilla's own zoom layer so the marked areas come out as rounded blobs rather than squares.
 * Wherever the finished map says jungle and the private map is marked, the jungle becomes bamboo
 * jungle; jungle hills become bamboo jungle hills. Rivers, beaches and everything else are already
 * in place and are left alone.
 *
 * <p>The result is the same proportion of bamboo jungle as 1.14, in areas of the same size. The
 * difference is that a bamboo jungle's edge follows its own blob rather than the jungle's, so it can
 * cover part of a jungle rather than all of one.
 *
 * <p>Any world type that builds its map from vanilla's layers and places vanilla's jungle gets
 * bamboo jungles, modded ones included. World types that generate biomes some other way are not
 * affected.
 */
final class BambooJungleLayers {

    /** Seeds this mod's layers so they never correlate with vanilla's. */
    private static final long SEED = 114_168L;

    /** Zoom steps from the 256-block cell scale down to the 4-block scale of the biome map. */
    private static final int ZOOMS = 6;

    private BambooJungleLayers() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
        Biome bamboo = BambooContent.bambooJungle;
        Biome bambooHills = BambooContent.bambooJungleHills;
        GenLayer[] layers = event.getNewBiomeGens();
        if (bamboo == null || bambooHills == null || layers == null || layers.length < 2) {
            return;
        }

        int zooms = ZOOMS + (event.getWorldType() == WorldType.LARGE_BIOMES ? 2 : 0);
        GenLayer mask = GenLayerZoom.magnify(SEED + 1, new Regions(SEED, ModConfig.bamboo.bambooJungleRarity), zooms);
        Swap biomes = new Swap(SEED + 2, layers[0], mask, bamboo, bambooHills);
        biomes.initWorldGenSeed(event.getSeed());

        GenLayer[] replaced = layers.clone();
        replaced[0] = biomes;
        if (layers[1].getClass() == GenLayerVoronoiZoom.class) {
            // What vanilla builds, now over the filtered map, so both always agree.
            replaced[1] = new GenLayerVoronoiZoom(10L, biomes);
        } else {
            // Somebody else's block-scale layer: filter it the same way, two zooms finer.
            GenLayer fineMask = GenLayerZoom.magnify(SEED + 1, new Regions(SEED, ModConfig.bamboo.bambooJungleRarity),
                    zooms + 2);
            replaced[1] = new Swap(SEED + 3, layers[1], fineMask, bamboo, bambooHills);
        }
        replaced[1].initWorldGenSeed(event.getSeed());
        if (replaced.length > 2 && layers[2] == layers[0]) {
            replaced[2] = biomes;
        }
        event.setNewBiomeGens(replaced);
        VillagerBackport.LOGGER.debug("Bamboo jungles added to the biome map ({})", event.getWorldType().getName());
    }

    /** One cell in {@code rarity} marked, the rest not. */
    private static final class Regions extends GenLayer {

        private final int rarity;

        Regions(long seed, int rarity) {
            super(seed);
            this.rarity = Math.max(1, rarity);
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int[] out = IntCache.getIntCache(areaWidth * areaHeight);
            for (int z = 0; z < areaHeight; z++) {
                for (int x = 0; x < areaWidth; x++) {
                    initChunkSeed(areaX + x, areaY + z);
                    out[x + z * areaWidth] = nextInt(rarity) == 0 ? 1 : 0;
                }
            }
            return out;
        }
    }

    /** The finished map, with marked jungle turned into bamboo jungle. */
    private static final class Swap extends GenLayer {

        private final GenLayer mask;
        private final int jungle;
        private final int jungleHills;
        private final int bamboo;
        private final int bambooHills;

        Swap(long seed, GenLayer parent, GenLayer mask, Biome bamboo, Biome bambooHills) {
            super(seed);
            this.parent = parent;
            this.mask = mask;
            this.jungle = Biome.getIdForBiome(Biomes.JUNGLE);
            this.jungleHills = Biome.getIdForBiome(Biomes.JUNGLE_HILLS);
            this.bamboo = Biome.getIdForBiome(bamboo);
            this.bambooHills = Biome.getIdForBiome(bambooHills);
        }

        @Override
        public void initWorldGenSeed(long seed) {
            super.initWorldGenSeed(seed);
            mask.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int[] base = parent.getInts(areaX, areaY, areaWidth, areaHeight);
            int[] marked = mask.getInts(areaX, areaY, areaWidth, areaHeight);
            int[] out = IntCache.getIntCache(areaWidth * areaHeight);
            for (int i = 0; i < out.length; i++) {
                int id = base[i];
                if (marked[i] != 0) {
                    if (id == jungle) {
                        id = bamboo;
                    } else if (id == jungleHills) {
                        id = bambooHills;
                    }
                }
                out[i] = id;
            }
            return out;
        }
    }
}
