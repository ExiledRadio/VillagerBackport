package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.restock.RestockRules;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;

/**
 * Sends a villager to the workstation it has claimed.
 *
 * <h2>Standing in for the brain</h2>
 * 1.14 does this with a scheduled brain task that runs during working hours and walks the villager
 * to its point of interest. 1.12.2 has neither brains nor schedules, so this is an ordinary
 * {@link EntityAIBase} that does the same job: walk to the workstation, and stop once standing at it.
 *
 * <h2>Walking only</h2>
 * Claiming a workstation deliberately does not happen here - see {@link JobSiteClaims} for why
 * putting it here made villagers ignore a workstation at their feet for minutes at a time. This goal
 * reserves the movement mutex, so it is only asked whether it wants to run when nothing more urgent
 * is moving the villager; that is the right rule for walking somewhere and quite the wrong one for
 * writing down which block is yours.
 *
 * <p>Deliberately undemanding. It only runs when the villager actually has something to gain -
 * trades to replenish - and gives up rather than pathing forever at an unreachable block. A villager
 * that cannot get to work should look like it gave up, not like it is stuck.
 */
public class EntityAIWorkAtSite extends EntityAIBase {

    /** Ticks to keep walking before giving up on a workstation that cannot be reached. */
    private static final int WALK_TIMEOUT = 400;

    private final EntityVillager villager;

    private BlockPos target;

    private int walkTime;

    public EntityAIWorkAtSite(EntityVillager villager) {
        this.villager = villager;

        // Movement only. Leaving look control alone means this never fights the villager's own
        // head-tracking, including while a player is trading with it.
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!ModConfig.jobs.enabled || this.villager.isChild() || this.villager.getCustomer() != null) {
            return false;
        }

        boolean unemployed = Employment.isUnemployed(this.villager);

        if (unemployed) {
            // An unemployed villager is looking for any workstation at all, so it is not waiting on
            // one being registered for a career it does not have.
            if (!ModConfig.jobs.professionsFromWorkstations) {
                return false;
            }
        } else {
            // A career with no workstation registered has nowhere to go, and nothing is gated on it.
            if (JobSiteRegistry.siteFor(this.villager) == null) {
                return false;
            }

            // Only bother when there is a reason to. Walking to a workstation with nothing to restock
            // is motion for its own sake, and villagers already have plenty of that.
            if (!JobSiteWork.needsToWork(this.villager)) {
                return false;
            }

            // And only when the trip could actually end in a restock. Sold-out trades are a reason to
            // want to work, not permission to: the allowance may be spent, the cooldown may be
            // running, or it may simply be the middle of the night. Setting off anyway means arriving,
            // achieving nothing, being released to whatever moves the villager next, and coming
            // straight back - which from the outside is a villager sprinting at its workstation over
            // and over for no visible reason.
            if (!RestockRules.couldRestockNow(this.villager)) {
                return false;
            }
        }

        // Nothing claimed yet means there is nowhere to walk. Claiming happens on the villager's
        // own tick rather than here, so it is not waiting on this goal winning the movement mutex.
        BlockPos site = JobSite.validated(this.villager);
        if (site == null) {
            return false;
        }

        if (JobSite.isAtSite(this.villager)) {
            JobSiteClaims.onArrival(this.villager);
            return false;
        }

        this.target = site;
        return true;
    }

    @Override
    public void startExecuting() {
        this.walkTime = 0;
        walkToTarget();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.target == null || this.villager.getCustomer() != null) {
            return false;
        }

        // Unreachable, or something keeps interrupting. Only stops walking - whether the claim is
        // worth keeping is decided on the villager's own tick, so that one owner decides it whether
        // or not this goal ever gets to run. See JobSiteClaims.
        if (++this.walkTime > WALK_TIMEOUT) {
            JobSiteClaims.noteUnreachable(this.villager, this.target);
            return false;
        }

        if (JobSite.isAtSite(this.villager)) {
            JobSiteClaims.onArrival(this.villager);
            return false;
        }

        // Re-issue the path periodically; navigation gives up on its own for all sorts of reasons.
        if (this.villager.getNavigator().noPath() && this.walkTime % 40 == 0) {
            walkToTarget();
        }

        return true;
    }

    @Override
    public void resetTask() {
        this.target = null;
        this.villager.getNavigator().clearPath();
    }

    private void walkToTarget() {
        WorkPathing.walkTo(this.villager, this.target, ModConfig.jobs.walkSpeed);
    }
}
