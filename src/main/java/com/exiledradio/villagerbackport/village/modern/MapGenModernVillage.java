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

    /** 1.12.2's village spacing in chunks, as copied from the generator replaced. */
    private int spacing = 32;

    public MapGenModernVillage(MapGenVillage replaced) {
        copy(replaced, "size", "field_75054_f");
        copy(replaced, "distance", "field_82665_g");
        copy(replaced, "minTownSeparation", "field_82666_h");
        try {
            Field field = ReflectionHelper.findField(MapGenVillage.class, "distance", "field_82665_g");
            field.setAccessible(true);
            this.spacing = Math.max(9, field.getInt(this));
        } catch (Exception e) {
            this.spacing = 32;
        }
    }

    /**
     * The nearest village of one 1.14 type, for {@code /locate village_plains} and friends.
     *
     * <p>Walks the same grid 1.12.2 places villages on, ring by ring outwards: each grid cell holds
     * at most one village, at a chunk fixed by the world seed, and its type is the type of the biome
     * it starts in - which is all that decides it, so villages not yet generated can be found too.
     * A village already generated the 1.12.2 way, before 1.14 villages were switched on, is skipped.
     *
     * @return the village's start position, or null if none lies within {@code maxRings} cells
     */
    @javax.annotation.Nullable
    public net.minecraft.util.math.BlockPos findNearest(net.minecraft.world.World world,
                                                        net.minecraft.util.math.BlockPos from,
                                                        ModernVillageStart.Type type, int maxRings) {
        this.world = world;
        int baseX = Math.floorDiv(from.getX() >> 4, this.spacing);
        int baseZ = Math.floorDiv(from.getZ() >> 4, this.spacing);
        net.minecraft.util.math.BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int foundRing = -1;

        for (int ring = 0; ring <= maxRings; ring++) {
            // A village one ring further out can still be nearer than the first one found.
            if (foundRing >= 0 && ring > foundRing + 1) {
                break;
            }
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    int cellX = baseX + dx;
                    int cellZ = baseZ + dz;
                    java.util.Random random = world.setRandomSeed(cellX, cellZ, 10387312);
                    int chunkX = cellX * this.spacing + random.nextInt(this.spacing - 8);
                    int chunkZ = cellZ * this.spacing + random.nextInt(this.spacing - 8);
                    if (!canSpawnStructureAtCoords(chunkX, chunkZ)) {
                        continue;
                    }
                    StructureStart existing = this.structureMap.get(net.minecraft.util.math.ChunkPos.asLong(chunkX, chunkZ));
                    if (existing != null && !(existing instanceof ModernVillageStart)) {
                        continue;
                    }
                    net.minecraft.world.biome.Biome biome = world.getBiomeProvider()
                            .getBiome(new net.minecraft.util.math.BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9));
                    if (ModernVillageStart.Type.of(biome) != type) {
                        continue;
                    }
                    net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos((chunkX << 4) + 8, 64, (chunkZ << 4) + 8);
                    double distance = pos.distanceSq(from.getX(), 64, from.getZ());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                        if (foundRing < 0) {
                            foundRing = ring;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** @return the overworld's village generator if it is this one and builds 1.14 villages, else null */
    @javax.annotation.Nullable
    public static MapGenModernVillage of(net.minecraft.world.World world) {
        if (ModConfig.villages.style != Style.MODERN || !(world instanceof net.minecraft.world.WorldServer)
                || TerrainHeights.forWorld(world) == null) {
            return null;
        }
        net.minecraft.world.gen.IChunkGenerator generator =
                ((net.minecraft.world.WorldServer) world).getChunkProvider().chunkGenerator;
        if (!(generator instanceof net.minecraft.world.gen.ChunkGeneratorOverworld)) {
            return null;
        }
        try {
            Field field = ReflectionHelper.findField(net.minecraft.world.gen.ChunkGeneratorOverworld.class,
                    "villageGenerator", "field_186005_x");
            field.setAccessible(true);
            Object village = field.get(generator);
            return village instanceof MapGenModernVillage ? (MapGenModernVillage) village : null;
        } catch (Exception e) {
            return null;
        }
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
