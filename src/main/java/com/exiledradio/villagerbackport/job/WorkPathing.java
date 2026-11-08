package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

import java.util.UUID;

/**
 * Lets a villager path further than usual, but only to its workstation, its bed or the bell.
 *
 * <h2>Why the follow range is left alone</h2>
 * Follow range is what {@code PathNavigate.getPathSearchRange} returns for <em>every</em> path an
 * entity asks for, so a villager given a permanently large one searches a far larger volume every
 * time it wanders, heads indoors, flees a zombie or follows a golem. In a village with a lot of
 * villagers everything then moves in jerks.
 *
 * <p>So the long reach never goes on the attribute. These paths are searched by {@link LongPathFinder}
 * with the range passed in directly, and every other goal searches the distance it was written for.
 */
public final class WorkPathing {

    /** The modifier older versions put on the follow range, found by id so it can be taken off. */
    private static final UUID REACH = UUID.fromString("7b1d5a4e-9c2f-4a30-8d61-2f9b0c7e4a11");

    private WorkPathing() {
    }

    /**
     * How a walk to a claim went, as 1.14's {@code WalkToTargetTask} sees it.
     */
    public enum Walk {
        /** A path that arrives was found, and the villager is on it. */
        REACHES,
        /**
         * No path arrives, but the villager is walking the nearest part of the way, or towards the
         * target, and will ask again from there. The can't-reach clock runs meanwhile.
         */
        PARTIAL,
        /** No way at all from here. */
        NONE;

        public boolean moving() {
            return this != NONE;
        }
    }

    /** How far past the radius the reach goes, so a claim at the edge of it can still be walked to. */
    private static final double MARGIN = 8.0D;

    /**
     * @return a path to the target with 1.14's search budget - see {@link LongPathFinder} - which may
     * end short of it when no route arrives
     */
    @Nullable
    public static Path pathTo(EntityVillager villager, BlockPos pos) {
        if (!ModConfig.jobs.extendPathfindingRange) {
            // Exactly as vanilla 1.12 pathfinds, as the config switch promises.
            return villager.getNavigator().getPathToPos(pos);
        }

        return LongPathFinder.find(villager, pos, (float) effectiveRange(villager));
    }

    /** @return true if the path ends against the target rather than merely near it */
    public static boolean arrives(@Nullable Path path, BlockPos pos) {
        net.minecraft.pathfinding.PathPoint end = path == null ? null : path.getFinalPathPoint();

        return end != null && Math.abs(end.x - pos.getX()) <= 1
                && Math.abs(end.y - pos.getY()) <= 1 && Math.abs(end.z - pos.getZ()) <= 1;
    }

    /**
     * Starts the villager walking to its workstation or bed, as 1.14's {@code WalkToTargetTask} does.
     *
     * <h2>Partial paths are walked, as in 1.14</h2>
     * This used to drop any path that did not arrive, so a villager behind a fence would not stand
     * pressed against it. With 1.12's 200-node search, though, nearly every path across a village fell
     * short, so a villager forty blocks from its bed was told there was no way home. It stood still,
     * gave the bed up a minute later, found nothing reachable from the edge of the village, and spent
     * the night there.
     *
     * <p>1.14 does neither. Its search has the budget to cross a village ({@link LongPathFinder}). When
     * a path still does not arrive, the villager walks it anyway and asks again from where it ends.
     * Only the can't-reach clock remembers that the path fell short, so a target that really is walled
     * off is still given up after a minute, and the villager at the fence stands there for that minute
     * and no longer. When there is no path at all, it takes a few steps towards the target, as 1.14
     * does, and tries again from there.
     */
    public static Walk walk(EntityVillager villager, BlockPos pos, double speed) {
        Path path = pathTo(villager, pos);

        if (path != null && villager.getNavigator().setPath(path, speed)) {
            if (arrives(path, pos)) {
                return Walk.REACHES;
            }

            // A path that ends where the villager already stands goes nowhere; treat it as none.
            net.minecraft.pathfinding.PathPoint end = path.getFinalPathPoint();
            if (end != null && end.distanceToSquared(new net.minecraft.pathfinding.PathPoint(
                    MathHelper.floor(villager.posX), MathHelper.floor(villager.posY),
                    MathHelper.floor(villager.posZ))) > 1.0F) {
                return Walk.PARTIAL;
            }

            villager.getNavigator().clearPath();
        }

        Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards(villager, 10, 7,
                new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));

        if (towards != null && villager.getNavigator().tryMoveToXYZ(towards.x, towards.y, towards.z, speed)) {
            return Walk.PARTIAL;
        }

        return Walk.NONE;
    }

    /**
     * Starts the villager walking to the target only if a path arrives.
     *
     * @return true if a path reaching the target was found and the villager is on its way
     */
    public static boolean walkTo(EntityVillager villager, BlockPos pos, double speed) {
        Path path = pathTo(villager, pos);

        if (!arrives(path, pos)) {
            return false;
        }

        return villager.getNavigator().setPath(path, speed);
    }

    /**
     * Takes off a modifier left behind by an older version of this mod.
     *
     * <p>Attributes are saved with the entity, so a villager that met the version which applied this
     * permanently is still carrying it - and would go on searching the larger volume forever, on a
     * world where nothing was doing that deliberately any more.
     */
    public static void removeStale(EntityVillager villager) {
        IAttributeInstance range = villager.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);

        if (range != null && range.getModifier(REACH) != null) {
            range.removeModifier(REACH);
        }
    }

    /**
     * @return how far a path for this villager will actually be searched
     *
     * <p>Not the same as reading the attribute: the extra reach is only on the villager while a path
     * is being worked out, so anything asking outside that window sees the base figure and would
     * wrongly conclude the villager cannot path that far.
     */
    public static double effectiveRange(EntityVillager villager) {
        IAttributeInstance range = villager.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        double base = range == null ? 16.0D : range.getAttributeValue();

        if (!ModConfig.jobs.extendPathfindingRange) {
            return base;
        }

        return Math.max(base, Math.max(ModConfig.jobs.searchRadius, ModConfig.homes.searchRadius) + MARGIN);
    }
}
