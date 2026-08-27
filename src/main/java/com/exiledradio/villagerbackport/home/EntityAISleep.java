package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.job.WorkPathing;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;

/**
 * Villagers going to bed at dusk and getting up at dawn.
 *
 * <h2>Sleeping where nothing sleeps</h2>
 * 1.14 has a sleeping state on every living entity - a bed position, a pose, and a renderer that
 * lays the model down. 1.12.2 has none of that outside the player, so all of it is built here: the
 * villager walks to its bed, is held in it until morning, and {@link
 * com.exiledradio.villagerbackport.client.SleepRenderer} draws it lying down.
 *
 * <p>Held rather than parked. A sleeping villager keeps hold of the movement mutex for as long as
 * it is asleep, which is what stops the wandering goals walking it out of bed - the same reason
 * 1.14 switches the whole brain to the REST activity rather than merely stopping the legs.
 *
 * <p>What it deliberately does not do is outrank fear. The flee goals sit above this one, so a
 * villager gets out of bed when something dangerous turns up, which is both 1.14's behaviour and
 * the reason a village under attack is a village of villagers awake enough to want a golem.
 */
public final class EntityAISleep extends EntityAIBase {

    /** Ticks in a Minecraft day, for reading the clock. */
    private static final long TICKS_PER_DAY = 24000L;

    /**
     * How high above the bed block a sleeping villager sits.
     *
     * <p>Vanilla lies a player at 0.6875; a villager is slightly taller and wants a touch more or
     * its legs hang through the mattress.
     */
    private static final double SLEEP_HEIGHT = 0.75D;

    /** How high a bed's collision box reaches - nine sixteenths - so a waking villager stands on it. */
    private static final double BED_TOP = 0.5625D;

    /**
     * How far a sleeping villager may be shoved before it is out of bed, from 1.14's
     * {@code SleepAtHomeTask.shouldContinueExecuting}: {@code blockpos.withinDistance(pos, 1.14D)}.
     */
    private static final double SHOVED_OUT = 1.14D;

    /** And how far it may be pushed down before the same test calls it out of bed. */
    private static final double LOWEST = 0.4D;

    /** 1.14's pause before a villager turned out of bed may get back into one. */
    private static final int SHOVED_COOLDOWN = 40;

    /** How long the villager will keep trying to reach its bed before giving up for the night. */
    private static final int WALK_TIMEOUT = 600;

    private final EntityVillager villager;

    private BlockPos bed;
    private int walkTime;

    public EntityAISleep(EntityVillager villager) {
        this.villager = villager;

        // Movement and looking. Holding both is what keeps a sleeping villager in bed.
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!ModConfig.homes.enabled || !ModConfig.homes.sleepAtNight) {
            return false;
        }

        // Babies sleep too, as they do from 1.14 - see the VILLAGER_BABY schedule, which rests at
        // 12000 alongside everybody else.
        if (this.villager.getCustomer() != null) {
            return false;
        }

        if (!isBedtime(this.villager) || HomeSite.isDisturbed(this.villager)) {
            return false;
        }

