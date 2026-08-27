package com.exiledradio.villagerbackport.block;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;

/**
 * Cuts one stone block into a chosen shape.
 *
 * <p>One input, one output, and a list of everything the input could become - see
 * {@link StonecutterRecipes} for where that list comes from. Which entry is chosen is the only extra
 * state, and it is the only thing that needs syncing.
 *
 * <h2>How the choice travels</h2>
 * Clicking an entry goes over vanilla's enchantment-button channel, which is a plain
 * "button {@code n} of the open window was pressed" message with no enchanting in it - 1.14's
 * stonecutter uses the same one for the same reason. The chosen index comes back as a window property,
 * the same mechanism a furnace uses for its burn time. So no packets of this mod's own are involved,
 * and nothing here depends on the mod being installed server-side beyond the block itself.
 */
public class ContainerStonecutter extends Container {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESULT = 1;

    /** Window property id for the chosen entry. Numbering is per-container, so 0 is free. */
    private static final int PROPERTY_SELECTED = 0;

    /** Nothing chosen. Held as a short over the wire, so a negative value is fine. */
    private static final int NONE = -1;

    private final World world;
    private final BlockPos pos;
    private final CutterKind kind;
    private final Block block;

    private final Slot inputSlot;
    private final Slot resultSlot;

    private final IInventory input = new InventoryBasic("Stonecutter", true, 1) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerStonecutter.this.onCraftMatrixChanged(this);
        }
    };

    private final IInventory result = new InventoryCraftResult();

    private List<ItemStack> results = Collections.emptyList();
    private int selected = NONE;
    private int lastSentSelected = Integer.MIN_VALUE;

    /** What the list was last built for, so it is only rebuilt when the input actually changes. */
    private ItemStack lastInput = ItemStack.EMPTY;

    /** The tick the saw was last heard, so a bulk cut does not play sixty-four of them at once. */
    private long lastCut = -1L;

    /** Lets the screen reset its scroll position when the input changes. Client only. */
    private Runnable listener;

    public ContainerStonecutter(InventoryPlayer playerInventory, World world, BlockPos pos,
                                CutterKind kind, Block block) {
        this.world = world;
        this.pos = pos;
        this.kind = kind;
        this.block = block;

        this.inputSlot = addSlotToContainer(new InputSlot(this.input, SLOT_INPUT, 20, 33));
        this.resultSlot = addSlotToContainer(new ResultSlot(this.result, SLOT_RESULT, 143, 33));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @SideOnly(Side.CLIENT)
    public void setListener(Runnable listener) {
        this.listener = listener;
    }

    @SideOnly(Side.CLIENT)
    public List<ItemStack> getResults() {
        return this.results;
    }

    @SideOnly(Side.CLIENT)
    public int getSelected() {
        return this.selected;
    }

    @SideOnly(Side.CLIENT)
    public boolean hasChoices() {
        return this.inputSlot.getHasStack() && !this.results.isEmpty();
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        ItemStack stack = this.inputSlot.getStack();

        // Rebuilding on every change would reset the choice each time one block is consumed, which
        // would make holding a stack down impossible. The list only depends on what the item is.
        if (stack.getItem() == this.lastInput.getItem()
                && stack.getMetadata() == this.lastInput.getMetadata()) {
            return;
        }

        this.lastInput = stack.copy();
        this.results = StonecutterRecipes.of(this.kind).resultsFor(stack);
        this.selected = NONE;
        this.resultSlot.putStack(ItemStack.EMPTY);

        if (this.listener != null) {
            this.listener.run();
        }
    }

    /**
     * Handles a click on one of the shapes.
     *
     * <p>Named for enchanting because that is what vanilla built the channel for; it carries any
     * container's button presses.
     */
    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (id >= 0 && id < this.results.size()) {
            this.selected = id;
            updateResult();
        }

        return true;
    }

    private void updateResult() {
        if (this.selected >= 0 && this.selected < this.results.size()) {
            this.resultSlot.putStack(this.results.get(this.selected).copy());
        } else {
            this.resultSlot.putStack(ItemStack.EMPTY);
        }

        detectAndSendChanges();
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, PROPERTY_SELECTED, this.selected);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (this.selected != this.lastSentSelected) {
            for (IContainerListener listener : this.listeners) {
                listener.sendWindowProperty(this, PROPERTY_SELECTED, this.selected);
            }
            this.lastSentSelected = this.selected;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == PROPERTY_SELECTED) {
            this.selected = data;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.world.getBlockState(this.pos).getBlock() == this.block
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D,
                        this.pos.getZ() + 0.5D) <= 64.0D;
    }

    /** Nothing may be merged into the shape list by double-clicking, as 1.14 also refuses. */
    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.result.removeStackFromSlot(0);

        if (!this.world.isRemote) {
            clearContainer(player, this.world, this.input);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        int playerStart = 2;
        int playerEnd = this.inventorySlots.size();
        int hotbarStart = playerEnd - 9;

        if (index == SLOT_RESULT) {
            stack.getItem().onCreated(stack, this.world, player);

            if (!mergeItemStack(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onSlotChange(stack, original);
        } else if (index == SLOT_INPUT) {
            if (!mergeItemStack(stack, playerStart, playerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (StonecutterRecipes.of(this.kind).isInput(stack)) {
            // Anything the saw can work on goes to the input slot, which is what makes shift-clicking
            // a stack of cobblestone from the inventory do the obvious thing.
            if (!mergeItemStack(stack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < hotbarStart) {
            if (!mergeItemStack(stack, hotbarStart, playerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!mergeItemStack(stack, playerStart, hotbarStart, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        detectAndSendChanges();
        return original;
    }

    /** Only blocks the saw has something to do with. */
    private class InputSlot extends Slot {

        InputSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return StonecutterRecipes.of(ContainerStonecutter.this.kind).isInput(stack);
        }
    }

    /**
     * Taking a result consumes one block and immediately cuts the next.
     *
     * <p>Refilling the slot rather than clearing it is what lets a whole stack be cut by holding the
     * mouse down, and is 1.14's behaviour.
     */
    private class ResultSlot extends Slot {

        ResultSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            stack.getItem().onCreated(stack, ContainerStonecutter.this.world, player);
            ContainerStonecutter.this.inputSlot.decrStackSize(1);

            // Shift-clicking cuts a whole stack in one tick, so the sound is limited to once per
            // tick - sixty-four saw noises at the same instant is a burst of noise, not a saw. 1.14
            // guards it the same way.
            long now = ContainerStonecutter.this.world.getTotalWorldTime();
            if (!ContainerStonecutter.this.world.isRemote && ContainerStonecutter.this.lastCut != now) {
                ContainerStonecutter.this.lastCut = now;
                ContainerStonecutter.this.world.playSound(null, ContainerStonecutter.this.pos,
                        ModSounds.stonecutterUse, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            // decrStackSize on the input marks it dirty, which re-runs onCraftMatrixChanged - but only
            // clears the list when the item itself changed, so the choice survives until the stack
            // runs out. Recutting here keeps the result slot full while it lasts.
            ContainerStonecutter.this.updateResult();

            return super.onTake(player, stack);
        }
    }
}
