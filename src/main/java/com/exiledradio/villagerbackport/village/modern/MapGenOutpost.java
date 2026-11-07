package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.backportcore.terrain.TerrainHeights;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.raid.Raids;

import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Where 1.14's pillager outposts go: {@code PillagerOutpostStructure}.
 *
 * <h2>1.14's rules</h2>
 * The village grid - one candidate per 32 chunks, 8 apart - with the outposts' own seed, then a
 * one-in-five roll, then the biome (plains, desert, savanna, taiga, snowy tundra), then no village
 * starting within ten chunks. Saved under 1.14's name, {@code Pillager_Outpost}.
 *
 * <h2>Not attached to the chunk generator</h2>
 * 1.12.2 gives a mod no slot in the overworld generator for a new structure, and replacing the
 * generator would fight every terrain mod. So {@link OutpostGenerator} drives this from the populate
 * step: each chunk populated first plans every outpost within eight chunks, then places its own part
 * of them. A chunk can only be populated once its neighbours exist, and an outpost reaches at most
 * five chunks from its start, so every outpost is planned before any chunk it touches is built.
 */
public class MapGenOutpost extends MapGenStructure {

    public static final String NAME = "Pillager_Outpost";

    /** 1.14's {@code PillagerOutpostStructure.getSeedModifier}. */
    private static final int SEED = 165745296;

    /** 1.14's village separation, which outposts share. */
    private static final int SEPARATION = 8;

    /** How far round an outpost 1.14 looks for a village start, in chunks. */
    private static final int VILLAGE_CLEARANCE = 10;

    private static final Set<Biome> VANILLA_BIOMES = new HashSet<Biome>(Arrays.asList(
            Biomes.PLAINS, Biomes.DESERT, Biomes.SAVANNA, Biomes.TAIGA, Biomes.ICE_PLAINS));

    private static final Map<World, MapGenOutpost> BY_WORLD = new WeakHashMap<World, MapGenOutpost>();
    private static final Map<World, Boolean> NONE = new WeakHashMap<World, Boolean>();

    private int spacing = 32;

    @Nullable
    private MapGenModernVillage villages;

    /** @return this world's outposts, or null where there are none */
    @Nullable
    public static synchronized MapGenOutpost of(World world) {
        // Asked for every monster spawn attempt, so a world without outposts is remembered as such.
        if (NONE.containsKey(world)) {
            return null;
        }
        if (!allowedIn(world)) {
            NONE.put(world, Boolean.TRUE);
            return null;
        }
        MapGenOutpost outposts = BY_WORLD.get(world);
        if (outposts == null) {
            outposts = new MapGenOutpost();
            outposts.world = world;
            outposts.villages = MapGenModernVillage.of(world);
            if (outposts.villages != null) {
                outposts.spacing = outposts.villages.spacing();
            }
            BY_WORLD.put(world, outposts);
        }
        return outposts;
    }

    /**
     * Outposts are this mod's own raid content: only with this mod's raids, only in the overworld,
     * only where structures are on, and only where the terrain can be predicted to plan on.
     * Superflat gets them only when its preset has villages.
     */
    private static boolean allowedIn(World world) {
        if (world.isRemote || !ModConfig.raids.outposts || !Raids.usingOwnRaids()
                || world.provider.getDimension() != 0 || !world.getWorldInfo().isMapFeaturesEnabled()) {
            return false;
        }
        if (world.getWorldType() == WorldType.FLAT
                && !world.getWorldInfo().getGeneratorOptions().contains("village")) {
            return false;
        }
        return TerrainHeights.forWorld(world) != null;
    }

    @Override
    public String getStructureName() {
        return NAME;
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, BlockPos pos, boolean findUnexplored) {
        this.world = world;
        return findNearestStructurePosBySpacing(world, this, pos, this.spacing, SEPARATION, SEED, false, 100, findUnexplored);
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX, this.spacing);
        int cellZ = Math.floorDiv(chunkZ, this.spacing);
        Random grid = this.world.setRandomSeed(cellX, cellZ, SEED);
        if (chunkX != cellX * this.spacing + grid.nextInt(this.spacing - SEPARATION)
                || chunkZ != cellZ * this.spacing + grid.nextInt(this.spacing - SEPARATION)) {
            return false;
        }

        // 1.14's own roll on top of the grid, seeded as it seeds it.
        int i = chunkX >> 4;
        int j = chunkZ >> 4;
        Random roll = new Random((long) (i ^ j << 4) ^ this.world.getSeed());
        roll.nextInt();
        if (roll.nextInt(5) != 0) {
            return false;
        }

        Biome biome = this.world.getBiomeProvider().getBiome(new BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9));
        if (!isOutpostBiome(biome)) {
            return false;
        }

        for (int x = chunkX - VILLAGE_CLEARANCE; x <= chunkX + VILLAGE_CLEARANCE; x++) {
            for (int z = chunkZ - VILLAGE_CLEARANCE; z <= chunkZ + VILLAGE_CLEARANCE; z++) {
                if (villageStartsAt(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 1.14's five biomes, and - with {@code raids.outpostsInModdedBiomes} - another mod's biome that
     * is like one of them: open plains, sandy desert, savanna, coniferous forest or snowy flats, but
     * not hills, mountains, water or beaches.
     */
    static boolean isOutpostBiome(Biome biome) {
        if (VANILLA_BIOMES.contains(biome)) {
            return true;
        }
        ResourceLocation name = biome.getRegistryName();
        if (!ModConfig.raids.outpostsInModdedBiomes || name == null || "minecraft".equals(name.getNamespace())) {
            return false;
        }
        for (BiomeDictionary.Type not : new BiomeDictionary.Type[]{BiomeDictionary.Type.HILLS, BiomeDictionary.Type.MOUNTAIN,
                BiomeDictionary.Type.WATER, BiomeDictionary.Type.BEACH, BiomeDictionary.Type.RIVER, BiomeDictionary.Type.OCEAN}) {
            if (BiomeDictionary.hasType(biome, not)) {
                return false;
            }
        }
        for (BiomeDictionary.Type like : new BiomeDictionary.Type[]{BiomeDictionary.Type.PLAINS, BiomeDictionary.Type.SANDY,
                BiomeDictionary.Type.SAVANNA, BiomeDictionary.Type.CONIFEROUS, BiomeDictionary.Type.SNOWY}) {
            if (BiomeDictionary.hasType(biome, like)) {
                return true;
            }
        }
        return false;
    }

    private boolean villageStartsAt(int chunkX, int chunkZ) {
        if (this.villages != null) {
            return this.villages.startsAt(this.world, chunkX, chunkZ);
        }
        // 1.12.2's own village grid, for worlds whose villages this mod does not generate.
        int cellX = Math.floorDiv(chunkX, 32);
        int cellZ = Math.floorDiv(chunkZ, 32);
        Random random = this.world.setRandomSeed(cellX, cellZ, 10387312);
        return chunkX == cellX * 32 + random.nextInt(24) && chunkZ == cellZ * 32 + random.nextInt(24)
                && this.world.getBiomeProvider().areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 0,
                MapGenVillage.VILLAGE_SPAWN_BIOMES);
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new OutpostStart(this.rand, chunkX, chunkZ, TerrainHeights.forWorld(this.world));
    }
}
