package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.job.WorkPathing;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.14's afternoon gathering: grown villagers meet at the bell.
 *
 * <p>From 15:00 to 17:00 (ticks 9000 to 11000) a villager heads for the nearest bell within 48
 * blocks, the village's meeting point, and spends the rest of the window near it: drifting around
 * within 8 blocks of the bell, now and then walking up to another villager to mingle, the way 1.14's
 * {@code meet} activity does with {@code StayNearPointTask}, {@code WorkTask} and
 * {@code CongregateTask}. Gossip spreads when villagers meet face to face, so this is also when news
 * gets round the village fastest.
 *
 * <p>A villager that cannot find a bell, or cannot get to one, carries on wandering, as in the game.
 */
public class EntityAIGatherAtBell extends EntityAIBase {

    /** 1.14's meeting point search range. */
    private static final int SEARCH_RADIUS = 48;

    /** {@code StayNearPointTask}'s completion range: close enough to count as at the meeting. */
    private static final int NEAR = 6;

    /** {@code WorkTask}: how far from the bell a villager drifts, and how often it picks a new spot. */
    private static final int DRIFT = 8;
    private static final int DRIFT_EVERY = 180;

    /** {@code CongregateTask}: within 4 of the bell, 1 in 100 per tick, a villager within ~5.7. */
    private static final double MINGLE_FROM_BELL = 4.0D;
    private static final double MINGLE_REACH_SQ = 32.0D;
    private static final int MINGLE_TICKS = 60;

    /** Ticks between bell searches when none was found, so a bell-less village costs almost nothing. */
    private static final int SEARCH_COOLDOWN = 200;

    /** Path attempts that may fail before the villager gives the meeting up for the afternoon. */
    private static final int MAX_FAILED_PATHS = 3;

    private static final double SPEED = 0.5D;
    private static final double MINGLE_SPEED = 0.3D;

    private final EntityVillager villager;
    @Nullable
    private BlockPos bell;
    private long nextSearch;
    private long nextDrift;
    private long givenUpUntil;
    private int failedPaths;
    @Nullable
    private EntityVillager mingling;
    private int mingleTicks;

    public EntityAIGatherAtBell(EntityVillager villager) {
        this.villager = villager;
        this.setMutexBits(3);
    }

    /** @return true during the afternoon gathering window */
    public static boolean isGatherTime(World world) {
        long time = world.getWorldTime() % 24000L;
        return time >= ModConfig.homes.gatherStartTime && time < ModConfig.homes.gatherEndTime;
    }

    private boolean applies() {
        long now = this.villager.world.getTotalWorldTime();
        return ModConfig.homes.gatherAtBell && !this.villager.isChild() && isGatherTime(this.villager.world)
                && now >= this.givenUpUntil && this.villager.getCustomer() == null
                && !HomeSite.isSleeping(this.villager) && !PanicWatch.isPanicking(this.villager);
    }

    @Override
    public boolean shouldExecute() {
        if (!applies()) {
            return false;
        }
        if (this.bell == null || !isBell(this.villager.world, this.bell)) {
            long now = this.villager.world.getTotalWorldTime();
            if (now < this.nextSearch) {
                return false;
            }
            this.bell = nearestBell();
            if (this.bell == null) {
                this.nextSearch = now + SEARCH_COOLDOWN;
                return false;
            }
            this.failedPaths = 0;
        }
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return applies() && this.bell != null && isBell(this.villager.world, this.bell);
    }

    @Override
    public void startExecuting() {
        this.nextDrift = 0;
        this.mingling = null;
    }

    @Override
    public void resetTask() {
        this.mingling = null;
        this.villager.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        if (this.bell == null) {
            return;
        }
        long now = this.villager.world.getTotalWorldTime();
        double distanceToBell = Math.sqrt(this.villager.getDistanceSqToCenter(this.bell));

        if (this.mingling != null) {
            this.villager.getLookHelper().setLookPositionWithEntity(this.mingling, 30.0F, 30.0F);
            if (--this.mingleTicks <= 0 || this.mingling.isDead) {
                this.mingling = null;
            }
            return;
        }

        if (distanceToBell > NEAR) {
            if (this.villager.getNavigator().noPath() && !walkToBell()) {
                if (++this.failedPaths >= MAX_FAILED_PATHS) {
                    // 1.14 forgets a meeting point it cannot reach; this gives up until tomorrow.
                    this.givenUpUntil = now + (ModConfig.homes.gatherEndTime - this.villager.world.getWorldTime() % 24000L);
                    this.bell = null;
                }
            }
            return;
        }

        if (distanceToBell <= MINGLE_FROM_BELL && this.villager.getRNG().nextInt(100) == 0 && startMingling()) {
            return;
        }

        if (now >= this.nextDrift && this.villager.getNavigator().noPath()) {
            this.nextDrift = now + DRIFT_EVERY;
            Vec3d spot = RandomPositionGenerator.findRandomTargetBlockTowards(this.villager, DRIFT, 6,
                    new Vec3d(this.bell.getX() + 0.5D, this.bell.getY(), this.bell.getZ() + 0.5D));
            if (spot != null && spot.squareDistanceTo(this.bell.getX() + 0.5D, spot.y, this.bell.getZ() + 0.5D) <= DRIFT * DRIFT) {
                this.villager.getNavigator().tryMoveToXYZ(spot.x, spot.y, spot.z, SPEED * 0.8D);
            }
        }
    }

    /** Walks to within {@link #NEAR} of the bell, if any path gets that close. */
    private boolean walkToBell() {
        Path path = WorkPathing.pathTo(this.villager, this.bell);
        if (path == null) {
            return false;
        }
        PathPoint end = path.getFinalPathPoint();
        if (end == null || Math.abs(end.x - this.bell.getX()) + Math.abs(end.z - this.bell.getZ()) > NEAR + 2
                || Math.abs(end.y - this.bell.getY()) > 3) {
            return false;
        }
        return this.villager.getNavigator().setPath(path, SPEED);
    }

    private boolean startMingling() {
        List<EntityVillager> nearby = this.villager.world.getEntitiesWithinAABB(EntityVillager.class,
                this.villager.getEntityBoundingBox().grow(6.0D, 2.0D, 6.0D));
        for (EntityVillager other : nearby) {
            if (other != this.villager && !other.isChild() && this.villager.getDistanceSq(other) <= MINGLE_REACH_SQ
                    && this.villager.canEntityBeSeen(other)) {
                this.mingling = other;
                this.mingleTicks = MINGLE_TICKS;
                this.villager.getNavigator().tryMoveToEntityLiving(other, MINGLE_SPEED);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos nearestBell() {
        World world = this.villager.world;
        BlockPos here = new BlockPos(this.villager);
        int chunks = (SEARCH_RADIUS >> 4) + 1;
        BlockPos best = null;
        double bestDistance = (double) SEARCH_RADIUS * SEARCH_RADIUS;
        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) {
                for (BlockPos pos : WorkstationIndex.bellsIn(world, (here.getX() >> 4) + dx, (here.getZ() >> 4) + dz)) {
                    double distance = pos.distanceSq(here);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private static boolean isBell(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        net.minecraft.util.ResourceLocation name = world.getBlockState(pos).getBlock().getRegistryName();
        return name != null && "bell".equals(name.getPath());
    }
}
