package com.exiledradio.villagerbackport.mechanics;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIDoorInteract;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * 1.14's {@code VindicatorEntity.BreakDoorGoal}: a vindicator in a raid breaks a wooden door in its way, on Normal and
 * Hard (1.12.2's {@code EntityAIBreakDoor} breaks only on Hard, and only zombies have it). 240 ticks, as
 * {@code BreakDoorGoal}'s floor ({@code max(240, 6)}); started on one try in ten, as 1.14's {@code nextInt(10)}.
 */
public class EntityAIRaidBreakDoor extends EntityAIDoorInteract {

    private static final int BREAK_TIME = 240;

    private int breakingTime;
    private int previousBreakProgress = -1;

    public EntityAIRaidBreakDoor(EntityLiving entity) {
        super(entity);
        this.setMutexBits(1); // 1.14: MOVE only
    }

    static boolean validDifficulty(EntityLiving entity) {
        EnumDifficulty d = entity.world.getDifficulty();
        return d == EnumDifficulty.NORMAL || d == EnumDifficulty.HARD;
    }

    private boolean raiding() {
        return ModConfig.mechanics.vindicatorsBreakDoors && Mechanics.inActiveRaid(this.entity);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.raiding() || this.entity.getRNG().nextInt(10) != 0 || !super.shouldExecute()) {
            return false;
        }
        if (!ForgeEventFactory.getMobGriefingEvent(this.entity.world, this.entity)
                || !this.entity.world.getBlockState(this.doorPosition).getBlock().canEntityDestroy(
                this.entity.world.getBlockState(this.doorPosition), this.entity.world, this.doorPosition, this.entity)
                || !ForgeEventFactory.onEntityDestroyBlock(this.entity, this.doorPosition, this.entity.world.getBlockState(this.doorPosition))) {
            return false;
        }
        return validDifficulty(this.entity) && !BlockDoor.isOpen(this.entity.world, this.doorPosition);
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.breakingTime = 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.raiding() && this.breakingTime <= BREAK_TIME && !BlockDoor.isOpen(this.entity.world, this.doorPosition)
                && this.entity.getDistanceSq(this.doorPosition) < 4.0D && validDifficulty(this.entity);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), this.doorPosition, -1);
    }

    @Override
    public void updateTask() {
        super.updateTask();
        if (this.entity.getRNG().nextInt(20) == 0) {
            this.entity.world.playEvent(1019, this.doorPosition, 0);
            if (!this.entity.isSwingInProgress) {
                this.entity.swingArm(this.entity.getActiveHand());
            }
        }
        ++this.breakingTime;
        int progress = (int) (this.breakingTime / (float) BREAK_TIME * 10.0F);
        if (progress != this.previousBreakProgress) {
            this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), this.doorPosition, progress);
            this.previousBreakProgress = progress;
        }
        if (this.breakingTime == BREAK_TIME && validDifficulty(this.entity)) {
            this.entity.world.setBlockToAir(this.doorPosition);
            this.entity.world.playEvent(1021, this.doorPosition, 0);
            this.entity.world.playEvent(2001, this.doorPosition, Block.getIdFromBlock(this.doorBlock));
        }
    }
}
