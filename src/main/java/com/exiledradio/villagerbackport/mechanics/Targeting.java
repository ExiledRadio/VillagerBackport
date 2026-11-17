package com.exiledradio.villagerbackport.mechanics;

import com.exiledradio.villagerbackport.ModConfig;
import com.google.common.base.Predicate;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;

/**
 * Who a mob may pick as a target, as current Java decides it, for 1.12.2's {@code EntityAINearestAttackableTarget}.
 *
 * <ul>
 *   <li>1.18 (21w37a, ravagers 1.18-pre8): illagers and ravagers never target a baby villager ({@code AbstractIllager.canAttack},
 *       the ravager's own predicate).</li>
 *   <li>1.14 (19w08a): {@code TargetingConditions} scales a mob's follow range by the target's visibility
 *       ({@code LivingEntity.getVisibilityPercent}): invisible, 0.7 x its armor cover (at least 0.1); sneaking 0.8;
 *       wearing the looker's own head 0.5; never under 2 blocks. 1.12.2 does this only for players found through
 *       {@code getNearestAttackablePlayer}, so guardians (which target any living thing) saw invisible players and
 *       zombies and illagers saw invisible villagers.</li>
 * </ul>
 */
public final class Targeting {

    private static final String TARGET_CLASS = "field_75307_b";
    private static final String SELECTOR = "field_82643_g";

    private Targeting() {
    }

    /** @return true if a villager target is allowed under {@link ModConfig.Mechanics#illagersSpareBabies} */
    public static boolean sparesBaby(@Nullable EntityLivingBase target) {
        return !(ModConfig.mechanics.illagersSpareBabies && target instanceof EntityVillager && target.isChild());
    }

    /**
     * @return false if {@link ModConfig.Mechanics#invisibilityHides} is on and the target is too hidden to be noticed
     * from where the looker stands (1.14's {@code TargetingConditions} range test)
     */
    public static boolean noticed(EntityLiving looker, @Nullable EntityLivingBase target) {
        if (target == null || !ModConfig.mechanics.invisibilityHides || !target.isInvisible()) {
            return true;
        }
        IAttributeInstance follow = looker.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        double range = follow == null ? 16.0D : follow.getAttributeValue();
        double reach = Math.max(range * visibility(looker, target), 2.0D);
        return looker.getDistanceSq(target) <= reach * reach;
    }

    /** 1.14's {@code LivingEntity.getVisibilityPercent}. */
    public static double visibility(@Nullable EntityLiving looker, EntityLivingBase target) {
        double d = 1.0D;
        if (target.isSneaking()) {
            d *= 0.8D;
        }
        if (target.isInvisible()) {
            float cover = armorCover(target);
            if (cover < 0.1F) {
                cover = 0.1F;
            }
            d *= 0.7D * cover;
        }
        if (looker != null) {
            ItemStack head = target.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
            if (head.getItem() == Items.SKULL) {
                int type = head.getMetadata();
                if (looker instanceof EntitySkeleton && type == 0 || looker instanceof EntityZombie && type == 2
                        || looker instanceof EntityCreeper && type == 4) {
                    d *= 0.5D;
                }
            }
        }
        return d;
    }

    /** 1.14's {@code getArmorCoverPercentage}: the share of the four armor slots that hold something. */
    static float armorCover(EntityLivingBase target) {
        int worn = 0;
        int slots = 0;
        for (ItemStack stack : target.getArmorInventoryList()) {
            slots++;
            if (!stack.isEmpty()) {
                worn++;
            }
        }
        return slots == 0 ? 0.0F : (float) worn / slots;
    }

    /**
     * Adds a test to every nearest-target task in the list whose target class can hold {@code targetClass}: a villager
     * task for {@code EntityVillager}, a guardian's any-living task for {@code EntityLivingBase}.
     *
     * @return how many tasks were changed
     */
    public static int restrict(final EntityLiving owner, Class<?> targetClass, final TargetTest test) {
        int changed = 0;
        for (EntityAITasks.EntityAITaskEntry entry : owner.targetTasks.taskEntries) {
            if (!(entry.action instanceof EntityAINearestAttackableTarget)) {
                continue;
            }
            EntityAINearestAttackableTarget<?> task = (EntityAINearestAttackableTarget<?>) entry.action;
            Class<?> cls = ObfuscationReflectionHelper.getPrivateValue(EntityAINearestAttackableTarget.class, task, TARGET_CLASS);
            if (cls == null || !cls.isAssignableFrom(targetClass) && !targetClass.isAssignableFrom(cls)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            final Predicate<Object> old = (Predicate<Object>) ObfuscationReflectionHelper.getPrivateValue(
                    EntityAINearestAttackableTarget.class, task, SELECTOR);
            if (old instanceof Restricted) {
                continue; // already done (an entity joining a world twice)
            }
            ObfuscationReflectionHelper.setPrivateValue(EntityAINearestAttackableTarget.class, task,
                    new Restricted(old, owner, test), SELECTOR);
            changed++;
        }
        return changed;
    }

    /** A target test for one looker. */
    public interface TargetTest {
        boolean allows(EntityLiving looker, EntityLivingBase target);
    }

    /** The task's own selector (vanilla's suitability test), then ours. */
    static final class Restricted implements Predicate<Object> {
        private final Predicate<Object> vanilla;
        private final EntityLiving owner;
        private final TargetTest test;

        Restricted(Predicate<Object> vanilla, EntityLiving owner, TargetTest test) {
            this.vanilla = vanilla;
            this.owner = owner;
            this.test = test;
        }

        @Override
        public boolean apply(@Nullable Object target) {
            return (this.vanilla == null || this.vanilla.apply(target))
                    && (!(target instanceof EntityLivingBase) || this.test.allows(this.owner, (EntityLivingBase) target));
        }
    }
}
