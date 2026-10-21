package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Other mods' village buildings, in 1.14 villages.
 *
 * <h2>Why they went missing</h2>
 * Mods add buildings to 1.12.2 villages through Forge's village hook: each registers a creation
 * handler, and the village layout draws from their pieces alongside vanilla's houses. RLCraft's own
 * village houses arrive that way (Recurrent Complex structures marked "vanilla"), and so does
 * Waystones' village waystone. 1.14 villages are assembled from 1.14's building pools instead, which
 * never ask those handlers anything - so without this, a 1.14 village has none of them.
 *
 * <h2>What this does</h2>
 * Once the 1.14 village is laid out, it finds plots beside its streets and asks the handlers for
 * buildings to put there, exactly as a 1.12.2 road would: the handler builds its piece from a
 * position and facing at the road's edge, refuses if it would overlap anything already in the
 * village, and the piece levels itself onto the ground when it is placed. Each piece is the mod's
 * own, so it builds, fills its chests and spawns its villagers as it would anywhere.
 *
 * <ul>
 * <li><b>How many:</b> as many as the mods' pieces would make up in a 1.12.2 village of the same
 * number of houses - their share of the total piece weight, vanilla's houses being the rest - and
 * never more than {@code villages.maxModPieces}. Each piece's own limit still applies, and a piece
 * that cannot be placed does not count.</li>
 * <li><b>The waystone:</b> Waystones' piece, if Waystones rolls one for the village (its own
 * "Generate in Villages" chance), is placed first and on the plot nearest the town centre, where the
 * bell is.</li>
 * <li><b>The stand-in village:</b> handlers expect a 1.12.2 village start, so they get one, built for
 * the village's position. It only answers their questions - the biome, the village type, whether it
 * is infested - and is never placed.</li>
 * </ul>
 *
 * <p>Draws from its own random, seeded from the world and the village's chunk, so the 1.14 layout
 * is unchanged by whatever the handlers do and a seed gives the same village every time.
 */
final class ModVillagePieces {

    /** The total weight of vanilla's nine house pieces, which mod pieces compete with in 1.12.2. */
    private static final int VANILLA_WEIGHT = 4 + 20 + 20 + 3 + 15 + 3 + 3 + 15 + 8;

    /** Plots tried per piece before giving up on it. */
    private static final int TRIES = 48;

    /** Spacing between candidate plots along a street's side. */
    private static final int STEP = 3;

    /** Most the ground may rise and fall across a building's footprint. */
    private static final int MAX_SLOPE = 3;

    /** Most a building's ground may sit above or below the street it stands beside. */
    private static final int MAX_STREET_GAP = 4;

    private static final Set<Class<?>> FAILED = Collections.synchronizedSet(new HashSet<Class<?>>());

    private static volatile boolean listed;

    private ModVillagePieces() {
    }

    static void add(World world, List<StructureComponent> components, final TerrainHeights heights, int chunkX, int chunkZ,
                    int size) {
        if (!ModConfig.villages.modPieces || components.isEmpty()) {
            return;
        }

        Map<Class<?>, VillagerRegistry.IVillageCreationHandler> handlers = handlers();
        if (handlers.isEmpty()) {
            return;
        }
        listOnce(handlers);

        Random random = new Random(world.getSeed() ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L) ^ 0x6D6F64L);

        StructureComponent centre = components.get(0);
        StructureBoundingBox centreBox = centre.getBoundingBox();
        int cx = (centreBox.minX + centreBox.maxX) / 2;
        int cz = (centreBox.minZ + centreBox.maxZ) / 2;

        StructureVillagePieces.Start start = new StandIn(world, random, cx, cz, size);

        List<StructureVillagePieces.PieceWeight> weights = new ArrayList<StructureVillagePieces.PieceWeight>();
        Map<StructureVillagePieces.PieceWeight, VillagerRegistry.IVillageCreationHandler> owners =
                new IdentityHashMap<StructureVillagePieces.PieceWeight, VillagerRegistry.IVillageCreationHandler>();
        StructureVillagePieces.PieceWeight waystone = null;
        int modWeight = 0;

