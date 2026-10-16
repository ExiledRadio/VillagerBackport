package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.init.Biomes;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

/**
 * The overworld's village generator, building 1.14 villages.
 *
 * <h2>How it gets in</h2>
 * Forge's {@link InitMapGenEvent} hands every mod the village generator as the overworld generator
 * is built, and lets it substitute its own. The substitute has to be a {@link MapGenVillage}, which
 * this is - placement, spacing, {@code /locate}, the "is this chunk a village" test the lake
 * generator asks, and the saved structure data all keep working through the parent class, so only
 * what a village <em>is</em> changes. Only vanilla's own generator is replaced; a mod that already
 * substituted its own keeps it.
 *
 * <p>Village spacing is copied from the generator being replaced, so a superflat preset's village
 * settings still apply.
 */
public class MapGenModernVillage extends MapGenVillage {

    private static boolean registered;

    public MapGenModernVillage(MapGenVillage replaced) {
        copy(replaced, "size", "field_75054_f");
        copy(replaced, "distance", "field_82665_g");
        copy(replaced, "minTownSeparation", "field_82666_h");
    }

    private void copy(MapGenVillage from, String... names) {
        try {
            Field field = ReflectionHelper.findField(MapGenVillage.class, names);
            field.setAccessible(true);
            field.set(this, field.get(from));
        } catch (Exception e) {
            VillagerBackport.LOGGER.debug("Could not copy village setting {}; using the default.", names[0]);
        }
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        if (ModConfig.villages.style == Style.MODERN) {
            TerrainHeights heights = TerrainHeights.forWorld(this.world);
            if (heights != null) {
                return new ModernVillageStart(this.world, this.rand, chunkX, chunkZ, heights);
            }
        }
        return super.getStructureStart(chunkX, chunkZ);
    }

    /**
     * Places the chunk's village pieces, then - for villages built 1.12.2's way - the workstations
     * 1.12.2's houses lack. See {@link com.exiledradio.villagerbackport.village.LegacyHouseWorkstations}.
     */
    @Override
    public synchronized boolean generateStructure(net.minecraft.world.World world, java.util.Random random,
                                                  net.minecraft.util.math.ChunkPos chunk) {
        boolean placed = super.generateStructure(world, random, chunk);
        if (placed && ModConfig.villages.legacyHouseWorkstations) {
            int x = (chunk.x << 4) + 8;
            int z = (chunk.z << 4) + 8;
            net.minecraft.world.gen.structure.StructureBoundingBox area =
                    new net.minecraft.world.gen.structure.StructureBoundingBox(x, z, x + 15, z + 15);
            for (StructureStart start : this.structureMap.values()) {
                if (!(start instanceof ModernVillageStart) && start.isSizeableStructure()
                        && start.getBoundingBox().intersectsWith(area)) {
                    com.exiledradio.villagerbackport.village.LegacyHouseWorkstations.furnish(world, start, area);
                }
            }
        }
        return placed;
    }

    public enum Style {
        /** 1.14's villages, from 1.14's own structure files. */
        MODERN,
        /** 1.12.2's villages. */
        LEGACY
    }

    /** Registers the saved-data names and the generator swap. Called once, during init. */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Registered whatever the setting, so villages already saved load after switching back.
        MapGenStructureIO.registerStructure(ModernVillageStart.class, "VBP_ModernVillage");
        MapGenStructureIO.registerStructureComponent(ModernVillagePiece.class, "VBP_ModernPiece");

        if (ModConfig.villages.style == Style.MODERN) {
            // 1.14 builds snowy villages on snowy tundra, which 1.12.2 never had villages on.
            BiomeManager.addVillageBiome(Biomes.ICE_PLAINS, true);
        }
        MinecraftForge.TERRAIN_GEN_BUS.register(new MapGenModernVillage.Swap());
    }

    public static final class Swap {
        @SubscribeEvent(priority = EventPriority.LOW)
        public void onInitMapGen(InitMapGenEvent event) {
            if (event.getType() == InitMapGenEvent.EventType.VILLAGE && event.getNewGen() != null
                    && event.getNewGen().getClass() == MapGenVillage.class) {
                event.setNewGen(new MapGenModernVillage((MapGenVillage) event.getNewGen()));
            }
        }
    }
}
