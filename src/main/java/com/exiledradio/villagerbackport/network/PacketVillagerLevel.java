package com.exiledradio.villagerbackport.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Tells a client what level a villager it can see has reached.
 *
 * <h2>Why this is separate from {@link PacketVillagerData}</h2>
 * That one is scoped to an open trade screen and carries no entity id, because the client never
 * learns which villager a merchant screen belongs to - vanilla hands it a throwaway stub. The level
 * badge has the opposite requirement: it has to be drawn on every villager in view, whether or not
 * anyone is trading with them, so each message has to say which villager it is about.
 *
 * <p>Sent when a player starts tracking a villager and again whenever one levels up. Villagers level
 * rarely and are tracked once on approach, so this is not periodic traffic.
 */
public class PacketVillagerLevel implements IMessage {

    private int entityId;
    private int level;

    /**
     * The villager's career, which the client has no other way to learn.
     *
     * <p>{@code EntityVillager} publishes its profession through a data parameter but not its
     * career, and it is the career that decides which 1.14 outfit a villager wears - one profession
     * covers up to four of them. Zero means unemployed, which renders as plain biome clothing.
     */
    private int careerId;

    /** Required no-arg constructor for the network layer to instantiate before reading. */
    public PacketVillagerLevel() {
    }

    public PacketVillagerLevel(int entityId, int level, int careerId) {
        this.entityId = entityId;
        this.level = level;
        this.careerId = careerId;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getLevel() {
        return level;
    }

    public int getCareerId() {
        return careerId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.level = buf.readInt();
        this.careerId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.level);
        buf.writeInt(this.careerId);
    }

    /**
     * Client-side receiver.
     *
     * <p>As with the other packet, the client class is named only inside the method body so the
     * handler can be registered on a dedicated server without resolving anything client-only.
     */
    public static class Handler implements IMessageHandler<PacketVillagerLevel, IMessage> {

        @Override
        public IMessage onMessage(PacketVillagerLevel message, MessageContext ctx) {
            if (ctx.side != Side.CLIENT) {
                return null;
            }

            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.client.VillagerLevelCache.accept(
                            message.getEntityId(), message.getLevel(), message.getCareerId());
                }
            });

            return null;
        }
    }
}
