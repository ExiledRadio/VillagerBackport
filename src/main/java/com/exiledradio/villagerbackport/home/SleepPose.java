package com.exiledradio.villagerbackport.home;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;

/**
 * A sleeping villager's size: a small box lying in the bed, not a standing one sticking out of it.
 *
 * <h2>What went wrong without it</h2>
 * 1.12.2 has no poses, so a villager laid in a bed kept its standing box - 0.6 wide and 1.95 tall,
 * starting three quarters of a block above the bed. That reached into the block two above the bed:
 * a player could hit the villager by punching the air up there, and a block placed there smothered
 * it, because the suffocation test is taken at a fixed eye height of 1.62.
 *
 * <p>1.14's {@code Pose.SLEEPING} is a 0.2 by 0.2 box with its eyes at 0.2. The box is matched
 * here, on both sides - the server for collisions and the client for what a punch can hit, since
 * neither tells the other an entity's size. The eye height cannot be changed without overriding the
 * villager class, so suffocation is refused outright while asleep instead: with 1.14's eyes the head
 * is inside the bed, which is never a solid block, so a sleeper there cannot suffocate either.
 */
public final class SleepPose {

    private static final float STANDING_WIDTH = 0.6F;
    private static final float STANDING_HEIGHT = 1.95F;

    /** 1.14's {@code EntitySize.fixed(0.2F, 0.2F)} for the sleeping pose. */
    private static final float SLEEPING_SIZE = 0.2F;

    /**
     * {@code EntityAgeable.setSize}, which is protected. It only records the adult size; the box
     * itself is rebuilt by {@code setScaleForAge}, which applies the baby scale on top.
     */
    private static final Method SET_SIZE = ReflectionHelper.findMethod(
            EntityAgeable.class, "setSize", "func_70105_a", float.class, float.class);

    /**
     * Gives the villager the box that goes with being asleep or awake. Cheap when nothing changes,
     * so it is safe to call every tick.
     */
    public static void apply(EntityVillager villager, boolean asleep) {
        float width = asleep ? SLEEPING_SIZE : STANDING_WIDTH;
        float height = asleep ? SLEEPING_SIZE : STANDING_HEIGHT;
        float scale = villager.isChild() ? 0.5F : 1.0F;

        if (Math.abs(villager.width - width * scale) < 1.0E-4F
                && Math.abs(villager.height - height * scale) < 1.0E-4F) {
            return;
        }

        try {
            SET_SIZE.invoke(villager, width, height);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not resize a villager", e);
        }

        villager.setScaleForAge(villager.isChild());

        // Entity.setSize keeps the box's corner where it was rather than its centre, so a box that
        // shrinks or grows ends up off to one side of the villager. Setting the position again
        // rebuilds it round the middle.
        villager.setPosition(villager.posX, villager.posY, villager.posZ);
    }

    /** Keeps the server's size in step with sleeping, including for villagers loaded asleep. */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntity() instanceof EntityVillager && !event.getEntity().world.isRemote) {
            EntityVillager villager = (EntityVillager) event.getEntity();
            apply(villager, HomeSite.isSleeping(villager));
        }
    }

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (event.getSource() != DamageSource.IN_WALL || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();
        if (!villager.world.isRemote && HomeSite.isSleeping(villager)) {
            event.setCanceled(true);
        }
    }
}
