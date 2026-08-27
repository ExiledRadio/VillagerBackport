package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Working out what a village looks like, for something to draw.
 *
 * <p>Reads the same bed index the villagers themselves read, so what gets drawn is what they can
 * actually see rather than a second opinion assembled for the picture. A debug view that disagrees
 * with the thing it is debugging is worse than none.
 */
public final class VillageOutline {

    private VillageOutline() {
    }

    /** @return every bed the index knows about within range of a point. */
    public static List<BlockPos> bedsAround(World world, BlockPos centre, int radius) {
        List<BlockPos> beds = new ArrayList<BlockPos>();

        int minChunkX = (centre.getX() - radius) >> 4;
        int maxChunkX = (centre.getX() + radius) >> 4;
        int minChunkZ = (centre.getZ() - radius) >> 4;
        int maxChunkZ = (centre.getZ() + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (BlockPos bed : WorkstationIndex.bedsIn(world, chunkX, chunkZ)) {
                    if (Math.abs(bed.getX() - centre.getX()) <= radius
                            && Math.abs(bed.getZ() - centre.getZ()) <= radius
                            && Math.abs(bed.getY() - centre.getY()) <= radius) {
                        beds.add(bed);
                    }
                }
            }
        }

        return beds;
    }

    /**
     * @return every bed position a villager has claimed within range
     *
     * <p>Read off the villagers rather than from any register of claims, because the villagers are
     * the register - see {@link HomeSite}.
     */
    public static Set<BlockPos> claimedAround(World world, BlockPos centre, int radius) {
        AxisAlignedBB box = new AxisAlignedBB(centre).grow(radius);
        Set<BlockPos> claimed = new HashSet<BlockPos>();

        for (EntityVillager villager : world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            BlockPos bed = HomeSite.get(villager);
            if (bed != null) {
                claimed.add(bed);
            }
        }

        return claimed;
    }

    /**
     * @return a box just containing every one of these beds, or null if there are none
     *
     * <p>Drawn around the beds rather than around 1.12.2's village radius on purpose. The village
     * radius is a circle grown from doors and says nothing about where anybody sleeps; the extent of
     * the beds is the shape that actually decides how far this village can grow.
     */
    @Nullable
    public static AxisAlignedBB boxOf(List<BlockPos> beds) {
        if (beds.isEmpty()) {
            return null;
        }

        BlockPos first = beds.get(0);
        int minX = first.getX();
        int minY = first.getY();
        int minZ = first.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        for (BlockPos bed : beds) {
            minX = Math.min(minX, bed.getX());
            minY = Math.min(minY, bed.getY());
            minZ = Math.min(minZ, bed.getZ());
            maxX = Math.max(maxX, bed.getX());
            maxY = Math.max(maxY, bed.getY());
            maxZ = Math.max(maxZ, bed.getZ());
        }

        return new AxisAlignedBB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
