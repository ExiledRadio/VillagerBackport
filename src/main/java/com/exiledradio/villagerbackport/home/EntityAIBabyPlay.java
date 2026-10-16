package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.block.BlockBed;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.14's baby villagers at play.
 *
 * <p>From 09:00 to 12:00 and again from 16:00 to 18:00 (ticks 3000-6000 and 10000-12000), babies
 * play, choosing among what 1.14's {@code play} activity offers, with its weights:
 * <ul>
 * <li><b>Chase</b> another baby. 1.14's {@code WalkToVillagerBabiesTask}: a baby someone is chasing
 * runs off, a group with an "it" closes in on whoever that is, otherwise any baby will do. That is
 * the game of tag the wiki describes.</li>
 * <li><b>Bounce on a bed</b>: 1.14's {@code JumpOnBedTask}, three to six jumps on the nearest bed
 * within 16 blocks.</li>
 * <li><b>Wander</b>, or <b>stand about</b> for a second or two.</li>
 * </ul>
 * This replaces 1.12.2's own {@code EntityAIPlay}, which had babies follow each other at any hour.
 */
public class EntityAIBabyPlay extends EntityAIBase {

    private static final double SPEED = 0.6D;
    private static final int BED_RANGE = 16;

    /** Who each baby is chasing, so the others can see a game of tag going on. */
    private static final Map<EntityVillager, EntityVillager> CHASING = new WeakHashMap<EntityVillager, EntityVillager>();

    private enum Game { CHASE, FLEE, BED, WANDER, IDLE }

    private final EntityVillager baby;
    private Game game;
    private int ticksLeft;
    @Nullable
    private EntityVillager target;
    @Nullable
    private BlockPos bed;
    private int jumpsLeft;
    private int jumpCooldown;

    public EntityAIBabyPlay(EntityVillager baby) {
        this.baby = baby;
        this.setMutexBits(1);
    }

    /** @return true during a baby's play hours */
    public static boolean isPlayTime(World world) {
        long time = world.getWorldTime() % 24000L;
        return (time >= 3000 && time < 6000) || (time >= 10000 && time < 12000);
    }

    private boolean applies() {
        return ModConfig.homes.babiesPlay && this.baby.isChild() && isPlayTime(this.baby.world)
                && !HomeSite.isSleeping(this.baby) && !PanicWatch.isPanicking(this.baby);
    }

