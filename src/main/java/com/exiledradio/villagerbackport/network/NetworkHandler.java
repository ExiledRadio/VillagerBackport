package com.exiledradio.villagerbackport.network;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * This mod's network channel.
 *
 * <p>Two messages server to client, both about villagers: the client is told what a villager's
 * experience is, and never asked to report or change anything. Nothing a client says can influence
 * trading, which is the part worth keeping one-directional.
 *
 * <p>One message goes the other way, and it is not about villagers at all - it carries the text
 * someone typed into a draft book on a lectern, which has to originate on the client because that is
 * where the typing happens. It is checked on arrival rather than trusted; see
 * {@link PacketLecternEdit}.
 */
public final class NetworkHandler {

    /**
     * Channel names share a global namespace and are capped at 20 characters, so this uses the mod
     * id rather than something descriptive that might collide with another mod.
     */
    private static final String CHANNEL = VillagerBackport.MOD_ID;

    private static SimpleNetworkWrapper channel;

    private NetworkHandler() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        channel.registerMessage(PacketVillagerData.Handler.class, PacketVillagerData.class, 0, Side.CLIENT);
        channel.registerMessage(PacketVillagerLevel.Handler.class, PacketVillagerLevel.class, 1, Side.CLIENT);
        channel.registerMessage(PacketLecternEdit.Handler.class, PacketLecternEdit.class, 2, Side.SERVER);
        channel.registerMessage(PacketRefillTrade.Handler.class, PacketRefillTrade.class, 3, Side.SERVER);
        channel.registerMessage(PacketVillagerSleep.Handler.class, PacketVillagerSleep.class, 4, Side.CLIENT);
        channel.registerMessage(PacketVillageOutline.Handler.class, PacketVillageOutline.class, 5, Side.CLIENT);
    }

    /**
     * Sends the text of a draft book being written on a lectern.
     *
     * <p>The one message that goes the other way. See {@link PacketLecternEdit} for why vanilla's
     * book-editing message could not be used and what the server checks before believing this one.
     */
    public static void sendLecternEdit(net.minecraft.util.math.BlockPos pos,
                                       net.minecraft.nbt.NBTTagList pages) {
        if (channel != null) {
            channel.sendToServer(new PacketLecternEdit(pos, pages));
        }
    }

    /**
     * Sends a villager's experience to one player.
     *
     * <p>Silently does nothing if the channel is not up yet. A player joining mid-initialisation is
     * not a case worth crashing over, and the next trade screen they open sends the data again.
     */
    public static void sendVillagerData(EntityPlayerMP player, int xp, int level, int[] tradeXp,
                                       int[] tradeUses, int[] basePrices) {
        if (channel != null) {
            channel.sendTo(new PacketVillagerData(xp, level, tradeXp, tradeUses, basePrices), player);
        }
    }

    /**
     * Asks the server to load the selected trade's cost into the merchant input slots.
     *
     * <p>One request rather than a burst of window clicks - see {@link PacketRefillTrade} for why
     * that distinction is the whole point.
     */
    public static void requestRefill(int recipeIndex, boolean replace) {
        if (channel != null) {
            channel.sendToServer(new PacketRefillTrade(recipeIndex, replace));
        }
    }

    /** Sends one villager's level to one player, for the rank badge. */
    public static void sendVillagerLevel(EntityPlayerMP player, Entity villager, int level, int careerId) {
        if (channel != null) {
            channel.sendTo(new PacketVillagerLevel(villager.getEntityId(), level, careerId), player);
        }
    }

    /**
     * Sends a villager's level to everyone nearby.
     *
     * <p>1.12.2's channel has no "everyone tracking this entity" option - that arrived in later
     * versions - so a radius around the villager stands in. Anyone close enough to see the badge
     * change is inside it.
     */
    /** Sends one player the beds of the village around them, for their client to draw. */
    public static void sendVillageOutline(EntityPlayerMP player,
                                          java.util.List<net.minecraft.util.math.BlockPos> beds,
                                          java.util.List<net.minecraft.util.math.BlockPos> claimed,
                                          int seconds) {
        if (channel != null) {
            channel.sendTo(new PacketVillageOutline(beds, claimed, seconds), player);
        }
    }

    /** Tells one player about a villager they have just come within range of. */
    public static void sendVillagerSleepTo(EntityPlayerMP player, Entity villager, boolean sleeping,
                                           net.minecraft.util.math.BlockPos bed) {
        if (channel != null) {
            channel.sendTo(new PacketVillagerSleep(villager.getEntityId(), sleeping,
                    bed.getX(), bed.getY(), bed.getZ()), player);
        }
    }

    /** Tells everyone who can see a villager that it has got into or out of bed. */
    public static void sendVillagerSleep(Entity villager, boolean sleeping,
                                         net.minecraft.util.math.BlockPos bed, double range) {
        if (channel == null) {
            return;
        }

        channel.sendToAllAround(
                new PacketVillagerSleep(villager.getEntityId(), sleeping,
                        bed.getX(), bed.getY(), bed.getZ()),
                new NetworkRegistry.TargetPoint(villager.world.provider.getDimension(),
                        villager.posX, villager.posY, villager.posZ, range));
    }

    public static void sendVillagerLevelNearby(Entity villager, int level, int careerId, double range) {
        if (channel == null) {
            return;
        }

        channel.sendToAllAround(
                new PacketVillagerLevel(villager.getEntityId(), level, careerId),
                new NetworkRegistry.TargetPoint(villager.world.provider.getDimension(),
                        villager.posX, villager.posY, villager.posZ, range));
    }
}
