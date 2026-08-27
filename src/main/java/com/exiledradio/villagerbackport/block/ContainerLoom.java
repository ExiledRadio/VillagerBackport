package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.item.ItemBannerPattern;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.DyeUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Applies one banner pattern: a banner, a dye, and a design picked from a list.
 *
 * <p>A third slot takes a {@link ItemBannerPattern banner pattern}, which is how the four designs
 * that are not in the list are reached. That item is read and not consumed - only the banner and the
 * dye are spent - so one pattern serves any number of banners, which is the whole reason 1.14
 * introduced it.
 *
 * <h2>How the choice travels</h2>
 * The same route the stonecutter uses: clicking a design goes over vanilla's enchantment-button
 * channel, which carries any container's button presses, and the chosen index comes back as a window
 * property. 1.14's loom uses that same channel for the same reason. So there are no packets of this
 * mod's own here.
 *
 * <h2>What the index means</h2>
 * A position in {@link BannerPatterns}, where zero is "nothing chosen" and anything above
 * {@link BannerPatterns#FREE_COUNT} is a design only a pattern item can select. Both of those are
 * 1.14's arrangement - see that class for why the ordering had to be rebuilt rather than taken from
 * the enum.
 */
public class ContainerLoom extends Container {

    public static final int SLOT_BANNER = 0;
    public static final int SLOT_DYE = 1;
    public static final int SLOT_PATTERN = 2;
    public static final int SLOT_RESULT = 3;

    /** Where the player's own inventory starts. The four slots above come first. */
    private static final int PLAYER_START = 4;

    /** Window property id for the chosen design. Numbering is per-container, so 0 is free. */
    private static final int PROPERTY_SELECTED = 0;

    private final World world;
    private final BlockPos pos;

    private final Slot bannerSlot;
    private final Slot dyeSlot;
    private final Slot patternSlot;
    private final Slot resultSlot;

    private int selected = BannerPatterns.NONE;
    private int lastSentSelected = Integer.MIN_VALUE;

    /** Lets the screen rebuild its preview when anything changes. Client only. */
    private Runnable listener;

    private final IInventory inputs = new InventoryBasic("Loom", true, 3) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerLoom.this.onCraftMatrixChanged(this);
            ContainerLoom.this.notifyListener();
        }
    };

    /**
     * The result, kept apart from the inputs so that writing to it cannot re-enter
     * {@link #onCraftMatrixChanged}. It is worked out rather than stored, and never saved anywhere.
     */
    private final IInventory result = new InventoryBasic("LoomResult", true, 1) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerLoom.this.notifyListener();
        }
    };

    public ContainerLoom(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;

        this.bannerSlot = addSlotToContainer(new Slot(this.inputs, SLOT_BANNER, 13, 26) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemBanner;
            }
        });

        this.dyeSlot = addSlotToContainer(new Slot(this.inputs, SLOT_DYE, 33, 26) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return DyeUtils.isDye(stack);
            }
        });

        this.patternSlot = addSlotToContainer(new Slot(this.inputs, SLOT_PATTERN, 23, 45) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemBannerPattern;
            }
        });

        this.resultSlot = addSlotToContainer(new ResultSlot(this.result, 0, 143, 58));

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

    private void notifyListener() {
        if (this.listener != null) {
            this.listener.run();
        }
    }

    @SideOnly(Side.CLIENT)
    public int getSelected() {
        return this.selected;
    }

    @SideOnly(Side.CLIENT)
    public Slot getBannerSlot() {
        return this.bannerSlot;
    }

    @SideOnly(Side.CLIENT)
    public Slot getDyeSlot() {
        return this.dyeSlot;
    }

    @SideOnly(Side.CLIENT)
    public Slot getPatternSlot() {
        return this.patternSlot;
    }

    @SideOnly(Side.CLIENT)
    public Slot getResultSlot() {
        return this.resultSlot;
    }

    /**
     * Works out what the loom can produce now, and forgets the choice when it no longer can.
     *
     * <p>A pattern item overrides whatever was chosen from the list, which is what makes dropping one
     * in switch the display straight to its design without a click. A banner already carrying six
     * patterns takes no more, and clears the choice instead.
     */
    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        ItemStack banner = this.bannerSlot.getStack();
        ItemStack dye = this.dyeSlot.getStack();
        ItemStack pattern = this.patternSlot.getStack();

        boolean usable = !banner.isEmpty() && !dye.isEmpty() && this.selected != BannerPatterns.NONE
                && (BannerPatterns.isFree(this.selected) || !pattern.isEmpty());

        if (this.resultSlot.getStack().isEmpty() || usable) {
            if (pattern.getItem() instanceof ItemBannerPattern) {
                this.selected = isFull(banner)
                        ? BannerPatterns.NONE
                        : BannerPatterns.indexOf(((ItemBannerPattern) pattern.getItem()).getPattern());
            }
        } else {
            this.resultSlot.putStack(ItemStack.EMPTY);
            this.selected = BannerPatterns.NONE;
        }

        updateResult();
        detectAndSendChanges();
    }

    /**
     * Handles a click on one of the designs.
     *
     * <p>Named for enchanting because that is what vanilla built the channel for; it carries any
     * container's button presses. Only the designs in the list are accepted here, so a client cannot
     * ask for one of the four that need a pattern item without holding one.
     */
    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (!BannerPatterns.isFree(id)) {
            return false;
        }

        this.selected = id;
        updateResult();
        detectAndSendChanges();
        return true;
    }

    /** Builds the patterned banner, leaving the result slot empty while anything is missing. */
    private void updateResult() {
        if (this.selected == BannerPatterns.NONE) {
            return;
        }

        ItemStack banner = this.bannerSlot.getStack();
        ItemStack dye = this.dyeSlot.getStack();
        ItemStack output = ItemStack.EMPTY;

        int color = DyeUtils.rawDyeDamageFromStack(dye);

        if (!banner.isEmpty() && color != -1) {
            output = banner.copy();
            output.setCount(1);

            BannerPattern pattern = BannerPatterns.byIndex(this.selected);
            NBTTagCompound blockEntity = output.getOrCreateSubCompound("BlockEntityTag");
            NBTTagList patterns;

            if (blockEntity.hasKey("Patterns", 9)) {
                patterns = blockEntity.getTagList("Patterns", 10);
            } else {
                patterns = new NBTTagList();
                blockEntity.setTag("Patterns", patterns);
            }

            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Pattern", pattern.getHashname());
            entry.setInteger("Color", color);
            patterns.appendTag(entry);
        }

        // Compared before writing so an unchanged result does not mark the slot dirty on every
        // change, which would have the screen rebuild its banner texture for nothing.
        if (!ItemStack.areItemStacksEqual(output, this.resultSlot.getStack())) {
            this.resultSlot.putStack(output);
        }
    }

    /** @return true if this banner already carries as many patterns as one can hold */
    private static boolean isFull(ItemStack banner) {
        return !banner.isEmpty() && TileEntityBanner.getPatterns(banner) >= BannerPatterns.MAX_PATTERNS;
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
        return this.world.getBlockState(this.pos).getBlock() instanceof BlockLoom
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D,
                        this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.result.removeStackFromSlot(0);

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
        int playerEnd = this.inventorySlots.size();
        int hotbarStart = playerEnd - 9;

        if (index == SLOT_RESULT) {
            if (!mergeItemStack(stack, PLAYER_START, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onSlotChange(stack, original);
        } else if (index == SLOT_BANNER || index == SLOT_DYE || index == SLOT_PATTERN) {
            if (!mergeItemStack(stack, PLAYER_START, playerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemBanner) {
            // Each ingredient knows which slot it belongs in, so shift-clicking any of the three
            // from the inventory does the obvious thing rather than shuffling it around the bar.
            if (!mergeItemStack(stack, SLOT_BANNER, SLOT_BANNER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DyeUtils.isDye(stack)) {
            if (!mergeItemStack(stack, SLOT_DYE, SLOT_DYE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ItemBannerPattern) {
            if (!mergeItemStack(stack, SLOT_PATTERN, SLOT_PATTERN + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < hotbarStart) {
            if (!mergeItemStack(stack, hotbarStart, playerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!mergeItemStack(stack, PLAYER_START, hotbarStart, false)) {
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

    /**
     * Taking the banner spends the banner and the dye, and nothing else.
     *
     * <p>The pattern item stays where it is, which is what lets one design be applied to a whole
     * chest of banners. Emptying either of the other two clears the choice, so the loom does not go
     * on offering a design it can no longer produce.
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
            ContainerLoom.this.bannerSlot.decrStackSize(1);
            ContainerLoom.this.dyeSlot.decrStackSize(1);

            if (!ContainerLoom.this.bannerSlot.getHasStack()
                    || !ContainerLoom.this.dyeSlot.getHasStack()) {
                ContainerLoom.this.selected = BannerPatterns.NONE;
            }

            if (!ContainerLoom.this.world.isRemote) {
                ContainerLoom.this.world.playSound(null, ContainerLoom.this.pos,
                        ModSounds.loomTakeResult, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }

            return super.onTake(player, stack);
        }
    }
}
