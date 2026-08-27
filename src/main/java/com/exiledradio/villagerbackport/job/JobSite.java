package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;

import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A villager's claim on a workstation.
 *
 * <h2>Claims without a registry</h2>
 * 1.14 tracks these in {@code PointOfInterestManager}, a per-chunk store with its own save format
 * and a ticket system so two villagers cannot take the same block. Rebuilding that is a great deal
 * of machinery for what it actually needs to answer: which block is mine, and is anyone else on it.
 *
 * <p>So each villager records its own claim in the data this mod already keeps on it, and a block is
 * taken if any villager nearby has recorded it. Answering by looking at the villagers means there is
 * nothing to keep in sync - no registry to leak entries when a villager dies, no save data to
 * migrate, and a claim cannot outlive its owner because it <em>is</em> its owner.
 *
 * <p>The cost is that checking is a search over nearby villagers rather than a map lookup. That
 * search only runs when a villager is actually looking for work, which is rare.
 */
public final class JobSite {

    private static final String ROOT = "villagerbackport";
    private static final String SITE_X = "JobSiteX";
    private static final String SITE_Y = "JobSiteY";
    private static final String SITE_Z = "JobSiteZ";

    /** When the claim was staked, and whether the villager ever actually got there. */
    private static final String SITE_SINCE = "JobSiteSince";
    private static final String SITE_REACHED = "JobSiteReached";

    private static final int TAG_ANY_NUMBER = 99;
    private static final int TAG_COMPOUND = 10;

    private JobSite() {
    }

    private static NBTTagCompound root(EntityVillager villager) {
        return VillagerTradeData.root(villager);
    }

