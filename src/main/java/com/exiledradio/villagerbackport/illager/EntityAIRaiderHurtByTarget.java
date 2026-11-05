package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.raid.Raids;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;

/**
 * Fights back, but never against another raider: 1.14's {@code HurtByTargetGoal(this,
 * AbstractRaiderEntity.class)}.
 *
 * <p>1.12.2's goal looks the same but is not: the classes it is given only stop it calling those
 * mobs for help, and it still turns on whoever hit it. A pillager's stray bolt in a ravager's flank
 * was enough to set the ravager on the pillagers.
 */
public final class EntityAIRaiderHurtByTarget extends EntityAIHurtByTarget {

    public EntityAIRaiderHurtByTarget(EntityCreature creature) {
        super(creature, true);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase attacker = this.taskOwner.getRevengeTarget();
        return attacker != null && !Raids.isRaider(attacker) && super.shouldExecute();
    }
}
