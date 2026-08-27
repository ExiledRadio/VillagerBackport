package com.exiledradio.villagerbackport.block;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

/**
 * Strips enchantments off an item and hands back some of the experience.
 *
 * <h2>What it does</h2>
 * Two inputs and one output. Put in a single enchanted item and it comes back plain, with experience
 * orbs for what was removed. Put in two of the same item and they are combined - durability added
 * together plus a five percent bonus - and the result comes out unenchanted too.
 *
 * <p>Curses are never removed. That is the point of a curse, and it is what keeps the grindstone from
 * being a way out of Binding or Vanishing.
 *
 * <h2>How much experience</h2>
 * 1.14's formula, kept exactly: each non-cursed enchantment is worth its
 * {@code getMinEnchantability} at the level it sits at - so a level-four enchantment returns far more
 * than a level-one - and the total is then halved and randomised as
 * {@code ceil(total / 2) + random(ceil(total / 2))}.
 *
 * <p>That means disenchanting is deliberately lossy and variable. It is a way to recover something
 * from an item you do not want, not a way to move enchantments between items.
 */
public class ContainerGrindstone extends Container {

    public static final int SLOT_TOP = 0;
    public static final int SLOT_BOTTOM = 1;
    public static final int SLOT_RESULT = 2;

    private final World world;
    private final BlockPos pos;

    private final IInventory result = new InventoryCraftResult();

