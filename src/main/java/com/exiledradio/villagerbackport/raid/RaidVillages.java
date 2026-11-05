package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.job.JobSite;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the village is, as 1.14's raids see it.
 *
 * <h2>1.14's rule</h2>
 * A position is in a village when its 16-block section is within one section of a section holding
 * an <em>occupied</em> point of interest: a bed someone sleeps in, a workstation someone works at,
 * a bell someone meets at. Not doors, which is what 1.12.2's own villages are made of.
 *
 * <p>Here the claims live on the villagers, so the villagers are asked: every bed and workstation
 * claimed by a villager nearby, and every bell with a villager within 48 blocks of it. 1.12.2's
 * door village counts as well, so a village whose villagers have no beds still has raids.
 *
 * <h2>Cost</h2>
 * Every raider asks this every few ticks while it walks in, so answers are kept per section for two
 * seconds. Villages change on the scale of minutes.
 */
public final class RaidVillages {

    private static final long CACHE_TICKS = 40L;

    /** How far from a point of interest its villager can be. 1.14 claims within 48. */
    private static final double CLAIM_REACH = 48.0D;

    private static final Map<World, Map<Long, Entry>> CACHE = new java.util.WeakHashMap<World, Map<Long, Entry>>();

    private RaidVillages() {
    }

    /** @return true if this position counts as inside a village: 1.14's {@code isVillage} */
    public static boolean isVillage(World world, BlockPos pos) {
        long key = sectionKey(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        long now = world.getTotalWorldTime();

        Map<Long, Entry> cache = CACHE.get(world);
        if (cache == null) {
            cache = new HashMap<Long, Entry>();
            CACHE.put(world, cache);
        }

        Entry entry = cache.get(key);
        if (entry == null || now - entry.at > CACHE_TICKS) {
            if (cache.size() > 4096) {
                cache.clear();
            }
            entry = new Entry(now, compute(world, pos));
            cache.put(key, entry);
        }
        return entry.village;
    }

    private static boolean compute(World world, BlockPos pos) {
        int sx = pos.getX() >> 4;
        int sy = pos.getY() >> 4;
        int sz = pos.getZ() >> 4;

        // The 3x3x3 sections around this one.
        AxisAlignedBB sections = new AxisAlignedBB((sx - 1) << 4, (sy - 1) << 4, (sz - 1) << 4,
                (sx + 2) << 4, (sy + 2) << 4, (sz + 2) << 4);

        if (!occupiedNear(world, sections).isEmpty()) {
            return true;
        }

        // 1.12.2's own door village, for villages built before beds or with homes turned off.
        net.minecraft.village.Village doors = world.getVillageCollection().getNearestVillage(pos, 32);
        return doors != null && doors.getCenter().distanceSq(pos) <= (double) doors.getVillageRadius() * doors.getVillageRadius();
    }

    /**
     * @return where a raid started here should be centred: the middle of the occupied points of
     *         interest within 64 blocks, as 1.14 has it, or the position itself if there are none
     */
    public static BlockPos centreFor(World world, BlockPos pos) {
        List<BlockPos> points = occupiedNear(world, new AxisAlignedBB(pos).grow(64.0D));
        if (points.isEmpty()) {
            return pos;
        }

        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (BlockPos point : points) {
            x += point.getX();
            y += point.getY();
            z += point.getZ();
        }
        return new BlockPos(x / points.size(), y / points.size(), z / points.size());
    }

    /** Claimed beds and workstations, and bells villagers are near, inside the box. */
    private static List<BlockPos> occupiedNear(World world, AxisAlignedBB box) {
        List<BlockPos> found = new ArrayList<BlockPos>();
        List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class, box.grow(CLAIM_REACH));

        for (EntityVillager villager : villagers) {
            if (villager.isDead) {
                continue;
            }
            addIfInside(found, box, HomeSite.get(villager));
            addIfInside(found, box, JobSite.get(villager));
        }

        if (!villagers.isEmpty()) {
            int minCx = (int) Math.floor(box.minX) >> 4;
            int maxCx = (int) Math.floor(box.maxX) >> 4;
            int minCz = (int) Math.floor(box.minZ) >> 4;
            int maxCz = (int) Math.floor(box.maxZ) >> 4;
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cz = minCz; cz <= maxCz; cz++) {
                    for (BlockPos bell : WorkstationIndex.bellsIn(world, cx, cz)) {
                        if (insideBox(box, bell) && anyNear(villagers, bell)) {
                            found.add(bell);
                        }
                    }
                }
            }
        }
        return found;
    }

    private static boolean anyNear(List<EntityVillager> villagers, BlockPos bell) {
        for (EntityVillager villager : villagers) {
            if (villager.getDistanceSq(bell) <= CLAIM_REACH * CLAIM_REACH) {
                return true;
            }
        }
        return false;
    }

    private static void addIfInside(List<BlockPos> found, AxisAlignedBB box, @javax.annotation.Nullable BlockPos pos) {
        if (pos != null && insideBox(box, pos)) {
            found.add(pos);
        }
    }

    private static boolean insideBox(AxisAlignedBB box, BlockPos pos) {
        return pos.getX() >= box.minX && pos.getX() < box.maxX && pos.getY() >= box.minY && pos.getY() < box.maxY
                && pos.getZ() >= box.minZ && pos.getZ() < box.maxZ;
    }

    private static long sectionKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFL) << 42 | ((long) z & 0x3FFFFFL) << 20 | ((long) y & 0xFFFFFL);
    }

    private static final class Entry {
        final long at;
        final boolean village;

        Entry(long at, boolean village) {
            this.at = at;
            this.village = village;
        }
    }
}
