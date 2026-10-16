package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;

/**
 * How long a villager has been unable to get to its workstation or its bed.
 *
 * <h2>1.14's rule</h2>
 * 1.14 keeps one timestamp, {@code CANT_REACH_WALK_TARGET_SINCE}: set when a walk fails to find a
 * path, cleared the moment one succeeds. Its {@code StayNearPointTask} for the job site and for the
 * home each read it, and a villager that has gone 1200 ticks - a minute - without being able to walk
 * to its claim lets the claim go and looks for another. That applies to every claim, at any time,
 * whether or not the villager has ever been there: a workstation walled off after it was claimed is
 * given up just the same as one that was never reachable.
 *
 * <p>Kept per claim here rather than shared, so a bed it cannot reach does not cost a villager the
 * workstation it walks to every day without trouble.
 */
public final class ClaimReach {

    /** 1.14's {@code StayNearPointTask} limit for both the job site and the home. */
    private static final long GIVE_UP_AFTER = 1200L;

    private static final int TAG_ANY_NUMBER = 99;

    /** The two claims a villager walks to. */
    public enum Claim {
        JOB("JobCantReachSince"),
        HOME("HomeCantReachSince");

        private final String key;

        Claim(String key) {
            this.key = key;
        }
    }

    private ClaimReach() {
    }

    /**
     * Records the outcome of asking to walk to a claim. A path found clears the failure, as 1.14's
     * walking task does; no path starts the clock if it is not already running.
     */
    public static void walked(EntityVillager villager, Claim claim, boolean pathFound) {
        if (pathFound) {
            reached(villager, claim);
        } else {
            failed(villager, claim);
        }
    }

    /** The walk failed: no path, or the villager got stuck on the way. */
    public static void failed(EntityVillager villager, Claim claim) {
        NBTTagCompound tag = VillagerTradeData.root(villager);
        if (!tag.hasKey(claim.key, TAG_ANY_NUMBER)) {
            tag.setLong(claim.key, villager.world.getTotalWorldTime());
        }
    }

    /** The villager got there, or can get there. */
    public static void reached(EntityVillager villager, Claim claim) {
        VillagerTradeData.root(villager).removeTag(claim.key);
    }

    /** @return true once the villager has been unable to reach this claim for a full minute */
    public static boolean givenUp(EntityVillager villager, Claim claim) {
        NBTTagCompound tag = VillagerTradeData.root(villager);
        if (!tag.hasKey(claim.key, TAG_ANY_NUMBER)) {
            return false;
        }

        long since = tag.getLong(claim.key);
        long now = villager.world.getTotalWorldTime();

        // A world whose time was set back would otherwise hold the claim until the clock caught up.
        if (now < since) {
            tag.setLong(claim.key, now);
            return false;
        }

        return now - since > GIVE_UP_AFTER;
    }
}
