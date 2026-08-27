package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Claiming a workstation, and taking the job it offers.
 *
 * <h2>Why this is not part of the walking goal</h2>
 * It was, and it made villagers wildly inconsistent: two standing beside a free cauldron would
 * ignore it for minutes, then one would suddenly take it.
 *
 * <p>The cause is that claiming was done inside {@code shouldExecute} on the goal that walks to
 * work, which reserves the movement mutex at priority 8. {@code EntityAITasks} does not ask a goal
 * whether it wants to run while a goal of the same or better priority holds a mutex it overlaps -
 * and a villager has eight of those competing for movement: four flee-from-monster goals at
 * priority 1, going indoors at 2, the two door goals at 3 and 4, staying near home at 5, mating at
 * 6, following a golem at 7. Whether a villager could even be <em>asked</em> about work therefore
 * depended on the time of day, the weather, whether a monster or a golem was nearby, whether it had
 * strayed from its home radius, and whether it was mating season.
 *
 * <p>None of that has anything to do with claiming a workstation. A claim is bookkeeping - it writes
 * a position into the villager's data - and a villager already standing next to a workstation does
 * not need to move at all, so making it compete for the right to move was wrong in the first place.
 *
 * <p>So claiming runs here, on the villager's own tick, and {@link EntityAIWorkAtSite} is left to do
 * only what it is actually for: walking to a workstation already claimed.
 */
public final class JobSiteClaims {

    /**
     * Ticks between attempts for a villager with no workstation.
     *
     * <p>Short enough that placing a workstation next to a villager gets a visible reaction, which
     * is the behaviour anyone would test first.
     */
    private static final int CLAIM_INTERVAL = 60;

    /**
     * How many workstations one search may ask the navigator about.
     *
     * <p>Each question is a pathfind, so this is not unbounded - but a profile of the whole search
     * put it at roughly a hundredth of a percent of a tick, so there is no reason to be mean with it
     * either. Failures are remembered regardless, so a later search carries on from here.
     *
     * <p>Raised once the answers became strict. A rejected candidate is now simply rejected rather
     * than falling back to a claim made on trust, so a villager in a village whose nearest few
     * workstations are all walled off needs enough questions in one search to get past them.
     */
    private static final int MAX_PATH_CHECKS = 8;


    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();

        if (villager.world.isRemote || !ModConfig.jobs.enabled) {
            return;
        }

        if (villager.isChild() || villager.getCustomer() != null) {
            return;
        }

        // Offset by the entity id so a village does not run every villager's search on one tick.
        if ((villager.ticksExisted + villager.getEntityId()) % CLAIM_INTERVAL != 0) {
            return;
        }

