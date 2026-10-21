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

    /**
     * Open Terrain Generator's own village generator, when this stands in for it: villages go only
     * where its biome configs allow, of the kind they name, and fall back to it where 1.14 villages
     * cannot be laid out. Null in every other world.
     */
    @javax.annotation.Nullable
    private net.minecraft.world.gen.structure.MapGenStructure otg;
    @javax.annotation.Nullable
    private java.util.List<net.minecraft.world.biome.Biome> otgBiomes;
    private final java.util.Map<net.minecraft.world.biome.Biome, ModernVillageStart.Type> otgTypes =
            new java.util.HashMap<net.minecraft.world.biome.Biome, ModernVillageStart.Type>();

    static final String OTG_VILLAGES = "com.pg85.otg.forge.generator.structure.OTGVillageGen";

    private MapGenModernVillage() {
    }

    /**
     * Stands in for Open Terrain Generator's village generator, keeping its spacing, its biomes and
     * the village kind each biome config names.
     */
    @SuppressWarnings("unchecked")
    static MapGenModernVillage forOtg(net.minecraft.world.gen.structure.MapGenStructure otg) throws ReflectiveOperationException {
        MapGenModernVillage village = new MapGenModernVillage();
        Class<?> type = otg.getClass();
        Field distance = type.getDeclaredField("distance");
        distance.setAccessible(true);
        int spacing = distance.getInt(otg);
        Field mine = ReflectionHelper.findField(MapGenVillage.class, "distance", "field_82665_g");
        mine.setAccessible(true);
        mine.setInt(village, spacing);
        village.spacing = Math.max(9, spacing);
        village.otg = otg;
        village.otgBiomes = (java.util.List<net.minecraft.world.biome.Biome>) type.getField("villageSpawnBiomes").get(otg);

        Field worldField = type.getSuperclass().getDeclaredField("forgeWorld");
        worldField.setAccessible(true);
        Object forgeWorld = worldField.get(otg);
        Object configs = forgeWorld.getClass().getMethod("getConfigs").invoke(forgeWorld);
        Object[] biomes = (Object[]) configs.getClass().getMethod("getBiomeArrayByOTGId").invoke(configs);
        for (Object biome : biomes) {
            if (biome == null) {
                continue;
            }
            Object config = biome.getClass().getMethod("getBiomeConfig").invoke(biome);
            Object kind = config.getClass().getField("villageType").get(config);
            net.minecraft.world.biome.Biome handle =
                    (net.minecraft.world.biome.Biome) biome.getClass().getMethod("getHandle").invoke(biome);
            ModernVillageStart.Type mapped = ModernVillageStart.Type.ofOtg(String.valueOf(kind), handle);
            if (mapped != null && handle != null) {
                village.otgTypes.put(handle, mapped);
            }
        }
        VillagerBackport.LOGGER.info("Standing in for Open Terrain Generator's villages: every {} chunks, {} village "
                + "biomes.", village.spacing, village.otgTypes.size());
        return village;
    }

    /** @return the 1.14 village kind for a village starting in this biome */
    ModernVillageStart.Type typeAt(net.minecraft.world.biome.Biome biome) {
        ModernVillageStart.Type type = this.otgTypes.get(biome);
        return type != null ? type : ModernVillageStart.Type.of(biome);
    }

    /** OTG's placement: 1.12.2's grid, its own biome list rather than vanilla's village biomes. */
    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        if (this.otgBiomes == null) {
            return super.canSpawnStructureAtCoords(chunkX, chunkZ);
        }
        int cellX = Math.floorDiv(chunkX, this.spacing);
        int cellZ = Math.floorDiv(chunkZ, this.spacing);
        java.util.Random random = this.world.setRandomSeed(cellX, cellZ, 10387312);
        int x = cellX * this.spacing + random.nextInt(this.spacing - 8);
        int z = cellZ * this.spacing + random.nextInt(this.spacing - 8);
        return chunkX == x && chunkZ == z
                && this.world.getBiomeProvider().areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 0, this.otgBiomes);
    }

    /** OTG saves its villages as "OTGVillage"; keeping the name keeps the villages already there. */
    @Override
    public String getStructureName() {
        return this.otg != null ? this.otg.getStructureName() : super.getStructureName();
    }

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
                    if (typeAt(biome) != type) {
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
        if (OtgTerrain.handles(generator)) {
            return unwrap(otgVillageGenerator(generator));
        }
        if (RtgTerrain.handles(generator)) {
            // RTG keeps its village generator in a field of its own, built through Forge's event.
            try {
                Field field = generator.getClass().getDeclaredField("villageGenerator");
                field.setAccessible(true);
                return unwrap(field.get(generator));
            } catch (Exception e) {
                return null;
            }
        }
        if (!(generator instanceof net.minecraft.world.gen.ChunkGeneratorOverworld)) {
            // Caves Not Cliffs builds the terrain itself and keeps a vanilla overworld generator
            // inside only for its structures, villages among them.
            generator = innerStructureGenerator(generator);
            if (generator == null) {
                return null;
            }
        }
        try {
            Field field = ReflectionHelper.findField(net.minecraft.world.gen.ChunkGeneratorOverworld.class,
                    "villageGenerator", "field_186005_x");
            field.setAccessible(true);
            return unwrap(field.get(generator));
        } catch (Exception e) {
            return null;
        }
    }

    @javax.annotation.Nullable
    private static MapGenModernVillage unwrap(@javax.annotation.Nullable Object village) {
        if (!(village instanceof MapGenModernVillage) && village != null) {
            Wrapped wrapped = Wrapped.in(village);
            village = wrapped == null ? null : wrapped.generator();
        }
        return village instanceof MapGenModernVillage ? (MapGenModernVillage) village : null;
    }

    /** @return the village generator OTG keeps on its world object, or null */
    @javax.annotation.Nullable
    private static Object otgVillageGenerator(net.minecraft.world.gen.IChunkGenerator generator) {
        try {
            Field field = generator.getClass().getDeclaredField("world");
            field.setAccessible(true);
            Object forgeWorld = field.get(generator);
            return forgeWorld.getClass().getField("villageGen").get(forgeWorld);
        } catch (Exception e) {
            return null;
        }
    }

    /** @return the vanilla overworld generator a terrain mod keeps for its structures, or null */
    @javax.annotation.Nullable
    private static net.minecraft.world.gen.IChunkGenerator innerStructureGenerator(
            @javax.annotation.Nullable net.minecraft.world.gen.IChunkGenerator outer) {
        if (outer == null) {
            return null;
        }
        try {
            Field field = outer.getClass().getDeclaredField("structureGenerator");
            field.setAccessible(true);
            Object inner = field.get(outer);
            return inner instanceof net.minecraft.world.gen.ChunkGeneratorOverworld
                    ? (net.minecraft.world.gen.IChunkGenerator) inner : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 1.12.2's village size setting, which other mods' pieces scale their limits by. */
    private int villageSize() {
        try {
            Field field = ReflectionHelper.findField(MapGenVillage.class, "size", "field_75054_f");
            field.setAccessible(true);
            return field.getInt(this);
        } catch (Exception e) {
            return 0;
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
                net.minecraft.world.biome.Biome biome = this.world.getBiomeProvider()
                        .getBiome(new net.minecraft.util.math.BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9));
                return new ModernVillageStart(this.world, this.rand, chunkX, chunkZ, heights, typeAt(biome),
                        villageSize());
            }
        }
        if (this.otg != null) {
            return otgStart(chunkX, chunkZ);
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
            try (AutoCloseable scope = WorldHeights.enterFullHeight()) {
                // Inside the scope, so the range asked for is the full one.
                net.minecraft.world.gen.structure.StructureBoundingBox area =
                        new net.minecraft.world.gen.structure.StructureBoundingBox(x, WorldHeights.minY(world), z,
                                x + 15, WorldHeights.maxYExclusive(world) - 1, z + 15);
                for (StructureStart start : this.structureMap.values()) {
                    if (!(start instanceof ModernVillageStart) && start.isSizeableStructure()
                            && start.getBoundingBox().intersectsWith(area)) {
                        com.exiledradio.villagerbackport.village.LegacyHouseWorkstations.furnish(world, start, area);
                    }
                }
            } catch (Exception e) {
                VillagerBackport.LOGGER.warn("Could not furnish a 1.12.2 village with workstations.", e);
            }
        }
        return placed;
    }

    /** OTG's own village, for where 1.14's cannot be laid out: its generator, with this one's world and random. */
    private StructureStart otgStart(int chunkX, int chunkZ) {
        try {
            Field world = ReflectionHelper.findField(net.minecraft.world.gen.MapGenBase.class, "world", "field_75039_c");
            Field rand = ReflectionHelper.findField(net.minecraft.world.gen.MapGenBase.class, "rand", "field_75038_b");
            world.set(this.otg, this.world);
            rand.set(this.otg, this.rand);
            java.lang.reflect.Method start = ReflectionHelper.findMethod(net.minecraft.world.gen.structure.MapGenStructure.class,
                    "getStructureStart", "func_75049_b", int.class, int.class);
            return (StructureStart) start.invoke(this.otg, chunkX, chunkZ);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not ask Open Terrain Generator for its village; using 1.12.2's.", e);
            return super.getStructureStart(chunkX, chunkZ);
        }
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
            if (event.getType() != InitMapGenEvent.EventType.VILLAGE || event.getNewGen() == null) {
                return;
            }
            if (event.getNewGen().getClass() == MapGenVillage.class) {
                event.setNewGen(new MapGenModernVillage((MapGenVillage) event.getNewGen()));
                return;
            }
            // Recurrent Complex (with its decorationHacks on, as RLCraft has it) wraps the vanilla
            // generator in a hook that can put its own structures in a village's place, and keeps the
            // generator it wrapped. Swapping that one keeps the hook and makes the villages 1.14's.
            if (event.getNewGen().getClass().getName().equals(OTG_VILLAGES)) {
                MapGenModernVillage otg = otgSwap(event.getNewGen());
                if (otg != null) {
                    event.setNewGen(otg);
                }
                return;
            }
            Wrapped wrapped = Wrapped.in(event.getNewGen());
            if (wrapped != null && wrapped.generator() != null
                    && wrapped.generator().getClass().getName().equals(OTG_VILLAGES)) {
                MapGenModernVillage otg = otgSwap(wrapped.generator());
                if (otg != null) {
                    wrapped.replace(otg);
                    VillagerBackport.LOGGER.info("Building 1.14 villages for Open Terrain Generator inside {}'s village "
                            + "generator.", event.getNewGen().getClass().getName());
                }
                return;
            }
            if (wrapped != null && wrapped.generator().getClass() == MapGenVillage.class) {
                wrapped.replace(new MapGenModernVillage((MapGenVillage) wrapped.generator()));
                VillagerBackport.LOGGER.info("Building 1.14 villages inside {}'s village generator.",
                        event.getNewGen().getClass().getName());
            }
        }
    }

    /** Only with 1.14 villages switched on: in 1.12.2 style OTG's own villages are the right ones. */
    @javax.annotation.Nullable
    private static MapGenModernVillage otgSwap(Object otg) {
        if (ModConfig.villages.style != Style.MODERN) {
            return null;
        }
        try {
            return forOtg((net.minecraft.world.gen.structure.MapGenStructure) otg);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not stand in for Open Terrain Generator's village generator; villages "
                    + "there stay its own.", e);
            return null;
        }
    }

    /**
     * A generator another mod has wrapped: the wrapper holds a hook object with a public
     * {@code base} field naming the generator it stands in front of.
     */
    private static final class Wrapped {
        private final Object hook;
        private final Field base;

        private Wrapped(Object hook, Field base) {
            this.hook = hook;
            this.base = base;
        }

        @javax.annotation.Nullable
        static Wrapped in(Object wrapper) {
            for (Class<?> c = wrapper.getClass(); c != null && c != MapGenVillage.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object hook = field.get(wrapper);
                        if (hook == null) {
                            continue;
                        }
                        Field base = hook.getClass().getField("base");
                        if (net.minecraft.world.gen.structure.MapGenStructure.class.isAssignableFrom(base.getType())
                                && base.get(hook) != null) {
                            return new Wrapped(hook, base);
                        }
                    } catch (NoSuchFieldException | IllegalAccessException | RuntimeException ignored) {
                        // Not a hook.
                    }
                }
            }
            return null;
        }

        Object generator() {
            try {
                return this.base.get(this.hook);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        void replace(MapGenVillage generator) {
            try {
                this.base.set(this.hook, generator);
            } catch (IllegalAccessException e) {
                VillagerBackport.LOGGER.warn("Could not put 1.14 villages inside a wrapped village generator.", e);
            }
        }
    }
}
