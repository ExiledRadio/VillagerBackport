package com.exiledradio.villagerbackport.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The beds of a village, sent to one player so their client can draw them.
 *
 * <p>Worked out on the server because that is where the index and the villagers' claims live, and
 * drawn on the client because that is where a picture can be. Sent once, when asked for, and shown
 * for a while - nothing about it refreshes, so a village that changes while the outline is up wants
 * the command running again.
 */
public class PacketVillageOutline implements IMessage {

    /** Guards against a malformed packet asking the client to allocate something enormous. */
    private static final int MAX_BEDS = 2048;

    private List<BlockPos> beds = new ArrayList<BlockPos>();
    private List<BlockPos> claimed = new ArrayList<BlockPos>();
    private int seconds;

    public PacketVillageOutline() {
    }

    public PacketVillageOutline(List<BlockPos> beds, List<BlockPos> claimed, int seconds) {
        this.beds = beds;
        this.claimed = claimed;
        this.seconds = seconds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.seconds = buf.readInt();
        this.beds = readPositions(buf);
        this.claimed = readPositions(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.seconds);
        writePositions(buf, this.beds);
        writePositions(buf, this.claimed);
    }

    private static List<BlockPos> readPositions(ByteBuf buf) {
        int count = Math.max(0, Math.min(MAX_BEDS, buf.readInt()));
        List<BlockPos> positions = new ArrayList<BlockPos>(count);

        for (int i = 0; i < count; i++) {
            positions.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
        }

        return positions;
    }

    private static void writePositions(ByteBuf buf, List<BlockPos> positions) {
        int count = Math.min(MAX_BEDS, positions.size());
        buf.writeInt(count);

        for (int i = 0; i < count; i++) {
            BlockPos pos = positions.get(i);
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }
    }

    public List<BlockPos> getBeds() {
        return this.beds;
    }

    public List<BlockPos> getClaimed() {
        return this.claimed;
    }

    public int getSeconds() {
        return this.seconds;
    }

    /** Client-side receiver. See {@link PacketVillagerData} on why the client type is not named here. */
    public static class Handler implements IMessageHandler<PacketVillageOutline, IMessage> {

        @Override
        public IMessage onMessage(final PacketVillageOutline message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.client.VillageOutlineRenderer.show(message);
                }
            });

            return null;
        }
    }
}
