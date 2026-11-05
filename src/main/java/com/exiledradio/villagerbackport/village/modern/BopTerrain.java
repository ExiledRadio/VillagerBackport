package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Terrain heights in a Biomes O' Plenty world - BOP's own world type, whose generator is written from
 * scratch rather than built on vanilla's.
 *
 * <p>BOP makes a chunk's ground in one public step, {@code setChunkAirStoneWater}: stone up to its
 * noise, water up to sea level, air above, before any surface blocks, caves or structures. That is
 * the same bare terrain the vanilla path reads heights from, so it answers the same question.
 *
 * <p>The step keeps its working arrays in the generator, so it is asked of a second generator built
 * from the world's own seed and settings rather than of the live one, which may be part-way through
 * a chunk of its own. BOP is not a dependency of this mod, so everything is reached by name; if a
 * later version changes it, villages in BOP worlds stay 1.12.2's.
 */
final class BopTerrain implements ColumnTerrain {

    static final String GENERATOR = "biomesoplenty.common.world.ChunkGeneratorOverworldBOP";

    /** A layout spans up to about 13 chunks each way; this holds all of one. */
    private static final int CACHED_CHUNKS = 400;

    /** Set on a column whose top block is water. Heights are 0-256, well below it. */
    private static final int WET = 1 << 16;

    private final Object generator;
    private final Method terrain;

    private final Map<Long, int[]> chunks = new LinkedHashMap<Long, int[]>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > CACHED_CHUNKS;
        }
    };

    private BopTerrain(Object generator, Method terrain) {
        this.generator = generator;
        this.terrain = terrain;
    }

    static boolean handles(@Nullable IChunkGenerator generator) {
        return generator != null && GENERATOR.equals(generator.getClass().getName());
    }

    /** @return a reader for this world's terrain, or null if BOP's generator could not be copied */
    @Nullable
    static BopTerrain of(World world, IChunkGenerator live) {
        try {
            Class<?> type = live.getClass();
            Object copy = type.getConstructor(World.class, long.class, boolean.class, String.class)
                    .newInstance(world, world.getSeed(), false, world.getWorldInfo().getGeneratorOptions());
            Method terrain = type.getMethod("setChunkAirStoneWater", int.class, int.class, ChunkPrimer.class);
            VillagerBackport.LOGGER.info("Laying out 1.14 villages on Biomes O' Plenty's terrain.");
            return new BopTerrain(copy, terrain);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Biomes O' Plenty's terrain could not be read, so villages in its worlds stay "
                    + "1.12.2's.", e);
            return null;
        }
    }

    @Override
    public int height(int x, int z) {
        return column(x, z) & (WET - 1);
    }

    @Override
    public boolean wet(int x, int z) {
        return (column(x, z) & WET) != 0;
    }

    private int column(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        int[] column = this.chunks.get(key);
        if (column == null) {
            column = sample(chunkX, chunkZ);
            this.chunks.put(key, column);
        }
        return column[(z & 15) << 4 | (x & 15)];
    }

    private int[] sample(int chunkX, int chunkZ) {
        int[] out = new int[256];
        try {
            ChunkPrimer primer = new ChunkPrimer();
            this.terrain.invoke(this.generator, chunkX, chunkZ, primer);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = 255;
                    while (y > 0 && primer.getBlockState(x, y, z).getMaterial() == Material.AIR) {
                        y--;
                    }
                    boolean wet = primer.getBlockState(x, y, z).getMaterial().isLiquid();
                    out[z << 4 | x] = (y + 1) | (wet ? WET : 0);
                }
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Reading Biomes O' Plenty's terrain failed at chunk {}, {}.", chunkX, chunkZ, e);
            java.util.Arrays.fill(out, 64);
        }
        return out;
    }
}
