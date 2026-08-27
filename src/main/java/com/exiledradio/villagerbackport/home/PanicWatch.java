package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;

import com.google.common.base.Predicate;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Frightened villagers calling for a golem.
 *
 * <h2>1.14's trigger</h2>
 * {@code PanicTask} runs while a villager can see a hostile or has been hurt, and every hundred
 * ticks of that it asks for an iron golem needing three eligible villagers. That is the whole of it:
 * the fright does not spawn anything by itself, it only asks, and the answer depends on whether the
 * village has villagers who have slept and worked recently.
 *
 * <p>1.12.2 has no brain and no hostile sensor, so the two conditions are read directly: something
 * hostile close enough to be worth panicking about, or something that has just hurt this villager.
 * The hundred-tick rhythm is 1.14's own, and it is what keeps a village under siege from asking on
 * every tick.
 */
public final class PanicWatch {

    /** 1.14 asks once per hundred ticks while panicking, and so does this. */
    private static final int ASK_INTERVAL = 100;

    /**
     * How close a hostile has to be to frighten a villager.
     *
     * <p>1.14 sets this per mob type in its hostile sensor - eight blocks for zombies, further for
     * illagers. Eight covers the case this exists for, which is a zombie at the door.
     */
    private static final double FRIGHT_RANGE = 8.0D;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();

        if (villager.world.isRemote || !ModConfig.homes.enabled || !ModConfig.homes.golemSpawning) {
            return;
        }

        if (villager.isChild() || villager.ticksExisted % ASK_INTERVAL != 0) {
            return;
        }

        if (isPanicking(villager)) {
            GolemSpawner.tryToSpawn(villager, GolemSpawner.PANIC_REQUIREMENT);
        }
    }

    /** 1.14's panic condition: a hostile in sight, or having been hurt by something. */
    private static boolean isPanicking(EntityVillager villager) {
        if (villager.getRevengeTarget() != null) {
            return true;
        }

        AxisAlignedBB box = villager.getEntityBoundingBox().grow(FRIGHT_RANGE);

        // Matched on the interface rather than a base class: most hostiles extend EntityMob, but
        // slimes and a good deal of modded life only implement IMob, and a villager is frightened
        // of those too.
        return !villager.world.getEntitiesWithinAABB(EntityLivingBase.class, box,
                new Predicate<EntityLivingBase>() {
                    @Override
                    public boolean apply(EntityLivingBase candidate) {
                        return candidate instanceof IMob && !candidate.isDead;
                    }
                }).isEmpty();
    }
}
