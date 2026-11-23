package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.structure.CutTrees;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.village.modern.MapGenModernVillage;
import com.exiledradio.villagerbackport.village.modern.MapGenOutpost;
import com.exiledradio.villagerbackport.village.modern.ModernVillageStart;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Leaves left in the air by trees a structure cut (Backport Core's {@code CutTrees}). Villages in savanna and taiga (built
 * in the populate step, before the chunk's own trees, cutting its neighbors' trees) and outposts (built from a world
 * generator, after every tree): one of each built without the pass, one with it. With it, around each structure every
 * decayable leaf must have a log within 6 steps through leaves (today's {@code LeavesBlock} rule); without it some must
 * not. The same count away from the structures is logged as 1.12's own baseline.
 */
final class CutTreesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "cuttrees";
    }

    @Override
    public void run(Check check) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        BlockPos spawn = world.getSpawnPoint();
        int cut = 0;

        MapGenModernVillage villages = MapGenModernVillage.of(world);
        if (check.ok(villages != null, "the overworld builds 1.14 villages")) {
            for (ModernVillageStart.Type type : new ModernVillageStart.Type[] {ModernVillageStart.Type.SAVANNA,
                    ModernVillageStart.Type.TAIGA}) {
                List<ChunkPos> found = find(world, villages.placement(world), spawn, 2, 7,
                        c -> villages.startTypeAt(world, c.x, c.z) == type);
                if (!check.eq(2, found.size(), "two " + type + " villages, not yet generated (" + found + ")")) {
                    continue;
                }
                cut += compare(check, world, villages, found, 7, type + " village");
            }
        }

        MapGenOutpost outposts = MapGenOutpost.of(world);
        if (check.ok(outposts != null, "the overworld builds outposts")) {
            List<ChunkPos> found = find(world, outposts.placement(), spawn, 2, 4, c -> outposts.startsAt(world, c.x, c.z)
                    && trees(world, c));
            if (check.eq(2, found.size(), "two outposts among trees, not yet generated (" + found + ")")) {
                cut += compare(check, world, outposts, found, 4, "outpost");
            }
        }
        check.ok(cut > 0, "without the pass the structures leave cut-off leaves (" + cut + ")");
    }

    private static int compare(Check check, WorldServer world, MapGenStructure structure, List<ChunkPos> found, int radius,
                                String name) {
        build(world, found.get(0), radius, false);
        build(world, found.get(1), radius, true);
        int[] without = survey(world, structure, found.get(0));
        int[] with = survey(world, structure, found.get(1));
        int[] control = control(world, found.get(1), radius);
        VillagerBackport.LOGGER.info("CUTTREES {}: without the pass {} of {} leaves cut off; with it {} of {}; "
                + "away from it {} of {}", name, without[0], without[1], with[0], with[1], control[0], control[1]);
        check.ok(with[1] > 0 && with[0] == 0, name + " with the pass leaves none (" + with[0] + " of " + with[1] + ")");
        // Away from any structure the count is 1.12's own trees: a big oak's low leaf clusters get no branch, and they
        // decay in 1.12 too once something sets them checking. A baseline, not a failure.
        return without[0];
    }

    /** Candidates on the grid, nearest the spawn first, whose surroundings were never generated. */
    private static List<ChunkPos> find(WorldServer world, ScatteredPlacement grid, BlockPos spawn, int wanted, int radius,
                                       Predicate<ChunkPos> accept) {
        List<ChunkPos> found = new ArrayList<ChunkPos>();
        int s = grid.spacing();
        int gx0 = (spawn.getX() >> 4) / s;
        int gz0 = (spawn.getZ() >> 4) / s;
        for (int r = 1; r < 80 && found.size() < wanted; r++) {
            for (int gx = gx0 - r; gx <= gx0 + r && found.size() < wanted; gx++) {
                for (int gz = gz0 - r; gz <= gz0 + r && found.size() < wanted; gz++) {
                    if (Math.abs(gx - gx0) != r && Math.abs(gz - gz0) != r) {
                        continue;
                    }
                    ChunkPos c = grid.candidate(world.getSeed(), gx * s, gz * s);
                    if (untouched(world, c, radius + 2) && accept.test(c)) {
                        found.add(c);
                    }
                }
            }
        }
        return found;
    }

    private static boolean untouched(WorldServer world, ChunkPos c, int radius) {
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                if (world.getChunkProvider().isChunkGeneratedAt(c.x + dx, c.z + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A biome with trees at the start: its decorator plants some. */
    private static boolean trees(WorldServer world, ChunkPos c) {
        return world.getBiomeProvider().getBiome(new BlockPos((c.x << 4) + 9, 0, (c.z << 4) + 9)).decorator.treesPerChunk > 0;
    }

    private static void build(WorldServer world, ChunkPos c, int radius, boolean pass) {
        CutTrees.setEnabled(pass);
        try {
            for (int dx = -radius; dx <= radius + 1; ++dx) {
                for (int dz = -radius; dz <= radius + 1; ++dz) {
                    world.getChunk(c.x + dx, c.z + dz);
                }
            }
        } finally {
            CutTrees.setEnabled(true);
        }
    }

    private static int[] survey(WorldServer world, MapGenStructure structure, ChunkPos c) {
        StructureStart start = starts(structure).get(ChunkPos.asLong(c.x, c.z));
        if (start == null || !start.isSizeableStructure()) {
            return new int[] {0, 0};
        }
        StructureBoundingBox b = start.getBoundingBox();
        return count(world, new StructureBoundingBox(b.minX - 6, Math.max(0, b.minY - 6), b.minZ - 6, b.maxX + 6,
                Math.min(255, b.maxY + 12), b.maxZ + 6), java.util.Collections.<StructureBoundingBox>emptyList(),
                "at the structure " + b);
    }

    /** The same count over the rest of what the build generated, away from every village and outpost. */
    private static int[] control(WorldServer world, ChunkPos c, int radius) {
        StructureBoundingBox all = new StructureBoundingBox((c.x - radius) << 4, 30, (c.z - radius) << 4,
                ((c.x + radius) << 4) + 15, 160, ((c.z + radius) << 4) + 15);
        List<StructureBoundingBox> near = new ArrayList<StructureBoundingBox>();
        for (MapGenStructure structure : new MapGenStructure[] {MapGenModernVillage.of(world), MapGenOutpost.of(world)}) {
            if (structure != null) {
                for (StructureStart start : starts(structure).values()) {
                    if (start.isSizeableStructure()) {
                        near.add(start.getBoundingBox());
                    }
                }
            }
        }
        return count(world, all, near, "away from the structures");
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, StructureStart> starts(MapGenStructure structure) {
        return (Map<Long, StructureStart>) ObfuscationReflectionHelper.getPrivateValue(MapGenStructure.class, structure,
                "structureMap", "field_75053_d");
    }

    /** @return {decayable leaves with no log within 6 steps through leaves, decayable leaves looked at} */
    private static int[] count(WorldServer world, StructureBoundingBox box, List<StructureBoundingBox> skip, String where) {
        int cut = 0;
        int all = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = box.minX; x <= box.maxX; ++x) {
            for (int z = box.minZ; z <= box.maxZ; ++z) {
                if (!settled(world, x, z) || near(skip, x, z)) {
                    continue;
                }
                for (int y = box.minY; y <= box.maxY; ++y) {
                    at.setPos(x, y, z);
                    IBlockState state = world.getBlockState(at);
                    if (!decayableLeaf(world, at, state)) {
                        continue;
                    }
                    all++;
                    if (!reachesLog(world, at.toImmutable())) {
                        cut++;
                        if (cut <= 5) {
                            java.util.TreeMap<String, Integer> around = new java.util.TreeMap<String, Integer>();
                            for (BlockPos q : BlockPos.getAllInBox(at.add(-6, -6, -6), at.add(6, 6, 6))) {
                                IBlockState qs = world.getBlockState(q);
                                if (qs.getMaterial() != net.minecraft.block.material.Material.AIR) {
                                    around.merge(String.valueOf(qs), 1, Integer::sum);
                                }
                            }
                            VillagerBackport.LOGGER.info("CUTTREES cut-off leaf {} at {} ({}, biome {}); within 6: {}", where, at, state,
                                    world.getBiome(at).getRegistryName(), around);
                        }
                    }
                }
            }
        }
        return new int[] {cut, all};
    }

    private static boolean near(List<StructureBoundingBox> boxes, int x, int z) {
        for (StructureBoundingBox b : boxes) {
            if (x >= b.minX - 16 && x <= b.maxX + 16 && z >= b.minZ - 16 && z <= b.maxZ + 16) {
                return true;
            }
        }
        return false;
    }

    /** Every chunk within 7 blocks is loaded and populated: nothing more will be written there. */
    private static boolean settled(WorldServer world, int x, int z) {
        for (int cx = (x - 7) >> 4; cx <= (x + 7) >> 4; ++cx) {
            for (int cz = (z - 7) >> 4; cz <= (z + 7) >> 4; ++cz) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
                Chunk next = world.getChunkProvider().getLoadedChunk(cx - 1, cz - 1);
                if (chunk == null || !chunk.isTerrainPopulated() || next == null || !next.isTerrainPopulated()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean reachesLog(WorldServer world, BlockPos from) {
        Set<BlockPos> seen = new HashSet<BlockPos>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        ArrayDeque<Integer> steps = new ArrayDeque<Integer>();
        queue.add(from);
        steps.add(0);
        seen.add(from);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = steps.poll();
            if (d >= 6) {
                continue;
            }
            for (EnumFacing f : EnumFacing.values()) {
                BlockPos n = p.offset(f);
                if (!seen.add(n) || n.getY() < 0 || n.getY() > 255) {
                    continue;
                }
                IBlockState s = world.getBlockState(n);
                Block b = s.getBlock();
                if (b instanceof BlockLog || b.canSustainLeaves(s, world, n) || b.isWood(world, n)) {
                    return true;
                }
                if (b.isLeaves(s, world, n)) {
                    queue.add(n);
                    steps.add(d + 1);
                }
            }
        }
        return false;
    }

    private static boolean decayableLeaf(WorldServer world, BlockPos pos, IBlockState state) {
        if (!state.getBlock().isLeaves(state, world, pos)) {
            return false;
        }
        for (IProperty<?> p : state.getPropertyKeys()) {
            if ("decayable".equals(p.getName())) {
                return Boolean.TRUE.equals(state.getValue(p));
            }
        }
        return false;
    }
}
