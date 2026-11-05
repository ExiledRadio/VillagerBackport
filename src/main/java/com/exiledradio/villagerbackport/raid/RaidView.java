package com.exiledradio.villagerbackport.raid;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Whichever raid system is running, as villagers need to see it.
 *
 * <p>Everything a villager does about a raid - hiding, ringing the bell, celebrating - asks this
 * and nothing else, so it is written once and works whether the raids are Raids Backport's or,
 * later, this mod's own.
 */
public interface RaidView {

    /** @return the raid covering this position - 1.14's {@code findRaid}, 96 blocks - or null */
    @Nullable
    Raid raidAt(World world, BlockPos pos);

    /** @return true if this entity is taking part in a raid right now */
    boolean inRaid(Entity entity);

    /** The parts of a raid a villager reacts to, in 1.14's terms. */
    interface Raid {

        /** Waves are still to come or still fighting. */
        boolean isActive();

        /** Every wave beaten. Villagers come out and celebrate. */
        boolean isVictory();

        /** The village fell or the raid timed out. */
        boolean isLoss();

        /** Over either way, and about to be forgotten. */
        boolean isStopped();

        /**
         * The first raiders have arrived. Before this is the 1.14 "pre-raid", when villagers gather
         * at the bell and ring it; after it they hide.
         */
        boolean hasFirstWaveSpawned();
    }

    /** No raid system at all: there is never a raid anywhere. */
    RaidView NONE = new RaidView() {
        @Nullable
        @Override
        public Raid raidAt(World world, BlockPos pos) {
            return null;
        }

        @Override
        public boolean inRaid(Entity entity) {
            return false;
        }
    };
}
