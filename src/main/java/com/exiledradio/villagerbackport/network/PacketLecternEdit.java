package com.exiledradio.villagerbackport.network;

import com.exiledradio.villagerbackport.block.ContainerLectern;
import com.exiledradio.villagerbackport.block.TileEntityLectern;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The text of a draft book being written on a lectern.
 *
 * <h2>Why this needs a message of its own</h2>
 * Vanilla already has one - {@code MC|BEdit} - and it cannot be used here, because it writes to
 * whatever the player is holding. A book on a lectern is not held by anyone, which is the whole point
 * of it being on a lectern, so the target has to be named explicitly.
 *
 * <p>This is the only message in this mod that travels client to server, so it is the only one whose
 * contents a modified client controls. It is treated accordingly: the block has to be a lectern, the
 * player has to have it open, they have to be close enough and allowed to build, and the book has to
 * still be a draft. The size limits are vanilla's own for the same reason vanilla has them - a page
 * list is otherwise a way to make the server store an arbitrary amount of text.
 */
public class PacketLecternEdit implements IMessage {

    /** Vanilla's ceilings for a book, applied here for the same reasons. */
    private static final int MAX_PAGES = 100;
    private static final int MAX_PAGE_LENGTH = 32767;

    private BlockPos pos = BlockPos.ORIGIN;
    private NBTTagList pages = new NBTTagList();

    public PacketLecternEdit() {
    }

    public PacketLecternEdit(BlockPos pos, NBTTagList pages) {
        this.pos = pos;
        this.pages = pages;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());

        NBTTagCompound wrapper = ByteBufUtils.readTag(buf);
        this.pages = wrapper != null ? wrapper.getTagList("pages", 8) : new NBTTagList();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());

        // Wrapped in a compound because the channel can write a tag but not a bare list.
        NBTTagCompound wrapper = new NBTTagCompound();
        wrapper.setTag("pages", this.pages);
        ByteBufUtils.writeTag(buf, wrapper);
    }

    public static class Handler implements IMessageHandler<PacketLecternEdit, IMessage> {

        @Override
        public IMessage onMessage(final PacketLecternEdit message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;

            // Onto the world thread before touching a block. Also keeps this in order with the page
            // turns the screen sends over vanilla's channel, which arrive the same way - so an edit
            // sent just before a page turn is applied before it.
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    apply(message, player);
                }
            });

            return null;
        }

        private static void apply(PacketLecternEdit message, EntityPlayerMP player) {
            if (!(player.openContainer instanceof ContainerLectern)
                    || !player.openContainer.canInteractWith(player)
                    || !player.capabilities.allowEdit) {
                return;
            }

            TileEntity tile = player.world.getTileEntity(message.pos);
            if (!(tile instanceof TileEntityLectern)) {
                return;
            }

            TileEntityLectern lectern = (TileEntityLectern) tile;
            ItemStack book = lectern.getBook();

            // A signed book is finished; nothing may rewrite one.
            if (book.getItem() != Items.WRITABLE_BOOK) {
                return;
            }

            NBTTagList pages = sanitise(message.pages);
            if (pages == null) {
                return;
            }

            ItemStack edited = book.copy();
            edited.setTagInfo("pages", pages);

            lectern.setBook(edited, lectern.getPage());
        }

        /** @return the pages, or null if they are past what a book may hold */
        private static NBTTagList sanitise(NBTTagList pages) {
            if (pages.tagCount() > MAX_PAGES) {
                return null;
            }

            NBTTagList clean = new NBTTagList();
            for (int i = 0; i < pages.tagCount(); i++) {
                String page = pages.getStringTagAt(i);

                if (page.length() > MAX_PAGE_LENGTH) {
                    return null;
                }
                clean.appendTag(new NBTTagString(page));
            }

            return clean;
        }
    }
}
