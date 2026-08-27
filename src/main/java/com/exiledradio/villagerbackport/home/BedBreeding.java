package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;

/**
 * Beds, not doors, decide whether a village grows.
 *
 * <h2>What 1.12.2 counts and what 1.14 counts</h2>
 * 1.12.2 counts doors. {@code EntityAIVillagerMating} breeds while the population is under 35% of
 * the door count, which is why a wall of doors in a field breeds a village. 1.14 counts beds: a
 * villager will only breed if there is a bed free for the child, which is why a 1.14 village grows
 * to the size someone built for it.
 *
 * <p>The door rule lives inside a vanilla AI task and cannot be edited without a coremod, so it is
 * left where it is and beds are added as a second requirement: a baby is only born if there is a
 * free bed near where it would be born. In a village with more doors than beds - which is every
 * 1.12.2 village - beds are the binding limit, which is the behaviour being ported.
 *
 * <p>Gating the birth rather than the courtship is deliberate. Whether two villagers are willing is
 * decided deep inside vanilla's task; whether a child appears is an event anybody can answer.
 */
public final class BedBreeding {

    /** How far from the child a bed may be and still count as its own. */
    private static final int BED_RANGE = 48;

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !ModConfig.homes.enabled
                || !ModConfig.homes.bedsForBreeding) {
            return;
        }

        if (!(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager baby = (EntityVillager) event.getEntity();

        // Only newborns. An adult joining the world is a chunk loading, not a birth.
        if (!baby.isChild()) {
            return;
        }

        if (!hasFreeBed(baby)) {
            event.setCanceled(true);
        }
    }

    /**
     * @return true if there is a bed near this villager that nobody has claimed
     *
     * <p>Counting claims rather than villagers, because a bed is spoken for by whoever claimed it -
     * a village of five villagers and five beds is full even if two of them are out in the fields.
     */
    public static boolean hasFreeBed(EntityVillager villager) {
        World world = villager.world;
        BlockPos origin = new BlockPos(villager);
        Set<BlockPos> taken = HomeSite.claimsNear(villager, BED_RANGE);

        int minChunkX = (origin.getX() - BED_RANGE) >> 4;
        int maxChunkX = (origin.getX() + BED_RANGE) >> 4;
        int minChunkZ = (origin.getZ() - BED_RANGE) >> 4;
        int maxChunkZ = (origin.getZ() + BED_RANGE) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (BlockPos bed : WorkstationIndex.bedsIn(world, chunkX, chunkZ)) {
                    if (Math.abs(bed.getX() - origin.getX()) > BED_RANGE
                            || Math.abs(bed.getZ() - origin.getZ()) > BED_RANGE) {
                        continue;
                    }

                    if (!taken.contains(bed)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
