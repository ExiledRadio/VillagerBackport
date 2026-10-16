package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityIllusionIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What frightens a villager, and the golems that reassure it.
 *
 * <h2>Panic</h2>
 * A villager panics while it has just been hurt, while whoever hurt it is still within six blocks,
 * or while it can see a zombie or an illager close by. A panicking villager wakes if it was asleep,
 * runs - see {@link EntityAIVillagerPanic} - and every hundred ticks asks for an iron golem, needing
 * three eligible villagers.
 *
 * <p>Seeing a hostile is looked up once a second, the rate the game's own sensors run at; being hurt
 * is read straight off the villager, so a hit is felt the moment it lands.
 *
 * <h2>Golems</h2>
 * Every 200 ticks a villager with an iron golem within 16 blocks is stamped as having seen one, and a
 * villager stamped in the last 600 ticks will not help summon another. With a golem about, the stamp
 * never goes stale, so a village keeps the golems it has and only gets a new one once they have died
 * or wandered off.
 */
public final class PanicWatch {

    /** The game asks for a golem once per hundred ticks while panicking. */
    private static final int ASK_INTERVAL = 100;

    /** The game's golem-seen sensor period. */
    private static final int GOLEM_LOOK_INTERVAL = 200;

    /** How often the hostile sensor looks, as the game's sensors do. */
    private static final int SENSE_INTERVAL = 20;

    /** The game's mob sensor box: everything a villager registers at all. */
    private static final double SENSE_RANGE = 16.0D;

    /** Whoever hurt a villager keeps it frightened while within this many blocks. */
    private static final double ATTACKER_RANGE = 6.0D;

    /** The hostile each villager last saw, refreshed once a second. Server side only. */
    private static final Map<EntityVillager, EntityLivingBase> SEEN_HOSTILE =
            new WeakHashMap<EntityVillager, EntityLivingBase>();

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();

        if (villager.world.isRemote || !ModConfig.homes.enabled) {
            return;
        }

        int phase = villager.ticksExisted + villager.getEntityId();

        if (phase % SENSE_INTERVAL == 0) {
            EntityLivingBase hostile = nearestHostile(villager);
            if (hostile == null) {
                SEEN_HOSTILE.remove(villager);
            } else {
                SEEN_HOSTILE.put(villager, hostile);
            }
        }

        if (!ModConfig.homes.golemSpawning) {
            return;
        }

        // Looked for before the panic is acted on, as the game's sensors run before its tasks, so a
        // villager standing beside a golem is never counted towards summoning a second.
        if (villager.ticksExisted % GOLEM_LOOK_INTERVAL == 0 && golemNearby(villager)) {
            GolemSpawner.markGolemSeen(villager, villager.world.getTotalWorldTime());
        }

        if (!villager.isChild() && villager.ticksExisted % ASK_INTERVAL == 0 && isPanicking(villager)) {
            GolemSpawner.tryToSpawn(villager, GolemSpawner.PANIC_REQUIREMENT);
        }
    }

    /**
     * @return true while this villager is frightened
     *
     * <p>Hurt is any damage at all, as the game reads it - a fall counts as much as an arrow - and it
     * lasts the two seconds the damage is remembered for. The attacker keeps it going for as long as
     * it stays within six blocks.
     */
    public static boolean isPanicking(EntityVillager villager) {
        if (villager.getLastDamageSource() != null) {
            return true;
        }

        EntityLivingBase attacker = villager.getRevengeTarget();
        if (attacker != null && attacker.isEntityAlive()
                && attacker.getDistanceSq(villager) <= ATTACKER_RANGE * ATTACKER_RANGE) {
            return true;
        }

        EntityLivingBase hostile = SEEN_HOSTILE.get(villager);
        return hostile != null && hostile.isEntityAlive();
    }

    /** @return what the villager is running from: whoever hurt it, or else the hostile it saw */
    @Nullable
    public static Entity threatOf(EntityVillager villager) {
        EntityLivingBase attacker = villager.getRevengeTarget();
        if (attacker != null && attacker.isEntityAlive()) {
            return attacker;
        }

        EntityLivingBase hostile = SEEN_HOSTILE.get(villager);
        return hostile != null && hostile.isEntityAlive() ? hostile : null;
    }

    private static boolean golemNearby(EntityVillager villager) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(SENSE_RANGE);
        return !villager.world.getEntitiesWithinAABB(EntityIronGolem.class, box).isEmpty();
    }

    /** @return the nearest hostile this villager can see within its fright range, or null */
    @Nullable
    private static EntityLivingBase nearestHostile(EntityVillager villager) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(SENSE_RANGE);
        EntityLivingBase nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (EntityLivingBase candidate : villager.world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double range = frightRange(candidate);
            if (range <= 0.0D || candidate.isDead) {
                continue;
            }

            double distance = candidate.getDistanceSq(villager);
            if (distance <= range * range && distance < nearestDistance && villager.canEntityBeSeen(candidate)) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * @return how close this mob has to be to frighten a villager, or 0 if it never does
     *
     * <p>The game's hostile list for villagers, at its distances: zombies of every kind - husks and
     * zombie villagers are subclasses here - and the illagers. Drowned, pillagers and ravagers do not
     * exist in 1.12.2. Zombie pigmen extend {@code EntityZombie} here but are not on the list.
     *
     * <p>It is a short list on purpose. Villagers are not frightened of skeletons, creepers or
     * spiders, and a test of "is it hostile" made every cave mob under a village a reason to panic.
     */
    private static double frightRange(EntityLivingBase mob) {
        if (mob instanceof EntityPigZombie) {
            return 0.0D;
        }
        if (mob instanceof EntityZombie || mob instanceof EntityVex) {
            return 8.0D;
        }
        if (mob instanceof EntityVindicator) {
            return 10.0D;
        }
        if (mob instanceof EntityEvoker || mob instanceof EntityIllusionIllager) {
            return 12.0D;
        }
        return 0.0D;
    }
}
