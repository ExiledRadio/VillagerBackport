package com.exiledradio.villagerbackport.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundEvent;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;

/**
 * A barrel's contents.
 *
 * <h2>Reusing the chest</h2>
 * A barrel holds the same twenty-seven slots as a chest and presents the same screen, so it uses
 * vanilla's {@link ContainerChest} rather than a container of its own. That also means the block can
 * simply call {@code displayGUIChest}, and the client opens the standard screen with no handler and
 * no packet of ours involved.
 *
 * <p>Extending {@link TileEntityLockableLoot} brings loot table support along with it, so a barrel
 * generated in a structure can be filled the way a chest would be. It also makes hoppers work from
 * every side without any extra plumbing - the lid's direction is decoration, not a restriction.
 *
 * <h2>What a barrel is not</h2>
 * It is not a chest. There is no double variant to pair with, no lid to animate, and - the reason it
 * exists at all - nothing has to be able to see the sky above it. Vanilla's chest refuses to open
 * with a solid block on top; a barrel does not care, which is why villages use them in cellars.
 */
public class TileEntityBarrel extends TileEntityLockableLoot {

    private static final int SIZE = 27;

    private NonNullList<ItemStack> contents = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    /** How many players have the screen open, which drives the open state and its sounds. */
    private int viewers;

    @Override
    public int getSizeInventory() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.contents) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.contents;
    }

    /** Vanilla's chest holds a full stack per slot, and so does this. */
    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    private static final String NAME_KEY = "container.villagerbackport.barrel";

    @Override
    public String getName() {
        return hasCustomName() ? this.customName : NAME_KEY;
    }

    /**
     * The title shown on the screen.
     *
     * <p>Resolved here rather than left as a translation key for the client to handle, because in a
     * pack where this mod's language file is not loaded the key is what the player would read. A
     * name given with an anvil still wins, as it should.
     */
    @Override
    public net.minecraft.util.text.ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentString(
                hasCustomName() ? this.customName : Names.translateOr(NAME_KEY, "Barrel"));
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player) {
        fillWithLoot(player);
        return new ContainerChest(playerInventory, this, player);
    }

    @Override
    public String getGuiID() {
        return "minecraft:chest";
    }

    // ------------------------------------------------------------- open state

    /**
     * Tracks viewers so the lid can show open while anyone is looking inside.
     *
     * <p>Counted rather than treated as a flag because two players can have the same barrel open, and
     * the first to close it should not shut the lid on the other.
     */
    @Override
    public void openInventory(EntityPlayer player) {
        if (player.isSpectator()) {
            return;
        }

        if (this.viewers < 0) {
            this.viewers = 0;
        }

        this.viewers++;
        if (this.viewers == 1) {
            setOpen(true, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN);
        }
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (player.isSpectator()) {
            return;
        }

        this.viewers--;
        if (this.viewers <= 0) {
            this.viewers = 0;
            setOpen(false, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE);
        }
    }

    private void setOpen(boolean open, SoundEvent sound) {
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockBarrel) || state.getValue(BlockBarrel.OPEN) == open) {
            return;
        }

        this.world.setBlockState(this.pos, state.withProperty(BlockBarrel.OPEN, open), 3);
        this.world.playSound(null, this.pos, sound, SoundCategory.BLOCKS,
                0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
    }

    /**
     * Keeps this tile entity alive when only the open flag changes.
     *
     * <p>Opening the screen writes the open state into the block, and a block state change normally
     * throws the tile entity away and builds a new one. That is fatal here: the container checks
     * {@code world.getTileEntity(pos) == this} every tick, so the instant the old object was orphaned
     * the screen closed again - which is why the barrel opened and shut without ever being usable.
     *
     * <p>Refreshing only when the block itself changes means the lid can move without the contents
     * being rebuilt underneath it.
     */
    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                 IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // -------------------------------------------------------------------- nbt

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.contents = NonNullList.withSize(SIZE, ItemStack.EMPTY);

        if (!checkLootAndRead(compound)) {
            ItemStackHelper.loadAllItems(compound, this.contents);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        if (!checkLootAndWrite(compound)) {
            ItemStackHelper.saveAllItems(compound, this.contents);
        }

        return compound;
    }
}
