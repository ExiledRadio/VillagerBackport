package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.job.JobSite;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * A villager's bed, and whether it is in it.
 *
 * <h2>The same shape as a job site, for the same reasons</h2>
 * 1.14 tracks a home the way it tracks a workplace - a point of interest with a ticket, held in the
 * villager's brain as {@code MemoryModuleType.HOME}. This mirrors {@link JobSite}: the claim lives
 * on the villager, and a bed is taken if a villager nearby says it has taken it. No registry, no
 * save format of its own, and a claim cannot outlive its owner because it is part of its owner.
 *
 * <p>What a home is for is worth saying plainly, because sleeping reads as decoration and is not.
 * From 1.14 a villager only counts towards an iron golem if it has slept within the last day, so a
 * village with nowhere to sleep is a village with no golems. Beds also decide whether a village can
 * grow. The bed is the centre of village life in a way the door it replaced never was.
 */
public final class HomeSite {

    private static final String BED_X = "HomeX";
    private static final String BED_Y = "HomeY";
    private static final String BED_Z = "HomeZ";

    /** Whether the villager is in bed now, and when it last got out of one. */
    private static final String SLEEPING = "Sleeping";
    private static final String LAST_SLEPT = "LastSlept";

    /** Set when something turns a villager out of bed, so it does not climb straight back in. */
    private static final String AWAKE_UNTIL = "AwakeUntil";

    /** How long a villager stays up after a player deliberately turns it out. */
    private static final int DISTURBED_TICKS = 200;

    private static final int TAG_ANY_NUMBER = 99;

    private HomeSite() {
    }

    private static NBTTagCompound root(EntityVillager villager) {
        return VillagerTradeData.root(villager);
    }

    /** @return the villager's bed, or null if it has none. */
    @Nullable
    public static BlockPos get(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(BED_X, TAG_ANY_NUMBER)) {
            return null;
        }
        return new BlockPos(tag.getInteger(BED_X), tag.getInteger(BED_Y), tag.getInteger(BED_Z));
    }

    public static void set(EntityVillager villager, BlockPos pos) {
        NBTTagCompound tag = root(villager);
        tag.setInteger(BED_X, pos.getX());
        tag.setInteger(BED_Y, pos.getY());
        tag.setInteger(BED_Z, pos.getZ());
    }

    public static void clear(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        tag.removeTag(BED_X);
        tag.removeTag(BED_Y);
        tag.removeTag(BED_Z);
        tag.removeTag(SLEEPING);
    }

    /**
     * @return the villager's bed if it is still a bed, clearing the claim if it is not
     *
     * <p>Checked on use rather than watched for, as job sites are. Unloaded chunks are left alone:
     * a villager should not lose its home because the player walked away from the village.
     */
    @Nullable
    public static BlockPos validated(EntityVillager villager) {
        BlockPos pos = get(villager);
        if (pos == null) {
            return null;
        }

        if (!villager.world.isBlockLoaded(pos)) {
            return pos;
        }

        if (!isBed(villager.world.getBlockState(pos))) {
            clear(villager);
            return null;
        }

        return pos;
    }

    /** @return true if this is the head half of a bed - the half a villager sleeps in. */
    public static boolean isBed(IBlockState state) {
        return state.getBlock() instanceof BlockBed
                && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
    }

    /** @return which way the bed faces, so a sleeper can be laid along it rather than across it. */
    public static EnumFacing facingOf(IBlockState state) {
        return state.getBlock() instanceof BlockBed
                ? state.getValue(BlockBed.FACING)
                : EnumFacing.NORTH;
    }

    public static boolean isSleeping(EntityVillager villager) {
        return root(villager).getBoolean(SLEEPING);
    }

    /**
     * Records that the villager is in bed, or has got out of it.
     *
     * <p>The waking time is what golem spawning reads: 1.14 asks whether a villager has slept within
     * the last 24000 ticks, and stamps that memory as it wakes.
     */
    public static void setSleeping(EntityVillager villager, boolean sleeping) {
        NBTTagCompound tag = root(villager);
        tag.setBoolean(SLEEPING, sleeping);

        if (!sleeping) {
            tag.setLong(LAST_SLEPT, villager.world.getTotalWorldTime());
        }
    }

    /** @return true if the villager has slept within the given number of ticks. */
    public static boolean sleptWithin(EntityVillager villager, long ticks) {
        if (isSleeping(villager)) {
            return true;
        }

        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(LAST_SLEPT, TAG_ANY_NUMBER)) {
            return false;
        }

        return villager.world.getTotalWorldTime() - tag.getLong(LAST_SLEPT) < ticks;
    }

    /**
     * Turns a villager out of bed and keeps it out for a while.
     *
     * <p>Called when a player wants the bed - or the villager - for something else. The delay is the
     * point: clearing the flag alone would have the sleeping goal put it straight back to bed on the
     * next tick, which from the outside looks like the villager ignoring you.
     */
    public static void disturb(EntityVillager villager) {
        disturb(villager, DISTURBED_TICKS);
    }

    /**
     * Turns a villager out of bed and keeps it out for a given time.
     *
     * <p>Two callers want different numbers. A player right-clicking wants the bed, so the villager
     * stays up long enough for them to have it. A villager shoved out by the crowd is 1.14's case,
     * and 1.14 gives it forty ticks - long enough not to climb straight back in mid-shove, short
     * enough that the village settles again once everyone stops pushing.
     */
    public static void disturb(EntityVillager villager, int ticks) {
        if (!isSleeping(villager)) {
            return;
        }

        setSleeping(villager, false);
        villager.setNoGravity(false);

        root(villager).setLong(AWAKE_UNTIL, villager.world.getTotalWorldTime() + ticks);

        SleepSync.broadcast(villager);
    }

    /** @return true if the villager has been turned out of bed too recently to go back. */
    public static boolean isDisturbed(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(AWAKE_UNTIL, TAG_ANY_NUMBER)) {
            return false;
        }

        return villager.world.getTotalWorldTime() < tag.getLong(AWAKE_UNTIL);
    }

    /** @return the villager asleep in this bed, or null if nobody is in it. */
    @Nullable
    public static EntityVillager sleeperIn(net.minecraft.world.World world, BlockPos bed) {
        AxisAlignedBB box = new AxisAlignedBB(bed).grow(2.0D);

        for (EntityVillager villager
                : world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            if (isSleeping(villager) && bed.equals(get(villager))) {
                return villager;
            }
        }

        return null;
    }

    /** @return true if the villager is close enough to its bed to get into it. */
    public static boolean isAtBed(EntityVillager villager) {
        BlockPos pos = validated(villager);
        return pos != null && JobSite.isTouching(villager, pos);
    }

    /**
     * @return every bed claimed by another villager nearby
     *
     * <p>Gathered once per search rather than asked per bed, for the reason given on
     * {@link JobSite#claimsNear}: one entity lookup instead of one for every candidate.
     */
    public static Set<BlockPos> claimsNear(EntityVillager villager, double radius) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(radius);
        Set<BlockPos> taken = new HashSet<BlockPos>();

        for (EntityVillager other : villager.world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            if (other == villager || other.isDead) {
                continue;
            }

            BlockPos claim = get(other);
            if (claim != null) {
                taken.add(claim);
            }
        }

        return taken;
    }

    /** @return true if any villager other than this one has claimed the bed. */
    public static boolean isClaimedByOther(EntityVillager villager, BlockPos pos) {
        double radius = ModConfig.homes.searchRadius;
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(radius);

        for (EntityVillager other : villager.world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            if (other != villager && !other.isDead && pos.equals(get(other))) {
                return true;
            }
        }

        return false;
    }
}
