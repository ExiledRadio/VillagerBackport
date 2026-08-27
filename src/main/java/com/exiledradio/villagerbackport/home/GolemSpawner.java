package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.job.Employment;

import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * 1.14's iron golem spawning, ported rule for rule.
 *
 * <h2>The rules, from {@code VillagerEntity}</h2>
 * A villager counts towards a golem only if all of this holds:
 *
 * <ul>
 *   <li>it has a profession - {@code NONE} and {@code NITWIT} are excluded</li>
 *   <li>it has slept within the last 24000 ticks</li>
 *   <li>it has worked at its job site within the last 36000 ticks</li>
 *   <li>it has not seen a golem spawn in the last 600 ticks</li>
 * </ul>
 *
 * <p>When one of them tries, it counts the eligible villagers within ten blocks - 1.14 stops
 * counting at five - and spawns a golem if there are enough. A panicking villager needs three; two
 * villagers gossiping need five. On success <em>every</em> villager nearby is marked as having seen
 * a golem, not merely the ones that counted, which is the thirty-second cooldown that stops a
 * frightened village producing a wall of them.
 *
 * <p>The sleeping and working conditions are the whole reason iron golem farms are built the way
 * they are from 1.14 onward, and they are why this had to wait for beds: a villager that cannot
 * reach a bed never qualifies, however frightened it is.
 */
public final class GolemSpawner {

    /** 1.14's constants, named. */
    private static final long SLEPT_WITHIN = 24000L;
    private static final long WORKED_WITHIN = 36000L;
    private static final long GOLEM_SEEN_WITHIN = 600L;
    private static final double NEIGHBOUR_RANGE = 10.0D;
    private static final int COUNT_LIMIT = 5;

    /** How many villagers a frightened one needs beside it, and how many gossiping ones need. */
    public static final int PANIC_REQUIREMENT = 3;
    public static final int GOSSIP_REQUIREMENT = 5;

    private static final String GOLEM_SEEN = "GolemLastSeen";
    private static final String LAST_WORKED = "LastWorkedAtSite";

    private static final int TAG_ANY_NUMBER = 99;

    private GolemSpawner() {
    }

    /**
     * Tries to bring an iron golem into the world on this villager's behalf.
     *
     * @param required how many eligible villagers must be nearby, including this one
     */
    public static void tryToSpawn(EntityVillager villager, int required) {
        if (!ModConfig.homes.enabled || !ModConfig.homes.golemSpawning) {
            return;
        }

        long now = villager.world.getTotalWorldTime();

        if (!isEligible(villager, now)) {
            if (VillageDebug.on()) {
                VillageDebug.say(villager, "golem: not eligible - " + reasonNotEligible(villager, now));
            }
            return;
        }

        AxisAlignedBB box = villager.getEntityBoundingBox().grow(NEIGHBOUR_RANGE);
        List<EntityVillager> nearby =
                villager.world.getEntitiesWithinAABB(EntityVillager.class, box);

        int eligible = 0;
        for (EntityVillager other : nearby) {
            if (isEligible(other, now) && ++eligible >= COUNT_LIMIT) {
                break;
            }
        }

        if (eligible < required) {
            VillageDebug.say(villager, "golem: " + eligible + "/" + required
                    + " eligible villagers nearby - no spawn");
            return;
        }

        EntityIronGolem golem = spawn(villager);

        if (golem == null) {
            VillageDebug.say(villager, "golem: " + eligible + "/" + required
                    + " eligible, but nowhere to put one");
            return;
        }

        VillageDebug.say(villager, "golem: spawned at "
                + golem.getPosition().getX() + ", " + golem.getPosition().getY()
                + ", " + golem.getPosition().getZ());

        {
            // Everyone nearby, not only the ones that counted - 1.14 marks the whole crowd.
            for (EntityVillager other : nearby) {
                markGolemSeen(other, now);
            }
        }
    }

