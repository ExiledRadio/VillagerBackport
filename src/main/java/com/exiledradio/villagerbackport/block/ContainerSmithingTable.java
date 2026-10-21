package com.exiledradio.villagerbackport.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
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

/**
 * 1.16's netherite upgrade: a diamond item and a netherite ingot make the netherite item.
 *
 * <p>Ported from 1.16's {@code SmithingTableContainer}. The result keeps everything the diamond item
 * carried - enchantments, name, repair cost - and its damage, because 1.16 copies the whole tag
 * across and damage lives in the tag there. In 1.12.2 damage is the metadata, so it is copied
 * separately. None of the netherite mods' own tables or crafting recipes do this; they all hand back
 * a fresh item.
 */
public class ContainerSmithingTable extends Container {

    public static final int SLOT_BASE = 0;
    public static final int SLOT_ADDITION = 1;
    public static final int SLOT_RESULT = 2;

    private final World world;
    private final BlockPos pos;

    private final IInventory result = new InventoryCraftResult();

    private final InventoryBasic inputs = new InventoryBasic("Smithing", true, 2) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerSmithingTable.this.onCraftMatrixChanged(this);
        }
    };

    public ContainerSmithingTable(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;

        // 1.16's positions.
        addSlotToContainer(new Slot(this.inputs, SLOT_BASE, 27, 47));
        addSlotToContainer(new Slot(this.inputs, SLOT_ADDITION, 76, 47));
        addSlotToContainer(new ResultSlot(this.result, SLOT_RESULT, 134, 47));

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
        this.result.setInventorySlotContents(0, upgrade(
                this.inputs.getStackInSlot(SLOT_BASE), this.inputs.getStackInSlot(SLOT_ADDITION)));
        detectAndSendChanges();
    }

    /** @return what these two inputs make, or empty */
    public static ItemStack upgrade(ItemStack base, ItemStack addition) {
        SmithingUpgrades.Upgrade upgrade = SmithingUpgrades.find(base, addition);
        if (upgrade == null) {
            return ItemStack.EMPTY;
        }

        ItemStack out = upgrade.result();

        NBTTagCompound tag = base.getTagCompound();
        if (tag != null) {
            NBTTagCompound merged = out.hasTagCompound() ? out.getTagCompound().copy() : new NBTTagCompound();
            merged.merge(tag);
            out.setTagCompound(merged);
        }

        if (out.isItemStackDamageable() && base.isItemStackDamageable()) {
            out.setItemDamage(Math.min(base.getItemDamage(), out.getMaxDamage() - 1));
        }

        return out;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.world.getBlockState(this.pos).getBlock() instanceof BlockSmithingTable
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
        int hotbarStart = playerEnd - 9;

        if (index < playerStart) {
            if (!mergeItemStack(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onSlotChange(stack, original);
        } else {
            // An upgradable item to the first slot, an ingot to the second, as 1.16 sorts them.
            boolean moved = false;
            if (SmithingUpgrades.isUpgradable(stack)) {
                moved = mergeItemStack(stack, SLOT_BASE, SLOT_BASE + 1, false);
            } else if (SmithingUpgrades.isIngot(stack) || acceptedAsAddition(stack)) {
                moved = mergeItemStack(stack, SLOT_ADDITION, SLOT_ADDITION + 1, false);
            }

            if (!moved) {
                if (index < hotbarStart) {
                    if (!mergeItemStack(stack, hotbarStart, playerEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!mergeItemStack(stack, playerStart, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
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

    /** True if the item in the first slot would take this as its addition. */
    private boolean acceptedAsAddition(ItemStack stack) {
        return SmithingUpgrades.find(this.inputs.getStackInSlot(SLOT_BASE), stack) != null;
    }

    /** Taking the result uses up one of each input, with 1.16's hammering sound. */
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
            stack.onCrafting(player.world, player, stack.getCount());

            ContainerSmithingTable.this.inputs.decrStackSize(SLOT_BASE, 1);
            ContainerSmithingTable.this.inputs.decrStackSize(SLOT_ADDITION, 1);

            World world = ContainerSmithingTable.this.world;
            if (!world.isRemote) {
                world.playSound(null, ContainerSmithingTable.this.pos, ModSounds.smithingTableUse,
                        SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
            }

            return stack;
        }
    }
}