        update(villager);
    }

    /** Claims a workstation if there is one to claim, and takes the job if standing at it. */
    public static void update(EntityVillager villager) {
        BlockPos site = JobSite.validated(villager);

        if (site != null) {
            if (JobSite.isAtSite(villager)) {
                JobSite.markReached(villager);
            } else if (JobSite.isUnreachedFor(villager, ModConfig.jobs.abandonUnreachedTicks)) {
                // Holding a claim is what stops a villager searching, so a claim it can never get to
                // would freeze it for good - it would ignore a workstation placed at its feet. The
                // goal that walks there used to be what noticed this, which was no good: that goal
                // only runs when it wins the movement mutex, so a villager could hold an unreachable
                // claim indefinitely without anything ever reconsidering it.
                //
                // Recorded as failed on the way out, or the next search would pick the very same
                // block again for the very same reason - it is still the nearest - and the villager
                // would spend the rest of its life alternating between claiming and giving up.
                UnreachableSites.remember(villager, site, villager.world.getTotalWorldTime());
                JobSite.clear(villager);
                site = null;
            }
        }

        if (site == null) {
            claimNearby(villager);
        }

        takeJobIfAtSite(villager);
    }

    /**
     * How far around a workstation villagers are told about it changing.
     *
     * <p>Only wide enough to cover whoever was using it or is standing over it. Everyone further
     * off finds out on their own schedule, which is what that schedule is for.
     */
    private static final double NEWS_RADIUS = 16.0D;

    /** How many villagers one placement may set searching, so a wall of lecterns cannot pile up. */
    private static final int MAX_WOKEN = 8;

    /** Workstations placed this tick, to be offered round at the end of it. */
    private static final List<Placement> PENDING = new ArrayList<Placement>();

    /** A workstation placement waiting for the tick to finish. */
    private static final class Placement {

        final World world;
        final BlockPos pos;

        Placement(World world, BlockPos pos) {
            this.world = world;
            this.pos = pos;
        }
    }

    /**
     * A workstation has appeared: let whoever is standing nearby have a go at it now.
     *
     * <h2>Why this is not left to the normal cycle</h2>
     * Villagers look for work on a fixed cycle, offset by entity id so a village does not search all
     * at once. That is right for a village going about its business and wrong for a player standing
     * over a lectern breaking and replacing it: the answer arrives somewhere between immediately and
     * three seconds later depending on which tick the block went down on, which reads as the villager
     * randomly deciding whether to co-operate.
     *
     * <p>A workstation being placed is a rare, deliberate event and the one moment a villager most
     * obviously ought to react, so it is answered on the spot instead.
     */
    public static void onWorkstationPlaced(World world, BlockPos pos) {
        if (!ModConfig.jobs.enabled) {
            return;
        }

        // Answered next tick rather than now. A block event is announced mid-placement, with the
        // block itself down but the rest of it - the tile entity, the neighbour updates, the state
        // the block settles into - still to come. A villager asking whether it can reach a
        // workstation in that window can get an answer that stops being true a moment later, and
        // failures are remembered. Waiting one tick costs nothing anyone can perceive and means the
        // question is only ever asked about a finished world.
        PENDING.add(new Placement(world, pos));
    }

    /** Runs the placement reactions queued during this tick, once the tick's block work is done. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }

        List<Placement> due = new ArrayList<Placement>(PENDING);
        PENDING.clear();

        for (Placement placement : due) {
            offerTo(placement.world, placement.pos);
        }
    }

    private static void offerTo(World world, BlockPos pos) {
        // The grudge is about a block that is gone; this is a new one.
        UnreachableSites.forget(pos);

        if (!JobSiteRegistry.isWorkstation(world.getBlockState(pos).getBlock())) {
            // Placed and gone again inside the tick.
            return;
        }

        List<EntityVillager> nearby = villagersNear(world, pos);
        int woken = 0;

        for (EntityVillager villager : nearby) {
            if (woken >= MAX_WOKEN) {
                return;
            }

            if (villager.isChild() || villager.getCustomer() != null || JobSite.get(villager) != null) {
                continue;
            }

            woken++;
            update(villager);

            // Sorted by distance, so the first one to take it is the nearest that could - which is
            // the one that should have it. Nobody further out needs to search on this tick.
            if (pos.equals(JobSite.get(villager))) {
                return;
            }
        }
    }

    /**
     * A workstation has gone: whoever had claimed it finds out now rather than on its next check.
     *
     * <p>This is what makes breaking a lectern take effect at once. The claim cannot be validated
     * away here because the block is still standing at the point the break is announced - the event
     * is cancellable, so it fires before the world changes - which is exactly why the position is
     * matched directly instead of being looked up in the world.
     *
     * <p>The job goes with it under 1.14's rule, which is the rule that makes rerolling work at all:
     * a villager that has never been traded with loses the profession and rolls a fresh set of
     * trades when it takes a workstation again. One that has been traded with keeps everything and
     * simply waits for its workstation to come back.
     */
    public static void onWorkstationBroken(World world, BlockPos pos) {
        if (!ModConfig.jobs.enabled) {
            return;
        }

        for (EntityVillager villager : villagersNear(world, pos)) {
            if (!pos.equals(JobSite.get(villager))) {
                continue;
            }

            JobSite.clear(villager);

            if (Employment.shouldLoseJob(villager)) {
                Employment.unassign(villager);
            }
        }
    }

    /** @return villagers around a position, nearest first. */
    private static List<EntityVillager> villagersNear(World world, final BlockPos pos) {
        List<EntityVillager> found = world.getEntitiesWithinAABB(
                EntityVillager.class, new AxisAlignedBB(pos).grow(NEWS_RADIUS));

        Collections.sort(found, new Comparator<EntityVillager>() {
            @Override
            public int compare(EntityVillager a, EntityVillager b) {
                return Double.compare(a.getDistanceSq(pos), b.getDistanceSq(pos));
            }
        });

        return found;
    }

    /**
     * Records that a villager could not get to a workstation, so the next search looks past it.
     *
     * <p>Called by the walking goal when it gives up. The claim itself is left alone - one owner
     * decides that, on the villager's tick - but the failure is worth knowing about immediately.
     */
    public static void noteUnreachable(EntityVillager villager, BlockPos site) {
        if (site != null) {
            UnreachableSites.remember(villager, site, villager.world.getTotalWorldTime());
        }
    }

    /**
     * Called when a villager reaches its workstation: the claim is now its for keeps, and if it had
     * no job it takes the one the block offers.
     */
    public static void onArrival(EntityVillager villager) {
        JobSite.markReached(villager);
        takeJobIfAtSite(villager);

        // 1.14 stamps LAST_WORKED_AT_POI from the task that uses the workstation, and golem
        // spawning reads it: a villager that has not been to work in a day and a half does not
        // count towards one. See GolemSpawner.
        com.exiledradio.villagerbackport.home.GolemSpawner.markWorked(villager);

        // Do the work now, while the villager is actually standing here. The goal that walked it
        // over finishes on arrival and hands it back to the other goals, so waiting for the next
        // scheduled evaluation means it has usually been led away before one comes round.
        com.exiledradio.villagerbackport.restock.RestockHandler.workNow(villager);
    }

    /**
     * Takes the job the claimed workstation offers, if the villager has not got one.
     *
     * <p>Tied to standing at the block rather than to holding the claim, so a villager cannot claim a
     * lectern across the village and become a librarian without ever walking to it. For one already
     * beside a workstation those are the same moment, which is the point.
     */
    public static void takeJobIfAtSite(EntityVillager villager) {
        if (!Employment.isUnemployed(villager)) {
            return;
        }

        BlockPos site = JobSite.validated(villager);
        if (site == null || !JobSite.isAtSite(villager)) {
            return;
        }

        String career = JobSiteRegistry.careerForBlock(
                villager.world.getBlockState(site).getBlock());

        if (career != null) {
            Employment.assign(villager, career);
        }
    }

    /**
     * Finds the nearest unclaimed workstation and takes it.
     *
     * <p>The nearest is preferred, which keeps villagers from crossing a village past three free
     * lecterns to reach a fourth.
     *
     * <h2>Why it walks chunks rather than positions</h2>
     * This used to ask the world for each position in turn. That is a chunk lookup per block, and it
     * put a hard ceiling on how far a villager could look - which showed up as villagers in the
     * larger villages a pack like RLCraft generates never finding work at all, because the nearest
     * free workstation was simply further away than they could see.
     *
     * <p>Walking the chunks instead means one lookup per chunk and then straight array reads through
     * its sections, and sections a chunk has nothing in are skipped whole. That is cheap enough to
     * afford a reach worth having: at the default radius this reads about ten times as many blocks
     * as the old search did, for less work than the old one cost.
     *
     * <p>Chunks that are not loaded are skipped rather than generated. Looking for work must never be
     * a reason to generate terrain.
     */
    @Nullable
    static BlockPos claimNearby(EntityVillager villager) {
        // Employed villagers want their own workstation; unemployed ones will take any job going.
        boolean unemployed = Employment.isUnemployed(villager);

        if (unemployed) {
            if (!ModConfig.jobs.professionsFromWorkstations) {
                return null;
            }
        } else if (JobSiteRegistry.siteFor(villager) == null) {
            // A career with no workstation registered has nowhere to go, and nothing is gated on it.
            return null;
        }

        Block wanted = unemployed ? null : JobSiteRegistry.siteFor(villager);

        World world = villager.world;
        int radius = ModConfig.jobs.searchRadius;
        int height = ModConfig.jobs.searchHeight;
        BlockPos origin = new BlockPos(villager);

        // Every claim that could collide with one made here, fetched once - see JobSite.claimsNear.
        // Radius, not twice it: this is an entity search over every chunk the box touches, and at
        // twice the radius that is four times the chunks for villagers that are almost never holding
        // a claim this far inside our reach anyway.
        Set<BlockPos> taken = JobSite.claimsNear(villager, radius);

        List<BlockPos> candidates = new ArrayList<BlockPos>();
        long now = world.getTotalWorldTime();

        int minChunkX = (origin.getX() - radius) >> 4;
        int maxChunkX = (origin.getX() + radius) >> 4;
        int minChunkZ = (origin.getZ() - radius) >> 4;
        int maxChunkZ = (origin.getZ() + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                for (BlockPos pos : WorkstationIndex.workstationsIn(world, chunkX, chunkZ)) {
                    // The index knows the whole column, so the reach is applied here: square
                    // horizontally, and much shallower vertically.
                    if (Math.abs(pos.getX() - origin.getX()) > radius
                            || Math.abs(pos.getZ() - origin.getZ()) > radius
                            || Math.abs(pos.getY() - origin.getY()) > height) {
                        continue;
                    }

                    if (!unemployed && world.getBlockState(pos).getBlock() != wanted) {
                        continue;
                    }

                    if (taken.contains(pos) || UnreachableSites.recentlyFailed(villager, pos, now)) {
                        continue;
                    }

                    candidates.add(pos);
                }
            }
        }

        BlockPos best = chooseReachable(villager, origin, candidates, now);

        if (best != null) {
            JobSite.set(villager, best);
            com.exiledradio.villagerbackport.home.VillageDebug.say(villager,
                    "work: claimed " + villager.world.getBlockState(best).getBlock().getLocalizedName()
                            + " at " + best.getX() + ", " + best.getY() + ", " + best.getZ());
        } else if (com.exiledradio.villagerbackport.home.VillageDebug.on()) {
            report(villager, unemployed, wanted, candidates.size());
        }

        return best;
    }

    /**
     * Says why no workstation was claimed.
     *
     * <p>Written out rather than left as a bare "no", because the reasons are very different
     * problems: a villager that already has a job wanting a workstation of a kind nobody built is
     * nothing like one standing beside a free lectern it cannot path to, and from outside the two
     * look identical - a villager doing nothing.
     */
    private static void report(EntityVillager villager, boolean unemployed, Block wanted, int candidates) {
        if (candidates == 0) {
            com.exiledradio.villagerbackport.home.VillageDebug.repeat(villager, "work-none",
                    unemployed
                            ? "work: no free workstation within " + ModConfig.jobs.searchRadius + " blocks"
                            : "work: no free " + (wanted == null ? "workstation" : wanted.getLocalizedName())
                                    + " within " + ModConfig.jobs.searchRadius + " blocks"
                                    + " (it is already employed, so only its own kind counts)");
            return;
        }

        com.exiledradio.villagerbackport.home.VillageDebug.repeat(villager, "work-unreachable",
                "work: " + candidates + " free nearby but none it can walk to and touch");
    }

    /**
     * Picks the nearest workstation the villager can actually walk to.
     *
     * <h2>Why nearness alone was not enough</h2>
     * Choosing purely by distance meant claiming whatever was closest as the crow flies, which is
     * routinely something behind a wall - a workstation in a house whose door is blocked is a couple
     * of blocks away and completely out of reach. The villager would then walk at it, and because
     * {@code tryMoveToXYZ} paths to the nearest point it <em>can</em> reach when the target is
     * unreachable, it would settle against the outside of the back wall and stay there.
     *
     * <p>That one mistake accounted for most of what this looked like from the outside: villagers
     * standing at walls, villagers apparently refusing to use doors - they were never routed through
     * a door, because the path they were given did not go into the building at all - and villagers
     * ignoring a perfectly good workstation slightly further off in favour of an unreachable one
     * nearby.
     *
     * <h2>What it costs</h2>
     * Asking the navigator for a path is not free, so only the nearest few are asked about, and each
     * failure is remembered so the next search starts past it. A villager therefore works its way
     * outwards over a few seconds rather than paying for every candidate at once.
     *
     * <p>Anything beyond the navigator's own search range cannot be answered - it will not path that
     * far - and is therefore not claimed. That reach is deliberately set past the search radius, so
     * in practice every candidate gets a real answer; what it rules out is a claim staked on a
     * workstation nobody ever established a route to. Waiting for the unreached timeout to undo
     * those was not good enough - for ten minutes the block is spoken for, and the villager who
     * could actually walk to it cannot have it.
     */
    @Nullable
    private static BlockPos chooseReachable(EntityVillager villager, final BlockPos origin,
                                            List<BlockPos> candidates, long now) {
        if (candidates.isEmpty()) {
            return null;
        }

        Collections.sort(candidates, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos a, BlockPos b) {
                return Double.compare(origin.distanceSq(a), origin.distanceSq(b));
            }
        });

        // The reach a path will actually be searched with - which is the borrowed one, not what the
        // villager's attribute reads right now. Reading the attribute here was worth a bug: the
        // extension is only applied around the path call itself, so this saw the base sixteen blocks
        // and waved through every candidate further off than that without ever checking it.
        double pathRange = WorkPathing.effectiveRange(villager);
        int asked = 0;

        for (BlockPos pos : candidates) {
            // Sorted by distance, so the first one out of the navigator's reach means every one
            // after it is too. Nothing beyond here can be answered, and an unanswered question is
            // not a yes.
            if (origin.distanceSq(pos) > pathRange * pathRange) {
                break;
            }

            if (asked >= MAX_PATH_CHECKS) {
                break;
            }
            asked++;

            if (canReach(villager, pos)) {
                return pos;
            }

            UnreachableSites.remember(villager, pos, now);
        }

        return null;
    }

    /** @return true if the navigator can find a path that actually arrives at the workstation. */
    private static boolean canReach(EntityVillager villager, BlockPos pos) {
        // Already there. Asking the navigator to prove a villager can walk to something it is
        // standing against is not just wasted work, it is a question that can answer no: a path to a
        // solid block is redirected upward to the first space above it, which for a workstation that
        // is not a full cube is somewhere the villager cannot stand at all. That no is then recorded
        // as a failure and the block ignored for the next minute - which is how a lectern replaced
        // under a villager's nose could end up being the one block it refused to take.
        if (JobSite.isTouching(villager, pos)) {
            return true;
        }

        Path path = WorkPathing.pathTo(villager, pos);

        if (path == null) {
            return false;
        }

        PathPoint end = path.getFinalPathPoint();
        if (end == null) {
            return false;
        }

        // A path that cannot get there comes back stopping at the closest point it could reach, so
        // where it ends is the question, not whether one was produced at all.
        //
        // And it has to end against the block, not merely near it. Measuring that as a distance was
        // the mistake: a workstation on the far side of a wall is two blocks from where the path
        // gives up, which any working distance loose enough to let a villager stand diagonally at
        // its own workstation will happily accept. Adjacency does not have that problem - a block
        // touches its neighbour or it does not, and one block of wall is exactly what separates the
        // two cases.
        if (Math.abs(end.x - pos.getX()) > 1
                || Math.abs(end.y - pos.getY()) > 1
                || Math.abs(end.z - pos.getZ()) > 1) {
            return false;
        }

        // A path ending in a block that shares a face with the workstation has arrived, and no
        // further test can improve on that: two blocks touching have nothing between them.
        //
        // This is not a shortcut, it is a correction. The sight line below traces to the middle of
        // the workstation, and from a cramped spot - a villager pen, a hut, a glass box with the
        // workstations inside it - that ray leaves the path's block, clips the corner of a wall or
        // the workstation's own shape, and reports a wall where there is none. The villager then
        // blacklists a workstation it is standing next to, and sixty seconds later finds nothing to
        // claim at all, which is exactly the alternation of "some free but unreachable" and "none
        // free" a full pen produces.
        //
        // 1.14 has no sight line here at all - GatherPOITask asks the navigator for a path and takes
        // it if it reaches. The ray is kept only for the case it was added for, a path stopping on
        // the far side of one block of wall, which is never face-adjacent.
        if (sharesFace(end, pos)) {
            return true;
        }

        return JobSite.canTouch(villager.world,
                new Vec3d(end.x + 0.5D, end.y + villager.getEyeHeight(), end.z + 0.5D), pos);
    }

    /** @return true if the path ended in a block touching the workstation face to face. */
    private static boolean sharesFace(PathPoint end, BlockPos pos) {
        return Math.abs(end.x - pos.getX())
                + Math.abs(end.y - pos.getY())
                + Math.abs(end.z - pos.getZ()) == 1;
    }
}
