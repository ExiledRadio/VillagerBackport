package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.block.ModSounds;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

/**
 * 1.14's {@code RangedCrossbowAttackGoal}: close in, load, hold aim for a moment, fire.
 *
 * <p>The same four states and timings. The load is timed here rather than by using the crossbow
 * item, so another mod's crossbow is only ever held - see {@link Crossbows}. The loading sounds
 * play where 1.14's crossbow plays them: a fifth of the way in, half way, and at the end.
 */
public final class EntityAICrossbowAttack extends EntityAIBase {

    private enum State {
        UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK
    }

    private final EntityPillager pillager;
    private final double speed;
    private final float rangeSq;

    private State state = State.UNCHARGED;
    private int seeTime;
    private int attackDelay;
    private int charge;

    public EntityAICrossbowAttack(EntityPillager pillager, double speed, float range) {
        this.pillager = pillager;
        this.speed = speed;
        this.rangeSq = range * range;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        return hasTarget() && this.pillager.isHoldingCrossbow();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return hasTarget() && (shouldExecute() || !this.pillager.getNavigator().noPath()) && this.pillager.isHoldingCrossbow();
    }

    private boolean hasTarget() {
        EntityLivingBase target = this.pillager.getAttackTarget();
        return target != null && target.isEntityAlive();
    }

    @Override
    public void resetTask() {
        this.pillager.setSwingingArms(false);
        this.pillager.setAttackTarget(null);
        this.seeTime = 0;
        if (this.state == State.CHARGING) {
            this.pillager.setCharging(false);
            this.state = State.UNCHARGED;
        }
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = this.pillager.getAttackTarget();
        if (target == null) {
            return;
        }

        boolean sees = this.pillager.getEntitySenses().canSee(target);
        if (sees != this.seeTime > 0) {
            this.seeTime = 0;
        }
        this.seeTime += sees ? 1 : -1;

        double distanceSq = this.pillager.getDistanceSq(target);
        boolean approach = (distanceSq > this.rangeSq || this.seeTime < 5) && this.attackDelay == 0;
        if (approach) {
            this.pillager.getNavigator().tryMoveToEntityLiving(target, this.state == State.UNCHARGED ? this.speed : this.speed * 0.5D);
        } else {
            this.pillager.getNavigator().clearPath();
        }
        this.pillager.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        this.pillager.setSwingingArms(true);

        switch (this.state) {
            case UNCHARGED:
                if (!approach) {
                    this.state = State.CHARGING;
                    this.charge = 0;
                    this.pillager.setCharging(true);
                }
                break;
            case CHARGING:
                tickCharge();
                break;
            case CHARGED:
                if (--this.attackDelay == 0) {
                    this.state = State.READY_TO_ATTACK;
                }
                break;
            case READY_TO_ATTACK:
                if (sees) {
                    this.pillager.attackEntityWithRangedAttack(target, 1.0F);
                    Crossbows.setCharged(this.pillager.getHeldItemMainhand(), false);
                    this.state = State.UNCHARGED;
                }
                break;
        }
    }

    private void tickCharge() {
        int total = this.pillager.chargeTicks();
        this.charge++;

        float progress = total == 0 ? 1.0F : (float) this.charge / total;
        if (this.charge == Math.max(1, Math.round(total * 0.2F))) {
            this.pillager.playSound(ModSounds.crossbowLoadingStart, 0.5F, 1.0F);
        } else if (this.charge == Math.round(total * 0.5F)) {
            this.pillager.playSound(ModSounds.crossbowLoadingMiddle, 0.5F, 1.0F);
        }

        if (progress >= 1.0F) {
            this.pillager.playSound(ModSounds.crossbowLoadingEnd, 1.0F,
                    1.0F / (this.pillager.getRNG().nextFloat() * 0.5F + 1.0F) + 0.2F);
            Crossbows.setCharged(this.pillager.getHeldItemMainhand(), true);
            this.pillager.setCharging(false);
            this.state = State.CHARGED;
            this.attackDelay = 20 + this.pillager.getRNG().nextInt(20);
        }
    }
}