    /** @return the villager's claimed workstation, or null if it has none. */
    @Nullable
    public static BlockPos get(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(SITE_X, TAG_ANY_NUMBER)) {
            return null;
        }
        return new BlockPos(tag.getInteger(SITE_X), tag.getInteger(SITE_Y), tag.getInteger(SITE_Z));
    }

    public static void set(EntityVillager villager, BlockPos pos) {
        NBTTagCompound tag = root(villager);
        tag.setInteger(SITE_X, pos.getX());
        tag.setInteger(SITE_Y, pos.getY());
        tag.setInteger(SITE_Z, pos.getZ());
        tag.setLong(SITE_SINCE, villager.world.getTotalWorldTime());
        tag.setBoolean(SITE_REACHED, false);
    }

    public static void clear(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        tag.removeTag(SITE_X);
        tag.removeTag(SITE_Y);
        tag.removeTag(SITE_Z);
        tag.removeTag(SITE_SINCE);
        tag.removeTag(SITE_REACHED);
    }

    /** Records that the villager has stood at its workstation, so the claim is its for keeps. */
    public static void markReached(EntityVillager villager) {
        root(villager).setBoolean(SITE_REACHED, true);
    }

    /**
     * @return true if this claim was staked long ago and never actually reached
     *
     * <p>What this guards against is a villager that claims a workstation it cannot get to and then
     * stops looking, because holding a claim is what stops it searching. Left alone that is
     * permanent: a workstation put down right beside it would never be noticed. So a claim that has
     * not been reached expires, and the villager goes back to looking.
     *
     * <p>Only ever applies before the first arrival. Once a villager has stood at its workstation the
     * claim is kept however far it wanders afterwards - villagers have their own lives, and 1.14 does
     * not take a job away for going for a walk.
     */
    public static boolean isUnreachedFor(EntityVillager villager, int ticks) {
        NBTTagCompound tag = root(villager);

        if (tag.getBoolean(SITE_REACHED)) {
            return false;
        }

        return villager.world.getTotalWorldTime() - tag.getLong(SITE_SINCE) > ticks;
    }

    /**
     * @return the villager's claim if it is still valid, clearing and returning null if not.
     *
     * <p>A claim goes stale when the block is broken or replaced, which is entirely normal - players
     * rearrange villages. Checking on use rather than watching for block changes means no block
     * event handling and no chance of missing one.
     *
     * <p>Only checked in loaded chunks. An unloaded claim is left alone rather than being treated as
     * broken, or a villager would abandon its workstation every time the player walked away.
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

        // An unemployed villager's claim is on whatever workstation it is heading for, which is not
        // yet "its" block - it has no career to match against until it arrives and takes the job.
        boolean stillValid = Employment.isUnemployed(villager)
                ? JobSiteRegistry.isWorkstation(villager.world.getBlockState(pos).getBlock())
                : JobSiteRegistry.isSiteFor(villager, villager.world, pos);

        if (!stillValid) {
            clear(villager);
            return null;
        }

        return pos;
    }

    /** @return true if the villager is close enough to its workstation to be working at it. */
    public static boolean isAtSite(EntityVillager villager) {
        BlockPos pos = validated(villager);
        if (pos == null) {
            return false;
        }

        return isTouching(villager, pos);
    }

    /**
     * @return true if the villager is close enough to the block, with nothing in between
     *
     * <p>A villager standing in a block that shares a face with this one is touching it by
     * definition - two blocks that touch have nothing between them to check - so that case is
     * answered outright. It matters because the sight line is traced to the middle of the block,
     * and from directly alongside, a workstation that is not a full cube can put its own edge or a
     * neighbouring block across that line. Getting a false no there is what makes a villager stand
     * beside its workstation doing nothing.
     */
    public static boolean isTouching(EntityVillager villager, BlockPos pos) {
        if (sharesFace(new BlockPos(villager), pos)) {
            return true;
        }

        double reach = ModConfig.jobs.workingDistance;
        return centreDistanceSq(villager, pos) <= reach * reach
                && canTouch(villager.world, eyesOf(villager), pos);
    }

    /** @return true if the two blocks are face to face, which no wall can fit between. */
    private static boolean sharesFace(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX())
                + Math.abs(a.getY() - b.getY())
                + Math.abs(a.getZ() - b.getZ()) == 1;
    }

    /**
     * @return the squared distance from the villager to the middle of the block
     *
     * <h2>Why not {@code Entity.getDistanceSq(BlockPos)}</h2>
     * That measures to the block's <em>corner</em>, not its middle, so the same villager standing
     * the same one block away scores differently depending on which side it is standing on: half a
     * block on the low side, one and a half on the high side. At a generous working distance the
     * difference is lost in the slack. Tightened down to "touching" it is the whole budget, and the
     * result is a villager that can work at a block from the west but not from the east - which is
     * what made turning the distance down look like it did nothing.
     *
     * <p>Measured from the villager's feet, matching how the pathfinder thinks about which block an
     * entity is standing in, so a workstation at the same level scores half a block up rather than
     * an eye height down.
     */
    private static double centreDistanceSq(EntityVillager villager, BlockPos pos) {
        double dx = pos.getX() + 0.5D - villager.posX;
        double dy = pos.getY() + 0.5D - villager.posY;
        double dz = pos.getZ() + 0.5D - villager.posZ;

        return dx * dx + dy * dy + dz * dz;
    }

    /** @return where a villager is looking from, for the sight line below. */
    private static Vec3d eyesOf(EntityVillager villager) {
        return new Vec3d(villager.posX, villager.posY + villager.getEyeHeight(), villager.posZ);
    }

    /**
     * @return true if nothing solid stands between that point and the workstation
     *
     * <h2>Why distance alone is not enough</h2>
     * The working distance is wider than a wall is thick, so a villager standing on the far side of
     * one is within reach of a workstation it cannot touch. Put a lectern in a glass box and every
     * villager outside the box counts as standing at it - they can see it, they are two blocks from
     * it, and the only one who can actually use it is the one shut in with it.
     *
     * <p>So the question asked is whether anything is in the way. A sight line from the villager to
     * the middle of the block is traced, and anything solid it meets first - glass, a wall, a door -
     * means no. Air and blocks without a solid shape are ignored, so a fence or a carpet underfoot
     * does not count against it.
     */
    public static boolean canTouch(World world, Vec3d from, BlockPos pos) {
        Vec3d target = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        RayTraceResult hit = world.rayTraceBlocks(from, target, false, true, false);

        return hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || pos.equals(hit.getBlockPos());
    }

    /**
     * @return every workstation claimed by another villager near this one
     *
     * <p>Gathered once before a search rather than asked per candidate. The question is the same
     * either way, but asking it per candidate means an entity lookup for every workstation the
     * search walks past, and the search grew large enough for that to matter.
     *
     * <p>The reach is generous on purpose: a villager standing outside the searched area can still
     * hold a claim inside it, and handing the same workstation to two villagers is worse than
     * looking at a few extra of them.
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

    /**
     * @return true if another villager has already claimed this block.
     *
     * <p>Searched over the villagers around the block rather than the whole world, because a claim
     * can only conflict with a villager close enough to use it.
     */
    public static boolean isClaimedByOther(EntityVillager villager, BlockPos pos) {
        double radius = ModConfig.jobs.searchRadius;
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(radius);

        List<EntityVillager> nearby = villager.world.getEntitiesWithinAABB(EntityVillager.class, box);
        for (EntityVillager other : nearby) {
            if (other == villager || other.isDead) {
                continue;
            }

            if (pos.equals(get(other))) {
                return true;
            }
        }

        return false;
    }
}
