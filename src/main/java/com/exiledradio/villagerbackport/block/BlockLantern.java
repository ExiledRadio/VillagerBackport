package com.exiledradio.villagerbackport.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's lantern: a light that stands on a block or hangs under one.
 *
 * <p>Not a workstation, but 1.14's villages are lit by them - sixty-five across the village
 * structures, most of them hanging from beams and porches where a torch has nothing to stand on.
 * Placed against the underside of a block it hangs; anywhere else it stands. Like 1.14's, it drops
 * when whatever holds it up goes away.
 */
public class BlockLantern extends Block {

    public static final PropertyBool HANGING = PropertyBool.create("hanging");

    /** 1.14's {@code LanternBlock} shapes, the body and the cap as one box each way up. */
    private static final AxisAlignedBB STANDING = new AxisAlignedBB(5 / 16D, 0, 5 / 16D, 11 / 16D, 9 / 16D, 11 / 16D);
    private static final AxisAlignedBB HANGING_BOX = new AxisAlignedBB(5 / 16D, 1 / 16D, 5 / 16D, 11 / 16D, 10 / 16D, 11 / 16D);

    public BlockLantern() {
        super(Material.IRON);
        setHardness(3.5F);
        setResistance(3.5F);
        setSoundType(SoundType.METAL);
        setLightLevel(1.0F);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(this.blockState.getBaseState().withProperty(HANGING, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HANGING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HANGING, Boolean.valueOf((meta & 1) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HANGING).booleanValue() ? 1 : 0;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer) {
        boolean hang = facing == EnumFacing.DOWN && canHang(world, pos);
        if (!hang && !canStand(world, pos) && canHang(world, pos)) {
            hang = true;
        }
        return getDefaultState().withProperty(HANGING, Boolean.valueOf(hang));
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && (canStand(world, pos) || canHang(world, pos));
    }

    private static boolean canStand(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlockFaceShape(world, pos.down(), EnumFacing.UP) != BlockFaceShape.UNDEFINED;
    }

    private static boolean canHang(IBlockAccess world, BlockPos pos) {
        IBlockState above = world.getBlockState(pos.up());
        // 1.14 lets a lantern hang from anything with an underside to grip, chains and fences included.
        return above.getBlockFaceShape(world, pos.up(), EnumFacing.DOWN) != BlockFaceShape.UNDEFINED
                || above.getBlock() instanceof net.minecraft.block.BlockFence;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        boolean held = state.getValue(HANGING).booleanValue() ? canHang(world, pos) : canStand(world, pos);
        if (!held) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(HANGING).booleanValue() ? HANGING_BOX : STANDING;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