        this.bed = HomeSite.validated(this.villager);
        return this.bed != null;
    }

    @Override
    public void startExecuting() {
        this.walkTime = 0;

        if (!HomeSite.isAtBed(this.villager)) {
            WorkPathing.walkTo(this.villager, this.bed, ModConfig.jobs.walkSpeed);
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.bed == null || this.villager.getCustomer() != null) {
            return false;
        }

        if (!isBedtime(this.villager) || HomeSite.validated(this.villager) == null) {
            return false;
        }

        // Turned out of bed by a player. Stop rather than climb back in.
        if (HomeSite.isDisturbed(this.villager)) {
            return false;
        }

        if (HomeSite.isSleeping(this.villager)) {
            return stillInBed();
        }

        // Still walking. A bed it cannot get to tonight is not worth standing outside until dawn.
        return ++this.walkTime <= WALK_TIMEOUT;
    }

    @Override
    public void updateTask() {
        if (HomeSite.isSleeping(this.villager)) {
            lieStill();
            return;
        }

        if (HomeSite.isAtBed(this.villager)) {
            fallAsleep();
            return;
        }

        // Navigation gives up for all sorts of reasons; ask again periodically rather than assume.
        if (this.villager.getNavigator().noPath() && this.walkTime % 40 == 0) {
            WorkPathing.walkTo(this.villager, this.bed, ModConfig.jobs.walkSpeed);
        }
    }

    @Override
    public void resetTask() {
        if (HomeSite.isSleeping(this.villager)) {
            if (stillInBed()) {
                wakeUp();
            } else {
                // Shoved out rather than woken by the morning, so it waits before trying again -
                // otherwise it climbs straight back in against whoever is pushing it.
                HomeSite.disturb(this.villager, SHOVED_COOLDOWN);
                this.villager.setNoGravity(false);
                VillageDebug.say(this.villager, "sleep: pushed out of bed");
            }
        }

        this.bed = null;
    }

    private void fallAsleep() {
        this.villager.getNavigator().clearPath();
        HomeSite.setSleeping(this.villager, true);

        // Put squarely in the bed once, on the way in. Every tick after this it is free to be moved.
        this.villager.setPosition(
                this.bed.getX() + 0.5D,
                this.bed.getY() + SLEEP_HEIGHT,
                this.bed.getZ() + 0.5D);
        this.villager.motionX = 0.0D;
        this.villager.motionZ = 0.0D;

        lieStill();
        SleepSync.broadcast(this.villager);
        VillageDebug.say(this.villager, "sleep: went to bed");
    }

    /**
     * Keeps the villager in the bed it is asleep in.
     *
     * <p>Set every tick rather than once. A villager is still a pushable entity while it sleeps -
     * other villagers walk into it, water moves it, a golem shoves past - and without this it drifts
     * out of its own bed over the night.
     */
    private void lieStill() {
        this.villager.getNavigator().clearPath();

        // Gravity is what made a sleeping villager bounce: holding it at a fixed height while the
        // physics pulled it down meant the two fought every tick. Turning gravity off settles that,
        // and a villager asleep has no business falling anywhere.
        this.villager.setNoGravity(true);
        this.villager.motionY = 0.0D;
        this.villager.fallDistance = 0.0F;
        this.villager.onGround = true;

        // Sideways is left alone on purpose, because in 1.14 a sleeping villager can be shoved. The
        // motion another entity imparts is allowed to move it, damped so a nudge settles rather than
        // sending it sliding across the room, and if it ends up far enough from the bed the test in
        // stillInBed has it up - which is exactly how a player pushes a villager out of bed.
        this.villager.motionX *= 0.8D;
        this.villager.motionZ *= 0.8D;

        // Pinned to 180 so vanilla's applyRotations - rotate(180 - yaw) - comes out as no rotation
        // at all. That is what lets the renderer apply vanilla's sleeping angles unmodified, rather
        // than composing them with whichever way the villager happened to be facing when it lay
        // down, which is what had them sleeping sideways.
        this.villager.rotationYaw = 180.0F;
        this.villager.renderYawOffset = 180.0F;
        this.villager.rotationYawHead = 180.0F;
        this.villager.prevRotationYaw = 180.0F;
        this.villager.prevRenderYawOffset = 180.0F;

        // Only the height is held. Pinning where it lies as well would undo any push before anybody
        // saw it, which is what stopped villagers being movable at all.
        this.villager.setPosition(
                this.villager.posX,
                this.bed.getY() + SLEEP_HEIGHT,
                this.villager.posZ);
    }

    /**
     * @return true if the villager is still in the bed rather than beside it
     *
     * <p>1.14's own test, from {@code SleepAtHomeTask}: still above the bed, and still within 1.14
     * blocks of it. Failing either is what being pushed out of bed actually is - there is no
     * separate notion of being shoved, only of no longer being in the bed.
     */
    private boolean stillInBed() {
        if (this.villager.posY <= this.bed.getY() + LOWEST) {
            return false;
        }

        double dx = this.villager.posX - (this.bed.getX() + 0.5D);
        double dz = this.villager.posZ - (this.bed.getZ() + 0.5D);

        return dx * dx + dz * dz <= SHOVED_OUT * SHOVED_OUT;
    }

    private void wakeUp() {
        HomeSite.setSleeping(this.villager, false);
        this.villager.setNoGravity(false);

        // Stood on top of the bed rather than inside it.
        //
        // A bed's collision box is nine sixteenths high, so putting the villager a tenth of a block
        // above the block position drops it inside the frame - waist deep in its own bed, stuck
        // there until something pushes it out. Standing it on the mattress instead lets it simply
        // walk off, and gravity has it on the floor a moment later.
        this.villager.setPosition(
                this.bed.getX() + 0.5D,
                this.bed.getY() + BED_TOP,
                this.bed.getZ() + 0.5D);

        SleepSync.broadcast(this.villager);
        VillageDebug.say(this.villager, "sleep: woke up");
    }

    /**
     * @return true if it is the part of the day 1.14 gives over to sleeping
     *
     * <p>1.14's schedule turns REST on at 12000 and leaves it at daybreak, so the window runs
     * through midnight - which is why this handles a start later than its end.
     */
    static boolean isBedtime(EntityVillager villager) {
        long timeOfDay = villager.world.getWorldTime() % TICKS_PER_DAY;
        int start = ModConfig.homes.sleepStartTime;
        int end = ModConfig.homes.sleepEndTime;

        if (start <= end) {
            return timeOfDay >= start && timeOfDay < end;
        }

        return timeOfDay >= start || timeOfDay < end;
    }
}
