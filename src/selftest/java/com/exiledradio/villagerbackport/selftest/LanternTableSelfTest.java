package com.exiledradio.villagerbackport.selftest;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Loader;

/**
 * A lantern stands on a furniture table (MrCrayfish's Furniture Mod, cfm in eclipse/mods): its tables answer
 * UNDEFINED for every face, so the lantern goes by the table's full-height top.
 */
public final class LanternTableSelfTest implements SelfTest {

    @Override
    public String name() {
        return "lanterntable";
    }

    @Override
    public void run(Check check) {
        WorldServer world = DimensionManager.getWorld(0);
        Block lantern = Block.getBlockFromName("villagerbackport:lantern");
        check.ok(lantern != null, "the lantern is registered");
        if (lantern == null) {
            return;
        }
        BlockPos post = new BlockPos(7000, 120, 7000);
        world.getChunk(post);
        for (BlockPos p : BlockPos.getAllInBox(post.add(-1, -1, -1), post.add(1, 3, 1))) {
            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2 | 16);
        }
        world.setBlockState(post, Blocks.STONE.getDefaultState(), 2 | 16);
        check.ok(lantern.canPlaceBlockAt(world, post.up()), "stone holds a lantern");
        world.setBlockState(post, Blocks.AIR.getDefaultState(), 2 | 16);
        check.ok(!lantern.canPlaceBlockAt(world, post.up()), "air does not");
        if (!Loader.isModLoaded("cfm")) {
            return;
        }
        Object[][] below = {
                {"cfm:table_oak", true}, {"cfm:desk_oak", true}, {"cfm:modern_table", true}, {"cfm:counter", true},
                {"cfm:coffee_table_oak", false},
        };
        for (Object[] b : below) {
            Block block = Block.getBlockFromName((String) b[0]);
            if (block == null) {
                check.fail(b[0] + " is not registered");
                continue;
            }
            world.setBlockState(post, block.getDefaultState(), 2 | 16);
            check.eq(b[1], lantern.canPlaceBlockAt(world, post.up()), b[0] + " under a lantern");
            if ((Boolean) b[1]) {
                world.setBlockState(post.up(), lantern.getDefaultState(), 2 | 16);
                world.neighborChanged(post.up(), Blocks.STONE, post);
                check.ok(world.getBlockState(post.up()).getBlock() == lantern, "a lantern stays on " + b[0]);
                world.setBlockState(post.up(), Blocks.AIR.getDefaultState(), 2 | 16);
            }
        }
        world.setBlockState(post, Blocks.AIR.getDefaultState(), 2 | 16);
    }
}
