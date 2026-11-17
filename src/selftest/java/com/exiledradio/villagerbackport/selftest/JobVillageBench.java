package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.job.JobSite;
import com.exiledradio.villagerbackport.job.JobSiteRegistry;
import com.exiledradio.villagerbackport.job.WorkPathing;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A measurement, not a pass/fail test: {@code -Pselftest=jobvillage}. Finds the nearest village, makes
 * every villager in it unemployed with no claim, ticks them by hand for {@value #TICKS} ticks of daytime
 * (three rounds) and logs who found work, who is still looking, and who stood still with a claim it
 * never reached, with its path, running goals and a trace of its movement. With the environment variable
 * {@code JOBBENCH_MAP=x0,y0,z0,x1,y1,z1} it first prints a block map of that box.
 */
public final class JobVillageBench {

    private static final int RADIUS = 80;
    private static final int TICKS = 3600;

    private int ticks;

    public static void start() {
        MinecraftForge.EVENT_BUS.register(new JobVillageBench());
    }

    private static void log(String line) {
        VillagerBackport.LOGGER.info("JOBBENCH " + line);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        this.ticks++;
        if (this.ticks == 20) {
            BlockPos village = world.findNearestStructure("Village", world.getSpawnPoint(), false);
            log("village " + village);
            if (village == null) {
                server.initiateShutdown();
                return;
            }
            world.setSpawnPoint(world.getTopSolidOrLiquidBlock(village));
            for (int cx = -8; cx <= 8; cx++) {
                for (int cz = -8; cz <= 8; cz++) {
                    world.getChunk((village.getX() >> 4) + cx, (village.getZ() >> 4) + cz);
                }
            }
        }
        if (this.ticks == 99 && System.getenv("JOBBENCH_MAP") != null) {
            String[] a = System.getenv("JOBBENCH_MAP").split(",");
            int x0 = Integer.parseInt(a[0]), y0 = Integer.parseInt(a[1]), z0 = Integer.parseInt(a[2]);
            int x1 = Integer.parseInt(a[3]), y1 = Integer.parseInt(a[4]), z1 = Integer.parseInt(a[5]);
            for (int y = y0; y <= y1; y++) {
                log("MAP y=" + y + " x " + x0 + ".." + x1);
                for (int z = z0; z <= z1; z++) {
                    StringBuilder sb = new StringBuilder("MAP z=" + z + " ");
                    for (int x = x0; x <= x1; x++) {
                        IBlockState st = world.getBlockState(new BlockPos(x, y, z));
                        sb.append(st.getMaterial() == net.minecraft.block.material.Material.AIR ? "." : st.getBlock().getRegistryName().getPath()
                                .replace("tallgrass", "tg").replace("farmland", "FL").replace("composter", "COMP").replace("grass_path", "gp")
                                + (st.getBlock() instanceof net.minecraft.block.BlockLog ? ":" + st.getValue(net.minecraft.block.BlockLog.LOG_AXIS) : ""));
                        sb.append(' ');
                    }
                    log(sb.toString());
                }
            }
        }
        if (this.ticks == 100) {
            for (int round = 1; round <= 3; round++) {
                log("ROUND " + round);
                run(world);
            }
            server.initiateShutdown();
        }
    }

    private void run(WorldServer world) {
        BlockPos centre = world.getSpawnPoint();
        AxisAlignedBB box = new AxisAlignedBB(centre).grow(RADIUS, 64, RADIUS);
        List<EntityVillager> villagers = new ArrayList<EntityVillager>();
        for (EntityVillager v : world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            if (v.isChild()) {
                continue;
            }
            JobSite.clear(v);
            if (!Employment.isNitwit(v)) {
                Employment.unassign(v);
            }
            villagers.add(v);
        }
        WorkstationIndex.invalidateAll();
        List<BlockPos> sites = new ArrayList<BlockPos>();
        for (int cx = (centre.getX() - RADIUS) >> 4; cx <= (centre.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (centre.getZ() - RADIUS) >> 4; cz <= (centre.getZ() + RADIUS) >> 4; cz++) {
                sites.addAll(WorkstationIndex.workstationsIn(world, cx, cz));
            }
        }
        log("villagers " + villagers.size() + ", workstations " + sites.size());

        long time = world.getWorldTime();
        world.setWorldTime(time - time % 24000L + 1000L);

        Map<EntityVillager, Integer> employedAt = new HashMap<EntityVillager, Integer>();
        Map<EntityVillager, double[]> mark = new HashMap<EntityVillager, double[]>();
        Map<EntityVillager, Integer> stillFor = new HashMap<EntityVillager, Integer>();
        Map<EntityVillager, Integer> claims = new HashMap<EntityVillager, Integer>();
        Map<EntityVillager, BlockPos> lastClaim = new HashMap<EntityVillager, BlockPos>();
        Map<EntityVillager, Integer> trace = new HashMap<EntityVillager, Integer>();

        for (int t = 1; t <= TICKS; t++) {
            world.getWorldInfo().setWorldTotalTime(world.getTotalWorldTime() + 1L);
            world.setWorldTime(world.getWorldTime() + 1L);
            for (EntityVillager v : villagers) {
                if (v.isDead) {
                    continue;
                }
                world.updateEntity(v);
                Integer traceUntil = trace.get(v);
                if (traceUntil != null && t <= traceUntil) {
                    net.minecraft.pathfinding.Path tp = v.getNavigator().getPath();
                    String cur = tp == null || tp.isFinished() ? "-" : tp.getPathPointFromIndex(tp.getCurrentPathIndex()).toString()
                            + " " + tp.getCurrentPathIndex() + "/" + tp.getCurrentPathLength();
                    log(String.format("  trace t=%d pos %.3f %.3f %.3f mot %.3f %.3f %.3f onGround=%s colH=%s jump=%s moveTo %.2f %.2f %.2f upd=%s point %s",
                            t, v.posX, v.posY, v.posZ, v.motionX, v.motionY, v.motionZ, v.onGround, v.collidedHorizontally,
                            v.getJumpHelper() != null, v.getMoveHelper().getX(), v.getMoveHelper().getY(), v.getMoveHelper().getZ(),
                            v.getMoveHelper().isUpdating(), cur));
                }
                BlockPos claim = JobSite.get(v);
                if (claim != null && !claim.equals(lastClaim.get(v))) {
                    claims.put(v, (claims.containsKey(v) ? claims.get(v) : 0) + 1);
                    log("t=" + t + " villager " + v.getEntityId() + " claims " + claim + " from " + new BlockPos(v));
                }
                lastClaim.put(v, claim);
                if (!employedAt.containsKey(v) && !Employment.isUnemployed(v)) {
                    employedAt.put(v, t);
                }
                if (t % 20 == 0) {
                    double[] was = mark.get(v);
                    boolean still = was != null && Math.abs(v.posX - was[0]) < 1.0D && Math.abs(v.posZ - was[1]) < 1.0D;
                    if (still && claim != null && Employment.isUnemployed(v)) {
                        stillFor.put(v, (stillFor.containsKey(v) ? stillFor.get(v) : 0) + 20);
                        if (stillFor.get(v) % 200 == 0 && stillFor.get(v) <= 600) {
                            log("STUCK t=" + t + " villager " + v.getEntityId() + " claim " + claim + " "
                                    + world.getBlockState(claim).getBlock().getRegistryName()
                                    + String.format(" exact %.3f %.3f %.3f onGround=%s collidedH=%s", v.posX, v.posY, v.posZ, v.onGround, v.collidedHorizontally));
                            surroundings(world, claim, v);
                            dump(world, "  navigator path", v.getNavigator().getPath());
                            dump(world, "  fresh path", WorkPathing.pathTo(v, claim));
                            goals(v);
                            if (!trace.containsKey(v)) {
                                trace.put(v, t + 80);
                                for (BlockPos q : BlockPos.getAllInBoxMutable(new BlockPos(v).add(-1, -1, -1), new BlockPos(v).add(1, 2, 1))) {
                                    IBlockState qs = world.getBlockState(q);
                                    AxisAlignedBB bb = qs.getCollisionBoundingBox(world, q);
                                    if (qs.getMaterial() != net.minecraft.block.material.Material.AIR) {
                                        log("  block " + q + " " + qs + " box " + bb);
                                    }
                                }
                            }
                        }
                    } else if (!still) {
                        mark.put(v, new double[]{v.posX, v.posZ});
                    }
                    if (was == null) {
                        mark.put(v, new double[]{v.posX, v.posZ});
                    }
                }
            }
        }

        Set<BlockPos> claimed = new HashSet<BlockPos>();
        int employed = 0;
        for (EntityVillager v : villagers) {
            BlockPos claim = JobSite.get(v);
            if (claim != null) {
                claimed.add(claim);
            }
            Integer at = employedAt.get(v);
            if (at != null) {
                employed++;
            }
            Integer still = stillFor.get(v);
            String where = claim == null ? "-" : String.format("%.2f %.2f %.2f",
                    v.posX - claim.getX() - 0.5D, v.posY - claim.getY(), v.posZ - claim.getZ() - 0.5D);
            log(String.format("villager %d at %s nitwit=%s employedAt=%s claim=%s %s rel=%s claims=%s stuckTicks=%s",
                    v.getEntityId(), new BlockPos(v), Employment.isNitwit(v), at, claim,
                    claim == null ? "" : world.getBlockState(claim).getBlock().getRegistryName(), where,
                    claims.get(v), still));
            if (claim != null && !JobSite.isAtSite(v) && still != null && still >= 200) {
                surroundings(world, claim, v);
                log(String.format("  exact %.3f %.3f %.3f onGround=%s collidedH=%s", v.posX, v.posY, v.posZ, v.onGround, v.collidedHorizontally));
                dump(world, "  navigator path", v.getNavigator().getPath());
                dump(world, "  fresh path", WorkPathing.pathTo(v, claim));
            }
        }
        log("employed " + employed + " of " + villagers.size());

        for (BlockPos site : sites) {
            if (claimed.contains(site)) {
                continue;
            }
            IBlockState state = world.getBlockState(site);
            if (!JobSiteRegistry.isWorkstation(state.getBlock())) {
                continue;
            }
            EntityVillager nearest = null;
            for (EntityVillager v : villagers) {
                if (JobSite.get(v) == null && Employment.isUnemployed(v)
                        && (nearest == null || v.getDistanceSq(site) < nearest.getDistanceSq(site))) {
                    nearest = v;
                }
            }
            String path = "";
            if (nearest != null) {
                Path p = WorkPathing.pathTo(nearest, site);
                PathPoint end = p == null ? null : p.getFinalPathPoint();
                path = "nearest unemployed " + nearest.getEntityId() + " at " + new BlockPos(nearest)
                        + String.format(" dist %.1f", Math.sqrt(nearest.getDistanceSq(site)))
                        + " path ends " + (end == null ? "none" : (end.x - site.getX()) + "," + (end.y - site.getY())
                        + "," + (end.z - site.getZ()) + " len " + p.getCurrentPathLength());
            }
            log("free " + state.getBlock().getRegistryName() + " at " + site + " " + path);
            if (nearest != null) {
                dump(world, "  path", WorkPathing.pathTo(nearest, site));
            }
            surroundings(world, site, null);
        }
    }

    private static void goals(EntityVillager v) {
        StringBuilder sb = new StringBuilder("  running:");
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry e : v.tasks.taskEntries) {
            if (!e.using) {
                continue;
            }
            sb.append(' ').append(e.priority).append(':').append(e.action.getClass().getSimpleName());
        }
        sb.append(" | tag ").append(com.exiledradio.villagerbackport.data.VillagerTradeData.root(v));
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry e : v.tasks.taskEntries) {
            if (e.action instanceof com.exiledradio.villagerbackport.job.EntityAIWorkAtSite) {
                sb.append(" | work goal p").append(e.priority);
                for (java.lang.reflect.Field f : e.action.getClass().getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    f.setAccessible(true);
                    try {
                        sb.append(' ').append(f.getName()).append('=').append(f.get(e.action));
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        }
        sb.append(" | now ").append(v.world.getTotalWorldTime());
        sb.append(" | unemployed=").append(Employment.isUnemployed(v)).append(" validated=").append(JobSite.validated(v))
                .append(" atSite=").append(JobSite.isAtSite(v)).append(" customer=").append(v.getCustomer())
                .append(" child=").append(v.isChild()).append(" aiDisabled=").append(v.isAIDisabled());
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry e : v.tasks.taskEntries) {
            if (e.action instanceof com.exiledradio.villagerbackport.job.EntityAIWorkAtSite) {
                sb.append(" shouldExecute=").append(e.action.shouldExecute());
                e.action.resetTask();
            }
        }
        log(sb.toString());
    }

    private static void dump(WorldServer world, String what, Path path) {
        if (path == null) {
            log(what + ": none");
            return;
        }
        StringBuilder sb = new StringBuilder(what + " (at " + path.getCurrentPathIndex() + "/" + path.getCurrentPathLength() + "):");
        for (int i = 0; i < path.getCurrentPathLength(); i++) {
            PathPoint p = path.getPathPointFromIndex(i);
            BlockPos pos = new BlockPos(p.x, p.y, p.z);
            sb.append(' ').append(p.x).append(',').append(p.y).append(',').append(p.z).append('[')
                    .append(world.getBlockState(pos).getBlock().getRegistryName().getPath()).append('/')
                    .append(world.getBlockState(pos.down()).getBlock().getRegistryName().getPath()).append(' ')
                    .append(p.nodeType).append(']');
        }
        log(sb.toString());
    }

    /** The 5x3x5 blocks around a workstation, layer by layer, so a log shows what the villager faced. */
    private static void surroundings(WorldServer world, BlockPos site, EntityVillager v) {
        BlockPos at = v == null ? null : new BlockPos(v);
        for (int dy = -1; dy <= 2; dy++) {
            StringBuilder sb = new StringBuilder("  y" + (dy >= 0 ? "+" : "") + dy + ": ");
            for (int dz = -2; dz <= 2; dz++) {
                for (int dx = -2; dx <= 2; dx++) {
                    BlockPos p = site.add(dx, dy, dz);
                    if (p.equals(at)) {
                        sb.append('V');
                        continue;
                    }
                    if (dx == 0 && dy == 0 && dz == 0) {
                        sb.append('W');
                        continue;
                    }
                    IBlockState s = world.getBlockState(p);
                    String name = s.getBlock().getRegistryName().getPath();
                    char ch = s.getMaterial() == net.minecraft.block.material.Material.AIR ? '.'
                            : s.getBlock() instanceof net.minecraft.block.BlockDoor ? 'D'
                            : s.getBlock() instanceof net.minecraft.block.BlockBed ? 'b'
                            : s.getBlock() instanceof net.minecraft.block.BlockCarpet ? 'c'
                            : s.getBlock() instanceof net.minecraft.block.BlockStairs ? 's'
                            : s.getBlock() instanceof net.minecraft.block.BlockSlab ? '_'
                            : s.isFullCube() ? '#' : Character.toLowerCase(name.charAt(0));
                    sb.append(ch);
                }
                sb.append(' ');
            }
            log(sb.toString());
        }
    }
}
