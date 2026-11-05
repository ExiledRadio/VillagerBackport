package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * What any mob does while it is a raider: 1.14's {@code AbstractRaiderEntity} goals, added to
 * mobs that were never written for raids.
 *
 * <ul>
 *   <li>{@link MoveTowardsRaid} - head for the village until inside it (priority 3).</li>
 *   <li>{@link InvadeHome} - then go from bed to bed looking for villagers (priority 4).</li>
 *   <li>{@link CelebrateLoss} - after a defeat for the village, jump about cheering (priority 5).</li>
 * </ul>
 *
 * <p>All three stand idle for a mob that is not in a raid, so adding them once and leaving them is
 * safe - the same mob outside a raid behaves exactly as its own mod wrote it.
 */
public final class RaiderGoals {

    private RaiderGoals() {
    }

    /** Adds the goals to a raider that does not have them yet. Only creatures can walk to a village. */
    static void attach(EntityLiving raider) {
        if (!(raider instanceof EntityCreature) || has(raider)) {
            return;
        }
        EntityCreature creature = (EntityCreature) raider;
        stopInfighting(creature);
        creature.tasks.addTask(3, new MoveTowardsRaid(creature));
        creature.tasks.addTask(4, new InvadeHome(creature));
        creature.tasks.addTask(5, new CelebrateLoss(creature));
    }

    /**
     * Swaps a raider's fight-back goal for one that never turns on other raiders.
     *
     * <p>1.12.2's vindicators and evokers fight back against anything that hits them, so one
     * pillager's stray bolt would start a brawl inside the raid. 1.14's raiders ignore each other.
     * Done here, when the mob joins, rather than from any goal of its own, so its task list is never
     * changed while it is being run.
     */
    private static void stopInfighting(EntityCreature creature) {
        Integer priority = null;
        for (EntityAITasks.EntityAITaskEntry entry : new ArrayList<EntityAITasks.EntityAITaskEntry>(creature.targetTasks.taskEntries)) {
            if (entry.action instanceof com.exiledradio.villagerbackport.illager.EntityAIRaiderHurtByTarget) {
                return;
            }
            if (entry.action instanceof net.minecraft.entity.ai.EntityAIHurtByTarget) {
                priority = priority == null ? entry.priority : Math.min(priority, entry.priority);
                creature.targetTasks.removeTask(entry.action);
            }
        }
        if (priority != null) {
            creature.targetTasks.addTask(priority, new com.exiledradio.villagerbackport.illager.EntityAIRaiderHurtByTarget(creature));
        }
    }

