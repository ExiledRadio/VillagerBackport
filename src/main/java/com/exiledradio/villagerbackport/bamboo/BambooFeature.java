package com.exiledradio.villagerbackport.bamboo;

import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 1.14's {@code BambooFeature}: one stalk of bamboo, grown in one go.
 *
 * <p>The stalk is 5 to 16 blocks tall, thick all the way up, and finished like a stalk that grew on
 * its own: large leaves on the top two blocks, small leaves on the one below, and the top marked as
 * done growing. Some stalks also lay a disc of podzol around their foot, radius 1 to 4, over grass
 * and dirt - which is where a bamboo jungle's podzol comes from.
 *
 * <h2>Chunk edges</h2>
 * Population may only touch the 16x16 area offset 8 blocks into the chunk; anything outside it can
 * land in a chunk that is not generated yet and make the game generate it, which cascades. The stalk
 * itself is one column inside that area. The podzol disc is clipped to it.
 */
final class BambooFeature {

    private BambooFeature() {
    }

    /**
     * @param podzolChance chance of the podzol disc: 0.2 in bamboo jungles, 0 elsewhere
     * @param areaX        west edge of the area population may touch
     * @param areaZ        north edge of that area
     * @return true if a stalk was placed
     */
    static boolean place(World world, Random rand, BlockPos pos, float podzolChance, int areaX, int areaZ) {
        BlockBamboo bamboo = BambooContent.bamboo;
        if (bamboo == null || !world.isAirBlock(pos) || !BlockBamboo.isSoil(world.getBlockState(pos.down()))) {
            return false;
        }

        int height = rand.nextInt(12) + 5;

        if (rand.nextFloat() < podzolChance) {
            int radius = rand.nextInt(4) + 1;
            for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
                for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                    int dx = x - pos.getX();
                    int dz = z - pos.getZ();
                    if (dx * dx + dz * dz > radius * radius
                            || x < areaX || x >= areaX + 16 || z < areaZ || z >= areaZ + 16) {
                        continue;
                    }
                    BlockPos ground = world.getHeight(new BlockPos(x, 0, z)).down();
                    if (isDirtLike(world.getBlockState(ground))) {
                        world.setBlockState(ground, Blocks.DIRT.getDefaultState()
                                .withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL), 2);
                    }
                }
            }
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos);
        for (int i = 0; i < height && world.isAirBlock(cursor); i++) {
            world.setBlockState(cursor, bamboo.trunk(), 2);
            cursor.move(net.minecraft.util.EnumFacing.UP);
        }

        // 1.14 crowns the stalk in the block above it, and the two below. It writes that top block
        // even when the stalk stopped short against something; here only into air.
        if (cursor.getY() - pos.getY() >= 3 && world.isAirBlock(cursor)) {
            IBlockState large = bamboo.trunk().withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.LARGE);
            world.setBlockState(cursor, large.withProperty(BlockBamboo.STAGE, 1), 2);
            world.setBlockState(cursor.move(net.minecraft.util.EnumFacing.DOWN), large, 2);
            world.setBlockState(cursor.move(net.minecraft.util.EnumFacing.DOWN),
                    bamboo.trunk().withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.SMALL), 2);
        }
        return true;
    }

    /** 1.14's {@code dirt_like} tag: dirt in all its kinds, grass and mycelium. */
    private static boolean isDirtLike(IBlockState state) {
        return state.getBlock() == Blocks.DIRT || state.getBlock() == Blocks.GRASS || state.getBlock() == Blocks.MYCELIUM;
    }
}
