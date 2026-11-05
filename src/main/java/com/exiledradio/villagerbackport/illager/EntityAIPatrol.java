package com.exiledradio.villagerbackport.illager;

import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.ai.EntityAIBase;

/**
 * 1.14's {@code PatrollerEntity.PatrolGoal}: the captain walks the patrol towards a point up to 500
 * blocks off in ten-block legs, swinging a little to one side, and the others follow its steps.
 *
 * <p>The captain picks a new destination once it is within ten blocks of the old one. A leg the
 * pathfinder cannot manage is replaced by a short wander, as in 1.14.
 */
public final class EntityAIPatrol extends EntityAIBase {

    private final EntityPillager owner;
    private final double followerSpeed;
    private final double leaderSpeed;

    public EntityAIPatrol(EntityPillager owner, double followerSpeed, double leaderSpeed) {
        this.owner = owner;
        this.followerSpeed = followerSpeed;
        this.leaderSpeed = leaderSpeed;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        return this.owner.isPatrolling() && this.owner.getAttackTarget() == null && !this.owner.isBeingRidden()
                && this.owner.patrolTarget() != null;
    }

    @Override
    public void updateTask() {
        boolean leader = this.owner.isPatrolLeader();
        PathNavigate navigator = this.owner.getNavigator();
        if (!navigator.noPath()) {
            return;
        }

        BlockPos target = this.owner.patrolTarget();
        if (target == null) {
            return;
        }
        if (leader && target.distanceSq(this.owner.posX, this.owner.posY, this.owner.posZ) < 100.0D) {
            this.owner.resetPatrolTarget();
            return;
        }

        // 1.14's swing: aim forty percent of the way round to the side, then step ten blocks that way.
        Vec3d goal = new Vec3d(target);
        Vec3d here = new Vec3d(this.owner.posX, this.owner.posY, this.owner.posZ);
        Vec3d swung = here.subtract(goal).rotateYaw(90.0F).scale(0.4D).add(goal);
        Vec3d step = swung.subtract(here).normalize().scale(10.0D).add(here);
        BlockPos next = this.owner.world.getHeight(new BlockPos(step));

        if (!navigator.tryMoveToXYZ(next.getX(), next.getY(), next.getZ(), leader ? this.leaderSpeed : this.followerSpeed)) {
            wander();
        } else if (leader) {
            for (EntityPillager follower : this.owner.world.getEntitiesWithinAABB(EntityPillager.class,
                    this.owner.getEntityBoundingBox().grow(16.0D), p -> p != null && !p.isPatrolLeader())) {
                follower.setPatrolTarget(next);
            }
        }
    }

    private void wander() {
        BlockPos near = this.owner.world.getHeight(new BlockPos(this.owner).add(
                -8 + this.owner.getRNG().nextInt(16), 0, -8 + this.owner.getRNG().nextInt(16)));
        this.owner.getNavigator().tryMoveToXYZ(near.getX(), near.getY(), near.getZ(), this.followerSpeed);
    }
}
