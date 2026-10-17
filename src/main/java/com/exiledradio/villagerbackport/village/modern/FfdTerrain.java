package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.gen.IChunkGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Terrain heights in a Farmer's Future Delight Caves &amp; Cliffs world.
 *
 * <p>That world's terrain is 1.18's, from its own density functions, so the vanilla noise the rest
 * of {@link TerrainHeights} copies would put every house at the wrong height. Its generator keeps a
 * terrain sample per chunk - the density and aquifer data a chunk is built from - and the sample can
 * say exactly where each column's surface is. Reading that from the live generator gives the height
 * the chunk will really have, before it exists. The generator's carvers never cut within four blocks
 * of the surface, so they cannot change the answer.
 *
 * <p>Those are private to that mod, so they are reached by reflection. If a later version renames
 * them, its public {@code estimateModernSurfaceHeight} - 1.18's preliminary surface level, the
 * estimate modern Minecraft itself plans structures with - is used instead: less exact, never wrong
 * by much.
 */
final class FfdTerrain {

    static final String GENERATOR = "xy177.farmersfuturedelight.common.world.ChunkGeneratorExtended";

    /** 1.14 counts water as surface; this is how far a column may be flooded before giving up. */
    private static final int MAX_WATER_DEPTH = 96;

    private final Object generator;
    private Method terrainSample;
    private Object surfaceRules;
    private Method surfaceY;
    private Method baseStateAt;
    private Method estimate;
    private boolean exact;

    static boolean handles(IChunkGenerator generator) {
        return generator != null && GENERATOR.equals(generator.getClass().getName());
    }

    FfdTerrain(IChunkGenerator generator) {
        this.generator = generator;
        Class<?> type = generator.getClass();
        try {
            this.estimate = type.getMethod("estimateModernSurfaceHeight", int.class, int.class);
        } catch (Exception e) {
            this.estimate = null;
        }
        try {
            this.terrainSample = type.getDeclaredMethod("terrainSample", int.class, int.class);
            this.terrainSample.setAccessible(true);
            Field rules = type.getDeclaredField("surfaceRules");
            rules.setAccessible(true);
            this.surfaceRules = rules.get(generator);
            Class<?> sample = this.terrainSample.getReturnType();
            this.surfaceY = sample.getDeclaredMethod("surfaceY", int.class, int.class, rules.getType());
            this.surfaceY.setAccessible(true);
            this.baseStateAt = sample.getDeclaredMethod("baseStateAt", int.class, int.class, int.class, int.class,
                    int.class, rules.getType());
            this.baseStateAt.setAccessible(true);
            this.exact = true;
            VillagerBackport.LOGGER.info("Laying out 1.14 villages on Farmer's Future Delight's terrain.");
        } catch (Exception e) {
            this.exact = false;
            VillagerBackport.LOGGER.warn("Farmer's Future Delight's terrain sample could not be read, so 1.14 villages "
                    + "use its surface estimate instead. Houses may sit a little off the ground.", e);
        }
    }

    /** @return true if heights can be had at all */
    boolean usable() {
        return this.exact || this.estimate != null;
    }

    /** 1.14's {@code WORLD_SURFACE_WG}: one above the top block of the bare terrain, water included. */
    int height(int x, int z) {
        if (this.exact) {
            try {
                Object sample = this.terrainSample.invoke(this.generator, x >> 4, z >> 4);
                int top = (Integer) this.surfaceY.invoke(sample, x & 15, z & 15, this.surfaceRules);
                int y = top + 1;
                for (int i = 0; i < MAX_WATER_DEPTH; i++) {
                    IBlockState state = (IBlockState) this.baseStateAt.invoke(sample, x & 15, y, z & 15, x, z,
                            this.surfaceRules);
                    if (state == null || !state.getMaterial().isLiquid()) {
                        break;
                    }
                    y++;
                }
                return y;
            } catch (Exception e) {
                this.exact = false;
                VillagerBackport.LOGGER.warn("Reading Farmer's Future Delight's terrain failed; using its surface estimate.", e);
            }
        }
        try {
            return Math.max((Integer) this.estimate.invoke(this.generator, x, z) + 1, 63);
        } catch (Exception e) {
            return 64;
        }
    }
}
