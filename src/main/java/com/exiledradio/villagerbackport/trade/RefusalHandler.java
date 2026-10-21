package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


/**
 * The "no" a villager gives when it has nothing to trade.
 *
 * <p>1.14 answers a right-click from a villager that has no offers, or from a child, by shaking its
 * head and making the villager's disagreeing noise. That covers nitwits, unemployed villagers and
 * babies. 1.12.2 does nothing at all for any of them, which reads as the click not having landed.
 *
 * <p>The head shake is drawn by the client ({@code HeadShake}), told by a packet, since 1.12.2's
 * villager has no field to carry it. Every click answers, as in 1.14 and since: each one says no
 * again and restarts the 40-tick shake, so holding the button down gives a steady string of "no"s
 * and a head that keeps shaking until it is let go.
 */
public final class RefusalHandler {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND
                || !(event.getTarget() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getTarget();

        // Name tags and spawn eggs do their own thing before trading is ever considered, in both
        // versions. A sleeping villager is BedInteraction's to answer.
        Item held = event.getItemStack().getItem();
        if (held == Items.NAME_TAG || held == Items.SPAWN_EGG || !villager.isEntityAlive()
                || villager.getCustomer() != null || HomeSite.isSleeping(villager)) {
            return;
        }

        if (!villager.isChild() && !Employment.isNitwit(villager) && !Employment.isUnemployed(villager)) {
            return;
        }

        // A villager's own pitch: a little random, and higher for a child.
        float pitch = (villager.getRNG().nextFloat() - villager.getRNG().nextFloat()) * 0.2F
                + (villager.isChild() ? 1.5F : 1.0F);

        villager.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0F, pitch);
        NetworkHandler.sendVillagerShake(villager, 64.0D);
    }
}
