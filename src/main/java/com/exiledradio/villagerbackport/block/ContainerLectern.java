package com.exiledradio.villagerbackport.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The open lectern: no slots, only a page number and four buttons.
 *
 * <h2>A container with nothing in it</h2>
 * There is nothing here to click on or drag - the book itself is drawn from the lectern's tile
 * entity, which every nearby client already has, since it is also what draws the book sitting on the
 * stand. What the container is for is the two channels that come with being one: vanilla's
 * button-press message carries "previous page", "next page" and "take book" from the screen, and the
 * window-property message carries the page back. 1.14's lectern works exactly this way, down to the
 * button numbers.
 *
 * <p>So this mod adds no packets of its own for reading a book, and anything that watches containers
 * sees an ordinary one.
 */
public class ContainerLectern extends Container {

    public static final int BUTTON_PREVIOUS_PAGE = 1;
    public static final int BUTTON_NEXT_PAGE = 2;
    public static final int BUTTON_TAKE_BOOK = 3;

    /** Buttons at or above this are a jump straight to page {@code id - BUTTON_GOTO_PAGE}. */
    public static final int BUTTON_GOTO_PAGE = 100;

    private static final int PROPERTY_PAGE = 0;

    private final TileEntityLectern lectern;

    private int page;
    private int lastSentPage = Integer.MIN_VALUE;

    public ContainerLectern(TileEntityLectern lectern) {
        this.lectern = lectern;
        this.page = lectern.getPage();
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (id >= BUTTON_GOTO_PAGE) {
            turnTo(id - BUTTON_GOTO_PAGE);
            return true;
        }

        switch (id) {
            case BUTTON_PREVIOUS_PAGE:
                turnTo(this.page - 1);
                return true;

            case BUTTON_NEXT_PAGE:
                turnTo(this.page + 1);
                return true;

            case BUTTON_TAKE_BOOK:
                return takeBook(player);

            default:
                return false;
        }
    }

    /**
     * Turns to a page, on whichever side is asking.
     *
     * <p>The client moves without waiting and without checking: a page it has just added by typing
     * exists on screen before the server has been told about it, so clamping against the server's
     * idea of how long the book is would refuse to turn to it. The screen knows the real length and
     * will not offer a page that is not there; the server clamps for real, and its answer arrives as
     * a window property a moment later.
     */
    private void turnTo(int wanted) {
        if (this.lectern.getWorld() != null && this.lectern.getWorld().isRemote) {
            this.page = Math.max(wanted, 0);
            return;
        }

        this.lectern.setPage(wanted);
        this.page = this.lectern.getPage();
        detectAndSendChanges();
    }

    /**
     * Hands the book to whoever asked, and empties the stand.
     *
     * <p>Refused in adventure mode, where a lectern is scenery someone placed on purpose - the same
     * check that stops blocks being broken.
     *
     * <h2>Server only, unlike the page buttons</h2>
     * The screen applies button presses locally before sending them, so a page turns under the cursor
     * instead of after a round trip. That is safe for a page and disastrous for this: taking the book
     * closes the window, and closing it client-side tore down the window <em>before</em> the press was
     * sent - so the server rejected the message as belonging to a window that was no longer open, and
     * never took anything.
     *
     * <p>The result looked like the book had been taken, because the client had emptied its own copy
     * of the lectern and handed itself the book. The server disagreed and, having done nothing, had
     * nothing to correct it with: the stand still held a book that the client believed was in its
     * hand, and could neither be read nor replaced.
     */
    private boolean takeBook(EntityPlayer player) {
        if (player.world.isRemote) {
            return true;
        }

        if (!player.capabilities.allowEdit) {
            return false;
        }

        ItemStack book = this.lectern.removeBook();
        BlockLectern.setHasBook(player.world, this.lectern.getPos(),
                player.world.getBlockState(this.lectern.getPos()), false);

        if (!book.isEmpty() && !player.inventory.addItemStackToInventory(book)) {
            player.dropItem(book, false);
        }

        player.closeScreen();
        return true;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, PROPERTY_PAGE, this.page);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (this.page != this.lastSentPage) {
            for (IContainerListener listener : this.listeners) {
                listener.sendWindowProperty(this, PROPERTY_PAGE, this.page);
            }
            this.lastSentPage = this.page;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == PROPERTY_PAGE) {
            this.page = data;
        }
    }

    @SideOnly(Side.CLIENT)
    public int getPage() {
        return this.page;
    }

    @SideOnly(Side.CLIENT)
    public ItemStack getBook() {
        return this.lectern.getBook();
    }

    /** Which lectern this is, so an edit can name the block it belongs to. */
    @SideOnly(Side.CLIENT)
    public net.minecraft.util.math.BlockPos getLecternPos() {
        return this.lectern.getPos();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.lectern.isUsableBy(player);
    }
}
