package com.exiledradio.villagerbackport.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

/**
 * Copies and zooms out maps, without a crafting grid.
 *
 * <h2>What it changes</h2>
 * Both operations already existed in 1.12.2 as crafting recipes - a map beside an empty map copies
 * it, a map surrounded by eight paper zooms it out. What 1.14 added was somewhere to do it that
 * shows you the result first, and that does not cost eight paper to widen a map by one step.
 *
 * <p>So the recipes are not replaced. This is a second, cheaper route to the same two results, which
 * is exactly the relationship 1.14's table has to its own crafting recipes.
 *
 * <h2>Locking</h2>
 * 1.14's third operation - a glass pane to lock a map so it stops updating - is not here. Locking
 * lives on a {@code locked} field that 1.12.2's {@code MapData} does not have, and the update it
 * would need to suppress happens inside {@code ItemMap.onUpdate}, which has no hook. Adding it would
 * mean patching a vanilla class, which this mod does not do anywhere else.
 */
public class ContainerCartographyTable extends Container {

    public static final int SLOT_MAP = 0;
    public static final int SLOT_MODIFIER = 1;
    public static final int SLOT_RESULT = 2;

    /** Paper widens a map by one zoom step, as the crafting recipe does. */
    private static final int SCALE_UP = 1;

    /** Vanilla's widest map. Zooming past it does nothing but spend a map id. */
    private static final int MAX_SCALE = 4;

    private final World world;
    private final BlockPos pos;

    private final IInventory result = new InventoryCraftResult();

    private final IInventory inputs = new InventoryBasic("CartographyTable", true, 2) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerCartographyTable.this.onCraftMatrixChanged(this);
        }
    };

    public ContainerCartographyTable(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;

        addSlotToContainer(new FilledMapSlot(this.inputs, SLOT_MAP, 15, 15));
        addSlotToContainer(new ModifierSlot(this.inputs, SLOT_MODIFIER, 15, 52));
        addSlotToContainer(new ResultSlot(this.result, SLOT_RESULT, 145, 39));

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

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        super.onCraftMatrixChanged(inventory);
        if (inventory == this.inputs) {
            updateResult();
        }
    }

    private void updateResult() {
        ItemStack map = this.inputs.getStackInSlot(SLOT_MAP);
        ItemStack modifier = this.inputs.getStackInSlot(SLOT_MODIFIER);

        if (map.isEmpty() || modifier.isEmpty() || map.getItem() != Items.FILLED_MAP) {
            setResult(ItemStack.EMPTY);
            return;
        }

        if (modifier.getItem() == Items.MAP) {
            setResult(copyOf(map));
        } else if (modifier.getItem() == Items.PAPER) {
            setResult(canZoom(map) ? zoomedOut(map) : ItemStack.EMPTY);
        } else {
            setResult(ItemStack.EMPTY);
        }
    }

    /**
     * @return two of the map - the one that went in, and its copy
     *
     * <p>Copies share a map id, which is what makes them keep updating together; that is the point of
     * copying one rather than drawing a new map beside it. Both come out of the result slot at once,
     * as 1.14 does it, because there is nowhere to leave the original behind.
     */
    private static ItemStack copyOf(ItemStack map) {
        ItemStack copies = map.copy();
        copies.setCount(2);
        return copies;
    }

    /**
     * @return the map marked to be widened a step
     *
     * <p>The tag is only a request. Vanilla reads it in {@code ItemMap.onCreated} and does the actual
     * work of allocating the wider map, which is why the result slot has to call that when the item
     * is taken rather than doing the scaling here.
     */
    private static ItemStack zoomedOut(ItemStack map) {
        ItemStack zoomed = map.copy();
        zoomed.setCount(1);

        if (!zoomed.hasTagCompound()) {
            zoomed.setTagCompound(new NBTTagCompound());
        }
        zoomed.getTagCompound().setInteger("map_scale_direction", SCALE_UP);

        return zoomed;
    }

    /**
     * @return true unless the map is already at the widest scale the game has
     *
     * <p>Without this, paper on a fully zoomed map would still produce something: vanilla clamps the
     * scale but allocates a fresh map id anyway, so the player would pay a paper for a map that looks
     * identical and no longer matches the copies of it they already had.
     *
     * <p>The client may not have the map's data yet and will let it through; the server recomputes
     * the same slot and its answer replaces this one, so the worst case is a result that appears for
     * a moment and then does not.
     */
    private boolean canZoom(ItemStack map) {
        MapData data = Items.FILLED_MAP.getMapData(map, this.world);
        return data == null || data.scale < MAX_SCALE;
    }

    private void setResult(ItemStack stack) {
        this.result.setInventorySlotContents(0, stack);
        detectAndSendChanges();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.world.getBlockState(this.pos).getBlock() instanceof BlockCartographyTable
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D,
                        this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);

        if (!this.world.isRemote) {
            clearContainer(player, this.world, this.inputs);
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
        int playerStart = 3;
        int playerEnd = this.inventorySlots.size();

        if (index < playerStart) {
            if (!mergeItemStack(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onSlotChange(stack, original);
        } else if (!mergeItemStack(stack, SLOT_MAP, SLOT_RESULT, false)) {
            int hotbarStart = playerEnd - 9;

            if (index < hotbarStart) {
                if (!mergeItemStack(stack, hotbarStart, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!mergeItemStack(stack, playerStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
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
        return original;
    }

    private static class FilledMapSlot extends Slot {

        FilledMapSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.getItem() == Items.FILLED_MAP;
        }
    }

    /** Empty maps to copy onto, or paper to widen with. */
    private static class ModifierSlot extends Slot {

        ModifierSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.getItem() == Items.MAP || stack.getItem() == Items.PAPER;
        }
    }

    /** Taking the result consumes the inputs, and lets vanilla finish a zoom. */
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
            // A zoom is only a request until this runs: onCreated is where vanilla reads the tag and
            // allocates the wider map. A copy carries no such tag, so the call does nothing to it.
            stack.getItem().onCreated(stack, ContainerCartographyTable.this.world, player);

            // One of each, whichever operation it was - a copy hands the original back as part of the
            // result rather than leaving it in the slot.
            ContainerCartographyTable.this.inputs.decrStackSize(SLOT_MAP, 1);
            ContainerCartographyTable.this.inputs.decrStackSize(SLOT_MODIFIER, 1);

            if (!ContainerCartographyTable.this.world.isRemote) {
                ContainerCartographyTable.this.world.playSound(null, ContainerCartographyTable.this.pos,
                        ModSounds.cartographyTableUse, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            return stack;
        }
    }
}
