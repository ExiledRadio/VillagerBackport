package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.locate.LocateChecks;
import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.FlatWorlds;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.bamboo.BambooContent;
import com.exiledradio.villagerbackport.flora.FloraContent;
import com.exiledradio.villagerbackport.fox.FoxContent;
import com.exiledradio.villagerbackport.illager.EntityPillager;
import com.exiledradio.villagerbackport.village.modern.MapGenModernVillage;
import com.exiledradio.villagerbackport.village.modern.MapGenOutpost;
import com.exiledradio.villagerbackport.village.modern.ModernVillageStart;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.List;

/**
 * Superflat worlds generate only what their preset lists, as 1.12.2's flat generator: this mod's flowers, berry
 * bushes and bamboo only with {@code decoration}, 1.14 villages only with {@code village}, and never a pillager
 * outpost. Run in a superflat world by {@code tools/flat/selftest.sh <preset>}; in the suite's own world it only
 * checks that nothing is held back there.
 */
final class FlatSelfTest implements SelfTest {

    /** Chunks scanned round spawn; one more ring is generated, so every scanned chunk is populated. */
    private static final int RADIUS = 6;

    @Override
    public String name() {
        return "flat";
    }

    @Override
    public void run(Check check) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        if (!FlatWorlds.isFlat(world)) {
            check.ok(FlatWorlds.decoration(world) && FlatWorlds.structure(world, Structures.PILLAGER_OUTPOST),
                    "a world that is not superflat holds nothing back");
            check.ok(!ModConfig.raids.outposts || MapGenOutpost.of(world) != null, "the suite's world has outposts");
            return;
        }

        String options = world.getWorldInfo().getGeneratorOptions();
        boolean decoration = options.contains("decoration");
        boolean villages = options.contains("village") && world.getWorldInfo().isMapFeaturesEnabled();
        com.exiledradio.villagerbackport.VillagerBackport.LOGGER.info("SELFTEST flat preset {}: decoration {}, villages {}",
                options, decoration, villages);
        check.eq(decoration, FlatWorlds.decoration(world), "decoration as the preset says");

        java.util.List<Block> layers = new java.util.ArrayList<Block>();
        layers.add(net.minecraft.init.Blocks.AIR);
        for (net.minecraft.world.gen.FlatLayerInfo layer : FlatWorlds.info(world).getFlatLayers()) {
            layers.add(layer.getLayerMaterial().getBlock());
        }
        int biome = FlatWorlds.info(world).getBiome();

        BlockPos spawn = world.getSpawnPoint();
        int cx = spawn.getX() >> 4;
        int cz = spawn.getZ() >> 4;
        LocateChecks.generateAround(world, cx, cz, RADIUS + 1);

        // Villages: as the preset's "village".
        MapGenModernVillage modern = MapGenModernVillage.of(world);
        check.eq(villages, modern != null, "1.14 villages are built (preset " + options + ")");
        StructureLocate.Found village = StructureLocate.nearest(world, spawn, "#minecraft:village");
        check.eq(villages, village != null, "locate finds a village: " + village);
        BlockPos vanilla = world.findNearestStructure("Village", spawn, false);
        check.eq(villages, vanilla != null, "/locate Village finds one: " + vanilla);
        if (village != null && modern != null) {
            int vx = village.pos.getX() >> 4;
            int vz = village.pos.getZ() >> 4;
            LocateChecks.generateAround(world, vx, vz, 4);
            StructureStart start = LocateChecks.savedStart(modern, vx, vz);
            check.ok(start instanceof ModernVillageStart && start.isSizeableStructure() && !start.getComponents().isEmpty(),
                    "a 1.14 village was laid out where locate said: " + village + ", " + start);
            boolean built = false;
            for (int y = 1; y < 40 && !built; y++) {
                for (int dx = 0; dx < 16 && !built; dx++) {
                    for (int dz = 0; dz < 16 && !built; dz++) {
                        Block block = world.getBlockState(new BlockPos((vx << 4) + dx, y, (vz << 4) + dz)).getBlock();
                        built = layers.indexOf(block) < 0;
                    }
                }
            }
            check.ok(built, "the village's start chunk has blocks that are not the preset's layers");
        }

        // Outposts: never.
        check.ok(MapGenOutpost.of(world) == null, "no outpost generator in superflat");
        check.ok(StructureLocate.nearest(world, spawn, "minecraft:pillager_outpost") == null, "locate finds no outpost");
        List<String> said = LocateChecks.run(server, world, spawn, "locate structure minecraft:pillager_outpost");
        check.ok(said.size() == 1 && said.get(0).startsWith("Could not find"), "/locate says no outpost: " + said);
        int pillagers = 0;
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof EntityPillager) {
                pillagers++;
            }
        }
        check.eq(0, pillagers, "no pillagers generated");

        // Features, outside any village (a village's own flowers are the village's).
        int cornflowers = 0;
        int lilies = 0;
        int bushes = 0;
        int bamboo = 0;
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                Chunk chunk = world.getChunk(x, z);
                check.ok(chunk.isTerrainPopulated(), "chunk " + x + ", " + z + " is populated");
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        int top = chunk.getHeightValue(dx, dz) + 2;
                        for (int y = 1; y < top && y < 256; y++) {
                            BlockPos pos = new BlockPos((x << 4) + dx, y, (z << 4) + dz);
                            Block block = chunk.getBlockState(pos).getBlock();
                            if (block != FloraContent.cornflower && block != FloraContent.lilyOfTheValley
                                    && block != FoxContent.bush && block != BambooContent.bamboo
                                    && block != BambooContent.bambooSapling) {
                                continue;
                            }
                            if (modern != null && modern.isInsideStructure(pos)) {
                                continue;
                            }
                            if (block == FloraContent.cornflower) {
                                cornflowers++;
                            } else if (block == FloraContent.lilyOfTheValley) {
                                lilies++;
                            } else if (block == FoxContent.bush) {
                                bushes++;
                            } else {
                                bamboo++;
                            }
                        }
                    }
                }
            }
        }
        com.exiledradio.villagerbackport.VillagerBackport.LOGGER.info(
                "SELFTEST flat: {} cornflowers, {} lilies, {} berry bushes, {} bamboo round spawn", cornflowers, lilies, bushes, bamboo);
        // Each feature where vanilla's biome would have it, and only with decoration (preset biome ids: 1 plains,
        // 4 forest, 5 taiga, 21 jungle).
        expect(check, decoration && biome == 1 && FloraContent.cornflower != null && ModConfig.flora.generateFlowers,
                cornflowers, "cornflowers", biome, options);
        expect(check, decoration && biome == 4 && FloraContent.lilyOfTheValley != null && ModConfig.flora.generateFlowers,
                lilies, "lilies of the valley", biome, options);
        expect(check, decoration && biome == 5 && FoxContent.bush != null && ModConfig.foxes.generateBushes,
                bushes, "berry bushes", biome, options);
        expect(check, decoration && biome == 21 && BambooContent.bamboo != null && ModConfig.bamboo.jungleBamboo,
                bamboo, "bamboo", biome, options);
    }

    private static void expect(Check check, boolean present, int count, String what, int biome, String options) {
        if (present) {
            check.ok(count > 0, what + " grow in biome " + biome + " with decoration (" + count + ")");
        } else {
            check.eq(0, count, "no " + what + " (biome " + biome + ", " + options + ")");
        }
    }
}
