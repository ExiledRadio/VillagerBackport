package com.exiledradio.villagerbackport.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A workstation with its own outline rather than a full cube - lectern, grindstone, stonecutter, bell.
 *
 * <p>Getting the cube answers wrong reaches well past the block itself: a full cube tells the
 * renderer that neighbouring faces can be skipped and tells lighting that it is blocked, so a
 * lectern claiming to be one makes the blocks behind it stop drawing and casts a solid shadow.
 *
 * <p>The three cube questions are answered by constant, not by field, because
 * {@link net.minecraft.block.Block}'s constructor asks them before any field is set. The outline
 * itself is a field, which is safe - it is only ever read during play.
 */
public class BlockWorkstationShaped extends BlockWorkstationFacing {

    private final AxisAlignedBB shape;

    public BlockWorkstationShaped(Material material, SoundType sound, float hardness, AxisAlignedBB shape) {
        this(material, sound, hardness, shape, false);
    }

    public BlockWorkstationShaped(Material material, SoundType sound, float hardness,
                                  AxisAlignedBB shape, boolean cutout) {
        super(material, sound, hardness, cutout);
        this.shape = shape;

        // Light passes through, which the constructor could not have worked out for itself.
        setLightOpacity(0);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.shape;
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
}
