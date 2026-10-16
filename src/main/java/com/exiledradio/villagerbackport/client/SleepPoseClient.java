package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.home.SleepPose;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client's half of {@link SleepPose}.
 *
 * <p>What a punch can hit is worked out on the client from the client's own copy of the villager,
 * and entity sizes are never sent over the network. So the server shrinking a sleeper changes
 * nothing about where a player can hit it; the client has to shrink its copy too, from what
 * {@link SleepCache} has been told.
 */
@SideOnly(Side.CLIENT)
public final class SleepPoseClient {

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntity() instanceof EntityVillager && event.getEntity().world.isRemote) {
            EntityVillager villager = (EntityVillager) event.getEntity();
            SleepPose.apply(villager, SleepCache.bedOf(villager.getEntityId()) != null);
        }
    }
}
