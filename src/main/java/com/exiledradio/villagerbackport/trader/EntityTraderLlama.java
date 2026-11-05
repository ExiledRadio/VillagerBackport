package com.exiledradio.villagerbackport.trader;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.14's trader llama: an ordinary llama that arrives on a lead held by a wandering trader.
 *
 * <p>From 1.14.4's {@code TraderLlama}:
 * <ul>
 *   <li>It wears the trader's blue and gold decor whenever it has no carpet (drawn client side).</li>
 *   <li>While the trader holds its lead it cannot be ridden, and it spits at whatever hurts the
 *       trader - {@link DefendTrader} - as well as at what hurts itself, as any llama does.</li>
 *   <li>It panics at speed 2 when hit.</li>
 *   <li>It leaves with the trader. While on the trader's lead its clock is the trader's minus one,
 *       so it goes the tick before the trader does and the lead goes with it rather than dropping as
 *       an item. Off the lead it has a clock of its own (47999 ticks) and leaves when that runs out -
 *       unless a player has tamed it, put it on their own lead, or is riding it, any of which keeps it
 *       for good. A player taking one with a lead is how 1.14 lets you keep them.</li>
 *   <li>Its foals are trader llamas too.</li>
 * </ul>
 */
public class EntityTraderLlama extends EntityLlama {

    private static final String TAG_DESPAWN = "DespawnDelay";

    private int despawnDelay = 47999;

    public EntityTraderLlama(World world) {
        super(world);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.addTask(1, new EntityAIPanic(this, 2.0D));
        this.targetTasks.addTask(1, new DefendTrader(this));
    }

    /** No riding while the trader has hold of it. */
    @Override
    protected void mountTo(EntityPlayer player) {
        if (!(getLeashHolder() instanceof EntityWanderingTrader)) {
            super.mountTo(player);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            tickDespawn();
        }
    }

    /** 1.14's {@code maybeDespawn}; kept on the entity tick, never in a goal. */
    private void tickDespawn() {
        if (!canLeave()) {
            return;
        }
        Entity holder = getLeashHolder();
        this.despawnDelay = holder instanceof EntityWanderingTrader
                ? ((EntityWanderingTrader) holder).getDespawnDelay() - 1
                : this.despawnDelay - 1;
        if (this.despawnDelay <= 0) {
            clearLeashed(true, false);
            setDead();
        }
    }

    /** Not tamed, not on anyone else's lead, nobody riding it. */
    private boolean canLeave() {
        boolean otherLead = getLeashed() && !(getLeashHolder() instanceof EntityWanderingTrader);
        return !isTame() && !otherLead && !isBeingRidden();
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    public void setDespawnDelay(int ticks) {
        this.despawnDelay = ticks;
    }

    /**
     * A trader llama foal. The llama's own breeding makes the strength and coat; they are carried
     * across through the saved form, since 1.12.2 keeps the strength setter private.
     */
    @Override
    public EntityLlama createChild(EntityAgeable other) {
        EntityLlama plain = super.createChild(other);
        EntityTraderLlama foal = new EntityTraderLlama(this.world);
        NBTTagCompound tag = new NBTTagCompound();
        plain.writeEntityToNBT(tag);
        foal.readEntityFromNBT(tag);
        return foal;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger(TAG_DESPAWN, this.despawnDelay);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey(TAG_DESPAWN, 99)) {
            this.despawnDelay = compound.getInteger(TAG_DESPAWN);
        }
    }

    /**
     * 1.14's {@code TraderLlamaDefendWanderingTraderGoal}: target whatever last hurt the trader
     * holding the lead, once per hurt. The llama's ranged attack does the rest - it spits.
     */
    static final class DefendTrader extends EntityAITarget {

        private final EntityLlama llama;

        @Nullable
        private EntityLivingBase offender;

        /**
         * The trader's revenge timer when this last fired, so one hit is answered once. The timer is
         * the trader's age at the hit, which is 0 for a hit on the tick it arrived, so "nothing
         * handled yet" has to be a value it can never take.
         */
        private int handledTimer = -1;

        DefendTrader(EntityLlama llama) {
            super(llama, false);
            this.llama = llama;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.llama.getLeashed() || !(this.llama.getLeashHolder() instanceof EntityWanderingTrader)) {
                return false;
            }
            EntityWanderingTrader trader = (EntityWanderingTrader) this.llama.getLeashHolder();
            this.offender = trader.getRevengeTarget();
            return trader.getRevengeTimer() != this.handledTimer && this.offender != null
                    && isSuitableTarget(this.offender, false);
        }

        @Override
        public void startExecuting() {
            this.llama.setAttackTarget(this.offender);
            Entity holder = this.llama.getLeashHolder();
            if (holder instanceof EntityWanderingTrader) {
                this.handledTimer = ((EntityWanderingTrader) holder).getRevengeTimer();
            }
            super.startExecuting();
        }
    }
}
