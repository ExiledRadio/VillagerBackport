package com.exiledradio.villagerbackport.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * A villager's trade list, sent again to the player whose trade screen is open on it: 26.3's
 * {@code sendMerchantOffers} after {@code updateTrades}, so a level's new trades appear without closing the screen.
 *
 * <p>1.12.2 sends the list once, on {@code MC|TrList}, and its client takes it only while the screen is a
 * {@code GuiMerchant} - this mod's own trade screen is not one. So the list goes on this mod's channel and the client
 * hands it to whichever merchant screen is open with that window id.
 *
 * <p>The list is kept as bytes until the client reads it: {@code MerchantRecipeList.readFromBuf} is client-only.
 */
public class PacketTradeList implements IMessage {

    private int windowId;
    private byte[] data = new byte[0];

    public PacketTradeList() {
    }

    public PacketTradeList(int windowId, MerchantRecipeList recipes) {
        this.windowId = windowId;
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        recipes.writeToBuf(buf);
        this.data = new byte[buf.readableBytes()];
        buf.readBytes(this.data);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        // Bounded: a trade list is a few kilobytes at most.
        int length = Math.max(0, Math.min(1 << 20, buf.readInt()));
        this.data = new byte[Math.min(length, buf.readableBytes())];
        buf.readBytes(this.data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeInt(this.data.length);
        buf.writeBytes(this.data);
    }

    /** Client-side receiver; client classes only inside the method body, as in {@link PacketVillagerData}. */
    public static class Handler implements IMessageHandler<PacketTradeList, IMessage> {

        @Override
        public IMessage onMessage(final PacketTradeList message, MessageContext ctx) {
            if (ctx.side != Side.CLIENT) {
                return null;
            }
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.client.MerchantOverlay.acceptTradeList(message.windowId, message.data);
                }
            });
            return null;
        }
    }
}
