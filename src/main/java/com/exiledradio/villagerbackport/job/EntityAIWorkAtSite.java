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

    /** 1.14's {@code SpawnGolemTask}, which is its work task, runs at most once per 300 ticks. */
    private static final long WORK_INTERVAL = 300L;

    private final EntityVillager villager;

    private BlockPos target;

    private int walkTime;

    /** Whether the last attempt found a way there. Without one there is no point standing about. */
    private boolean onTheWay;

    /** After failing to find a way, how long before trying again. */
    private static final long RETRY_TICKS = 200L;

    private long retryAt;

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

        if (this.villager.world.getTotalWorldTime() < this.retryAt) {
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
            if (!JobSiteRegistry.hasSite(this.villager)) {
                return false;
            }

            // 1.14's working day. Every employed villager goes to its workstation during work hours,
            // whether or not it has anything to restock, and uses it - which is what stamps it as
            // having worked, plays its work sound, and restocks it if a restock is due. Going only
            // when a trade had sold out left a village nobody traded with looking idle all day, and
            // after a day and a half no longer able to raise a golem, because golems need villagers
            // who have worked.
            if (!RestockRules.isWorkingHours(this.villager.world)) {
                return false;
            }

            // And no more often than 1.14 uses a workstation: once every fifteen seconds. Between
            // uses the villager is free to wander off and come back, as it does in 1.14.
            if (com.exiledradio.villagerbackport.home.GolemSpawner.workedWithin(this.villager, WORK_INTERVAL)) {
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

        // No way there: let the villager get on with something else, and do not ask again for a
        // while - a path search is not free. The failure is on record, and a minute of them costs
        // the villager the claim - see ClaimReach.
        if (!this.onTheWay) {
            this.retryAt = this.villager.world.getTotalWorldTime() + RETRY_TICKS;
            return false;
        }

        // Unreachable, or something keeps interrupting. Only stops walking - whether the claim is
        // worth keeping is decided on the villager's own tick, so that one owner decides it whether
        // or not this goal ever gets to run. See JobSiteClaims.
        if (++this.walkTime > WALK_TIMEOUT) {
            JobSiteClaims.noteUnreachable(this.villager, this.target);
            ClaimReach.failed(this.villager, ClaimReach.Claim.JOB);
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
        this.onTheWay = WorkPathing.walkTo(this.villager, this.target, ModConfig.jobs.walkSpeed);
        ClaimReach.walked(this.villager, ClaimReach.Claim.JOB, this.onTheWay);
    }
}
