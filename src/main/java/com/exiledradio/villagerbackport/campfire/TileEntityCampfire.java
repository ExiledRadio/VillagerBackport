package com.exiledradio.villagerbackport.campfire;

import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * A campfire's four cooking spots, ported from 1.14's {@code CampfireTileEntity}.
 *
 * <h2>Cooking</h2>
 * Right-clicking with something {@link CampfireRecipes} accepts puts one of it in the first empty spot,
 * each with its own timer (600 ticks, {@code campfire.cookTime}). Only a lit campfire cooks. When a
 * timer runs out the cooked item pops off into the world, as 1.14's does - there is no taking it back
 * by hand, and a hopper cannot reach the spots: 1.14 deliberately gave the campfire no inventory, so
 * this is not an {@code IInventory} either. A hopper underneath still collects what pops off.
 *
 * <h2>Going out</h2>
 * Put out, the food stays where it is and its timer runs backwards at twice the speed it ran forward,
 * never below zero; relit, it picks up from wherever that left it. Current Java keeps the food on an
 * extinguished campfire (1.14.4 threw it off, a bug fixed in 20w46a).
 *
 * <h2>The client</h2>
 * The four items are sent with the block's update packet, for the renderer to draw lying on the logs.
 * Timers stay on the server: the client has no use for them. The smoke is spawned from the client's
 * tick, as 1.14 spawns it. The call into {@code client.CampfireParticles} takes only common types,
 * so a dedicated server verifying this class never has to load it (see {@code ClientSetup}).
 */
public class TileEntityCampfire extends TileEntity implements ITickable {

    /** 1.14 has four spots, one per side of the fire. */
    public static final int SLOTS = 4;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final int[] cookingTimes = new int[SLOTS];
    private final int[] cookingTotalTimes = new int[SLOTS];

    public NonNullList<ItemStack> items() {
        return this.items;
    }

    /** @return the cooking progress in one spot, in ticks; for tests */
    public int cookingTime(int slot) {
        return this.cookingTimes[slot];
    }

    @Override
    public void update() {
        IBlockState state = this.world.getBlockState(this.pos);
        boolean lit = BlockCampfire.isLit(state);
        if (this.world.isRemote) {
            if (lit) {
                com.exiledradio.villagerbackport.campfire.client.CampfireParticles.tick(this.world, this.pos, state, this.items);
            }
            return;
        }
        if (lit) {
            cook();
        } else {
            for (int i = 0; i < SLOTS; i++) {
                if (this.cookingTimes[i] > 0) {
                    this.cookingTimes[i] = MathHelper.clamp(this.cookingTimes[i] - 2, 0, this.cookingTotalTimes[i]);
                }
            }
        }
    }

    /** 1.14's {@code cook}: one tick for every occupied spot, and anything done pops off. */
    private void cook() {
        boolean cooking = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            cooking = true;
            this.cookingTimes[i]++;
            if (this.cookingTimes[i] >= this.cookingTotalTimes[i]) {
                ItemStack result = CampfireRecipes.result(stack);
                // Anything the recipes stopped accepting since it went on comes back off raw.
                ItemStack out = result.isEmpty() ? stack : result;
                InventoryHelper.spawnItemStack(this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), out);
                this.items.set(i, ItemStack.EMPTY);
                markUpdated();
            }
        }
        if (cooking) {
            // 1.12.2 saves a chunk only when told it changed; the timers are worth keeping.
            this.world.markChunkDirty(this.pos, this);
        }
    }

    /**
     * 1.14's {@code placeFood}: one of the stack into the first empty spot.
     *
     * @param stack the player's stack, shrunk by one on success (pass a copy for creative)
     * @return false if all four spots are full
     */
    public boolean placeFood(ItemStack stack, int cookTime) {
        for (int i = 0; i < SLOTS; i++) {
            if (this.items.get(i).isEmpty()) {
                this.cookingTotalTimes[i] = cookTime;
                this.cookingTimes[i] = 0;
                this.items.set(i, stack.splitStack(1));
                markUpdated();
                return true;
            }
        }
        return false;
    }

    /** Put out: nothing is thrown off any more (see the class notes), but the client is told. */
    public void dowse() {
        markUpdated();
    }

    private void markUpdated() {
        markDirty();
        if (this.world != null) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    /** @return which way spot {@code slot} faces: 1.14 counts round from the block's own facing */
    public static EnumFacing slotDirection(EnumFacing facing, int slot) {
        return EnumFacing.byHorizontalIndex(Math.floorMod(slot + facing.getHorizontalIndex(), 4));
    }

    /**
     * The block's state changing (lit, out, hay underneath) must not throw away the tile entity and
     * the food on it; only a different block may.
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // ---------------------------------------------------------------------------------------------
    // Saving and syncing - 1.14's names, so a converted world would read the same tags
    // ---------------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readItems(tag);
        int[] times = tag.getIntArray("CookingTimes");
        System.arraycopy(times, 0, this.cookingTimes, 0, Math.min(times.length, SLOTS));
        int[] totals = tag.getIntArray("CookingTotalTimes");
        System.arraycopy(totals, 0, this.cookingTotalTimes, 0, Math.min(totals.length, SLOTS));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeItems(tag);
        tag.setIntArray("CookingTimes", this.cookingTimes.clone());
        tag.setIntArray("CookingTotalTimes", this.cookingTotalTimes.clone());
        return tag;
    }

    private void readItems(NBTTagCompound tag) {
        for (int i = 0; i < SLOTS; i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        net.minecraft.inventory.ItemStackHelper.loadAllItems(tag, this.items);
    }

    private void writeItems(NBTTagCompound tag) {
        // true: write the list even when empty, so the client hears that an item has gone.
        net.minecraft.inventory.ItemStackHelper.saveAllItems(tag, this.items, true);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        writeItems(tag);
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        // 1.14 uses id 13 for the campfire; in 1.12.2 any id past vanilla's own reaches onDataPacket.
        return new SPacketUpdateTileEntity(this.pos, 13, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readItems(packet.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readItems(tag);
    }
}
