package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.structure.FreezeTopLayer;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.village.modern.MapGenModernVillage;
import com.exiledradio.villagerbackport.village.modern.MapGenOutpost;
import com.exiledradio.villagerbackport.village.modern.ModernVillageStart;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Snow over outposts and villages in a cold biome (1.14's freeze_top_layer runs after the structures): one outpost built
 * without Backport Core's pass (1.12's look: bare pads and tops), one with it, and a snowy village. Every column a
 * piece covers whose top could take a snow layer must have one.
 */
final class SnowSelfTest implements SelfTest {

    @Override
    public String name() {
        return "snow";
    }

    @Override
    public void run(Check check) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        MapGenOutpost outposts = MapGenOutpost.of(world);
        if (!check.ok(outposts != null, "the overworld builds outposts")) {
            return;
        }
        List<ChunkPos> found = new ArrayList<ChunkPos>();
        ScatteredPlacement grid = outposts.placement();
        int s = grid.spacing();
        BlockPos spawn = world.getSpawnPoint();
        int gx0 = (spawn.getX() >> 4) / s;
        int gz0 = (spawn.getZ() >> 4) / s;
        for (int r = 1; r < 60 && found.size() < 2; r++) {
            for (int gx = gx0 - r; gx <= gx0 + r && found.size() < 2; gx++) {
                for (int gz = gz0 - r; gz <= gz0 + r && found.size() < 2; gz++) {
                    if (Math.abs(gx - gx0) != r && Math.abs(gz - gz0) != r) {
                        continue;
                    }
                    ChunkPos c = grid.candidate(world.getSeed(), gx * s, gz * s);
                    if (outposts.startsAt(world, c.x, c.z) && cold(world, c, 4)) {
                        found.add(c);
                    }
                }
            }
        }
        if (!check.eq(2, found.size(), "two outposts in cold biomes, not yet generated (" + found + ")")) {
            return;
        }
        build(world, found.get(0), 4, false);
        build(world, found.get(1), 4, true);
        int[] without = survey(world, outposts, found.get(0), "outpost without the pass");
        int[] with = survey(world, outposts, found.get(1), "outpost with the pass");
        VillagerBackport.LOGGER.info("SNOW before/after: outpost without the pass {} of {} piece columns snowed ({} could be), "
                + "with it {} of {} ({} could be)", without[0], without[2], without[1], with[0], with[2], with[1]);
        check.ok(without[1] > 0, "without the pass the outpost has bare tops a snow layer could take (" + without[1] + ")");
        check.ok(with[2] > 0 && with[1] == 0, "with the pass every outpost column that can snow did (" + with[0] + " snowed, "
                + with[1] + " open of " + with[2] + ")");

