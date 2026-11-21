package com.exiledradio.villagerbackport.job;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathHeap;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkCache;

import javax.annotation.Nullable;

/**
 * The game's path search, with 1.14's budget instead of 1.12's.
 *
 * <h2>Why villagers could not find their way home</h2>
 * 1.12's {@code PathFinder} stops after looking at 200 nodes, whatever the distance it was asked about.
 * That is plenty for a zombie chasing a player across a field and nowhere near enough to cross a
 * village: a route that turns through a door, round a well and up a path between houses spends its 200
 * nodes long before it gets anywhere, and the search hands back the closest point it reached instead.
 * A villager forty blocks from its bed was asking for a path it could never be given.
 *
 * <p>1.14 fixed exactly this. Its navigator builds its path finder with a budget of the follow range
 * times sixteen ({@code PathNavigator}: {@code getPathFinder(floor(FOLLOW_RANGE * 16))}), which for a
 * villager's 48 blocks is 768 nodes, and its villagers cross their villages without trouble.
 *
 * <p>It uses 1.12's heap and the villager's own node processor (with its door and water settings),
 * with 1.14.4's search on top: its budget, costs, heuristic and stopping rule (see {@link #search}).
 * It is used for the paths this mod asks for (to a workstation, a bed or the bell), never for
 * vanilla's own goals, so a villager wandering about costs exactly what it did before.
 */
public final class LongPathFinder {

    /** 1.14's budget: nodes per block of search range. */
    private static final int NODES_PER_BLOCK = 16;

    /** How close counts as there: 1.14's valid range for a job site or a bed, in blocks, Manhattan. */
    private static final int ACCURACY = 1;

    /** 1.12's own budget, which this never goes below. */
    private static final int VANILLA_NODES = 200;

    private LongPathFinder() {
    }

    /**
     * @return a path towards the block, or null if the entity cannot path at all from where it is
     *
     * <p>Like the game's, a path that cannot reach the target comes back ending at the closest point it
     * did reach. Callers decide whether that is good enough.
     */
    @Nullable
    public static Path find(EntityLiving entity, BlockPos target, float range) {
        if (!canNavigate(entity)) {
            return null;
        }

        NodeProcessor processor = entity.getNavigator().getNodeProcessor();
        if (processor == null) {
            return null;
        }

        BlockPos origin = new BlockPos(entity);
        int margin = (int) (range + 8.0F);
        ChunkCache region = new ChunkCache(entity.world,
                origin.add(-margin, -margin, -margin), origin.add(margin, margin, margin), 0);

        int budget = Math.max(VANILLA_NODES, MathHelper.floor(range * NODES_PER_BLOCK));

        entity.world.profiler.startSection("villagerbackport_pathfind");
        try {
            processor.init(region, entity);
            PathPoint start = processor.getStart();
            PathPoint end = processor.getPathPointToCoords(
                    target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
            return search(entity, processor, start, end, range, budget);
        } finally {
            processor.postProcess();
            entity.world.profiler.endSection();
        }
    }

    /**
     * Whether the navigator would look for a path at all right now: {@code PathNavigateGround}'s rule,
     * on the ground, swimming, or riding. Its own check is protected.
     */
    private static boolean canNavigate(EntityLiving entity) {
        return entity.onGround || entity.isRiding() || entity.isInWater() || entity.isInLava();
    }

    /**
     * 1.14.4's {@code PathFinder.func_224779_a}, for one target: Euclidean step costs, the distance to
     * the target weighted by 1.5 as the heuristic, the search range measured from the start, and done
     * as soon as a node is within {@value #ACCURACY} block (Manhattan) of the target.
     *
     * <h2>Why villagers walked to the wrong side of the wall</h2>
     * This used to be 1.12's search with 1.14's budget, and 1.12's stops only on the target block
     * itself. A workstation or a bed is never a node anyone stands in, so every search ran through its
     * whole budget and handed back the node nearest the target by Manhattan distance - for a lectern in
     * a house, very often the grass outside its back wall, three blocks away through stone, rather than
     * the floor inside the door. 1.14 asks for the job site's valid range (one block, {@code
     * GatherPOITask}) and stops on it, and its greedier heuristic gets through the door long before the
     * budget runs out. Measured on eight generated villages, a third of the workstations were out of
     * reach before this and their villagers stayed unemployed.
     */
    @Nullable
    private static Path search(EntityLiving entity, NodeProcessor processor, PathPoint from, PathPoint to, float maxDistance,
                               int budget) {
        PathHeap open = new PathHeap();
        PathPoint[] options = new PathPoint[32];

        from.totalPathDistance = 0.0F;
        from.distanceToNext = from.distanceTo(to);
        from.distanceToTarget = from.distanceToNext;
        open.addPoint(from);

        // 1.14's FlaggedPathPoint keeps the node it has seen nearest the target, by straight distance.
        PathPoint closest = from;
        float closestDistance = from.distanceTo(to);
        int visited = 0;

        while (!open.isPathEmpty()) {
            if (++visited >= budget) {
                break;
            }

            PathPoint current = open.dequeue();
            current.visited = true;

            if (current.distanceManhattan(to) <= ACCURACY) {
                closest = current;
                break;
            }

            if (current.distanceTo(from) >= maxDistance) {
                continue;
            }

            // No target filter: 1.12's processor drops options further than this from the target,
            // which 1.14's does not. The range is applied below, from the start, as 1.14 does.
            int count = fitting(entity, options, processor.findPathOptions(options, current, to, Float.MAX_VALUE));

            for (int i = 0; i < count; i++) {
                PathPoint next = options[i];
                float step = current.distanceTo(next);
                next.distanceFromOrigin = current.distanceFromOrigin + step;
                float total = current.totalPathDistance + step + next.costMalus;

                if (next.distanceFromOrigin < maxDistance && (!next.isAssigned() || total < next.totalPathDistance)) {
                    next.previous = current;
                    next.totalPathDistance = total;
                    float distance = next.distanceTo(to);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = next;
                    }
                    next.distanceToNext = distance * 1.5F;

                    if (next.isAssigned()) {
                        open.changeDistance(next, next.totalPathDistance + next.distanceToNext);
                    } else {
                        next.distanceToTarget = next.totalPathDistance + next.distanceToNext;
                        open.addPoint(next);
                    }
                }
            }
        }

        if (closest == from) {
            return null;
        }

        int length = 1;
        for (PathPoint point = closest; point.previous != null; point = point.previous) {
            length++;
        }

        PathPoint[] points = new PathPoint[length];
        PathPoint point = closest;
        points[--length] = point;

        while (point.previous != null) {
            point = point.previous;
            points[--length] = point;
        }

        return new Path(points);
    }