    /** The two inputs. Recomputing on change is what keeps the result slot honest. */
    private final IInventory inputs = new InventoryBasic("Grindstone", true, 2) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerGrindstone.this.onCraftMatrixChanged(this);
        }
    };

    public ContainerGrindstone(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;

        addSlot(new InputSlot(this.inputs, SLOT_TOP, 49, 19));
        addSlot(new InputSlot(this.inputs, SLOT_BOTTOM, 49, 40));
        addSlot(new ResultSlot(this.result, SLOT_RESULT, 129, 34));

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

    private void addSlot(Slot slot) {
        addSlotToContainer(slot);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        super.onCraftMatrixChanged(inventory);
        if (inventory == this.inputs) {
            updateResult();
        }
    }

    /**
     * Works out what the two inputs produce, if anything.
     *
     * <p>Follows 1.14 step for step, including its refusals: stacks of more than one are rejected
     * outright, as are two items that are not the same thing, and a lone plain item with nothing to
     * strip produces nothing rather than a pointless copy of itself.
     */
    private void updateResult() {
        ItemStack top = this.inputs.getStackInSlot(SLOT_TOP);
        ItemStack bottom = this.inputs.getStackInSlot(SLOT_BOTTOM);

        boolean any = !top.isEmpty() || !bottom.isEmpty();
        boolean both = !top.isEmpty() && !bottom.isEmpty();

        if (!any) {
            setResult(ItemStack.EMPTY);
            return;
        }

        // A plain, unenchanted item has nothing to give up.
        boolean plain = isPlain(top) || isPlain(bottom);

        if (top.getCount() > 1 || bottom.getCount() > 1 || (!both && plain)) {
            setResult(ItemStack.EMPTY);
            return;
        }

        int damage;
        int count = 1;
        ItemStack base;

        if (both) {
            if (top.getItem() != bottom.getItem()) {
                setResult(ItemStack.EMPTY);
                return;
            }

            // Durability of both, plus five percent of the maximum as a bonus for combining.
            int topLeft = top.getMaxDamage() - top.getItemDamage();
            int bottomLeft = bottom.getMaxDamage() - bottom.getItemDamage();
            int combined = topLeft + bottomLeft + top.getMaxDamage() * 5 / 100;

            damage = Math.max(top.getMaxDamage() - combined, 0);
            base = mergeEnchantments(top, bottom);

            if (!base.getItem().isRepairable()) {
                damage = top.getItemDamage();
            }

            // Something that cannot be repaired can still be stacked, but only if the two are
            // genuinely identical - otherwise there is no sensible single result.
            if (!base.isItemStackDamageable() || !base.getItem().isRepairable()) {
                if (!ItemStack.areItemStacksEqual(top, bottom)) {
                    setResult(ItemStack.EMPTY);
                    return;
                }
                count = 2;
            }
        } else {
            base = top.isEmpty() ? bottom : top;
            damage = base.getItemDamage();
        }

        setResult(strip(base, damage, count));
    }

    /** @return true if the stack is a damageable item carrying nothing worth removing. */
    private static boolean isPlain(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() != Items.ENCHANTED_BOOK
                && !stack.isItemEnchanted();
    }

    /**
     * @return a copy of the first stack carrying the second's non-cursed enchantments as well
     *
     * <p>Only used to work out what is being destroyed, so the experience can be paid for both
     * items - the enchantments are stripped from the result immediately afterwards.
     */
    private static ItemStack mergeEnchantments(ItemStack first, ItemStack second) {
        ItemStack merged = first.copy();
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(merged);

        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(second).entrySet()) {
            Enchantment enchantment = entry.getKey();

            if (!enchantment.isCurse()) {
                continue;
            }

            Integer current = existing.get(enchantment);
            existing.put(enchantment, Math.max(current == null ? 0 : current, entry.getValue()));
        }

        EnchantmentHelper.setEnchantments(existing, merged);
        return merged;
    }

    /**
     * @return the stack with its enchantments removed, curses excepted
     */
    private static ItemStack strip(ItemStack stack, int damage, int count) {
        ItemStack stripped = stack.copy();
        stripped.setItemDamage(damage);
        stripped.setCount(count);

        Map<Enchantment, Integer> kept = new java.util.LinkedHashMap<Enchantment, Integer>();
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stripped).entrySet()) {
            if (entry.getKey().isCurse()) {
                kept.put(entry.getKey(), entry.getValue());
            }
        }

        // Clears the tag outright first: setEnchantments merges rather than replaces, so without
        // this the enchantments being removed would survive.
        stripped.getTagCompound();
        if (stripped.hasTagCompound()) {
            stripped.getTagCompound().removeTag("ench");
            stripped.getTagCompound().removeTag("StoredEnchantments");
        }

        if (!kept.isEmpty()) {
            EnchantmentHelper.setEnchantments(kept, stripped);
        }

        // An enchanted book with nothing left on it is just a book.
        if (stripped.getItem() == Items.ENCHANTED_BOOK && kept.isEmpty()) {
            stripped = new ItemStack(Items.BOOK, count);
        }

        return stripped;
    }

    private void setResult(ItemStack stack) {
        this.result.setInventorySlotContents(0, stack);
        detectAndSendChanges();
    }

    /**
     * @return experience owed for what was destroyed
     *
     * <p>Each non-cursed enchantment is worth what it would cost to apply at that level, halved and
     * randomised. Curses pay nothing, since they are not being removed.
     */
    private int experienceFor() {
        int total = enchantmentValue(this.inputs.getStackInSlot(SLOT_TOP))
                + enchantmentValue(this.inputs.getStackInSlot(SLOT_BOTTOM));

        if (total <= 0) {
            return 0;
        }

        int half = (int) Math.ceil(total / 2.0D);
        return half + this.world.rand.nextInt(half);
    }

    private static int enchantmentValue(ItemStack stack) {
        int value = 0;

        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            if (!entry.getKey().isCurse()) {
                value += entry.getKey().getMinEnchantability(entry.getValue());
            }
        }

        return value;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.world.getBlockState(this.pos).getBlock() instanceof BlockGrindstone
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D,
                        this.pos.getZ() + 0.5D) <= 64.0D;
    }

    /** Inputs go back to the player rather than being lost when the screen closes. */
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
            // Out of the grindstone and into the inventory.
            if (!mergeItemStack(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onSlotChange(stack, original);
        } else if (!mergeItemStack(stack, SLOT_TOP, SLOT_RESULT, false)) {
            // Into the grindstone, or between the inventory and the hotbar if it will not go.
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

    /** Only things that can actually be ground down. */
    private static class InputSlot extends Slot {

        InputSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.isItemStackDamageable()
                    || stack.getItem() == Items.ENCHANTED_BOOK
                    || stack.isItemEnchanted();
        }
    }

    /** Taking the result consumes both inputs and pays out the experience. */
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
            World world = ContainerGrindstone.this.world;
            BlockPos pos = ContainerGrindstone.this.pos;

            if (!world.isRemote) {
                // Split into ordinary orbs rather than granting the total directly, so it behaves
                // like every other experience source in the game.
                int owed = experienceFor();
                while (owed > 0) {
                    int orb = EntityXPOrb.getXPSplit(owed);
                    owed -= orb;
                    world.spawnEntity(new EntityXPOrb(world,
                            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, orb));
                }

                world.playSound(null, pos, ModSounds.grindstoneUse, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            ContainerGrindstone.this.inputs.setInventorySlotContents(SLOT_TOP, ItemStack.EMPTY);
            ContainerGrindstone.this.inputs.setInventorySlotContents(SLOT_BOTTOM, ItemStack.EMPTY);

            return stack;
        }
    }
}
