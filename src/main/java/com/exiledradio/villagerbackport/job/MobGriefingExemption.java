package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;

/**
 * Villagers farming and picking up food with the {@code mobGriefing} gamerule off, when
 * {@code jobs.ignoreMobGriefing} says so.
 *
 * <h2>Why the gamerule stops them</h2>
 * In 1.12.2 and in current Minecraft alike, a villager may only harvest, replant or pick items up
 * while {@code mobGriefing} is on. Plenty of worlds turn it off to keep creepers from blowing holes
 * in things, and their farmers then stand beside ripe fields doing nothing - which looks like a bug
 * in this mod, and is the vanilla rule.
 *
 * <h2>Why not just answer yes to the gamerule for villagers</h2>
 * Forge lets a mod overrule the gamerule for one entity, but that answer goes to every question
 * asked of it, and one of those is whether a mob landing on farmland tramples it. Saying yes for
 * villagers would let them flatten the very fields they are meant to be tending. So the two things
 * this is for are let through by name instead: the farming goal asks {@link #allowsFarming}, and
 * the item pickup 1.12.2 skips is done here, the same way it does it.
 */
public final class MobGriefingExemption {

    /** {@code EntityLiving.updateEquipmentIfNeeded}, which the villager overrides to take food and seeds. */
    private static Method pickUp;
    private static boolean pickUpMissing;

    /** @return true if this villager may farm: the gamerule allows it, or the setting overrides it */
    public static boolean allowsFarming(EntityVillager villager) {
        return ModConfig.jobs.ignoreMobGriefing || ForgeEventFactory.getMobGriefingEvent(villager.world, villager);
    }

    /**
     * 1.12.2's own pickup, for when the gamerule kept it from running: every item lying within a
     * block of the villager, offered to it the same way.
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!ModConfig.jobs.ignoreMobGriefing || !(event.getEntityLiving() instanceof EntityVillager)) {
            return;
        }
        EntityVillager villager = (EntityVillager) event.getEntityLiving();
        if (villager.world.isRemote || !villager.canPickUpLoot() || villager.isDead
                || ForgeEventFactory.getMobGriefingEvent(villager.world, villager)) {
            // With the gamerule on, 1.12.2 already does this itself.
            return;
        }

        Method method = pickUp();
        if (method == null) {
            return;
        }
        for (EntityItem item : villager.world.getEntitiesWithinAABB(EntityItem.class,
                villager.getEntityBoundingBox().grow(1.0D, 0.0D, 1.0D))) {
            if (!item.isDead && !item.getItem().isEmpty() && !item.cannotPickup()) {
                try {
                    method.invoke(villager, item);
                } catch (Exception e) {
                    VillagerBackport.LOGGER.warn("Could not let a villager pick up an item.", e);
                    pickUpMissing = true;
                    return;
                }
            }
        }
    }

    private static Method pickUp() {
        if (pickUp == null && !pickUpMissing) {
            try {
                pickUp = ReflectionHelper.findMethod(EntityLiving.class, "updateEquipmentIfNeeded", "func_175445_a",
                        EntityItem.class);
                pickUp.setAccessible(true);
            } catch (Exception e) {
                VillagerBackport.LOGGER.warn("Villagers cannot pick items up with mobGriefing off: the pickup method "
                        + "was not found.", e);
                pickUpMissing = true;
            }
        }
        return pickUp;
    }
}
