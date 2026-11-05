package com.exiledradio.villagerbackport.raid;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Which villagers heard a bell, and when: 1.14's {@code HEARD_BELL_TIME} memory.
 *
 * <p>Kept in memory only, as 1.14 keeps it. Hiding lasts fifteen seconds; a villager saved halfway
 * through and loaded again simply comes out, which is what it would have done anyway.
 */
public final class BellHearing {

    /** 1.14's bell reaches every villager within 32 blocks. */
    public static final double REACH = 32.0D;

    /** 1.14's {@code ExpireHidingTask}: out again fifteen seconds after the bell. */
    public static final long HIDE_TICKS = 300L;

    private static final Map<EntityVillager, Long> HEARD = new WeakHashMap<EntityVillager, Long>();

    private BellHearing() {
    }

    /** Tells every villager in earshot that the bell at this position just rang. */
    public static void ring(World world, BlockPos pos) {
        if (world.isRemote) {
            return;
        }

        long now = world.getTotalWorldTime();
        double reachSq = REACH * REACH;

        for (EntityVillager villager : world.getEntitiesWithinAABB(EntityVillager.class,
                new AxisAlignedBB(pos).grow(REACH))) {
            if (villager.isEntityAlive() && villager.getDistanceSq(pos) <= reachSq) {
                HEARD.put(villager, now);
            }
        }
    }

    /** @return true if this villager heard a bell within the last fifteen seconds */
    public static boolean heardRecently(EntityVillager villager) {
        Long heard = HEARD.get(villager);
        return heard != null && villager.world.getTotalWorldTime() - heard < HIDE_TICKS;
    }

    /** The villager has hidden for long enough, or the bell stopped mattering. */
    public static void forget(EntityVillager villager) {
        HEARD.remove(villager);
    }
}
