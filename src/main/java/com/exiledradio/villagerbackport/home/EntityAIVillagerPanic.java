package com.exiledradio.villagerbackport.home;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/**
 * A frightened villager running.
 *
 * <p>The game's panic: while a villager is hurt, or can see a zombie or an illager, it drops what it
 * is doing - including sleeping, see {@link EntityAISleep} - and moves at half as fast again as it
 * walks. If whatever it is afraid of is within six blocks it runs away from it, up to sixteen blocks;
 * otherwise it shuffles about nearby, ready to run.
 *
 * <p>1.12.2's villager only runs from a few monsters and never from whoever is hitting it, so a
 * player or a skeleton could beat one to death without it moving.
 */
public final class EntityAIVillagerPanic extends EntityAIBase {

    /** The game's panic speed: its villager walking speed of 0.5, times 1.5. */
    private static final double SPEED = 0.75D;

    /** Within this many blocks, the villager runs away from the threat rather than milling about. */
    private static final double FLEE_WITHIN = 6.0D;

    private final EntityVillager villager;

    public EntityAIVillagerPanic(EntityVillager villager) {
        this.villager = villager;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!PanicWatch.isPanicking(this.villager)) {
            return false;
        }

        Vec3d target = pickTarget();
        if (target == null) {
            return false;
        }

        return this.villager.getNavigator().tryMoveToXYZ(target.x, target.y, target.z, SPEED);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.villager.getNavigator().noPath() && PanicWatch.isPanicking(this.villager);
    }

    @Nullable
    private Vec3d pickTarget() {
        Entity threat = PanicWatch.threatOf(this.villager);

        if (threat != null && threat.getDistanceSq(this.villager) < FLEE_WITHIN * FLEE_WITHIN) {
            Vec3d from = new Vec3d(threat.posX, threat.posY, threat.posZ);

            // The game tries ten times for somewhere away from the threat before giving up.
            for (int attempt = 0; attempt < 10; attempt++) {
                Vec3d away = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.villager, 16, 7, from);
                if (away != null) {
                    return away;
                }
            }
            return null;
        }

        return RandomPositionGenerator.findRandomTarget(this.villager, 2, 2);
    }
}
