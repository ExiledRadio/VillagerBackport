package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.entity.ai.EntityAIHarvestFarmland;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.ArrayList;

/**
 * Only farmers tend crops.
 *
 * <p>In the game, farming is the farmer's job alone. 1.12.2 hands its harvesting goal to a villager
 * once, as it spawns or loads, by profession rather than by career - and its farmer profession also
 * covers the fisherman, the shepherd and the fletcher. So all four farmed, and a villager that took
 * up farming later on never started until the world was reloaded.
 *
 * <p>Checked on the villager's regular poll, so a change of job is picked up within a second.
 */
public final class FarmWork {

    /** Where 1.12.2 puts the goal for a grown villager. */
    private static final int PRIORITY = 8;

    /** 1.12.2's walking speed for it. */
    private static final double SPEED = 0.6D;

    private FarmWork() {
    }

    public static void sync(EntityVillager villager) {
        if (villager.isChild()) {
            return;
        }

        boolean farmer = isFarmer(villager);
        EntityAIHarvestFarmland goal = goalOf(villager);

        if (farmer && goal == null) {
            villager.tasks.addTask(PRIORITY, new EntityAIHarvestFarmland(villager, SPEED));
        } else if (!farmer && goal != null) {
            villager.tasks.removeTask(goal);
        }
    }

    private static boolean isFarmer(EntityVillager villager) {
        if (Employment.isUnemployed(villager) || Employment.isNitwit(villager)) {
            return false;
        }

        try {
            VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
            int careerId = VillagerAccess.getCareerId(villager);
            if (profession == null || careerId <= 0) {
                return false;
            }

            VillagerRegistry.VillagerCareer career = profession.getCareer(careerId - 1);
            return career != null && "farmer".equals(career.getName());
        } catch (RuntimeException e) {
            // An out-of-range career, which a mod reassigning professions can cause.
            return false;
        }
    }

    private static EntityAIHarvestFarmland goalOf(EntityVillager villager) {
        for (EntityAITasks.EntityAITaskEntry entry
                : new ArrayList<EntityAITasks.EntityAITaskEntry>(villager.tasks.taskEntries)) {
            if (entry.action instanceof EntityAIHarvestFarmland) {
                return (EntityAIHarvestFarmland) entry.action;
            }
        }
        return null;
    }
}
