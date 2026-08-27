package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.job.JobSite;
import com.exiledradio.villagerbackport.job.WorkPathing;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Villagers claiming a bed to call home.
 *
 * <h2>Deliberately the same design as claiming a workstation</h2>
 * The problem is the same one, so the answer is: claiming runs on the villager's own tick rather
 * than inside an AI goal, because a claim is bookkeeping and a villager standing beside a free bed
 * should not have to win the movement mutex to notice it. That mistake cost a great deal of time
 * when workstations were built, and there is no reason to repeat it here.
 *
 * <p>A bed is only claimed if the villager can actually walk to it and end up touching it, which is
 * the same standard a workstation is held to, and for the same reason: a home behind a wall is not
 * a home, and holding a claim on one stops the villager looking for a real one.
 */
public final class HomeClaims {

    /** Ticks between attempts for a villager with no bed. Loose - homes are not urgent. */
    private static final int CLAIM_INTERVAL = 100;

    /** How many beds one search may ask the navigator about. Each question is a pathfind. */
    private static final int MAX_PATH_CHECKS = 6;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();

        if (villager.world.isRemote || !ModConfig.homes.enabled) {
            return;
        }

        // Children included. 1.14's baby schedule is IDLE, PLAY, IDLE, PLAY, REST at 12000 - the
        // same bedtime the adults keep - and a baby acquires a home the same way, so it needs a bed
        // of its own to go to.
        if (villager.getCustomer() != null) {
            return;
        }

        // Offset by entity id so a village does not run every villager's search on one tick.
        if ((villager.ticksExisted + villager.getEntityId()) % CLAIM_INTERVAL != 0) {
            return;
        }

        if (HomeSite.validated(villager) == null) {
            claimNearby(villager);
        }
    }

    /**
     * Finds the nearest bed nobody has taken and takes it.
     *
     * <p>Reads the same per-chunk index the work search uses, which already knows where every bed
     * is because it records them in the same pass that records workstations. Nothing here reads the
     * world block by block, and unloaded chunks are skipped rather than generated.
     */
    @Nullable
    public static BlockPos claimNearby(EntityVillager villager) {
        World world = villager.world;
        int radius = ModConfig.homes.searchRadius;
        final BlockPos origin = new BlockPos(villager);

        Set<BlockPos> taken = HomeSite.claimsNear(villager, radius);
        List<BlockPos> candidates = new ArrayList<BlockPos>();

        int minChunkX = (origin.getX() - radius) >> 4;
        int maxChunkX = (origin.getX() + radius) >> 4;
        int minChunkZ = (origin.getZ() - radius) >> 4;
        int maxChunkZ = (origin.getZ() + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (BlockPos pos : WorkstationIndex.bedsIn(world, chunkX, chunkZ)) {
                    if (Math.abs(pos.getX() - origin.getX()) > radius
                            || Math.abs(pos.getZ() - origin.getZ()) > radius
                            || Math.abs(pos.getY() - origin.getY()) > radius) {
                        continue;
                    }

                    if (!taken.contains(pos)) {
                        candidates.add(pos);
                    }
                }
            }
        }

        BlockPos best = chooseReachable(villager, origin, candidates);

        if (best != null) {
            HomeSite.set(villager, best);
            VillageDebug.say(villager, "bed: claimed one at "
                    + best.getX() + ", " + best.getY() + ", " + best.getZ());
        } else if (VillageDebug.on() && !candidates.isEmpty()) {
            VillageDebug.repeat(villager, "bed-unreachable", "bed: " + candidates.size()
                    + " free nearby but none it can walk to");
        }

        return best;
    }

    /** Picks the nearest bed the villager can actually walk to and touch. */
    @Nullable
    private static BlockPos chooseReachable(EntityVillager villager, final BlockPos origin,
                                            List<BlockPos> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        Collections.sort(candidates, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos a, BlockPos b) {
                return Double.compare(origin.distanceSq(a), origin.distanceSq(b));
            }
        });

        double pathRange = WorkPathing.effectiveRange(villager);
        int asked = 0;

        for (BlockPos pos : candidates) {
            // Sorted by distance, so the first one out of the navigator's reach means the rest are.
            if (origin.distanceSq(pos) > pathRange * pathRange || asked >= MAX_PATH_CHECKS) {
                break;
            }

            asked++;

            if (canReach(villager, pos)) {
                return pos;
            }
        }

        return null;
    }

    /** @return true if a path exists that ends against the bed rather than merely near it. */
    private static boolean canReach(EntityVillager villager, BlockPos pos) {
        if (JobSite.isTouching(villager, pos)) {
            return true;
        }

        Path path = WorkPathing.pathTo(villager, pos);
        if (path == null) {
            return false;
        }

        PathPoint end = path.getFinalPathPoint();
        if (end == null) {
            return false;
        }

        return Math.abs(end.x - pos.getX()) <= 1
                && Math.abs(end.y - pos.getY()) <= 1
                && Math.abs(end.z - pos.getZ()) <= 1;
    }
}
