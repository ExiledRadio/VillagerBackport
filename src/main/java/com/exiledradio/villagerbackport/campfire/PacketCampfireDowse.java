package com.exiledradio.villagerbackport.campfire;

import com.exiledradio.villagerbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet 18: a campfire was doused with water, so nearby clients draw 1.14's burst of smoke.
 *
 * <p>1.14 spawns the burst on the client as the water flows in. Here water reaches a campfire only
 * through the server - a bucket or a splash bottle - and 1.12.2 has no world event for a smoke plume,
 * so the server says where and the client does the rest. Twenty plumes plus the low wisps, the same
 * as 1.14's {@code placeLiquid}.
 */
public class PacketCampfireDowse implements IMessage {

    private BlockPos pos = BlockPos.ORIGIN;
    private boolean signal;

    public PacketCampfireDowse() {
    }

    public PacketCampfireDowse(BlockPos pos, boolean signal) {
        this.pos = pos;
        this.signal = signal;
    }

    /** Sends the burst to everyone within 64 blocks of the campfire. */
    static void send(World world, BlockPos pos, boolean signal) {
        if (NetworkHandler.channel() == null) {
            return;
        }
        NetworkHandler.channel().sendToAllAround(new PacketCampfireDowse(pos, signal),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(),
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 64.0D));
    }

    public BlockPos pos() {
        return this.pos;
    }

    public boolean signal() {
        return this.signal;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.signal = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeBoolean(this.signal);
    }

    /** Client-side receiver; the client classes are named only inside the method body. */
    public static class Handler implements IMessageHandler<PacketCampfireDowse, IMessage> {

        @Override
        public IMessage onMessage(final PacketCampfireDowse message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.campfire.client.CampfireParticles.dowse(message);
                }
            });
            return null;
        }
    }
}