    @Override
    public boolean shouldExecute() {
        return applies() && this.baby.getRNG().nextInt(10) == 0 && choose();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!applies() || this.ticksLeft <= 0) {
            return false;
        }
        switch (this.game) {
            case CHASE:
                return this.target != null && !this.target.isDead && this.baby.getDistanceSq(this.target) < 24 * 24;
            case BED:
                return this.bed != null && this.jumpsLeft > 0 && isBed(this.baby.world, this.bed);
            case IDLE:
                return true;
            default:
                return !this.baby.getNavigator().noPath();
        }
    }

    @Override
    public void resetTask() {
        CHASING.remove(this.baby);
        this.target = null;
        this.bed = null;
        this.baby.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        this.ticksLeft--;
        if (this.game == Game.CHASE && this.target != null) {
            this.baby.getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);
            if (this.baby.ticksExisted % 10 == 0) {
                this.baby.getNavigator().tryMoveToEntityLiving(this.target, SPEED);
            }
            // Caught them: they are "it" now, and this baby runs.
            if (this.baby.getDistanceSq(this.target) < 1.5D) {
                CHASING.put(this.target, this.baby);
                this.ticksLeft = 0;
            }
        } else if (this.game == Game.BED && this.bed != null) {
            bounce();
        }
    }

    /** Picks what to play, with 1.14's weights; a chased baby always runs first. */
    private boolean choose() {
        EntityVillager chaser = chasedBy();
        if (chaser != null && runAwayFrom(chaser)) {
            return true;
        }

        List<EntityVillager> babies = this.baby.world.getEntitiesWithinAABB(EntityVillager.class,
                this.baby.getEntityBoundingBox().grow(16.0D, 4.0D, 16.0D));
        babies.removeIf(other -> other == this.baby || !other.isChild() || !this.baby.canEntityBeSeen(other));

        if (!babies.isEmpty() && this.baby.getRNG().nextInt(5) < 3) {
            return chase(pickPlaymate(babies));
        }

        int roll = this.baby.getRNG().nextInt(6);
        if (roll < 2) {
            BlockPos nearestBed = nearestBed();
            if (nearestBed != null) {
                this.game = Game.BED;
                this.bed = nearestBed;
                this.jumpsLeft = 3 + this.baby.getRNG().nextInt(4);
                this.jumpCooldown = 0;
                this.ticksLeft = 100;
                this.baby.getNavigator().tryMoveToXYZ(nearestBed.getX() + 0.5D, nearestBed.getY() + 0.6D,
                        nearestBed.getZ() + 0.5D, SPEED);
                return true;
            }
        }
        if (roll < 4) {
            this.game = Game.IDLE;
            this.ticksLeft = 20 + this.baby.getRNG().nextInt(21);
            return true;
        }
        Vec3d spot = RandomPositionGenerator.findRandomTarget(this.baby, 10, 7);
        if (spot == null) {
            return false;
        }
        this.game = Game.WANDER;
        this.ticksLeft = 200;
        return this.baby.getNavigator().tryMoveToXYZ(spot.x, spot.y, spot.z, SPEED);
    }

    /** Prefer whoever a few others are already chasing, so a group plays one game. */
    private EntityVillager pickPlaymate(List<EntityVillager> babies) {
        EntityVillager best = null;
        int bestCount = 0;
        for (EntityVillager candidate : babies) {
            int count = 0;
            for (EntityVillager chased : CHASING.values()) {
                if (chased == candidate) {
                    count++;
                }
            }
            if (count > 0 && count <= 5 && (best == null || count < bestCount)) {
                best = candidate;
                bestCount = count;
            }
        }
        return best != null ? best : babies.get(this.baby.getRNG().nextInt(babies.size()));
    }

    private boolean chase(EntityVillager other) {
        this.game = Game.CHASE;
        this.target = other;
        this.ticksLeft = 100 + this.baby.getRNG().nextInt(100);
        CHASING.put(this.baby, other);
        return this.baby.getNavigator().tryMoveToEntityLiving(other, SPEED);
    }

    @Nullable
    private EntityVillager chasedBy() {
        for (Map.Entry<EntityVillager, EntityVillager> entry : CHASING.entrySet()) {
            if (entry.getValue() == this.baby && !entry.getKey().isDead && entry.getKey().getDistanceSq(this.baby) < 16 * 16) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean runAwayFrom(EntityVillager chaser) {
        for (int i = 0; i < 10; i++) {
            Vec3d spot = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.baby, 20, 8,
                    new Vec3d(chaser.posX, chaser.posY, chaser.posZ));
            if (spot != null && this.baby.getNavigator().tryMoveToXYZ(spot.x, spot.y, spot.z, SPEED)) {
                this.game = Game.FLEE;
                this.ticksLeft = 100;
                return true;
            }
        }
        return false;
    }

    private void bounce() {
        BlockPos here = new BlockPos(this.baby);
        boolean onBed = isBed(this.baby.world, here) || isBed(this.baby.world, here.down());
        if (!onBed) {
            if (this.baby.getNavigator().noPath()) {
                this.baby.getNavigator().tryMoveToXYZ(this.bed.getX() + 0.5D, this.bed.getY() + 0.6D,
                        this.bed.getZ() + 0.5D, SPEED);
            }
            return;
        }
        this.ticksLeft = Math.max(this.ticksLeft, 20);
        if (this.jumpCooldown > 0) {
            this.jumpCooldown--;
        } else if (this.baby.onGround) {
            this.baby.getJumpHelper().setJumping();
            this.jumpsLeft--;
            this.jumpCooldown = 5;
        }
    }

    @Nullable
    private BlockPos nearestBed() {
        World world = this.baby.world;
        BlockPos here = new BlockPos(this.baby);
        BlockPos best = null;
        double bestDistance = (double) BED_RANGE * BED_RANGE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (BlockPos pos : WorkstationIndex.bedsIn(world, (here.getX() >> 4) + dx, (here.getZ() >> 4) + dz)) {
                    double distance = pos.distanceSq(here);
                    if (distance < bestDistance && !HomeSite.isOccupiedByOther(this.baby, pos)) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private static boolean isBed(World world, BlockPos pos) {
        return world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() instanceof BlockBed;
    }
}
