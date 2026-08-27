package com.exiledradio.villagerbackport.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Carries a villager's trading experience to the client that is about to open its trade screen.
 *
 * <p>Everything this mod tracks lives on the server. The client needs the experience total to draw
 * the level and progress bar, and vanilla's merchant screen packet has no room for it - 1.12.2's
 * {@code SPacketOpenWindow} sends a window id, a type string and a title, and the recipe list
 * follows on a separate custom channel. So we send our own.
 *
 * <p>There is deliberately no entity id in the message. The client never learns which villager a
 * merchant screen belongs to: {@code NetHandlerPlayClient.handleOpenWindow} builds a throwaway
 * {@code NpcMerchant} stub rather than resolving the real entity, so an id would be useless for
 * matching. A player can only have one trade screen open at a time, which makes "the villager the
 * player is currently interacting with" an unambiguous target on its own.
 */
public class PacketVillagerData implements IMessage {

    private int xp;

    /**
     * The level actually in effect, which lags experience until the trade screen closes.
     * Sent rather than derived from experience so the rank on screen does not jump ahead of it.
     */
    private int level = 1;

    /**
     * Experience each trade grants, in the same order as the villager's trade list.
     *
     * <p>Sent rather than derived client-side because the amount depends on the tier a trade
     * unlocked at, and only the server knows that - 1.12.2 keeps no per-trade record of it, so this
     * mod tracks it in the villager's own saved data.
     */
    private int[] tradeXp = new int[0];

    /**
     * How many times each trade has been used, in trade-list order.
     *
     * <p>The client keeps its own count, incremented as it predicts each trade, and nothing tells it
     * when the server disagrees - a restock zeroes the real figures without a word. That drift is
     * what let a trade show as sold out while still working, and the reverse. Sending the
     * authoritative counts alongside the experience keeps the lock state honest.
     */
    private int[] tradeUses = new int[0];

    /**
     * What each trade cost before demand moved it, in trade-list order.
     *
     * <p>The client is sent the trade list with the adjusted price already in it and nothing to
     * compare it against, so it cannot tell an expensive trade from a trade that has been made
     * expensive. 1.14's screen shows both - the old price struck out beside the new one - and only
     * the server knows the old one, because the base is what this mod records rather than something
     * the recipe still carries. Zero means the price has not moved.
     */
    private int[] basePrices = new int[0];

    /** Required no-arg constructor for the network layer to instantiate before reading. */
    public PacketVillagerData() {
    }

    public PacketVillagerData(int xp, int level, int[] tradeXp, int[] tradeUses, int[] basePrices) {
        this.xp = xp;
        this.level = level;
        this.tradeXp = tradeXp;
        this.tradeUses = tradeUses;
        this.basePrices = basePrices;
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public int[] getTradeXp() {
        return tradeXp;
    }

    public int[] getTradeUses() {
        return tradeUses;
    }

    public int[] getBasePrices() {
        return basePrices;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.xp = buf.readInt();
        this.level = buf.readInt();

        // Bounded so a malformed or hostile packet cannot make the client allocate an enormous
        // array. No villager has anywhere near this many trades.
        int count = Math.max(0, Math.min(256, buf.readInt()));
        this.tradeXp = new int[count];
        for (int i = 0; i < count; i++) {
            this.tradeXp[i] = buf.readInt();
        }

        int useCount = Math.max(0, Math.min(256, buf.readInt()));
        this.tradeUses = new int[useCount];
        for (int i = 0; i < useCount; i++) {
            this.tradeUses[i] = buf.readInt();
        }

        int baseCount = Math.max(0, Math.min(256, buf.readInt()));
        this.basePrices = new int[baseCount];
        for (int i = 0; i < baseCount; i++) {
            this.basePrices[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.xp);
        buf.writeInt(this.level);
        buf.writeInt(this.tradeXp.length);
        for (int value : this.tradeXp) {
            buf.writeInt(value);
        }

        buf.writeInt(this.tradeUses.length);
        for (int value : this.tradeUses) {
            buf.writeInt(value);
        }

        buf.writeInt(this.basePrices.length);
        for (int value : this.basePrices) {
            buf.writeInt(value);
        }
    }

    /**
     * Client-side receiver.
     *
     * <p>The reference to {@code Minecraft} sits inside the method body rather than in a field or
     * signature on purpose. Registering a handler causes its class to be loaded on both sides, but
     * the JVM only resolves a method-body reference when that method actually runs - which here is
     * client-only. Putting a client class in a field or parameter type would resolve at class load
     * and crash a dedicated server.
     */
    public static class Handler implements IMessageHandler<PacketVillagerData, IMessage> {

        @Override
        public IMessage onMessage(PacketVillagerData message, MessageContext ctx) {
            if (ctx.side != Side.CLIENT) {
                return null;
            }

            // Network messages arrive on a netty thread. Touching client state from there races
            // with rendering, so hand the work back to the main thread.
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.exiledradio.villagerbackport.client.MerchantOverlay.acceptData(message.getXp(), message.getLevel(),
                            message.getTradeXp(), message.getTradeUses(), message.getBasePrices());
                }
            });

            return null;
        }
    }
}
