package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Publishes a villager's sleeping state to the clients that can see it.
 *
 * <p>Three moments cover it: falling asleep, waking, and a player coming close enough to see the
 * bed. That third one is not optional. A villager goes to sleep once and the news goes out once, so
 * without it anybody who arrives afterwards - walks over from the next village, logs back in, or
 * simply renders the chunk late - is told nothing and sees a villager standing bolt upright on its
 * mattress. 1.14 never has to think about this because sleeping is part of the entity's own synced
 * data and arrives with it; here it is a message, and messages only reach whoever was listening.
 */
public final class SleepSync {

    /** Comfortably past entity tracking distance, so everyone who can see the bed is told. */
    private static final double RANGE = 96.0D;

    /**
     * Tells a player about a villager they have just come within range of.
     *
     * <p>Only when it is actually asleep. A villager awake needs no packet - awake is what the
     * client assumes for everything it has not been told about.
     */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EntityVillager)
                || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getTarget();
        BlockPos bed = HomeSite.get(villager);

        if (bed != null && HomeSite.isSleeping(villager)) {
            NetworkHandler.sendVillagerSleepTo((EntityPlayerMP) event.getEntityPlayer(),
                    villager, true, bed);
        }
    }

    public static void broadcast(EntityVillager villager) {
        BlockPos bed = HomeSite.get(villager);
        if (bed == null) {
            return;
        }

        NetworkHandler.sendVillagerSleep(villager, HomeSite.isSleeping(villager), bed, RANGE);
    }
}
