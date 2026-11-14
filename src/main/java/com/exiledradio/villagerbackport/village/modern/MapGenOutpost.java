package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.StartRule;
import com.exiledradio.backportcore.structure.StartRules;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.terrain.TerrainHeights;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.raid.Raids;

import com.exiledradio.backportcore.worldgen.StructurePlacements;

import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
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
     * Never in superflat: no 1.12.2 preset can list them, and today's default preset has villages and no outposts
     * (Backport Core's {@code FlatWorlds}).
     */
    private static boolean allowedIn(World world) {
        if (world.isRemote || !ModConfig.raids.outposts || !Raids.usingOwnRaids()
                || world.provider.getDimension() != 0 || !world.getWorldInfo().isMapFeaturesEnabled()) {
            return false;
        }
        if (!com.exiledradio.backportcore.worldgen.FlatWorlds.structure(world, Structures.PILLAGER_OUTPOST)) {
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
        StartRule rule = rule();
        if (rule != null) {
            net.minecraft.util.math.ChunkPos c = rule.grid().nearest(world.getSeed(), pos, rule.grid().spacing() * 100,
                    p -> startsAt(world, p.x, p.z) && !(findUnexplored && world.isChunkGeneratedAt(p.x, p.z)));
            return c == null ? null : new BlockPos((c.x << 4) + 8, 64, (c.z << 4) + 8);
        }
        return findNearestStructurePosBySpacing(world, this, pos, this.spacing, SEPARATION, SEED, false, 100, findUnexplored);
    }

    /**
     * The world generator's rule for where outposts start, where it has one (Caves &amp; Cliffs Backport's 1.18
     * terrain, through Backport Core's {@code StartRules}: 1.18's outposts), or null for 1.14's.
     */
    @Nullable
    private StartRule rule() {
        return this.world == null ? null : StartRules.of(this.world, Structures.PILLAGER_OUTPOST);
    }

    /** @return the outposts' grid spacing in chunks (the village grid's, or the world generator's) */
    public int spacing() {
        StartRule rule = rule();
        return rule != null ? rule.grid().spacing() : this.spacing;
    }

    /** @return the grid outposts start on in this world: 1.14's, or the world generator's ({@link #rule}) */
    public ScatteredPlacement placement() {
        StartRule rule = rule();
        return rule != null ? rule.grid() : new ScatteredPlacement(this.spacing, SEPARATION, SEED);
    }

    /**
     * Every outpost whose box (pillagers' margin included: 12 blocks, 1.18's adapt_noise box) reaches a chunk, laid
     * out first if it was not, for Backport Core's {@code TerrainAdaptation}: a 1.18 generator shapes its terrain
     * around them before the chunk exists. Loads and generates nothing.
     */
    void startsNear(World world, int chunkX, int chunkZ, java.util.function.Consumer<StructureStart> out) {
        this.world = world;
        ScatteredPlacement grid = placement();
        int s = grid.spacing();
        int x0 = chunkX << 4;
        int z0 = chunkZ << 4;
        for (int gx = Math.floorDiv(chunkX - 8, s); gx <= Math.floorDiv(chunkX + 8, s); gx++) {
            for (int gz = Math.floorDiv(chunkZ - 8, s); gz <= Math.floorDiv(chunkZ + 8, s); gz++) {
                net.minecraft.util.math.ChunkPos c = grid.candidate(world.getSeed(), gx * s, gz * s);
                if (Math.abs(c.x - chunkX) > 8 || Math.abs(c.z - chunkZ) > 8 || !canSpawnStructureAtCoords(c.x, c.z)) {
                    continue;
                }
                long k = net.minecraft.util.math.ChunkPos.asLong(c.x, c.z);
                StructureStart start = this.structureMap.get(k);
                if (start == null) {
                    generate(world, c.x, c.z, null);
                    start = this.structureMap.get(k);
                }
                StructureBoundingBox b = start == null ? null : start.getBoundingBox();
                if (b != null && start.isSizeableStructure() && b.maxX >= x0 && b.minX <= x0 + 15 && b.maxZ >= z0
                        && b.minZ <= z0 + 15) {
                    out.accept(start);
                }
            }
        }
    }

    /** {@code PillagerOutpostStructure}'s seed modifier: the outposts' grid salt. */
    public static int salt() {
        return SEED;
    }

    /**
     * @return true if an outpost starts in this chunk (by the seed, laid out or not), skipping a start laid out with
     *         nothing in it; for Backport Core's {@code /locate}. Loads and generates nothing.
     */
    public boolean startsAt(World world, int chunkX, int chunkZ) {
        if (this.world == null) {
            this.world = world;
        }
        if (!canSpawnStructureAtCoords(chunkX, chunkZ)) {
            return false;
        }
        StructureStart existing = this.structureMap.get(net.minecraft.util.math.ChunkPos.asLong(chunkX, chunkZ));
        return existing == null || existing.isSizeableStructure();
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        StartRule rule = rule();
        if (rule != null) {
            return rule.startAt(chunkX, chunkZ) != null;
        }
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

        // The newest installed version's biome point (Backport Core's table): block (9, 9) in 1.14 and 1.15,
        // the 1:4 cell at (4x + 2, 4z + 2) from 1.16.
        Biome biome = StructurePlacements.biomeAt(this.world.getBiomeProvider(), chunkX, chunkZ);
        if (biome == null || !isOutpostBiome(biome)) {
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
        // 1.16's PillagerOutpostStructure.isNearVillage asks only the village grid (StructureFeature.VILLAGE's
        // getPotentialFeatureChunk), not the village's biome; 1.14 and 1.15 ask the whole village rule.
        if (StructurePlacements.isAtLeast("1.16")) {
            return MapGenModernVillage.isGridCandidate(this.world, this.villages != null ? this.villages.spacing() : 32, chunkX, chunkZ);
        }
        if (this.villages != null) {
            return this.villages.startsAt(this.world, chunkX, chunkZ);
        }
        // Worlds whose villages this mod does not generate: the village rule of 1.14 and later on 1.12.2's grid.
        return MapGenModernVillage.startsAtModern(this.world, 32, chunkX, chunkZ);
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        // Where the world generator rules starts, its version's layout random: see MapGenModernVillage.getStructureStart.
        boolean ruled = rule() != null;
        Random random = ruled ? ModernVillageStart.largeFeatureRandom(this.world.getSeed(), chunkX, chunkZ) : this.rand;
        return new OutpostStart(random, chunkX, chunkZ, TerrainHeights.forWorld(this.world), ruled);
    }
}
