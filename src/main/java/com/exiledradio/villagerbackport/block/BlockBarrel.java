package com.exiledradio.villagerbackport.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A barrel: a chest that does not need room above it.
 *
 * <h2>The point of the block</h2>
 * A chest refuses to open with a solid block on top, which is why villages could never put storage
 * under a staircase or in a cellar. A barrel has no lid to swing, so it has no such requirement -
 * that is the whole reason 1.14 added it, and it is the one behaviour that must not be lost in
 * porting it.
 *
 * <h2>Facing</h2>
 * Unusually among these blocks, a barrel faces any of six directions rather than four - the lid can
 * point at the floor or ceiling. It is placed facing away from the player, so looking down at the
 * ground gives an upward-facing barrel, which is what 1.14 does.
 */
public class BlockBarrel extends BlockWorkstation {

    /** All six directions, not just the horizontal four - the lid can face up or down. */
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    /** Whether anyone currently has it open, which swaps the lid texture. */
    public static final PropertyBool OPEN = PropertyBool.create("open");

    public BlockBarrel(Material material, SoundType sound, float hardness) {
        super(material, sound, hardness);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.UP)
                .withProperty(OPEN, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, OPEN);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityBarrel();
    }

    /**
     * Opens the standard chest screen.
     *
     * <p>No handler and no packet of ours: the tile entity reports itself as a chest-shaped
     * container, so vanilla's own window handling does the rest.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityBarrel) {
            player.displayGUIChest((TileEntityBarrel) tile);
        }

        return true;
    }

    /**
     * Placed with the lid toward the player, so standing over one and placing it gives an
     * upward-facing barrel.
     *
     * <p>Deliberately no {@code getOpposite()} here. {@code getDirectionFromEntityLiving} already
     * answers "which way is the player from this block" - it returns UP when they are standing above
     * it - so taking the opposite turned the lid away and buried it in the floor. 1.14 needs the
     * inversion because its helper reports where the player is *looking*, which is the other
     * question.
     */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer))
                .withProperty(OPEN, Boolean.FALSE);
    }

    /** Contents spill when it is broken, as any container's should. */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityBarrel) {
            InventoryHelper.dropInventoryItems(world, pos, (TileEntityBarrel) tile);
            world.updateComparatorOutputLevel(pos, this);
        }

        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof net.minecraft.inventory.IInventory
                ? net.minecraft.inventory.Container.calcRedstoneFromInventory(
                        (net.minecraft.inventory.IInventory) tile)
                : 0;
    }

    // ---------------------------------------------------------------- metadata

    /**
     * Six directions and an open flag pack into four bits with one to spare - direction in the low
     * three, open in the fourth.
     */
    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byIndex(meta & 7);
        return getDefaultState()
                .withProperty(FACING, facing)
                .withProperty(OPEN, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex() | (state.getValue(OPEN) ? 8 : 0);
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
