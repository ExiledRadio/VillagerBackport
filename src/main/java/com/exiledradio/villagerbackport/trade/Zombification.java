package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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

        boolean matches(EntityZombieVillager zombie) {
            if (zombie.world.provider.getDimension() != this.dimension) {
                return false;
            }

            double dx = zombie.posX - this.x;
            double dy = zombie.posY - this.y;
            double dz = zombie.posZ - this.z;

            return dx * dx + dy * dy + dz * dz <= HANDOVER_RANGE * HANDOVER_RANGE;
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
     */
    @SubscribeEvent
    public void onZombieJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityZombieVillager)) {
            return;
        }

        EntityZombieVillager zombie = (EntityZombieVillager) event.getEntity();
        long now = zombie.world.getTotalWorldTime();

        expire(now);

        for (Iterator<Snapshot> it = PENDING.iterator(); it.hasNext(); ) {
            Snapshot snapshot = it.next();

            if (snapshot.matches(zombie)) {
                zombie.getEntityData().setTag(CARRIED, snapshot.data);
                it.remove();
                return;
            }
        }
    }

    /**
     * Gives a cured villager back the life it had before it was bitten.
     *
     * <p>Called from {@link ReputationEvents}, which is already holding the zombie this villager
     * came out of. Everything is restored before the gossip for the cure is added, so the discount
     * lands on the trades it was earned against rather than on a fresh set.
     */
    public static void restore(EntityVillager villager, EntityZombieVillager zombie) {
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

    /** @return the converting zombie villager this one is standing on top of, if any. */
    @Nullable
    static EntityZombieVillager convertingUnder(EntityVillager villager) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(1.0D);

        for (EntityZombieVillager zombie
                : villager.world.getEntitiesWithinAABB(EntityZombieVillager.class, box)) {
            if (zombie.isConverting()) {
                return zombie;
            }
        }

        return null;
    }

}
