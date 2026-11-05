package com.exiledradio.villagerbackport.flora;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * A flower pot holding one of this package's flowers.
 *
 * <h2>Why a block per flower</h2>
 * 1.12.2's flower pot keeps its plant in a tile entity, but it only knows how to draw the plants in
 * its own hard-coded list, and nothing outside the class can add to that list. 1.14 solved the
 * same problem by making every potted plant a block of its own - {@code potted_cornflower} and so on
 * - and that is what this does too, with 1.14's names. A pot is turned into one of these when a
 * player puts the flower in (see {@link FlowerPots}) and back into an ordinary empty pot when they
 * take it out.
 *
 * <h2>Behavior</h2>
 * 1.14's: right-click a full pot with anything that is not itself a pottable plant and the flower
 * comes out, into the empty hand or the inventory or, failing both, onto the ground. Broken, it
 * drops the pot and the flower. It needs a solid top under it, as 1.12.2's pot does. A wither rose
 * in a pot does not wither anything.
 */
public class BlockFloraPot extends Block {

    /** 1.12.2's pot shape: six pixels square, six tall. */
    private static final AxisAlignedBB SHAPE =
            new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.375D, 0.6875D);

    private final Block flower;

    public BlockFloraPot(Block flower) {
        super(Material.CIRCUITS);
        this.flower = flower;
        setSoundType(SoundType.STONE);
        setHardness(0.0F);
    }

    /** @return the flower this pot holds */
    public Block getFlower() {
        return this.flower;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * 1.14's {@code FlowerPotBlock.onBlockActivated}: holding a pottable plant does nothing to a full
     * pot; holding anything else takes the flower out.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (FlowerPots.isPottable(held)) {
            return false;
        }

        if (!world.isRemote) {
            ItemStack flowerStack = new ItemStack(this.flower);
            if (held.isEmpty()) {
                player.setHeldItem(hand, flowerStack);
            } else if (!player.addItemStackToInventory(flowerStack)) {
                player.dropItem(flowerStack, false);
            }
            world.setBlockState(pos, Blocks.FLOWER_POT.getDefaultState(), 3);
        }
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && world.getBlockState(pos.down()).isTopSolid();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.getBlockState(pos.down()).isTopSolid()) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.FLOWER_POT;
    }

    /** The pot and the flower, as 1.14's loot table for every potted plant. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
                         int fortune) {
        drops.add(new ItemStack(Items.FLOWER_POT));
        drops.add(new ItemStack(this.flower));
    }

    /** Pick block gives the flower, as in 1.14. */
    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(this.flower);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.flower);
    }
}
