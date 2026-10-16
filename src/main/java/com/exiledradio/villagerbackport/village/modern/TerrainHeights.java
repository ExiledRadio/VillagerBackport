package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Terrain height before anything is built on it, for chunks that do not exist yet.
 *
 * <h2>Why this is needed</h2>
 * 1.14 lays a village out in one go when its first chunk generates, and places every building at
 * the height the terrain will have - it asks the chunk generator, which can answer for any column
 * from the noise alone ({@code func_222532_b}). 1.12.2's generator has no such question. Its
 * villages work around that by measuring the ground later, piece by piece, from whatever part of
 * the piece happens to be in the chunk being populated - which is why 1.12.2 houses so often float
 * or sink.
 *
 * <h2>How</h2>
 * A second copy of the world's own overworld generator, built from the same seed and settings,
 * whose only job is {@code setBlocksInChunk}: the stone-and-water shape of a chunk, from noise, with
 * no caves, decoration or structures. That is exactly what 1.14 measures. It is a separate instance
 * because the world's generator keeps per-chunk working state in fields, and it is in the middle of
 * using them when a village is laid out.
 *
 * <p>Only possible when the world uses the vanilla overworld generator (or a subclass that can be
 * built the same way). Anywhere else there is no sampler, and villages there stay 1.12.2's.
 */
public final class TerrainHeights {

    private static final Map<World, TerrainHeights> SAMPLERS = new WeakHashMap<World, TerrainHeights>();
    private static final Map<World, Boolean> UNSUPPORTED = new WeakHashMap<World, Boolean>();

    private final ChunkGeneratorOverworld generator;

    /** Column heights per chunk, kept only for one village's layout. */
    private final Map<Long, int[]> heights = new HashMap<Long, int[]>();

    private TerrainHeights(ChunkGeneratorOverworld generator) {
        this.generator = generator;
    }

    /** @return a sampler for this world, or null if its terrain cannot be predicted */
    @Nullable
    public static synchronized TerrainHeights forWorld(World world) {
        TerrainHeights sampler = SAMPLERS.get(world);
        if (sampler != null) {
            sampler.heights.clear();
            return sampler;
        }
        if (UNSUPPORTED.containsKey(world) || !(world instanceof WorldServer)) {
            return null;
        }

        IChunkGenerator live = ((WorldServer) world).getChunkProvider().chunkGenerator;
        ChunkGeneratorOverworld copy = null;
        if (live instanceof ChunkGeneratorOverworld) {
            try {
                Constructor<? extends IChunkGenerator> ctor = live.getClass()
                        .getConstructor(World.class, long.class, boolean.class, String.class);
                copy = (ChunkGeneratorOverworld) ctor.newInstance(world, world.getSeed(), false,
                        world.getWorldInfo().getGeneratorOptions());
            } catch (Exception e) {
                VillagerBackport.LOGGER.warn("Could not copy the world generator {} to lay out 1.14 villages; "
                        + "villages in this world stay 1.12.2's.", live.getClass().getName(), e);
            }
        } else {
            VillagerBackport.LOGGER.info("World generator {} is not the overworld generator, so 1.14 villages "
                    + "cannot be laid out in it. Villages there stay 1.12.2's.", live == null ? "none" : live.getClass().getName());
        }

        if (copy == null) {
            UNSUPPORTED.put(world, Boolean.TRUE);
            return null;
        }
        sampler = new TerrainHeights(copy);
        SAMPLERS.put(world, sampler);
        return sampler;
    }

    /**
     * 1.14's {@code WORLD_SURFACE_WG}: one above the highest non-air block of the bare terrain,
     * water included.
     */
    public int height(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        int[] column = this.heights.get(key);
        if (column == null) {
            column = sample(chunkX, chunkZ);
            this.heights.put(key, column);
        }
        return column[(z & 15) << 4 | (x & 15)];
    }

    private int[] sample(int chunkX, int chunkZ) {
        ChunkPrimer primer = new ChunkPrimer();
        this.generator.setBlocksInChunk(chunkX, chunkZ, primer);
        int[] out = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int y = 255;
                while (y > 0 && primer.getBlockState(x, y, z).getMaterial() == Material.AIR) {
                    y--;
                }
                out[z << 4 | x] = y + 1;
            }
        }
        return out;
    }
}
