package com.exiledradio.villagerbackport.flora;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Tells a client whether a mooshroom is brown. Packet 19 on this mod's channel.
 *
 * <p>The type lives in the mooshroom's Forge data on the server (see {@link Mooshrooms}), which is
 * saved with it but never sent anywhere; the client needs it only to pick a texture. Sent to a player
 * when they start tracking a mooshroom, and to everyone tracking it when it changes.
 */
public class PacketMooshroomType implements IMessage {

    private int entityId;
    private boolean brown;

    public PacketMooshroomType() {
    }

    public PacketMooshroomType(int entityId, boolean brown) {
        this.entityId = entityId;
        this.brown = brown;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.brown = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeBoolean(this.brown);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public boolean isBrown() {
        return this.brown;
    }

    public static class Handler implements IMessageHandler<PacketMooshroomType, IMessage> {

        @Override
        public IMessage onMessage(final PacketMooshroomType message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.flora.client.MooshroomClient.accept(message);
                }
            });
            return null;
        }
    }
}
