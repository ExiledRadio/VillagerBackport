package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.compat.ConvertedVillagers;
import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.entity.Entity;

import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Carries a villager's working life through zombification and back out the other side.
 *
 * <h2>What 1.12.2 loses and 1.14 keeps</h2>
 * 1.12.2 hands over almost nothing. {@code EntityZombie.onKillEntity} copies the profession, whether
 * it was a child and whether it had AI, and that is the lot; {@code finishConversion} then builds a
 * brand new villager from those scraps. The career is re-rolled, the trades are gone, and everything
 * this mod tracks - level, experience, demand, gossip - goes with them.
 *
 * <p>1.14 moves the whole villager across. {@code ZombieVillagerEntity} holds the offers, the
 * experience, the villager data and the gossip while it is a zombie, and hands them all back when it
 * is cured. That is what makes curing worth doing: the librarian you cure is the same librarian,
 * with the same book, now selling it cheaper.
 *
 * <p>So the same is done here. Neither half of the conversion has an event, but each leaves a
 * window: the villager dies immediately before the zombie spawns in its place, and the zombie is
 * still standing there when the cured villager appears. The record rides on the zombie's own saved
 * data in between, so it survives the world being saved and reloaded mid-conversion - which, at five
 * minutes a cure, is not a rare case.
 *
 * <h2>Rows of villagers</h2>
 * Players cure for the discount, so they keep villagers in cells a block apart and cure the whole
 * row at once. Each half of the conversion therefore picks the closest candidate, not the first one
 * in reach: the zombie stands up exactly where the villager died, and the cured villager appears
 * exactly where the zombie stood. Taking the first one in reach let a cured villager pick up its
 * neighbour's record, and the neighbour then came out of its own cure with a fresh set of trades.
 */
public final class Zombification {

    private static final String ROOT = "villagerbackport";
    private static final String CARRIED = "CarriedVillager";
    private static final String OFFERS = "Offers";
    private static final String CAREER_ID = "CareerId";
    private static final String CAREER_LEVEL = "CareerLevel";

    private static final int TAG_COMPOUND = 10;

    /** How far the zombie may spawn from where the villager died, in blocks. */
    private static final double HANDOVER_RANGE = 2.0D;

    /**
     * How far a cured villager may stand from a zombie that is still alive, in blocks. Vanilla
     * removes the zombie first and spawns the villager on its exact spot, so this is only for a mod
     * that cures in the other order.
     */
    private static final double SAME_SPOT = 0.1D;

    /** How long a snapshot waits to be claimed. The spawn follows the death in the same tick. */
    private static final long HANDOVER_TICKS = 5L;

    /** Villagers that have just died, waiting to see whether a zombie stands up in their place. */
    private static final List<Snapshot> PENDING = new ArrayList<Snapshot>();

    private static final class Snapshot {

        final int dimension;
        final double x;
        final double y;
        final double z;
        final long at;
        final NBTTagCompound data;

        Snapshot(EntityVillager villager, NBTTagCompound data) {
            this.dimension = villager.world.provider.getDimension();
            this.x = villager.posX;
            this.y = villager.posY;
            this.z = villager.posZ;
            this.at = villager.world.getTotalWorldTime();
            this.data = data;
        }

        /** @return the squared distance to the zombie, or -1 if it is out of reach */
        double distanceTo(EntityZombieVillager zombie) {
            if (zombie.world.provider.getDimension() != this.dimension) {
                return -1.0D;
            }

            double dx = zombie.posX - this.x;
            double dy = zombie.posY - this.y;
            double dz = zombie.posZ - this.z;
            double distance = dx * dx + dy * dy + dz * dz;

            return distance <= HANDOVER_RANGE * HANDOVER_RANGE ? distance : -1.0D;
        }
    }

