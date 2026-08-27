package com.exiledradio.villagerbackport.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A composter: a bin you can stand in, rather than a solid block.
 *
 * <h2>Why this is its own class</h2>
 * Every other workstation collides as either a full cube or one box. A composter is neither - it is
 * open at the top, so its collision is four walls and a floor with a hole in the middle. One
 * bounding box cannot describe that, and {@code getBoundingBox} only returns one.
 *
 * <p>1.14 expresses it as a subtraction: the whole cube minus an inner box from
 * {@code (2, 2, 2)} to {@code (14, 16, 14)}. 1.12.2 has no shape algebra, so the same result is
 * built the other way round, by listing the five pieces that are left.
 *
 * <p>The outline drawn when you look at it stays a full cube, which is what 1.14 does too and what
 * makes the block feel solid to aim at even though you can step inside it.
 */
public class BlockComposter extends BlockWorkstation {

    /** Wall thickness and floor height, in sixteenths, matching 1.14's inner box. */
    private static final double THICKNESS = 2.0D / 16.0D;

    private static final AxisAlignedBB FLOOR = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, THICKNESS, 1.0D);
    private static final AxisAlignedBB NORTH_WALL = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, THICKNESS);
    private static final AxisAlignedBB SOUTH_WALL = new AxisAlignedBB(0.0D, 0.0D, 1.0D - THICKNESS, 1.0D, 1.0D, 1.0D);
    private static final AxisAlignedBB WEST_WALL = new AxisAlignedBB(0.0D, 0.0D, 0.0D, THICKNESS, 1.0D, 1.0D);
    private static final AxisAlignedBB EAST_WALL = new AxisAlignedBB(1.0D - THICKNESS, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    /**
     * How full the composter is. Levels 1 to 7 are compost piling up; 8 means it has finished and is
     * holding bone meal, which is what makes the ready state visible before you take it.
     */
    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 8);

    /** The level at which composting is done and bone meal can be collected. */
    public static final int READY = 8;

    public BlockComposter(Material material, SoundType sound, float hardness) {
        super(material, sound, hardness, true);
        setLightOpacity(0);
        setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEVEL, Math.min(meta, READY));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL);
    }

    /**
     * Feed it, or empty it.
     *
     * <p>Two distinct interactions on one block, chosen by state rather than by what is held: a
     * finished composter always gives up its bone meal, and any other state accepts compostables.
     * That ordering matters - handing bone meal to a full composter should collect, not feed it.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        int level = state.getValue(LEVEL);

        if (level == READY) {
            harvest(world, pos, state);
            return true;
        }

        return compost(world, pos, state, player, hand);
    }

    /** Takes the bone meal and empties the bin. */
    private void harvest(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            spawn(world, pos, new ItemStack(Items.DYE, 1, 15));
            world.setBlockState(pos, state.withProperty(LEVEL, 0), 3);
        }

        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * Adds one item, which may or may not raise the level.
     *
     * <p>The item is always consumed even when the level does not rise - that is 1.14's behaviour and
     * the reason the chances mean anything. Filling the last slot moves it to ready rather than to
     * level 8 directly, so the bin visibly finishes before it can be emptied.
     */
    private boolean compost(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        float chance = Compostables.chanceFor(held);

        if (chance <= 0.0F) {
            return false;
        }

        if (!world.isRemote) {
            int level = state.getValue(LEVEL);
            boolean grew = level == 0 || world.rand.nextFloat() < chance;

            if (grew) {
                int next = level + 1;
                world.setBlockState(pos, state.withProperty(LEVEL, Math.min(next, READY)), 3);
            }

            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }

            world.playSound(null, pos,
                    grew ? SoundEvents.BLOCK_GRASS_PLACE : SoundEvents.BLOCK_GRASS_HIT,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        return true;
    }

    /**
     * Empties a full composter without dropping anything, for when a hopper takes the bone meal.
     */
    public static void empty(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockComposter && state.getValue(LEVEL) == READY) {
            world.setBlockState(pos, state.withProperty(LEVEL, 0), 3);
        }
    }

    /**
     * Composts one item pushed in by a hopper.
     *
     * <p>Shares the chance roll with the right-click path, so a hopper is neither faster nor slower
     * than doing it by hand - only tidier.
     */
    public static void compostFromHopper(World world, BlockPos pos, ItemStack stack) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockComposter)) {
            return;
        }

        int level = state.getValue(LEVEL);
        if (level >= READY) {
            return;
        }

        float chance = Compostables.chanceFor(stack);
        if (chance <= 0.0F) {
            return;
        }

        if (level == 0 || world.rand.nextFloat() < chance) {
            world.setBlockState(pos, state.withProperty(LEVEL, Math.min(level + 1, READY)), 3);
            world.playSound(null, pos, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    // A tile entity purely so hoppers have an IInventory to talk to - see TileEntityComposter.
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityComposter();
    }

    private static void spawn(World world, BlockPos pos, ItemStack stack) {
        EntityItem item = new EntityItem(world,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack);
        item.setDefaultPickupDelay();
        world.spawnEntity(item);
    }

    /**
     * @return how strongly a comparator reads it, which is simply how full it is.
     */
    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    /**
     * Adds each solid piece separately, which is what lets an entity occupy the middle.
     *
     * <p>The default implementation offers a single box and would seal the block. Listing the pieces
     * means the hollow is genuinely empty as far as collision is concerned.
     */
    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
                                      boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, FLOOR);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, NORTH_WALL);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, SOUTH_WALL);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, WEST_WALL);
        addCollisionBoxToList(pos, entityBox, collidingBoxes, EAST_WALL);
    }

    /** The aiming outline stays a full cube, as it is in 1.14. */
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /**
     * The sides are solid enough to support a torch, but the top is not - it is open.
     */
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.UP ? BlockFaceShape.UNDEFINED : BlockFaceShape.SOLID;
    }
}