    private static boolean has(EntityLiving raider) {
        for (EntityAITasks.EntityAITaskEntry entry : raider.tasks.taskEntries) {
            if (entry.action instanceof MoveTowardsRaid) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static VillageRaid raidOf(EntityCreature raider) {
        return raider.world instanceof WorldServer ? RaidManager.get((WorldServer) raider.world).raidOf(raider) : null;
    }

    /** 1.14's {@code MoveTowardsRaidGoal}: walk at the village while outside it and not fighting. */
    static final class MoveTowardsRaid extends EntityAIBase {
        private final EntityCreature raider;

        MoveTowardsRaid(EntityCreature raider) {
            this.raider = raider;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            VillageRaid raid = raidOf(this.raider);
            return this.raider.getAttackTarget() == null && !this.raider.isBeingRidden() && raid != null
                    && raid.isActive() && !raid.isOver()
                    && !RaidVillages.isVillage(this.raider.world, new BlockPos(this.raider));
        }

        @Override
        public boolean shouldContinueExecuting() {
            VillageRaid raid = raidOf(this.raider);
            return raid != null && raid.isActive() && !raid.isOver() && this.raider.getAttackTarget() == null
                    && !RaidVillages.isVillage(this.raider.world, new BlockPos(this.raider));
        }

        @Override
        public void updateTask() {
            VillageRaid raid = raidOf(this.raider);
            if (raid == null) {
                return;
            }
            if (this.raider.ticksExisted % 20 == 0) {
                recruit(raid);
            }
            if (this.raider.getNavigator().noPath()) {
                BlockPos centre = raid.getCenter();
                Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards(this.raider, 15, 4,
                        new Vec3d(centre.getX() + 0.5D, centre.getY(), centre.getZ() + 0.5D));
                if (towards != null) {
                    this.raider.getNavigator().tryMoveToXYZ(towards.x, towards.y, towards.z, 1.0D);
                }
            }
        }

        /** 1.14 sweeps raiders it walks past into the raid: illagers met on the way join in. */
        private void recruit(VillageRaid raid) {
            for (EntityLiving other : this.raider.world.getEntitiesWithinAABB(EntityLiving.class,
                    this.raider.getEntityBoundingBox().grow(16.0D))) {
                if (other != this.raider && other.isEntityAlive() && RaiderData.raidId(other) == 0
                        && Raids.isRaider(other) && other.getIdleTime() <= 2400) {
                    raid.joinRaid(raid.getGroupsSpawned(), other, true);
                }
            }
        }
    }

    /** 1.14's {@code InvadeHomeGoal}: visit beds within 48 blocks, one after another. */
    static final class InvadeHome extends EntityAIBase {
        private final EntityCreature raider;
        private final List<BlockPos> visited = new ArrayList<BlockPos>();
        @Nullable
        private BlockPos target;
        private boolean stuck;

        InvadeHome(EntityCreature raider) {
            this.raider = raider;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (this.visited.size() > 2) {
                this.visited.remove(0);
            }
            VillageRaid raid = raidOf(this.raider);
            if (raid == null || !raid.isActive() || raid.isOver() || this.raider.getAttackTarget() != null) {
                return false;
            }
            this.target = randomBed();
            return this.target != null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.target != null && !this.raider.getNavigator().noPath() && this.raider.getAttackTarget() == null
                    && this.raider.getDistanceSqToCenter(this.target) > (this.raider.width + 1.0D) * (this.raider.width + 1.0D)
                    && !this.stuck;
        }

        @Override
        public void startExecuting() {
            this.raider.getNavigator().tryMoveToXYZ(this.target.getX(), this.target.getY(), this.target.getZ(), 1.05D);
            this.stuck = false;
        }

        @Override
        public void resetTask() {
            if (this.target != null && this.raider.getDistanceSqToCenter(this.target) <= 1.0D) {
                this.visited.add(this.target);
            }
        }

        @Override
        public void updateTask() {
            if (this.target != null && this.raider.getNavigator().noPath()) {
                Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards(this.raider, 8, 7,
                        new Vec3d(this.target.getX(), this.target.getY(), this.target.getZ()));
                if (towards == null) {
                    this.stuck = true;
                    return;
                }
                this.raider.getNavigator().tryMoveToXYZ(towards.x, towards.y, towards.z, 1.05D);
            }
        }

        @Nullable
        private BlockPos randomBed() {
            BlockPos here = new BlockPos(this.raider);
            List<BlockPos> beds = new ArrayList<BlockPos>();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (BlockPos bed : WorkstationIndex.bedsIn(this.raider.world, (here.getX() >> 4) + dx, (here.getZ() >> 4) + dz)) {
                        if (bed.distanceSq(here) <= 48.0D * 48.0D && !this.visited.contains(bed)) {
                            beds.add(bed);
                        }
                    }
                }
            }
            return beds.isEmpty() ? null : beds.get(this.raider.getRNG().nextInt(beds.size()));
        }
    }

    /** 1.14's {@code CelebrateRaidLossGoal}: the village fell, so jump about. */
    static final class CelebrateLoss extends EntityAIBase {
        private final EntityCreature raider;

        CelebrateLoss(EntityCreature raider) {
            this.raider = raider;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            VillageRaid raid = raidOf(this.raider);
            return this.raider.isEntityAlive() && this.raider.getAttackTarget() == null && raid != null && raid.isLoss();
        }

        @Override
        public void updateTask() {
            if (!this.raider.isSilent() && this.raider.getRNG().nextInt(100) == 0) {
                if (this.raider instanceof com.exiledradio.villagerbackport.illager.Celebrates) {
                    this.raider.playSound(((com.exiledradio.villagerbackport.illager.Celebrates) this.raider).celebrateSound(),
                            1.0F, 1.0F);
                } else {
                    this.raider.playLivingSound();
                }
            }
            if (!this.raider.isRiding() && this.raider.getRNG().nextInt(50) == 0) {
                this.raider.getJumpHelper().setJumping();
            }
        }
    }
}