    /**
     * Takes a copy of everything worth keeping the moment a villager dies.
     *
     * <p>Taken on every villager death rather than only on the ones that turn, because at this point
     * there is no way to know: the zombie decides whether to convert after the kill, and by the time
     * it has, the villager is gone from the world entirely. A copy that nobody claims is dropped a
     * few ticks later.
     */
    @SubscribeEvent
    public void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager) || event.getEntityLiving().world.isRemote) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntityLiving();
        long now = villager.world.getTotalWorldTime();

        expire(now);
        PENDING.add(new Snapshot(villager, snapshot(villager)));
    }

    /**
     * A zombie villager has appeared: if a villager just died where it is standing, it is that one.
     *
     * <p>Highest priority, so it is marked as a converted villager before Infernal Mobs, Scaling
     * Health and Champions decide what to make of it - see {@link ConvertedVillagers}. That happens
     * again each time it is loaded, for the mods that roll again then.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onZombieJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityZombieVillager)) {
            return;
        }

        EntityZombieVillager zombie = (EntityZombieVillager) event.getEntity();
        if (isCarrying(zombie)) {
            ConvertedVillagers.keepOrdinary(zombie);
            return;
        }

        long now = zombie.world.getTotalWorldTime();

        expire(now);

        Snapshot nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Snapshot snapshot : PENDING) {
            double distance = snapshot.distanceTo(zombie);
            if (distance >= 0.0D && distance < nearestDistance) {
                nearest = snapshot;
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            return;
        }

        PENDING.remove(nearest);
        zombie.getEntityData().setTag(CARRIED, nearest.data);
        ConvertedVillagers.keepOrdinary(zombie);

        // One a player has traded with stays put until it is cured, as in the game, so a villager
        // zombified on purpose for the cure discount is still there to cure.
        if (nearest.data.getCompoundTag(ROOT).getInteger("Xp") > 0) {
            zombie.enablePersistence();
        }
    }

    /**
     * A villager appearing on top of a converting zombie villager is that zombie being cured.
     *
     * <p>There is no event for it; see {@link ReputationEvents#onEntityJoin}, which credits the cure
     * afterwards. Highest priority so the villager is itself again before any other mod looks at
     * it, and EagleMixins sees a librarian it has already rolled.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onVillagerJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();
        EntityZombieVillager zombie = convertingUnder(villager);
        if (zombie == null) {
            return;
        }

        restore(villager, zombie);
        ConvertedVillagers.keepOrdinary(villager);
    }

    /** @return true if this is a zombie villager holding the record of the villager it was */
    public static boolean isCarrying(Entity entity) {
        return entity instanceof EntityZombieVillager && entity.getEntityData().hasKey(CARRIED, TAG_COMPOUND);
    }

    /**
     * Gives a cured villager back the life it had before it was bitten.
     *
     * <p>Done before {@link ReputationEvents} adds the gossip for the cure, so the discount lands on
     * the trades it was earned against rather than on a fresh set.
     */
    static void restore(EntityVillager villager, EntityZombieVillager zombie) {
        NBTTagCompound carried = zombie.getEntityData().getCompoundTag(CARRIED);
        if (carried.getSize() == 0) {
            return;
        }

        // Career first: the trades belong to it, and vanilla has just rolled a random one.
        VillagerAccess.setCareerId(villager, carried.getInteger(CAREER_ID));
        VillagerAccess.setCareerLevel(villager, carried.getInteger(CAREER_LEVEL));

        if (carried.hasKey(OFFERS, TAG_COMPOUND)) {
            VillagerAccess.setBuyingList(villager,
                    new MerchantRecipeList(carried.getCompoundTag(OFFERS)));
        }

        // Everything this mod tracks, in one piece: level, experience, demand, restock timers and
        // the gossip the villager held about every player it had met.
        if (carried.hasKey(ROOT, TAG_COMPOUND)) {
            villager.getEntityData().setTag(ROOT, carried.getCompoundTag(ROOT).copy());
        }

        zombie.getEntityData().removeTag(CARRIED);
    }

    private static NBTTagCompound snapshot(EntityVillager villager) {
        NBTTagCompound data = new NBTTagCompound();

        data.setInteger(CAREER_ID, VillagerAccess.getCareerId(villager));
        data.setInteger(CAREER_LEVEL, VillagerAccess.getCareerLevel(villager));

        MerchantRecipeList recipes = VillagerAccess.getBuyingList(villager);
        if (recipes != null && !recipes.isEmpty()) {
            data.setTag(OFFERS, recipes.getRecipiesAsTags());
        }

        NBTTagCompound ours = villager.getEntityData().getCompoundTag(ROOT);
        if (ours.getSize() > 0) {
            data.setTag(ROOT, ours.copy());
        }

        return data;
    }

    /** Drops snapshots nobody came for. */
    private static void expire(long now) {
        for (Iterator<Snapshot> it = PENDING.iterator(); it.hasNext(); ) {
            if (now - it.next().at > HANDOVER_TICKS) {
                it.remove();
            }
        }
    }

    /**
     * @return the converting zombie villager this one was just cured out of, if any
     *
     * <p>Vanilla removes the zombie and then spawns the villager on its exact spot, so the zombie is
     * the one already marked dead, still in its chunk's entity list until the end of the tick. In a
     * row of villagers being cured together the neighbours are converting too, a block away; they
     * are still alive, and a live one only counts if it stands on the very same spot.
     */
    @Nullable
    static EntityZombieVillager convertingUnder(EntityVillager villager) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(1.0D);

        EntityZombieVillager best = null;
        double bestDistance = Double.MAX_VALUE;

        for (EntityZombieVillager zombie
                : villager.world.getEntitiesWithinAABB(EntityZombieVillager.class, box)) {
            if (!zombie.isConverting()) {
                continue;
            }

            double distance = zombie.getDistanceSq(villager);
            if (!zombie.isDead && distance > SAME_SPOT * SAME_SPOT) {
                continue;
            }

            // The removed zombie wins over a live one on the same spot; otherwise the nearest.
            boolean better = best == null
                    || zombie.isDead && !best.isDead
                    || zombie.isDead == best.isDead && distance < bestDistance;
            if (better) {
                best = zombie;
                bestDistance = distance;
            }
        }

        return best;
    }

}
