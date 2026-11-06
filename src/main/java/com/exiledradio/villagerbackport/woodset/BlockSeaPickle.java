package com.exiledradio.villagerbackport.woodset;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.14's sea pickle: one to four in a block, glowing only in water.
 *
 * <h2>What carries over from 1.14.4</h2>
 * <ul>
 *   <li>{@code pickles} 1 to 4. Right-clicking a pickle block holding a sea pickle adds one, up to four,
 *       and breaking drops one item per pickle. 1.14's outline boxes per count.</li>
 *   <li>Light {@code 3 + 3 * pickles} - 6, 9, 12, 15 - when in water, none when not.</li>
 *   <li>Stands on any block with a collision box (1.14 asks that the block below projects a non-empty
 *       collision shape upward), and pops off when that block goes.</li>
 *   <li>No collision of its own, broken instantly, slime sounds, green on maps, destroyed by pistons.</li>
 *   <li>Smelts to lime dye, which 1.12.2 already has (dye metadata 10).</li>
 * </ul>
 *
 * <h2>"In water" without waterlogging</h2>
 * 1.12.2 has no waterlogging, so a block cannot be in water. 1.14's {@code waterlogged} property is
 * kept - same name, so a structure file's value carries across and the 1.14 blockstate file's dead and
 * live models fit it unchanged - and here it means:
 * <ul>
 *   <li>when placed: the block it replaces was water (placing on the sea floor replaces a water
 *       source), or water is directly beside or above it;</li>
 *   <li>afterwards: water is directly beside or above it, checked again whenever a neighbor changes.</li>
 * </ul>
 * So a pickle on the floor of a pond or the sea glows, as one in 1.14 would, and one left dry when the
 * water around it is drained stops glowing. What 1.14 can do that this cannot: a lone pickle in a
 * one-block hole of water, with no water beside or above it, goes dark the first time a neighbor
 * changes, because the only water it had is the source it replaced.
 *
 * <p>The water itself does not wash pickles away: the material blocks liquids, as a waterlogged
 * block's does in 1.14, while having no collision box.
 *
 * <h2>Not done</h2>
 * Bone meal on a pickle in water spreads pickles across coral in 1.14. 1.12.2 has no coral, so that is
 * left out; bone meal does nothing here.
 */
public class BlockSeaPickle extends Block {

    public static final PropertyInteger PICKLES = PropertyInteger.create("pickles", 1, 4);
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

    /**
     * Not solid, not light-blocking, but blocking liquids: water and lava treat a block whose material
     * blocks movement as a wall (see {@code BlockDynamicLiquid.isBlocked}) instead of washing it away.
     */
    static final Material MATERIAL = new Material(MapColor.GREEN) {
        {
            this.setNoPushMobility();
        }

        @Override
        public boolean isSolid() {
            return false;
        }

        @Override
        public boolean blocksLight() {
            return false;
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    };

    /** 1.14.4's {@code SeaPickleBlock} shapes, by pickle count. */
    private static final AxisAlignedBB[] SHAPES = {
            new AxisAlignedBB(6 / 16.0, 0, 6 / 16.0, 10 / 16.0, 6 / 16.0, 10 / 16.0),
            new AxisAlignedBB(3 / 16.0, 0, 3 / 16.0, 13 / 16.0, 6 / 16.0, 13 / 16.0),
            new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 6 / 16.0, 14 / 16.0),
            new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 7 / 16.0, 14 / 16.0),
    };

    public BlockSeaPickle() {
        super(MATERIAL, MapColor.GREEN);
        this.setHardness(0.0F);
        this.setSoundType(SoundType.SLIME);
        this.setDefaultState(this.blockState.getBaseState().withProperty(PICKLES, 1).withProperty(WATERLOGGED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] {PICKLES, WATERLOGGED});
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(PICKLES, (meta & 3) + 1).withProperty(WATERLOGGED, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(PICKLES) - 1) | (state.getValue(WATERLOGGED) ? 4 : 0);
    }

    // ------------------------------------------------------------------------------------------
    // Water
    // ------------------------------------------------------------------------------------------

    /** @return true if water is directly above or beside this position */
    static boolean waterNextTo(IBlockAccess world, BlockPos pos) {
        for (EnumFacing side : EnumFacing.values()) {
            if (side != EnumFacing.DOWN && world.getBlockState(pos.offset(side)).getMaterial() == Material.WATER) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer) {
        // Still the block being replaced: placing on the sea floor replaces a water source.
        boolean wet = world.getBlockState(pos).getMaterial() == Material.WATER || waterNextTo(world, pos);
        return this.getDefaultState().withProperty(WATERLOGGED, wet);
    }

    /**
     * Placed by anything else - a structure, a command - it is in water if water is next to it. A
     * state that already says so is kept: the water it replaced is gone by now.
     */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && !state.getValue(WATERLOGGED) && waterNextTo(world, pos)) {
            world.setBlockState(pos, state.withProperty(WATERLOGGED, true), 2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (world.isRemote) {
            return;
        }
        if (!canStayOn(world, pos.down())) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
            return;
        }
        boolean wet = waterNextTo(world, pos);
        if (wet != state.getValue(WATERLOGGED)) {
            world.setBlockState(pos, state.withProperty(WATERLOGGED, wet), 2);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Placing and adding
    // ------------------------------------------------------------------------------------------

    /** @return true if the block at {@code below} has collision a pickle can stand on */
    @SuppressWarnings("deprecation")
    static boolean canStayOn(World world, BlockPos below) {
        IBlockState state = world.getBlockState(below);
        return state.getCollisionBoundingBox(world, below) != NULL_AABB;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && canStayOn(world, pos.down());
    }

    /**
     * Right-clicking with a sea pickle adds one, as 1.14 does by letting the pickle item replace the
     * block it clicks. Done here rather than in the item so it needs no item class of its own.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != Item.getItemFromBlock(this) || state.getValue(PICKLES) >= 4) {
            return false;
        }
        if (!world.isRemote) {
            world.setBlockState(pos, state.withProperty(PICKLES, state.getValue(PICKLES) + 1), 3);
            SoundType sound = this.getSoundType(state, world, pos, player);
            world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Light, shape, drops
    // ------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return state.getValue(WATERLOGGED) ? 3 + 3 * state.getValue(PICKLES) : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPES[state.getValue(PICKLES) - 1];
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
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
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        return state.getValue(PICKLES);
    }
}
