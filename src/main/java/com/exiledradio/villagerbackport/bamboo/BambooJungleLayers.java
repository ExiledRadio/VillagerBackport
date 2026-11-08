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
 * Puts 1.14's bamboo jungles into the biome map.
 *
 * <h2>Vanilla's world types: 1.14's own layers</h2>
 * For the default, large biomes and amplified world types, while no other mod has changed the map yet, the
 * map is rebuilt as 1.14.4 builds it ({@link Layers114}): one jungle cell in ten becomes bamboo jungle right
 * after the biome layer, and the later layers treat it as 1.14's do, so a seed gives the same map as on
 * 1.14's seed maps. Update Aquatic's ocean temperatures, if installed, are mixed in after this (a lower
 * priority), as 1.14 mixes them last.
 *
 * <h2>Other maps: a filter</h2>
 * A map another mod built or changed is not rebuilt, which would undo its changes. It is filtered instead:
 * beside it runs a second, private map with one cell in ten (the {@code bambooJungleRarity} setting) marked at
 * the same 256-block scale and zoomed in with vanilla's zoom layer; wherever the finished map says jungle
 * and the private map is marked, the jungle becomes bamboo jungle, and jungle hills bamboo jungle hills. The
 * proportion and size of bamboo jungles are 1.14's; their places are not.
 */
final class BambooJungleLayers {

    /** Seeds this mod's layers so they never correlate with vanilla's. */
    private static final long SEED = 114_168L;

    /** Zoom steps from the 256-block cell scale down to the 4-block scale of the biome map. */
    private static final int ZOOMS = 6;

    private BambooJungleLayers() {
    }

    /** High, so the rebuilt map is the one later handlers (Update Aquatic's oceans) work on. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
        Biome bamboo = BambooContent.bambooJungle;
        Biome bambooHills = BambooContent.bambooJungleHills;
        GenLayer[] layers = event.getNewBiomeGens();
        if (bamboo == null || bambooHills == null || layers == null || layers.length < 2) {
            return;
        }
        WorldType type = event.getWorldType();
        if ((type == WorldType.DEFAULT || type == WorldType.LARGE_BIOMES || type == WorldType.AMPLIFIED)
                && unchanged(event)) {
            event.setNewBiomeGens(Layers114.build(event.getSeed(), type, bamboo, bambooHills,
                    ModConfig.bamboo.bambooJungleRarity));
            VillagerBackport.LOGGER.debug("Biome map built with 1.14's layers ({})", type.getName());
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

    /** @return true if the map is still the one vanilla built (nobody before this changed it) */
    private static boolean unchanged(WorldTypeEvent.InitBiomeGens event) {
        GenLayer[] original = event.getOriginalBiomeGens();
        GenLayer[] current = event.getNewBiomeGens();
        if (original == null || original.length != current.length) {
            return false;
        }
        for (int i = 0; i < original.length; i++) {
            if (original[i] != current[i]) {
                return false;
            }
        }
        return original[0] instanceof net.minecraft.world.gen.layer.GenLayerRiverMix;
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
