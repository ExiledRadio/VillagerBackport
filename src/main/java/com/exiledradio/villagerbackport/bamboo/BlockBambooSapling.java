package com.exiledradio.villagerbackport.bamboo;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The bamboo shoot: what planting a piece of bamboo on soil makes, and the bottom block of every
 * stalk until something grows above it.
 *
 * <p>A third of random ticks it sends up the first block of stalk, already wearing small leaves,
 * given air above and light 9 or more. Bone meal does the same at once. As soon as there is stalk
 * above it, it becomes stalk itself - a thin, bare block - so the shoot is only ever seen alone.
 *
 * <p>No collision, a 8x12x8 outline nudged off-center like the stalk, and the same sword rule.
 */
public class BlockBambooSapling extends Block implements IGrowable {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(4 / 16D, 0, 4 / 16D, 12 / 16D, 12 / 16D, 12 / 16D);

    public BlockBambooSapling() {
        super(BlockBamboo.MATERIAL);
        setHardness(1.0F);
        setResistance(1.0F);
        setLightOpacity(0);
        setTickRandomly(true);
        setHarvestLevel("axe", 0);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && !world.getBlockState(pos).getMaterial().isLiquid()
                && BlockBamboo.isSoil(world.getBlockState(pos.down()));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!BlockBamboo.isSoil(world.getBlockState(pos.down()))) {
            world.scheduleUpdate(pos, this, 1);
            return;
        }
        if (fromPos.equals(pos.up()) && world.getBlockState(fromPos).getBlock() == BambooContent.bamboo) {
            world.setBlockState(pos, BambooContent.bamboo.getDefaultState(), 2);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!BlockBamboo.isSoil(world.getBlockState(pos.down()))) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!BlockBamboo.isSoil(world.getBlockState(pos.down()))) {
            world.destroyBlock(pos, true);
            return;
        }
        if (rand.nextInt(3) == 0 && world.isAirBlock(pos.up()) && world.getLight(pos.up()) >= 9) {
            growStalk(world, pos);
        }
    }

    private void growStalk(World world, BlockPos pos) {
        world.setBlockState(pos.up(), BambooContent.bamboo.getDefaultState()
                .withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.SMALL), 3);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.isAirBlock(pos.up());
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        growStalk(world, pos);
    }

    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        if (player.getHeldItemMainhand().getItem() instanceof ItemSword) {
            return 1.0F;
        }
        return super.getPlayerRelativeBlockHardness(state, player, world, pos);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return BambooContent.bambooItem;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(BambooContent.bambooItem);
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        return BambooContent.bambooSaplingSound != null ? BambooContent.bambooSaplingSound : SoundType.PLANT;
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE.offset(state.getOffset(source, pos));
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
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

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }
}