        for (Map.Entry<Class<?>, VillagerRegistry.IVillageCreationHandler> entry : handlers.entrySet()) {
            VillagerRegistry.IVillageCreationHandler handler = entry.getValue();
            if (!allowed(handler)) {
                continue;
            }
            StructureVillagePieces.PieceWeight weight;
            try {
                weight = handler.getVillagePieceWeight(random, size);
            } catch (Exception e) {
                fail(handler, e);
                continue;
            }
            if (weight == null || weight.villagePiecesLimit <= 0 || weight.villagePieceWeight <= 0) {
                continue;
            }
            owners.put(weight, handler);
            if (isWaystone(handler)) {
                waystone = weight;
            } else {
                weights.add(weight);
                modWeight += weight.villagePieceWeight;
            }
        }

        Ground ground = new Ground(heights);
        List<Plot> plots = plots(components, heights);
        if (plots.isEmpty()) {
            return;
        }

        int placed = 0;

        // The waystone first, on the plot nearest the bell.
        if (waystone != null && ModConfig.villages.waystoneNearBell) {
            sortByDistance(plots, cx, cz);
            if (place(waystone, owners.get(waystone), start, components, plots, ground, random, false)) {
                placed++;
            }
        } else if (waystone != null) {
            weights.add(waystone);
            modWeight += waystone.villagePieceWeight;
        }

        int houses = 0;
        for (StructureComponent piece : components) {
            if (piece instanceof ModernVillagePiece && ((ModernVillagePiece) piece).poolName().contains("/houses")) {
                houses++;
            }
        }
        int target = Math.min(ModConfig.villages.maxModPieces,
                (int) Math.round(houses * (double) modWeight / VANILLA_WEIGHT));
        if (modWeight > 0 && target == 0 && random.nextInt(VANILLA_WEIGHT) < modWeight) {
            target = 1;
        }

        Collections.shuffle(plots, random);
        // Only buildings actually placed count towards the target. One that cannot go anywhere - no
        // free plot, or a biome its mod will not build it in - is dropped and another drawn, rather
        // than using up a place, which is what left villages short of their mod buildings.
        int added = 0;
        while (added < target && !weights.isEmpty()) {
            StructureVillagePieces.PieceWeight pick = pick(weights, random);
            if (pick == null) {
                break;
            }
            if (place(pick, owners.get(pick), start, components, plots, ground, random, true)) {
                added++;
                placed++;
            } else {
                // Nowhere it fits in this village; stop offering it.
                weights.remove(pick);
            }
        }

        if (placed > 0) {
            VillagerBackport.LOGGER.debug("Added {} mod village pieces to the 1.14 village at chunk {}, {}.",
                    placed, chunkX, chunkZ);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Placing
    // ---------------------------------------------------------------------------------------------

    /** A spot at a street's edge, and the way a building there faces away from the street. */
    private static final class Plot {
        final int x;
        final int y;
        final int z;
        final EnumFacing facing;

        Plot(int x, int y, int z, EnumFacing facing) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.facing = facing;
        }
    }

    /**
     * Plots along every side of every street and of the town centre, one block out from its edge,
     * facing away from it - the position and facing a 1.12.2 road hands a house beside it.
     */
    private static List<Plot> plots(List<StructureComponent> components, TerrainHeights heights) {
        List<Plot> plots = new ArrayList<Plot>();
        for (int i = 0; i < components.size(); i++) {
            StructureComponent piece = components.get(i);
            if (!(piece instanceof ModernVillagePiece)) {
                continue;
            }
            String pool = ((ModernVillagePiece) piece).poolName();
            if (i != 0 && !pool.contains("/streets")) {
                continue;
            }
            StructureBoundingBox box = piece.getBoundingBox();
            for (int x = box.minX; x <= box.maxX; x += STEP) {
                plots.add(plot(heights, x, box.minZ - 1, EnumFacing.NORTH));
                plots.add(plot(heights, x, box.maxZ + 1, EnumFacing.SOUTH));
            }
            for (int z = box.minZ; z <= box.maxZ; z += STEP) {
                plots.add(plot(heights, box.minX - 1, z, EnumFacing.WEST));
                plots.add(plot(heights, box.maxX + 1, z, EnumFacing.EAST));
            }
        }
        return plots;
    }

    private static Plot plot(TerrainHeights heights, int x, int z, EnumFacing facing) {
        return new Plot(x, heights.height(x, z), z, facing);
    }

