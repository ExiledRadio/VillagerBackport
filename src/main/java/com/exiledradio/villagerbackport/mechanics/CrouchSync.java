package com.exiledradio.villagerbackport.mechanics;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The short crouch has to be the same on the server and on each player's own client, which both work the pose out
 * (a 1.5 box on one side and 1.65 on the other is a player the server keeps pulling back). The server's
 * {@link ModConfig.Mechanics#shortCrouch} counts: it is sent to each player who joins, and again if it changes. A
 * client on a server without this mod never hears it and keeps 1.12.2's crouch.
 */
public final class CrouchSync {

    /** Network id, after the 1.14 completion streams' 10-19. */
    public static final int PACKET_ID = 20;

    /** Client side: what the server said; null when not connected to a server that runs this mod. */
    private static volatile Boolean serverSays;

    private static boolean lastSent;

    private CrouchSync() {
    }

    static void init() {
        MinecraftForge.EVENT_BUS.register(new CrouchSync.Events());
    }

    public static void register() {
        NetworkHandler.channel().registerMessage(Handler.class, Packet.class, PACKET_ID, Side.CLIENT);
    }

    /** @return the short crouch on this thread's side: the server's setting, and on a client what the server sent */
    static boolean active() {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            return ModConfig.mechanics.shortCrouch;
        }
        Boolean says = serverSays;
        return says != null && says;
    }

    public static final class Events {

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP && NetworkHandler.channel() != null) {
                NetworkHandler.channel().sendTo(new Packet(ModConfig.mechanics.shortCrouch), (EntityPlayerMP) event.player);
            }
        }

        /** Once a second: a changed setting reaches everyone. */
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null || server.getTickCounter() % 20 != 0) {
                return;
            }
            boolean now = ModConfig.mechanics.shortCrouch;
            if (now != lastSent) {
                lastSent = now;
                if (NetworkHandler.channel() != null) {
                    NetworkHandler.channel().sendToAll(new Packet(now));
                }
            }
        }

        @SubscribeEvent
        public void onServerStarted(net.minecraftforge.event.world.WorldEvent.Load event) {
            if (!event.getWorld().isRemote) {
                lastSent = ModConfig.mechanics.shortCrouch;
            }
        }

        @SubscribeEvent
        public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
            serverSays = null;
        }
    }

    public static final class Packet implements IMessage {
        boolean on;

        public Packet() {
        }

        Packet(boolean on) {
            this.on = on;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.on = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeBoolean(this.on);
        }
    }

    public static final class Handler implements IMessageHandler<Packet, IMessage> {
        @Override
        public IMessage onMessage(Packet message, MessageContext ctx) {
            final boolean on = message.on;
            Minecraft.getMinecraft().addScheduledTask(() -> serverSays = on);
            return null;
        }
    }
}
