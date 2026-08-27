package com.exiledradio.villagerbackport.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A workstation that remembers which way it was placed.
 *
 * <p>{@link #createBlockState()} is overridden rather than branching on a field, because it is
 * called from {@link net.minecraft.block.Block}'s constructor - see the note on
 * {@link BlockWorkstation} for what happened when it was a field.
 */
public class BlockWorkstationFacing extends BlockWorkstation {

    public BlockWorkstationFacing(Material material, SoundType sound, float hardness) {
        this(material, sound, hardness, false);
    }

    public BlockWorkstationFacing(Material material, SoundType sound, float hardness, boolean cutout) {
        super(material, sound, hardness, cutout);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    /** Placed facing the player, as every directional workstation does in 1.14. */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    /**
     * Horizontal index rather than the full one: these only ever face sideways, so four values are
     * enough and there is no vertical case to guard against.
     */
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }
}
