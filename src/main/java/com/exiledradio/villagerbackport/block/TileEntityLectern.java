package com.exiledradio.villagerbackport.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * The book on a lectern, and which page it is open at.
 *
 * <h2>Why the page is saved</h2>
 * Because it is the output. A lectern's whole redstone contribution is a comparator reading how far
 * through the book the reader has got, so the page is as much part of the block's state as the book
 * is - a lectern that forgot its page on reload would reset every circuit built on it.
 *
 * <p>Both are sent to clients as tile entity data rather than through the open screen, because both
 * are needed by anyone who has not opened it: the book to draw it sitting on the stand, and the page
 * so a second reader opens where the first left off.
 */
public class TileEntityLectern extends TileEntity {

    private ItemStack book = ItemStack.EMPTY;
    private int page;
    private int pages;

    public ItemStack getBook() {
        return this.book;
    }

    /** @return true if what is held is something with pages to turn */
    public boolean hasBook() {
        Item item = this.book.getItem();
        return item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK;
    }

    public int getPage() {
        return this.page;
    }

    public int getPages() {
        return this.pages;
    }

    /**
     * Puts a book on the stand.
     *
     * <p>Does not touch the block state - {@link BlockLectern#tryPlaceBook} does that, because the
     * state has to change in the world whether or not a tile entity happens to be loaded.
     */
    public void setBook(ItemStack stack) {
        setBook(stack, 0);
    }

    /**
     * As {@link #setBook(ItemStack)}, opened at a chosen page.
     *
     * <p>Used when a draft is written on: the text changed but the reader did not turn a page, and
     * snapping back to the first one after every keystroke would make it impossible to write on any
     * other.
     */
    public void setBook(ItemStack stack, int openAt) {
        this.book = stack;
        this.pages = pagesIn(stack);
        this.page = MathHelper.clamp(openAt, 0, Math.max(this.pages - 1, 0));
        sync();
    }

    /** @return the book that was on the stand, leaving it empty */
    public ItemStack removeBook() {
        ItemStack taken = this.book;

        this.book = ItemStack.EMPTY;
        this.page = 0;
        this.pages = 0;
        sync();

        return taken;
    }

    /**
     * Turns to a page, and tells the world about it.
     *
     * <p>The redstone pulse is the point: 1.14 has a lectern emit a brief signal on every page turn,
     * which is what makes a book on a lectern usable as a button that counts.
     */
    public void setPage(int wanted) {
        int clamped = MathHelper.clamp(wanted, 0, Math.max(this.pages - 1, 0));

        if (clamped == this.page) {
            return;
        }

        this.page = clamped;
        sync();

        if (this.world != null && !this.world.isRemote) {
            BlockLectern.pulse(this.world, this.pos, this.world.getBlockState(this.pos));
        }
    }

    /**
     * @return what a comparator beside the lectern should read
     *
     * <p>1.14's formula: the first page reads 1, the last reads 15, and anything between is spread
     * across the range. A lectern with no book reads nothing at all, which is what distinguishes
     * "not started" from "empty".
     */
    public int comparatorLevel() {
        float progress = this.pages > 1 ? this.page / (float) (this.pages - 1) : 1.0F;
        return MathHelper.floor(progress * 14.0F) + (hasBook() ? 1 : 0);
    }

    /** @return how many pages the book has, or 0 if it is not a book */
    private static int pagesIn(ItemStack stack) {
        Item item = stack.getItem();

        if (item != Items.WRITABLE_BOOK && item != Items.WRITTEN_BOOK) {
            return 0;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return 0;
        }

        // Both kinds keep their text in a list of strings under the same key; a written book's are
        // JSON components and a draft's are plain, but either way the count is the page count.
        return tag.getTagList("pages", 8).tagCount();
    }

    private void sync() {
        markDirty();

        if (this.world != null && !this.world.isRemote) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.book = compound.hasKey("Book", 10)
                ? new ItemStack(compound.getCompoundTag("Book"))
                : ItemStack.EMPTY;

        this.pages = pagesIn(this.book);
        this.page = MathHelper.clamp(compound.getInteger("Page"), 0, Math.max(this.pages - 1, 0));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        if (!this.book.isEmpty()) {
            compound.setTag("Book", this.book.writeToNBT(new NBTTagCompound()));
            compound.setInteger("Page", this.page);
        }

        return compound;
    }

    /** Everything here is worth sending, so the update tag is just the save. */
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    /**
     * Keeps the tile entity when only the block state changed - and here it changes constantly, since
     * placing a book, taking it, and every page turn all rewrite the state. Without this the book
     * would be destroyed by the act of turning a page.
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /** The book lies just above the block, so the renderer has to survive the block being culled. */
    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        return new net.minecraft.util.math.AxisAlignedBB(this.pos).grow(1.0D);
    }

    /** Whoever is reading has to still be there, and still be near enough. */
    public boolean isUsableBy(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this
                && hasBook()
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D,
                        this.pos.getZ() + 0.5D) <= 64.0D;
    }
}
