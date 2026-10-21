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
 *
 * <p>A farmer's goal is this mod's {@link EntityAIFarm}, which farms by 1.14's rules - see there.
 * One still holding 1.12.2's own goal has it swapped on the same poll.
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
        boolean hasOurs = false;

        // Every farming goal, not just the first: 1.12.2 can hand a villager its own goal again
        // later (it does on growing up and on loading), which would sit beside this one.
        for (EntityAITasks.EntityAITaskEntry entry
                : new ArrayList<EntityAITasks.EntityAITaskEntry>(villager.tasks.taskEntries)) {
            if (entry.action instanceof EntityAIHarvestFarmland) {
                villager.tasks.removeTask(entry.action);
            } else if (entry.action instanceof EntityAIFarm) {
                if (farmer && !hasOurs) {
                    hasOurs = true;
                } else {
                    villager.tasks.removeTask(entry.action);
                }
            }
        }

        if (farmer && !hasOurs) {
            villager.tasks.addTask(PRIORITY, new EntityAIFarm(villager, SPEED));
        }
    }

    public static boolean isFarmer(EntityVillager villager) {
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
}
