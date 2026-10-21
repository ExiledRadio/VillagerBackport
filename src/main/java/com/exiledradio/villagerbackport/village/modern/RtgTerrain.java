package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Terrain heights in a Realistic Terrain Generation world.
 *
 * <p>RTG works out a chunk's ground in two steps: a "landscape" of one height per column, from its
 * biomes' own noise, then a fill that puts stone up to that height and water up to sea level. Its
 * generator hands the landscape out ({@code getLandscape}) and caches it, so reading it for a chunk
 * that does not exist yet gives the height that chunk will have, and the work is reused when the
 * chunk is made.
 *
 * <p>The landscape is public API in RTG, but this mod does not depend on RTG, so it is reached by
 * reflection. If a later version changes it, villages in RTG worlds stay 1.12.2's.
 */
final class RtgTerrain implements ColumnTerrain {

    private static final String GENERATOR = "rtg.world.gen.ChunkGeneratorRTG";

    private final World world;
    private final Object generator;
    private final Method landscape;
    private final Field noise;
    private final int seaLevel;

    /** The heights last read, since a layout asks about many columns of the same chunk in a row. */
    private float[] lastNoise;
    private long lastKey = Long.MIN_VALUE;

    private RtgTerrain(World world, Object generator, Method landscape, Field noise) {
        this.world = world;
        this.generator = generator;
        this.landscape = landscape;
        this.noise = noise;
        this.seaLevel = world.getSeaLevel();
    }

    static boolean handles(@Nullable IChunkGenerator generator) {
        return generator != null && GENERATOR.equals(generator.getClass().getName());
    }

    @Nullable
    static RtgTerrain of(World world, IChunkGenerator generator) {
        try {
            Method landscape = generator.getClass().getMethod("getLandscape", BiomeProvider.class, ChunkPos.class);
            Field noise = landscape.getReturnType().getField("noise");
            VillagerBackport.LOGGER.info("Laying out 1.14 villages on Realistic Terrain Generation's terrain.");
            return new RtgTerrain(world, generator, landscape, noise);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Realistic Terrain Generation's landscape could not be read, so villages "
                    + "in its worlds stay 1.12.2's.", e);
            return null;
        }
    }

    /** @return the bare ground's top block in this column: RTG fills stone up to and including it */
    private int ground(int x, int z) throws Exception {
        long key = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xFFFFFFFFL);
        if (key != this.lastKey) {
            Object chunk = this.landscape.invoke(this.generator, this.world.getBiomeProvider(),
                    new ChunkPos(x >> 4, z >> 4));
            this.lastNoise = (float[]) this.noise.get(chunk);
            this.lastKey = key;
        }
        return (int) this.lastNoise[(x & 15) * 16 + (z & 15)];
    }

    @Override
    public int height(int x, int z) {
        try {
            // Water fills everything below sea level that the ground does not.
            return Math.max(ground(x, z), this.seaLevel - 1) + 1;
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Reading Realistic Terrain Generation's landscape failed at {}, {}.", x, z, e);
            return this.seaLevel;
        }
    }

    @Override
    public boolean wet(int x, int z) {
        try {
            return ground(x, z) < this.seaLevel - 1;
        } catch (Exception e) {
            return false;
        }
    }
}
