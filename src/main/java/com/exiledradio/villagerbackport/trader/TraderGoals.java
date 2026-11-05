package com.exiledradio.villagerbackport.trader;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The wandering trader's own AI goals - the ones 1.14 has and 1.12.2 lacks, or has only for
 * {@code EntityVillager}.
 *
 * <p>None of them touches a task list: goals run while the list is being iterated, and adding or
 * removing a task from inside one is a {@code ConcurrentModificationException} that deletes entities.
 */
final class TraderGoals {

    private TraderGoals() {
    }

    /**
     * 1.14's {@code UseItemGoal}: when a condition holds, put an item in the hand and use it, then put
     * it away with a sound. The trader's two are the invisibility potion at night and milk by day.
     *
     * <p>It takes no mutex bits, as in 1.14, so the trader keeps walking (or fleeing) while it drinks.
     * The drink itself is vanilla's item use: holding the hand active for the item's 32 ticks, then the
     * item's own {@code onItemUseFinish}, which is what applies the potion or clears the effects.
     */
    static final class UseItem extends EntityAIBase {

        enum Sound {
            DISAPPEARED, REAPPEARED;

            SoundEvent event() {
                return this == DISAPPEARED ? TraderContent.disappeared : TraderContent.reappeared;
            }
        }

        private final EntityWanderingTrader trader;
        private final ItemStack item;
        private final Sound finish;

        /** True: drink at night while visible. False: drink by day while invisible. */
        private final boolean atNight;

        UseItem(EntityWanderingTrader trader, ItemStack item, Sound finish, boolean atNight) {
            this.trader = trader;
            this.item = item;
            this.finish = finish;
            this.atNight = atNight;
            setMutexBits(0);
        }

        @Override
        public boolean shouldExecute() {
            // isDaytime is decided by the sky light the server tracks; the same check 1.14 makes.
            boolean day = this.trader.world.isDaytime();
            boolean invisible = this.trader.isInvisible();
            return this.atNight ? !day && !invisible : day && invisible;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.trader.isHandActive();
        }

        @Override
        public void startExecuting() {
            this.trader.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.item.copy());
            this.trader.setActiveHand(EnumHand.MAIN_HAND);
        }

        @Override
        public void resetTask() {
            this.trader.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.trader.playSound(this.finish.event(), 1.0F, this.trader.getRNG().nextFloat() * 0.2F + 0.9F);
        }
    }

    /**
     * 1.14's {@code TradeWithPlayerGoal}: stand still while someone has the trade screen open, and let
     * go of them when they walk off or close it.
     *
     * <p>1.12.2's {@code EntityAITradePlayer} is the same goal but takes an {@code EntityVillager}.
     */
    static final class TradeWithPlayer extends EntityAIBase {

        private final EntityWanderingTrader trader;

        TradeWithPlayer(EntityWanderingTrader trader) {
            this.trader = trader;
            setMutexBits(5);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.trader.isEntityAlive() || this.trader.isInWater() || !this.trader.onGround
                    || this.trader.velocityChanged) {
                return false;
            }
            EntityPlayer customer = this.trader.getCustomer();
            if (customer == null) {
                return false;
            }
            if (this.trader.getDistanceSq(customer) > 16.0D) {
                return false;
            }
            return customer.openContainer instanceof ContainerMerchant;
        }

        @Override
        public void startExecuting() {
            this.trader.getNavigator().clearPath();
        }

        @Override
        public void resetTask() {
            this.trader.setCustomer(null);
        }
    }

    /** 1.14's {@code LookAtTradingPlayerGoal}: watch whoever is trading, and nobody else while they are. */
    static final class LookAtCustomer extends EntityAIWatchClosest {

        private final EntityWanderingTrader trader;

        LookAtCustomer(EntityWanderingTrader trader) {
            super(trader, EntityPlayer.class, 8.0F);
            this.trader = trader;
        }

        @Override
        public boolean shouldExecute() {
            if (this.trader.isTrading()) {
                this.closestEntity = this.trader.getCustomer();
                return true;
            }
            return false;
        }
    }

    /**
     * 1.14's {@code WanderToPositionGoal}: walk to the spot the trader was sent to.
     *
     * <p>Far away it heads ten blocks at a time along the straight line, so no single path is longer
     * than a mob's follow range allows - which is also why this never needs that range raised. Within
     * ten blocks it paths to the spot itself. Within {@code stopDistance} the goal ends, and ending -
     * for any reason, including being interrupted by panic or a zombie - forgets the target for good,
     * as 1.14's does. From then on the trader's home distance of 16 keeps it nearby.
     */
    static final class WanderToTarget extends EntityAIBase {

        private final EntityWanderingTrader trader;
        private final double stopDistance;
        private final double speed;

        WanderToTarget(EntityWanderingTrader trader, double stopDistance, double speed) {
            this.trader = trader;
            this.stopDistance = stopDistance;
            this.speed = speed;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            BlockPos target = this.trader.getWanderTarget();
            return target != null && isFarther(target, this.stopDistance);
        }

        @Override
        public void resetTask() {
            this.trader.setWanderTarget(null);
            this.trader.getNavigator().clearPath();
        }

        @Override
        public void updateTask() {
            BlockPos target = this.trader.getWanderTarget();
            if (target == null || !this.trader.getNavigator().noPath()) {
                return;
            }
            if (isFarther(target, 10.0D)) {
                Vec3d step = new Vec3d(target.getX() - this.trader.posX, target.getY() - this.trader.posY,
                        target.getZ() - this.trader.posZ).normalize().scale(10.0D)
                        .add(this.trader.posX, this.trader.posY, this.trader.posZ);
                this.trader.getNavigator().tryMoveToXYZ(step.x, step.y, step.z, this.speed);
            } else {
                this.trader.getNavigator().tryMoveToXYZ(target.getX(), target.getY(), target.getZ(), this.speed);
            }
        }

        private boolean isFarther(BlockPos target, double distance) {
            return target.distanceSqToCenter(this.trader.posX, this.trader.posY, this.trader.posZ) > distance * distance;
        }
    }
}
