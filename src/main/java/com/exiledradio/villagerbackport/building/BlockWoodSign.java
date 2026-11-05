package com.exiledradio.villagerbackport.building;

import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The standing and wall forms of the five new wood signs.
 *
 * <p>Both extend vanilla's own sign blocks, so placement rules, popping off when their support goes,
 * rotation, mirroring and the outline all behave exactly as an oak sign's do. What changes is the
 * tile (see {@link TileEntityWoodSign}) and what the block drops and is picked as.
 *
 * <p>The numbers are vanilla's: hardness 1, wood sounds, and no statistics of their own, since a sign
 * block is never held - the item is.
 */
public final class BlockWoodSign {

    private BlockWoodSign() {
    }

    /** The {@link SignWood} of a sign block, or null if it is not one of these. */
    public static SignWood woodOf(net.minecraft.block.Block block) {
        if (block instanceof Standing) {
            return ((Standing) block).wood;
        }
        if (block instanceof Wall) {
            return ((Wall) block).wood;
        }
        return null;
    }

    /** On top of a block, rotated to any of sixteen directions. */
    public static final class Standing extends BlockStandingSign {
        final SignWood wood;

        Standing(SignWood wood) {
            this.wood = wood;
            setHardness(1.0F);
            setSoundType(SoundType.WOOD);
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileEntityWoodSign();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return BuildingContent.signItem(this.wood);
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(BuildingContent.signItem(this.wood));
        }
    }

    /** On the side of a block. */
    public static final class Wall extends BlockWallSign {
        final SignWood wood;

        Wall(SignWood wood) {
            this.wood = wood;
            setHardness(1.0F);
            setSoundType(SoundType.WOOD);
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileEntityWoodSign();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return BuildingContent.signItem(this.wood);
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(BuildingContent.signItem(this.wood));
        }
    }
}
