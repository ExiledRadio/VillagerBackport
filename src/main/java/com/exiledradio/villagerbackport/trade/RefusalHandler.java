package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.job.Employment;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The "no" a villager gives when it has nothing to trade.
 *
 * <p>1.14 answers a right-click from a villager that has no offers, or from a child, by shaking its
 * head and making the villager's disagreeing noise. That covers nitwits, unemployed villagers and
 * babies. 1.12.2 does nothing at all for any of them, which reads as the click not having landed.
 *
 * <p>Only the sound is reproduced. The head shake is a model animation 1.12.2's villager has no
 * field or renderer support for. The 40 ticks it lasts are kept, though, because in 1.14 a shaking
 * villager ignores further clicks: so a player clicking repeatedly hears one "no", not one per click.
 */
public final class RefusalHandler {

    /** How long 1.14's head shake lasts, during which it will not say no again. */
    private static final long SHAKE_TICKS = 40L;

    private static final Map<EntityVillager, Long> LAST_REFUSED = new WeakHashMap<EntityVillager, Long>();

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

        long now = villager.world.getTotalWorldTime();
        Long last = LAST_REFUSED.get(villager);
        if (last != null && now >= last.longValue() && now < last.longValue() + SHAKE_TICKS) {
            return;
        }
        LAST_REFUSED.put(villager, Long.valueOf(now));

        // A villager's own pitch: a little random, and higher for a child.
        float pitch = (villager.getRNG().nextFloat() - villager.getRNG().nextFloat()) * 0.2F
                + (villager.isChild() ? 1.5F : 1.0F);

        villager.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0F, pitch);
    }
}
