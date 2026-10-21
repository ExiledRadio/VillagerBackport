package com.exiledradio.villagerbackport.network;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Tells nearby clients that a villager is shaking its head.
 *
 * <p>1.14 keeps a head-shake countdown on the villager as synchronised entity data. 1.12.2's
 * villager has no such field and no room to add one, so the start of a shake is sent instead; the
 * client counts the 40 ticks down itself. Sent only when a villager refuses a player, at most once
 * per shake.
 */
public class PacketVillagerShake implements IMessage {

    private int entityId;

    public PacketVillagerShake() {
    }

    public PacketVillagerShake(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    public int getEntityId() {
        return this.entityId;
    }

    /**
     * Client-side receiver. The client class is named only inside the method body; see
     * {@link PacketVillagerSleep.Handler}.
     */
    public static class Handler implements IMessageHandler<PacketVillagerShake, IMessage> {

        @Override
        public IMessage onMessage(final PacketVillagerShake message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.client.HeadShake.accept(message);
                }
            });

            return null;
        }
    }
}