        // Villages are built in 1.12's populate, before its snow pass: snowed already.
        MapGenModernVillage villages = MapGenModernVillage.of(world);
        if (villages == null) {
            return;
        }
        ChunkPos village = null;
        ScatteredPlacement vgrid = villages.placement(world);
        int vs = vgrid.spacing();
        for (int r = 1; r < 60 && village == null; r++) {
            for (int gx = gx0 - r; gx <= gx0 + r && village == null; gx++) {
                for (int gz = gz0 - r; gz <= gz0 + r && village == null; gz++) {
                    if (Math.abs(gx - gx0) != r && Math.abs(gz - gz0) != r) {
                        continue;
                    }
                    ChunkPos c = vgrid.candidate(world.getSeed(), gx * vs, gz * vs);
                    if (villages.startTypeAt(world, c.x, c.z) == ModernVillageStart.Type.SNOWY && cold(world, c, 7)) {
                        village = c;
                    }
                }
            }
        }
        if (!check.ok(village != null, "a snowy village not yet generated")) {
            return;
        }
        build(world, village, 7, true);
        int[] v = survey(world, villages, village, "snowy village");
        VillagerBackport.LOGGER.info("SNOW snowy village: {} of {} piece columns snowed ({} could be)", v[0], v[2], v[1]);
        check.ok(v[2] > 0 && v[1] == 0, "every snowy village column that can snow did (" + v[0] + " snowed, " + v[1]
                + " open of " + v[2] + ")");
    }

    private static boolean cold(WorldServer world, ChunkPos c, int radius) {
        for (int dx = -radius - 1; dx <= radius + 1; ++dx) {
            for (int dz = -radius - 1; dz <= radius + 1; ++dz) {
                if (world.getChunkProvider().isChunkGeneratedAt(c.x + dx, c.z + dz)) {
                    return false;
                }
            }
        }
        Biome biome = world.getBiomeProvider().getBiome(new BlockPos((c.x << 4) + 9, 0, (c.z << 4) + 9));
        return biome.getDefaultTemperature() < 0.15F;
    }

    private static void build(WorldServer world, ChunkPos c, int radius, boolean pass) {
        FreezeTopLayer.setEnabled(pass);
        try {
            for (int dx = -radius; dx <= radius + 1; ++dx) {
                for (int dz = -radius; dz <= radius + 1; ++dz) {
                    world.getChunk(c.x + dx, c.z + dz);
                }
            }
        } finally {
            FreezeTopLayer.setEnabled(true);
        }
    }

    private static boolean carvedLater(WorldServer world, BlockPos top) {
        for (int y = 1; y <= 8; ++y) {
            if (world.getBlockState(top.up(y)).getBlock() == Blocks.SNOW_LAYER) {
                return true;
            }
        }
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                if (world.getBlockState(top.add(dx, -1, dz)).getMaterial() == net.minecraft.block.material.Material.WATER) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static StructureStart start(MapGenStructure structure, ChunkPos c) {
        Map<Long, StructureStart> starts = (Map<Long, StructureStart>) ObfuscationReflectionHelper.getPrivateValue(
                MapGenStructure.class, structure, "structureMap", "field_75053_d");
        return starts.get(ChunkPos.asLong(c.x, c.z));
    }

    /**
     * @return {snowed columns, columns where a snow layer could still go, columns surveyed}: the columns the start's
     * pieces cover, where the chunk that decorates them is populated and the biome there is cold
     */
    private static int[] survey(WorldServer world, MapGenStructure structure, ChunkPos c, String name) {
        StructureStart start = start(structure, c);
        if (start == null || !start.isSizeableStructure()) {
            return new int[] {0, 0, 0};
        }
        StructureBoundingBox all = start.getBoundingBox();
        int w = all.maxX - all.minX + 1;
        int d = all.maxZ - all.minZ + 1;
        boolean[] covered = new boolean[w * d];
        for (StructureComponent piece : start.getComponents()) {
            StructureBoundingBox b = piece.getBoundingBox();
            for (int x = b.minX; x <= b.maxX; ++x) {
                for (int z = b.minZ; z <= b.maxZ; ++z) {
                    covered[(x - all.minX) + (z - all.minZ) * w] = true;
                }
            }
        }
        int snowed = 0;
        int open = 0;
        int columns = 0;
        StringBuilder map = new StringBuilder();
        for (int z = all.minZ; z <= all.maxZ; ++z) {
            map.append('\n');
            for (int x = all.minX; x <= all.maxX; ++x) {
                if (!covered[(x - all.minX) + (z - all.minZ) * w]
                        || !world.getChunk((x - 8) >> 4, (z - 8) >> 4).isTerrainPopulated()) {
                    map.append(' ');
                    continue;
                }
                BlockPos top = world.getPrecipitationHeight(new BlockPos(x, 0, z));
                if (world.getBiome(top).getTemperature(top) >= 0.15F) {
                    map.append('~');
                    continue;
                }
                columns++;
                Block under = world.getBlockState(top.down()).getBlock();
                if (world.getBlockState(top).getBlock() == Blocks.SNOW_LAYER) {
                    snowed++;
                    map.append('*');
                } else if (world.isAirBlock(top) && Blocks.SNOW_LAYER.canPlaceBlockAt(world, top) && carvedLater(world, top)) {
                    // A 1.12 lake from a later chunk's populate reaches past its own area and cuts the ground after the
                    // snow (it does the same to the terrain): snow left floating above, or the lake's water beside it.
                    map.append('l');
                } else if (world.isAirBlock(top) && Blocks.SNOW_LAYER.canPlaceBlockAt(world, top)) {
                    // Today's rule while decorating: no light check (light comes after the features).
                    open++;
                    if (open <= 10 && !name.contains("without")) {
                        VillagerBackport.LOGGER.info("SNOW open column in the {} at {}: top {}, under {}, block light {}", name, top,
                                world.getBlockState(top), world.getBlockState(top.down()),
                                world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, top));
                    }
                    map.append(under == Blocks.GRASS ? 'g' : 'o');
                } else {
                    map.append('.');
                }
            }
        }
        VillagerBackport.LOGGER.info("SNOW top view, {} at {} (* snow, g bare grass, o other bare top, l cut by a later lake, . cannot snow: "
                + "water, ...):{}", name, all, map);
        return new int[] {snowed, open, columns};
    }
}
