package com.exiledradio.villagerbackport.network;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Tells nearby clients that a villager has got into or out of bed.
 *
 * <p>Sleeping is server-side state with no route to the client of its own - it lives in the
 * villager's Forge data, which is never transmitted - and it has to reach the client because that
 * is where the villager is drawn lying down. 1.14 has no such packet because sleeping is a pose on
 * the entity itself, synchronised like any other; 1.12.2 has no pose to use.
 *
 * <p>Sent only when a villager falls asleep or wakes, which is twice a day per villager.
 */
public class PacketVillagerSleep implements IMessage {

    private int entityId;
    private boolean sleeping;
    private int bedX;
    private int bedY;
    private int bedZ;

    public PacketVillagerSleep() {
    }

    public PacketVillagerSleep(int entityId, boolean sleeping, int bedX, int bedY, int bedZ) {
        this.entityId = entityId;
        this.sleeping = sleeping;
        this.bedX = bedX;
        this.bedY = bedY;
        this.bedZ = bedZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.sleeping = buf.readBoolean();
        this.bedX = buf.readInt();
        this.bedY = buf.readInt();
        this.bedZ = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeBoolean(this.sleeping);
        buf.writeInt(this.bedX);
        buf.writeInt(this.bedY);
        buf.writeInt(this.bedZ);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public boolean isSleeping() {
        return this.sleeping;
    }

    public int getBedX() {
        return this.bedX;
    }

    public int getBedY() {
        return this.bedY;
    }

    public int getBedZ() {
        return this.bedZ;
    }

    /**
     * Client-side receiver.
     *
     * <p>The reference to the client cache sits inside the method body rather than in a field or a
     * signature, for the reason spelled out on {@link PacketVillagerData}: a client-only type in a
     * method descriptor is resolved when the class loads and crashes a dedicated server.
     */
    public static class Handler implements IMessageHandler<PacketVillagerSleep, IMessage> {

        @Override
        public IMessage onMessage(final PacketVillagerSleep message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.client.SleepCache.accept(message);
                }
            });

            return null;
        }
    }
}
