package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Terrain heights in an Open Terrain Generator world.
 *
 * <p>OTG builds a chunk's terrain in {@code ChunkProviderOTG}: noise, water, then the biome's
 * surface blocks ({@code generateTerrainA}), and only after that caves, ravines and structures. Its
 * own "height in an unloaded chunk" answer, {@code OTGChunkGenerator.getHighestBlockYInUnloadedChunk},
 * cannot be used here: it runs the world's own terrain provider, whose last step prepares structures
 * - villages among them - so asking it during a village layout re-enters the layout and clears the
 * chunk buffer the chunk being generated is still using. Its column cache also stores an empty array
 * in place of the real one, so a second question about the same column fails.
 *
 * <p>So this keeps a second terrain provider, built from the world's own settings and seed, and asks
 * it only for {@code generateTerrainA}: the same noise, water and surface the chunk will get, with
 * none of the structure side effects. Those are private to OTG, so they are reached by reflection;
 * if a later version moves them, villages in its worlds stay OTG's own.
 */
final class OtgTerrain implements ColumnTerrain {

    static final String GENERATOR = "com.pg85.otg.forge.generator.OTGChunkGenerator";
    private static final String PROVIDER = "com.pg85.otg.generator.ChunkProviderOTG";
    private static final String CONFIG_PROVIDER = "com.pg85.otg.network.ConfigProvider";
    private static final String LOCAL_WORLD = "com.pg85.otg.common.LocalWorld";
    private static final String CHUNK_BUFFER = "com.pg85.otg.forge.generator.ForgeChunkBuffer";
    private static final String CHUNK_COORDINATE = "com.pg85.otg.util.ChunkCoordinate";

    /** A layout spans up to about 13 chunks each way; this holds all of one. */
    private static final int CACHED_CHUNKS = 400;

    private final Object provider;
    private final Method terrain;
    private final Random random;
    private final Constructor<?> buffer;
    private final Method coordinate;
    private final Method primer;

    private final Map<Long, int[]> chunks = new LinkedHashMap<Long, int[]>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
            return size() > CACHED_CHUNKS;
        }
    };

    private OtgTerrain(Object provider, Method terrain, Random random, Constructor<?> buffer, Method coordinate,
            Method primer) {
        this.provider = provider;
        this.terrain = terrain;
        this.random = random;
        this.buffer = buffer;
        this.coordinate = coordinate;
        this.primer = primer;
    }

    static boolean handles(@Nullable IChunkGenerator generator) {
        return generator != null && GENERATOR.equals(generator.getClass().getName());
    }

    /** @return a reader for this world's terrain, or null if OTG's internals could not be reached */
    @Nullable
    static OtgTerrain of(World world, IChunkGenerator generator) {
        try {
            ClassLoader loader = OtgTerrain.class.getClassLoader();
            Field worldField = generator.getClass().getDeclaredField("world");
            worldField.setAccessible(true);
            Object forgeWorld = worldField.get(generator);
            Object configs = forgeWorld.getClass().getMethod("getConfigs").invoke(forgeWorld);

            Class<?> providerType = Class.forName(PROVIDER, false, loader);
            Object provider = providerType.getConstructor(Class.forName(CONFIG_PROVIDER, false, loader),
                    Class.forName(LOCAL_WORLD, false, loader)).newInstance(configs, forgeWorld);
            Class<?> bufferType = Class.forName(CHUNK_BUFFER, false, loader);
            Method terrain = providerType.getDeclaredMethod("generateTerrainA", int.class, int.class,
                    bufferType.getSuperclass());
            terrain.setAccessible(true);
            Field randomField = providerType.getDeclaredField("random");
            randomField.setAccessible(true);
            Class<?> coordinateType = Class.forName(CHUNK_COORDINATE, false, loader);
            Constructor<?> buffer = bufferType.getDeclaredConstructor(coordinateType);
            buffer.setAccessible(true);
            Method coordinate = coordinateType.getMethod("fromChunkCoords", int.class, int.class);
            Method primer = bufferType.getMethod("getChunkPrimer");
            VillagerBackport.LOGGER.info("Laying out 1.14 villages on Open Terrain Generator's terrain.");
            return new OtgTerrain(provider, terrain, (Random) randomField.get(provider), buffer, coordinate, primer);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Open Terrain Generator's terrain could not be read, so villages in its worlds "
                    + "stay its own.", e);
            return null;
        }
    }

    /** Set on a column whose top block is water. OTG heights are 0-256, well below it. */
    private static final int WET = 1 << 16;

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
            Object buffer = this.buffer.newInstance(this.coordinate.invoke(null, chunkX, chunkZ));
            // As ChunkProviderOTG.generate seeds it, so the surface blocks come out the same.
            this.random.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);
            this.terrain.invoke(this.provider, chunkX, chunkZ, buffer);
            ChunkPrimer primer = (ChunkPrimer) this.primer.invoke(buffer);
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
            VillagerBackport.LOGGER.warn("Reading Open Terrain Generator's terrain failed at chunk {}, {}.", chunkX, chunkZ, e);
            java.util.Arrays.fill(out, 64);
        }
        return out;
    }
}
