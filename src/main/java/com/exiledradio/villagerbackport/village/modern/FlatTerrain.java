package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Superflat worlds: the easiest terrain there is, and the one generator Forge's village event never
 * reaches.
 *
 * <p>Every column of a superflat world is the same stack of layers, which the generator keeps as one
 * block per height ({@code cachedBlockIDs}). The surface is one above the highest of them, wherever
 * the column is.
 *
 * <p>{@code ChunkGeneratorFlat} builds its village generator with {@code new MapGenVillage(...)} straight
 * into its own map of structures, without the {@code InitMapGenEvent} every other generator posts, so
 * {@link MapGenModernVillage.Swap} never sees it. It is swapped here instead, keeping the preset's own
 * village size and spacing.
 *
 * <p>When matters. A new superflat world generates chunks before Forge says it has loaded: vanilla
 * looks for grass at sea level to spawn on, superflat's ground is far below it, so the search wanders
 * up to a thousand chunks away, every one generated - and every village it passes near saved as a
 * 1.12.2 village, to be built as one later. So the swap happens as the spawn search begins, and again
 * on load for a world that already has its spawn.
 */
final class FlatTerrain implements ColumnTerrain {

    private final int height;
    private final boolean wet;

    private FlatTerrain(int height, boolean wet) {
        this.height = height;
        this.wet = wet;
    }

    /** Only vanilla's own flat generator: a subclass may lay its layers out some other way. */
    static boolean handles(@Nullable IChunkGenerator generator) {
        return generator != null && generator.getClass() == ChunkGeneratorFlat.class;
    }

    /** @return the terrain, or null for a world with no ground at all (the void preset) */
    @Nullable
    static FlatTerrain of(IChunkGenerator generator) {
        try {
            Field field = ReflectionHelper.findField(ChunkGeneratorFlat.class, "cachedBlockIDs", "field_82700_c");
            field.setAccessible(true);
            IBlockState[] layers = (IBlockState[]) field.get(generator);
            for (int y = layers.length - 1; y >= 0; y--) {
                IBlockState state = layers[y];
                // A snow layer on top (Snowy Kingdom) is not ground: houses stand on the grass under
                // it, as they do on snowy ground anywhere else. Water still counts.
                if (state != null && state.getMaterial() != Material.AIR
                        && (!state.getMaterial().isReplaceable() || state.getMaterial().isLiquid())) {
                    VillagerBackport.LOGGER.info("Laying out 1.14 villages on a superflat world's ground, at y {}.", y + 1);
                    return new FlatTerrain(y + 1, state.getMaterial().isLiquid());
                }
            }
            return null;
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("A superflat world's layers could not be read, so its villages stay 1.12.2's.", e);
            return null;
        }
    }

    @Override
    public int height(int x, int z) {
        return this.height;
    }

    @Override
    public boolean wet(int x, int z) {
        return this.wet;
    }

    /** @return the superflat generator's village generator, or null if it has none */
    @Nullable
    static Object villageGenerator(IChunkGenerator generator) {
        Map<String, MapGenStructure> structures = structures(generator);
        return structures == null ? null : structures.get("Village");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Map<String, MapGenStructure> structures(IChunkGenerator generator) {
        try {
            Field field = ReflectionHelper.findField(ChunkGeneratorFlat.class, "structureGenerators", "field_82696_f");
            field.setAccessible(true);
            return (Map<String, MapGenStructure>) field.get(generator);
        } catch (Exception e) {
            return null;
        }
    }

    /** Swaps a superflat world's village generator for this mod's before it makes any chunk. */
    public static final class Swap {
        /** A new world, before its spawn search generates anything. */
        @SubscribeEvent
        public void onCreateSpawn(WorldEvent.CreateSpawnPosition event) {
            swap(event.getWorld());
        }

        /** A world that already has its spawn. Does nothing if the spawn search already swapped it. */
        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            swap(event.getWorld());
        }

        private static void swap(net.minecraft.world.World world) {
            if (!(world instanceof net.minecraft.world.WorldServer)) {
                return;
            }
            IChunkGenerator generator = ((net.minecraft.world.WorldServer) world).getChunkProvider().chunkGenerator;
            if (!handles(generator)) {
                return;
            }
            // A preset without "village" has no village generator, and gets none.
            Map<String, MapGenStructure> structures = structures(generator);
            if (structures == null) {
                VillagerBackport.LOGGER.warn("A superflat world's structures could not be read, so its villages stay 1.12.2's.");
                return;
            }
            MapGenStructure villages = structures.get("Village");
            if (villages != null && villages.getClass() == MapGenVillage.class) {
                structures.put("Village", new MapGenModernVillage((MapGenVillage) villages));
                VillagerBackport.LOGGER.info("Superflat world: building this mod's villages.");
            }
        }
    }
}