    /**
     * @return which of 1.14's four conditions this villager is failing
     *
     * <p>Only ever built for a debug line. The conditions are cheap to test but invisible from
     * outside, and "not eligible" on its own tells a tester nothing they can act on.
     */
    private static String reasonNotEligible(EntityVillager villager, long now) {
        if (villager.isChild()) {
            return "still a child";
        }
        if (Employment.isUnemployed(villager)) {
            return "no profession";
        }
        if (!HomeSite.sleptWithin(villager, SLEPT_WITHIN)) {
            return HomeSite.get(villager) == null
                    ? "has no bed, so has never slept"
                    : "has not slept in the last " + SLEPT_WITHIN + " ticks";
        }
        if (!workedWithin(villager, WORKED_WITHIN)) {
            return "has not been to its workstation in the last " + WORKED_WITHIN + " ticks";
        }
        if (hasSeenGolemRecently(villager, now)) {
            return "saw a golem in the last " + GOLEM_SEEN_WITHIN + " ticks";
        }
        return "unknown";
    }

    /** 1.14's {@code canSpawnGolem}: employed, rested, recently at work, and not just served. */
    public static boolean isEligible(EntityVillager villager, long now) {
        if (villager.isChild() || Employment.isUnemployed(villager)) {
            return false;
        }

        if (!HomeSite.sleptWithin(villager, SLEPT_WITHIN)) {
            return false;
        }

        if (!workedWithin(villager, WORKED_WITHIN)) {
            return false;
        }

        return !hasSeenGolemRecently(villager, now);
    }

    /**
     * Records that this villager was at its job site.
     *
     * <p>1.14 stamps {@code LAST_WORKED_AT_POI} from the task that has a villager use its
     * workstation, which is the same moment this mod restocks from.
     */
    public static void markWorked(EntityVillager villager) {
        VillagerTradeData.root(villager)
                .setLong(LAST_WORKED, villager.world.getTotalWorldTime());
    }

    private static boolean workedWithin(EntityVillager villager, long ticks) {
        NBTTagCompound tag = VillagerTradeData.root(villager);
        if (!tag.hasKey(LAST_WORKED, TAG_ANY_NUMBER)) {
            return false;
        }

        return villager.world.getTotalWorldTime() - tag.getLong(LAST_WORKED) < ticks;
    }

    private static boolean hasSeenGolemRecently(EntityVillager villager, long now) {
        NBTTagCompound tag = VillagerTradeData.root(villager);
        if (!tag.hasKey(GOLEM_SEEN, TAG_ANY_NUMBER)) {
            return false;
        }

        return now - tag.getLong(GOLEM_SEEN) <= GOLEM_SEEN_WITHIN;
    }

    private static void markGolemSeen(EntityVillager villager, long now) {
        VillagerTradeData.root(villager).setLong(GOLEM_SEEN, now);
    }

    /**
     * Looks for somewhere to put a golem, and puts one there.
     *
     * <p>1.14's search exactly: ten attempts, each at a random offset within eight blocks, scanning
     * down from six above the villager to twelve below for a space with solid ground under it. A
     * candidate is only used if the golem both fits and is allowed to spawn there, so a golem never
     * appears inside a wall or on top of a player.
     */
    private static EntityIronGolem spawn(EntityVillager villager) {
        World world = villager.world;
        BlockPos origin = new BlockPos(villager);

        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = world.rand.nextInt(16) - 8;
            int dz = world.rand.nextInt(16) - 8;
            int dy = 6;

            for (int step = 0; step >= -12; step--) {
                BlockPos probe = origin.add(dx, dy + step, dz);

                boolean open = world.isAirBlock(probe)
                        || world.getBlockState(probe).getMaterial().isLiquid();

                if (open && world.getBlockState(probe.down()).getMaterial().isOpaque()) {
                    dy += step;
                    break;
                }
            }

            BlockPos at = origin.add(dx, dy, dz);
            EntityIronGolem golem = new EntityIronGolem(world);

            golem.setPlayerCreated(false);
            golem.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);

            if (golem.getCanSpawnHere() && golem.isNotColliding()) {
                world.spawnEntity(golem);
                return golem;
            }

            golem.setDead();
        }

        return null;
    }

    /** @return true if anything golem-shaped is already about, so a village keeps just the one. */
    public static boolean golemNearby(EntityVillager villager, double range) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(range);
        return !villager.world.getEntitiesWithinAABB(EntityGolem.class, box).isEmpty();
    }
}
