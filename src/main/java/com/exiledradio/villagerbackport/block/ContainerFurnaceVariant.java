package com.exiledradio.villagerbackport.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * A furnace screen whose input slot refuses what the block cannot cook.
 *
 * <h2>Why the tile entity's own check is not enough</h2>
 * {@code TileEntityFurnace.isItemValidForSlot} governs hoppers, but not the screen: vanilla's
 * {@link ContainerFurnace} builds its input as a plain {@link Slot}, and a plain slot accepts
 * anything. Without this a player could put iron in a smoker by hand while a hopper could not, which
 * is both inconsistent and the wrong way round.
 *
 * <p>Rather than rebuild the container, the existing input slot is replaced in place - everything
 * else about the screen, including quick-move behaviour and the fuel and result slots, stays
 * vanilla's.
 */
public class ContainerFurnaceVariant extends ContainerFurnace {

    private static final int INPUT_SLOT = 0;

    public ContainerFurnaceVariant(InventoryPlayer playerInventory, IInventory furnace) {
        super(playerInventory, furnace);

        Slot original = this.inventorySlots.get(INPUT_SLOT);
        Slot filtered = new FilteredInput(furnace, original.getSlotIndex(), original.xPos, original.yPos,
                furnace instanceof TileEntityFurnaceVariant
                        ? ((TileEntityFurnaceVariant) furnace).kind()
                        : FurnaceKind.SMOKER);

        filtered.slotNumber = INPUT_SLOT;
        this.inventorySlots.set(INPUT_SLOT, filtered);
    }

    private static class FilteredInput extends Slot {

        private final FurnaceKind kind;

        FilteredInput(IInventory inventory, int index, int x, int y, FurnaceKind kind) {
            super(inventory, index, x, y);
            this.kind = kind;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return this.kind.accepts(stack);
        }
    }
}