    private static void sortByDistance(List<Plot> plots, final int cx, final int cz) {
        Collections.sort(plots, new Comparator<Plot>() {
            @Override
            public int compare(Plot a, Plot b) {
                long da = (long) (a.x - cx) * (a.x - cx) + (long) (a.z - cz) * (a.z - cz);
                long db = (long) (b.x - cx) * (b.x - cx) + (long) (b.z - cz) * (b.z - cz);
                return Long.compare(da, db);
            }
        });
    }

    /**
     * Asks the handler to build its piece at the first plot it accepts. Handlers check for overlap
     * against the list themselves, which holds the 1.14 village's own pieces too.
     */
    private static boolean place(StructureVillagePieces.PieceWeight weight, VillagerRegistry.IVillageCreationHandler handler,
                                 StructureVillagePieces.Start start, List<StructureComponent> components,
                                 List<Plot> plots, Ground ground, Random random, boolean anywhere) {
        if (handler == null) {
            return false;
        }
        // The waystone tries every plot, nearest the bell first; anything else gives up after a few.
        int limit = anywhere ? TRIES : plots.size();
        int tries = 0;
        for (int i = 0; i < plots.size() && tries < limit; i++) {
            Plot plot = plots.get(anywhere ? (i + random.nextInt(plots.size())) % plots.size() : i);
            tries++;
            StructureVillagePieces.Village piece;
            try {
                piece = handler.buildComponent(weight, start, components, random, plot.x, plot.y, plot.z,
                        plot.facing, 1);
            } catch (Exception e) {
                fail(handler, e);
                return false;
            }
            if (piece != null && StructureComponent.findIntersecting(components, piece.getBoundingBox()) == null
                    && ground.suits(piece.getBoundingBox(), plot.y)) {
                components.add(piece);
                weight.villagePiecesSpawned++;
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a building's footprint is fit to build on, judged from the predicted terrain before
     * anything is placed.
     *
     * <p>A 1.12.2 village piece levels itself to the average ground height under it when it is
     * built, and Recurrent Complex fills dirt down to the ground beneath. On ground that falls away
     * steeply - Dregora's terraced badlands and mountainsides - that average leaves one side buried
     * and the other standing on a wall of dirt. On a lake the average is the water's surface, and
     * the building floats. 1.12.2's own villages have the same weakness; they are just rarely built
     * on ground this rough. So a plot is only taken if its ground is dry, rises and falls by no more
     * than {@value #MAX_SLOPE} blocks, and sits within {@value #MAX_STREET_GAP} of the street.
     */
    private static final class Ground {
        private final TerrainHeights heights;

        Ground(TerrainHeights heights) {
            this.heights = heights;
        }

        boolean suits(StructureBoundingBox box, int streetY) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            long total = 0;
            int samples = 0;
            int stepX = Math.max(1, (box.maxX - box.minX) / 4);
            int stepZ = Math.max(1, (box.maxZ - box.minZ) / 4);
            for (int x = box.minX; x <= box.maxX; x += stepX) {
                for (int z = box.minZ; z <= box.maxZ; z += stepZ) {
                    if (this.heights.isWater(x, z)) {
                        return false;
                    }
                    int y = this.heights.height(x, z);
                    min = Math.min(min, y);
                    max = Math.max(max, y);
                    total += y;
                    samples++;
                }
            }
            if (samples == 0 || max - min > MAX_SLOPE) {
                return false;
            }
            int average = (int) (total / samples);
            return Math.abs(average - streetY) <= MAX_STREET_GAP;
        }
    }

    /** 1.12.2's weighted pick, honouring each piece's limit. */
    private static StructureVillagePieces.PieceWeight pick(List<StructureVillagePieces.PieceWeight> weights, Random random) {
        List<StructureVillagePieces.PieceWeight> open = new ArrayList<StructureVillagePieces.PieceWeight>();
        int total = 0;
        for (StructureVillagePieces.PieceWeight weight : weights) {
            if (weight.villagePiecesSpawned < weight.villagePiecesLimit) {
                open.add(weight);
                total += weight.villagePieceWeight;
            }
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (StructureVillagePieces.PieceWeight weight : open) {
            roll -= weight.villagePieceWeight;
            if (roll < 0) {
                return weight;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Handlers
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, VillagerRegistry.IVillageCreationHandler> handlers() {
        try {
            Field field = VillagerRegistry.class.getDeclaredField("villageCreationHandlers");
            field.setAccessible(true);
            Object value = field.get(VillagerRegistry.instance());
            if (value instanceof Map) {
                return (Map<Class<?>, VillagerRegistry.IVillageCreationHandler>) value;
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not read other mods' village pieces; 1.14 villages will not have them.", e);
        }
        return Collections.emptyMap();
    }

    private static boolean isWaystone(VillagerRegistry.IVillageCreationHandler handler) {
        return handler.getClass().getName().toLowerCase(Locale.ROOT).contains("waystone");
    }

    /**
     * The names a handler's pieces can be excluded by: its mod's id, the mod id with the piece's
     * class name, and for Recurrent Complex the structure's own name.
     */
    static List<String> names(VillagerRegistry.IVillageCreationHandler handler) {
        List<String> names = new ArrayList<String>();
        String mod = modIdOf(handler.getClass());
        Class<?> component;
        try {
            component = handler.getComponentClass();
        } catch (Exception e) {
            component = handler.getClass();
        }
        names.add(mod);
        names.add(mod + ":" + component.getSimpleName());
        try {
            Field structure = handler.getClass().getDeclaredField("structureID");
            structure.setAccessible(true);
            Object id = structure.get(handler);
            if (id != null) {
                names.add(mod + ":" + id);
            }
        } catch (Exception e) {
            // Not a Recurrent Complex handler.
        }
        return names;
    }

    private static boolean allowed(VillagerRegistry.IVillageCreationHandler handler) {
        if (FAILED.contains(handler.getClass())) {
            return false;
        }
        String[] excluded = ModConfig.villages.modPieceExclusions;
        if (excluded == null || excluded.length == 0) {
            return true;
        }
        for (String name : names(handler)) {
            for (String entry : excluded) {
                if (entry != null && name.equalsIgnoreCase(entry.trim())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String modIdOf(Class<?> type) {
        try {
            File source = jarOf(type);
            for (ModContainer mod : Loader.instance().getModList()) {
                if (mod.getSource() != null && source.equals(mod.getSource().getCanonicalFile())) {
                    return mod.getModId();
                }
            }
        } catch (Exception | LinkageError e) {
            // Fall through.
        }
        return type.getName();
    }

    /** @return the jar or folder a class was loaded from, as a canonical path */
    static File jarOf(Class<?> type) throws Exception {
        String path = type.getName().replace('.', '/') + ".class";
        java.net.URL url = type.getClassLoader().getResource(path);
        if (url != null && "jar".equals(url.getProtocol())) {
            String file = url.getPath();
            file = file.substring(0, file.indexOf("!/"));
            return new File(new java.net.URI(file)).getCanonicalFile();
        }
        return new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile();
    }

    private static void fail(VillagerRegistry.IVillageCreationHandler handler, Exception e) {
        if (FAILED.add(handler.getClass())) {
            VillagerBackport.LOGGER.warn("Village piece from {} failed in a 1.14 village and will not be offered again "
                    + "this session.", handler.getClass().getName(), e);
        }
    }

    /** Logs the pieces on offer once, with the names the exclusion list accepts. */
    private static void listOnce(Map<Class<?>, VillagerRegistry.IVillageCreationHandler> handlers) {
        if (listed) {
            return;
        }
        listed = true;
        List<String> lines = new ArrayList<String>();
        for (VillagerRegistry.IVillageCreationHandler handler : handlers.values()) {
            List<String> names = names(handler);
            lines.add(names.get(names.size() - 1));
        }
        VillagerBackport.LOGGER.info("Other mods' village pieces joining 1.14 villages ({}): {}", lines.size(), lines);
    }

    /** The 1.12.2 village start the handlers are given, standing for the 1.14 village. */
    private static final class StandIn extends StructureVillagePieces.Start {

        StandIn(World world, Random random, int x, int z, int size) {
            super(world.getBiomeProvider(), 0, random, x, z,
                    new ArrayList<StructureVillagePieces.PieceWeight>(), size);
            // 1.12.2 makes one village in fifty a zombie village; a 1.14 village decides that itself.
            this.isZombieInfested = false;
        }
    }
}
