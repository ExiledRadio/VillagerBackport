package com.exiledradio.villagerbackport.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.inventory.ISidedInventory;

import javax.annotation.Nullable;

/**
 * Lets hoppers work a composter.
 *
 * <h2>Why there is a tile entity at all</h2>
 * The composter has nothing to remember - its entire state is the fill level, which lives in the
 * block state. It needs one anyway, because a 1.12.2 hopper only talks to an {@link ISidedInventory},
 * and that has to be attached to a tile entity. 1.14 has no such requirement: its composter provides
 * an inventory on demand without storing one.
 *
 * <p>So this holds no items of its own. It presents a two-slot inventory as a facade over the block
 * state: putting something in the top slot composts it immediately and the slot goes back to empty,
 * and the bottom slot appears to contain bone meal exactly when the composter is full.
 *
 * <h2>Sides</h2>
 * In from the top, out from the bottom, nothing from the sides - which is how 1.14 wires it and also
 * the only arrangement that reads correctly for a bin you tip things into.
 */
public class TileEntityComposter extends TileEntity implements ISidedInventory {

    /** Where compostables go in. */
    public static final int SLOT_INPUT = 0;

    /** Where bone meal comes out. */
    public static final int SLOT_OUTPUT = 1;

    private static final int[] TOP_SLOTS = {SLOT_INPUT};
    private static final int[] BOTTOM_SLOTS = {SLOT_OUTPUT};
    private static final int[] NO_SLOTS = {};

    // ------------------------------------------------------------ block state

    private int level() {
        IBlockState state = this.world.getBlockState(this.pos);
        return state.getBlock() instanceof BlockComposter
                ? state.getValue(BlockComposter.LEVEL)
                : 0;
    }

    private boolean isReady() {
        return level() >= BlockComposter.READY;
    }

    // ------------------------------------------------------------- inventory

    @Override
    public int getSizeInventory() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return !isReady();
    }

    /**
     * @return bone meal in the output slot when full, and nothing anywhere else.
     *
     * <p>The input slot always reads empty because nothing is ever stored there - an inserted item is
     * composted on the spot.
     */
    @Override
    public ItemStack getStackInSlot(int index) {
        return index == SLOT_OUTPUT && isReady() ? new ItemStack(Items.DYE, 1, 15) : ItemStack.EMPTY;
    }

    /**
     * Taking the bone meal empties the composter.
     *
     * <p>The whole stack goes at once whatever is asked for, because there is only ever one item in
     * there and it is not divisible into a partial pull.
     */
    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index != SLOT_OUTPUT || !isReady() || count <= 0) {
            return ItemStack.EMPTY;
        }

        BlockComposter.empty(this.world, this.pos);
        return new ItemStack(Items.DYE, 1, 15);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return decrStackSize(index, 1);
    }

    /**
     * Composts whatever a hopper pushes in.
     *
     * <p>Nothing is stored: the item is applied to the fill level and dropped. That is what keeps the
     * two views consistent, since the slot has to read empty again immediately or a hopper would
     * believe the composter is backed up and stop feeding it.
     */
    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == SLOT_INPUT && !stack.isEmpty()) {
            BlockComposter.compostFromHopper(this.world, this.pos, stack);
        }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == SLOT_INPUT && !isReady() && Compostables.isCompostable(stack);
    }

    // ----------------------------------------------------------------- sides

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        if (side == EnumFacing.UP) {
            return TOP_SLOTS;
        }
        return side == EnumFacing.DOWN ? BOTTOM_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return direction == EnumFacing.UP && isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return direction == EnumFacing.DOWN && index == SLOT_OUTPUT && isReady();
    }

    // ------------------------------------------------------------ boilerplate

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return false;   // there is no screen; interaction is by right-click on the block
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    /** Nothing to clear - the fill level is the block's, not this object's. */
    @Override
    public void clear() {
    }

    @Override
    public String getName() {
        return "container.villagerbackport.composter";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(Names.translateOr(getName(), "Composter"));
    }
}
