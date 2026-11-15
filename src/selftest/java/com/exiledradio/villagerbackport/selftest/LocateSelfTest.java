package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.locate.LocateChecks;
import com.exiledradio.backportcore.locate.StructureLocate;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.village.modern.MapGenModernVillage;
import com.exiledradio.villagerbackport.village.modern.MapGenOutpost;
import com.exiledradio.villagerbackport.village.modern.ModernVillageStart;
import com.exiledradio.villagerbackport.village.modern.VillageLocate;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.List;
import java.util.Locale;

/**
 * Villages and outposts in Backport Core's {@code /locate}: each type's id finds a start the generator then lays out
 * as that type, in today's search order; {@code #minecraft:village} and the hidden {@code Village} name; the 1.12.2
 * fallback with 1.14 villages off; tab completion.
 */
final class LocateSelfTest implements SelfTest {

    @Override
    public String name() {
        return "locate";
    }

    @Override
    public void run(Check check) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        BlockPos spawn = world.getSpawnPoint();
        MapGenModernVillage villages = MapGenModernVillage.of(world);
        check.ok(villages != null, "the overworld builds 1.14 villages");
        if (villages == null) {
            return;
        }

        check.eq(5, StructureLocate.members("#minecraft:village").size(), "#minecraft:village has the five types");
        check.ok(StructureLocate.ids().contains("minecraft:pillager_outpost"), "pillager_outpost is registered");

        for (ModernVillageStart.Type type : ModernVillageStart.Type.values()) {
            String id = VillageLocate.id(type);
            StructureLocate.Found found = StructureLocate.nearest(world, spawn, id);
            if (!check.ok(found != null && id.equals(found.id), "locate finds " + id + ": " + found)) {
                continue;
            }
            String why = LocateChecks.checkRingOrder(world, spawn, id, found);
            check.ok(why == null, id + " is today's ring order: " + why);
            int cx = found.pos.getX() >> 4;
            int cz = found.pos.getZ() >> 4;
            LocateChecks.generateAround(world, cx, cz, 0);
            StructureStart start = LocateChecks.savedStart(villages, cx, cz);
            String pool = start == null || start.getComponents().isEmpty() ? null
                    : start.getComponents().get(0).createStructureBaseNBT().getString("Pool");
            check.ok(start instanceof ModernVillageStart && start.isSizeableStructure()
                            && pool != null && pool.startsWith("village/" + type.name().toLowerCase(Locale.ROOT) + "/"),
                    "the generator laid out a " + id + " where locate said (" + found + ", pool " + pool + ")");
        }

        StructureLocate.Found any = StructureLocate.nearest(world, spawn, "#minecraft:village");
        check.ok(any != null && any.id.startsWith("minecraft:village_"), "#minecraft:village finds a typed village: " + any);
        if (any != null) {
            String why = LocateChecks.checkRingOrder(world, spawn, "#minecraft:village", any);
            check.ok(why == null, "#minecraft:village is today's ring order: " + why);
        }

        MapGenOutpost outposts = MapGenOutpost.of(world);
        check.ok(outposts != null, "the overworld has outposts");
        StructureLocate.Found outpost = StructureLocate.nearest(world, spawn, "minecraft:pillager_outpost");
        if (check.ok(outpost != null, "locate finds an outpost: " + outpost) && outposts != null) {
            String why = LocateChecks.checkRingOrder(world, spawn, "minecraft:pillager_outpost", outpost);
            check.ok(why == null, "the outpost is today's ring order: " + why);
            int cx = outpost.pos.getX() >> 4;
            int cz = outpost.pos.getZ() >> 4;
            LocateChecks.generateAround(world, cx, cz, 2);
            StructureStart start = LocateChecks.savedStart(outposts, cx, cz);
            check.ok(start != null && start.isSizeableStructure(), "the generator laid out an outpost where locate said: " + outpost);
        }

        // The command (Backport Core 1.3.1 dropped 1.12.2's names such as /locate Village, as today's game has none).
        List<String> said = LocateChecks.run(server, world, spawn, "locate structure #minecraft:village");
        check.ok(said.size() == 1 && any != null && said.get(0).startsWith("The nearest #minecraft:village (" + any.id + ") is at ["
                + any.pos.getX() + ", ~, " + any.pos.getZ() + "]"), "the tag's reply names the type: " + said);
        said = LocateChecks.run(server, world, spawn, "locate structure village_plains");
        check.ok(said.size() == 1 && said.get(0).startsWith("The nearest minecraft:village_plains is at ["), "a typed village: " + said);

        // 1.14 villages off: the types say why, the tag and the name still find the world's 1.12.2 villages.
        MapGenModernVillage.Style style = ModConfig.villages.style;
        ModConfig.villages.style = MapGenModernVillage.Style.LEGACY;
        try {
            said = LocateChecks.run(server, world, spawn, "locate structure minecraft:village_desert");
            check.ok(said.size() == 1 && said.get(0).startsWith("Village types can only be located with villages.style set to MODERN"),
                    "typed villages say why with 1.12.2 villages: " + said);
            StructureLocate.Found legacy = StructureLocate.nearest(world, spawn, "#minecraft:village");
            BlockPos vanilla = world.findNearestStructure("Village", spawn, false);
            check.ok(legacy != null && legacy.pos.equals(vanilla), "#minecraft:village falls back to 1.12.2's search: " + legacy + " vs " + vanilla);
        } finally {
            ModConfig.villages.style = style;
        }

        // Nether: no villages, no reason given.
        WorldServer nether = server.getWorld(-1);
        said = LocateChecks.run(server, nether, BlockPos.ORIGIN, "locate structure #minecraft:village");
        check.ok(said.size() == 1 && said.get(0).equals("Could not find a structure of type \"#minecraft:village\" nearby"),
                "no villages in the nether: " + said);

        List<String> first = LocateChecks.complete(server, world, spawn, "locate ");
        check.ok(first.contains("structure") && !first.contains("Village"), "Village is not listed: " + first);
        List<String> types = LocateChecks.complete(server, world, spawn, "locate structure village_");
        check.eq(5, types.size(), "completion lists the five types: " + types);
        List<String> tags = LocateChecks.complete(server, world, spawn, "locate structure #");
        check.ok(tags.contains("#minecraft:village"), "completion lists #minecraft:village: " + tags);
        List<String> outpostIds = LocateChecks.complete(server, world, spawn, "locate structure pillager");
        check.ok(outpostIds.contains("minecraft:pillager_outpost"), "completion lists the outpost: " + outpostIds);
    }
}
