package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.GuiHandler;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A lectern: holds one book for anyone to read, and reports how far through it they are.
 *
 * <h2>What it is for</h2>
 * A librarian's workstation, and the only block in 1.14 that turns reading into a redstone signal.
 * A comparator beside one reads the open page, and every page turn sends a brief pulse - which is
 * what lets a book be used as a selector rather than just decoration.
 *
 * <h2>Three properties in four bits</h2>
 * Facing, whether a book is on it, and whether it is pulsing: four directions times two times two is
 * exactly sixteen, which is all the metadata 1.12.2 has. It fits with nothing to spare, which is why
 * facing is stored as a horizontal index rather than a full one.
 */
public class BlockLectern extends BlockWorkstationShaped {

    public static final PropertyBool HAS_BOOK = PropertyBool.create("has_book");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    private static final int META_HAS_BOOK = 4;
    private static final int META_POWERED = 8;

    /** How long the pulse lasts. Two ticks is enough for a repeater to catch, and is 1.14's. */
    private static final int PULSE_TICKS = 2;

    public BlockLectern(Material material, SoundType sound, float hardness, AxisAlignedBB shape) {
        super(material, sound, hardness, shape);

        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(HAS_BOOK, Boolean.FALSE)
                .withProperty(POWERED, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HAS_BOOK, POWERED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(HAS_BOOK, Boolean.valueOf((meta & META_HAS_BOOK) != 0))
                .withProperty(POWERED, Boolean.valueOf((meta & META_POWERED) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();

        if (state.getValue(HAS_BOOK).booleanValue()) {
            meta |= META_HAS_BOOK;
        }
        if (state.getValue(POWERED).booleanValue()) {
            meta |= META_POWERED;
        }

        return meta;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityLectern();
    }

    /**
     * Right-click puts a book on, or opens the one already there.
     *
     * <p>An empty lectern with an empty hand does nothing at all, rather than opening a blank screen -
     * the block is only interactive once there is something to read.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (state.getValue(HAS_BOOK).booleanValue()) {
            if (!world.isRemote) {
                player.openGui(VillagerBackport.instance, GuiHandler.LECTERN, world,
                        pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }

        ItemStack held = player.getHeldItem(hand);
        if (isBook(held)) {
            if (!world.isRemote) {
                tryPlaceBook(world, pos, state, player.capabilities.isCreativeMode ? held.copy() : held);
            }
            return true;
        }

        return false;
    }

    public static boolean isBook(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK;
    }

    /** @return true if the book went on the stand */
    public static boolean tryPlaceBook(World world, BlockPos pos, IBlockState state, ItemStack stack) {
        if (state.getValue(HAS_BOOK).booleanValue()) {
            return false;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityLectern)) {
            return false;
        }

        ((TileEntityLectern) tile).setBook(stack.splitStack(1));
        setHasBook(world, pos, state, true);
        world.playSound(null, pos, ModSounds.bookPut, SoundCategory.BLOCKS, 1.0F, 1.0F);

        return true;
    }

    public static void setHasBook(World world, BlockPos pos, IBlockState state, boolean hasBook) {
        world.setBlockState(pos, state
                .withProperty(POWERED, Boolean.FALSE)
                .withProperty(HAS_BOOK, Boolean.valueOf(hasBook)), 3);

        notifyBelow(world, pos, state);
    }

    /**
     * Flicks the signal on, and schedules it back off.
     *
     * <p>Deliberately a pulse rather than a level: a page turn is an event, and a lectern that stayed
     * powered would say "a page was turned once" forever.
     */
    public static void pulse(World world, BlockPos pos, IBlockState state) {
        setPowered(world, pos, state, true);
        world.scheduleUpdate(pos, state.getBlock(), PULSE_TICKS);

        // Played from the block rather than by the reader's own client, so anyone standing nearby
        // hears the page turn - the same reason the page itself lives on the lectern.
        world.playSound(null, pos, ModSounds.bookPageTurn, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private static void setPowered(World world, BlockPos pos, IBlockState state, boolean powered) {
        world.setBlockState(pos, state.withProperty(POWERED, Boolean.valueOf(powered)), 3);
        notifyBelow(world, pos, state);
    }

    /**
     * The signal comes out of the bottom, as 1.14's does - so a lectern can sit on top of the circuit
     * it drives without the wire being visible beside it.
     */
    private static void notifyBelow(World world, BlockPos pos, IBlockState state) {
        world.notifyNeighborsOfStateChange(pos.down(), state.getBlock(), false);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isRemote) {
            setPowered(world, pos, state, false);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (state.getValue(HAS_BOOK).booleanValue()) {
            dropBook(world, pos, state);
        }

        if (state.getValue(POWERED).booleanValue()) {
            notifyBelow(world, pos, state);
        }

        super.breakBlock(world, pos, state);
    }

    /** Dropped just in front of the stand rather than inside it, so it is easy to see and pick up. */
    private static void dropBook(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityLectern)) {
            return;
        }

        ItemStack book = ((TileEntityLectern) tile).removeBook();
        if (book.isEmpty()) {
            return;
        }

        EnumFacing facing = state.getValue(FACING);
        EntityItem dropped = new EntityItem(world,
                pos.getX() + 0.5D + 0.25D * facing.getXOffset(),
                pos.getY() + 1,
                pos.getZ() + 0.5D + 0.25D * facing.getZOffset(),
                book);

        dropped.setDefaultPickupDelay();
        world.spawnEntity(dropped);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED).booleanValue() ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP && state.getValue(POWERED).booleanValue() ? 15 : 0;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        if (!state.getValue(HAS_BOOK).booleanValue()) {
            return 0;
        }

        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityLectern ? ((TileEntityLectern) tile).comparatorLevel() : 0;
    }
}