    /**
     * Drops the options the entity would not actually fit into, and returns how many are left.
     *
     * <h2>Why carpet stranded villagers</h2>
     * 1.12's walk node processor treats a carpet, snow layer or other thin block as open air: a
     * node on it is walkable when two blocks of headroom sit above the floor block. But the entity
     * stands on top of the carpet, a sixteenth higher, and a villager is 1.95 tall - so under a
     * ceiling two blocks up it no longer fits. The search sent villagers across exactly such carpets
     * in plains houses, and they stood pressed against the step all night, the path reaching the
     * bed on paper so the give-up clock never ran. This checks the entity's own box, standing on
     * whatever the node's block really is, before the node is used.
     */
    private static int fitting(EntityLiving entity, PathPoint[] options, int count) {
        if (entity.width >= 1.0F) {
            return count;
        }
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (fits(entity, options[i])) {
                options[kept++] = options[i];
            }
        }
        for (int i = kept; i < count; i++) {
            options[i] = null;
        }
        return kept;
    }

    private static boolean fits(EntityLiving entity, PathPoint node) {
        BlockPos pos = new BlockPos(node.x, node.y, node.z);
        IBlockState state = entity.world.getBlockState(pos);
        AxisAlignedBB own = state.getCollisionBoundingBox(entity.world, pos);
        if (own == null || own.maxY >= 1.0D || own.maxY <= 0.0D) {
            // Nothing to stand on inside the block itself (air, a door, a full block the processor
            // already judged): 1.12's own headroom test stands.
            return true;
        }
        double floor = node.y + own.maxY;
        double half = entity.width / 2.0D;
        AxisAlignedBB box = new AxisAlignedBB(node.x + 0.5D - half, floor + 0.001D, node.z + 0.5D - half,
                node.x + 0.5D + half, floor + entity.height - 0.002D, node.z + 0.5D + half);
        return !entity.world.collidesWithAnyBlock(box);
    }
}
