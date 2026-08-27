package com.exiledradio.villagerbackport.job;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Workstations a villager has tried and failed to get to, remembered briefly.
 *
 * <h2>Why remembering matters</h2>
 * Without it a villager picks the nearest workstation, fails to reach it, gives the claim up, and
 * then picks the same one again - because it is still the nearest. That is a loop, and it is what
 * kept villagers walking at a blocked door for minutes on end.
 *
 * <p>So a failure is remembered for long enough that the next search passes over it and looks at the
 * next one out. A few rounds of that and the villager settles on something it can actually walk to.
 *
 * <h2>Why it is forgotten again</h2>
 * The reason a workstation was unreachable is usually temporary - a door someone barricaded, a
 * villager asleep in the way, a path that happened not to solve. A permanent blacklist would leave a
 * villager refusing a workstation long after the wall in front of it came down. A minute is long
 * enough to stop the loop and short enough that clearing the way is noticed.
 *
 * <p>Kept in memory rather than on the villager. This is a note about a failed attempt, not
 * something about the villager worth saving - and forgetting it all when the game restarts just
 * means everyone tries once more, which is the right behaviour anyway.
 */
final class UnreachableSites {

    /** Ticks a failure is remembered for. */
    private static final long FORGET_AFTER = 1200L;

    private static final Map<UUID, Map<BlockPos, Long>> FAILED = new HashMap<UUID, Map<BlockPos, Long>>();

    private UnreachableSites() {
    }

    static boolean recentlyFailed(EntityVillager villager, BlockPos pos, long now) {
        Map<BlockPos, Long> mine = FAILED.get(villager.getUniqueID());
        if (mine == null) {
            return false;
        }

        Long when = mine.get(pos);
        return when != null && now - when < FORGET_AFTER;
    }

    /**
     * Clears every villager's grudge against one position.
     *
     * <p>For when a workstation appears there. A failure recorded against a position outlives the
     * block it was about - break a lectern while a villager is on its way to it and the attempt
     * fails, which is remembered - so a lectern put straight back could be ignored for the rest of
     * the minute by the one villager most likely to want it. The note was about a block that no
     * longer exists; a new one deserves a fresh attempt.
     */
    static void forget(BlockPos pos) {
        for (Map<BlockPos, Long> mine : FAILED.values()) {
            mine.remove(pos);
        }
    }

    static void remember(EntityVillager villager, BlockPos pos, long now) {
        Map<BlockPos, Long> mine = FAILED.get(villager.getUniqueID());

        if (mine == null) {
            mine = new HashMap<BlockPos, Long>();
            FAILED.put(villager.getUniqueID(), mine);
        }

        mine.put(pos, now);
        prune(now);
    }

    /**
     * Drops everything old.
     *
     * <p>Swept on write rather than on a timer: writes are rare - a villager only records one when it
     * has actually failed to reach somewhere - and it means nothing has to own a schedule.
     */
    private static void prune(long now) {
        Iterator<Map.Entry<UUID, Map<BlockPos, Long>>> villagers = FAILED.entrySet().iterator();

        while (villagers.hasNext()) {
            Map<BlockPos, Long> mine = villagers.next().getValue();
            Iterator<Map.Entry<BlockPos, Long>> sites = mine.entrySet().iterator();

            while (sites.hasNext()) {
                if (now - sites.next().getValue() >= FORGET_AFTER) {
                    sites.remove();
                }
            }

            if (mine.isEmpty()) {
                villagers.remove();
            }
        }
    }
}
