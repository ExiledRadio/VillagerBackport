package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.job.JobSiteRegistry;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A measurement, not a pass/fail test: {@code -Pselftest=census}. Finds up to {@code CENSUS_N} (8) villages
 * on a grid around the origin and logs, per village, its workstation blocks by type, beds, and adult
 * villagers split into employed by career, true nitwits and unemployed - right after generation and
 * after {@code CHECKPOINTS} ticks of daytime, ticking the villagers by hand as {@link JobVillageBench}
 * does. Lines start {@code CENSUS}.
 */
public final class VillageCensusBench {

    private static final int VILLAGES = System.getenv("CENSUS_N") == null ? 8 : Integer.parseInt(System.getenv("CENSUS_N"));
    private static final int RADIUS = 80;
    private static final int[] CHECKPOINTS = {1200, 6000, 12000, 24000};

    private int ticks;

    public static void start() {
        MinecraftForge.EVENT_BUS.register(new VillageCensusBench());
    }

    static void log(String line) {
        VillagerBackport.LOGGER.info("CENSUS " + line);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++this.ticks != 20) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        List<BlockPos> villages = new ArrayList<BlockPos>();
        for (int r = 0; r <= 6 && villages.size() < VILLAGES; r++) {
            for (int i = -r; i <= r && villages.size() < VILLAGES; i++) {
                for (int j = -r; j <= r && villages.size() < VILLAGES; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != r) {
                        continue;
                    }
                    BlockPos found = world.findNearestStructure("Village", new BlockPos(i * 900, 64, j * 900), false);
                    if (found == null) {
                        continue;
                    }
                    boolean dup = false;
                    for (BlockPos v : villages) {
                        dup |= v.distanceSq(found.getX(), v.getY(), found.getZ()) < 200 * 200;
                    }
                    if (!dup) {
                        villages.add(found);
                    }
                }
            }
        }
        Totals before = new Totals("generated");
        Totals[] after = new Totals[CHECKPOINTS.length];
        for (int c = 0; c < after.length; c++) {
            after[c] = new Totals("after " + CHECKPOINTS[c] + " ticks");
        }
        for (BlockPos centre : villages) {
            if (System.getenv("CENSUS_PIECES") != null) {
                world.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
                log("VILLAGE " + centre + " " + world.getBiome(centre).getRegistryName());
                pieces(world, centre);
                continue;
            }
            survey(world, centre, before, after);
        }
        before.log();
        for (Totals t : after) {
            t.log();
        }
        log("DONE villages=" + villages.size());
        server.initiateShutdown();
    }

    static final class Totals {
        final String label;
        int villages, sites, beds, adults, employed, nitwits, unemployed, smithSites, smithChests;

        Totals(String label) {
            this.label = label;
        }

        void log() {
            VillageCensusBench.log(String.format("TOTAL %s: villages=%d sites=%d beds=%d adults=%d employed=%d nitwits=%d unemployed=%d smithSites=%d smithChests=%d",
                    label, villages, sites, beds, adults, employed, nitwits, unemployed, smithSites, smithChests));
        }
    }

    private static void survey(WorldServer world, BlockPos centre, Totals before, Totals[] after) {
        int r = (RADIUS >> 4) + 1;
        for (int cx = -r; cx <= r; cx++) {
            for (int cz = -r; cz <= r; cz++) {
                world.getChunk((centre.getX() >> 4) + cx, (centre.getZ() >> 4) + cz);
            }
        }
        WorkstationIndex.invalidateAll();
        String biome = world.getBiome(centre).getRegistryName().toString();
        Map<String, Integer> blocks = new TreeMap<String, Integer>();
        int sites = 0, beds = 0, smith = 0, smithChests = 0;
        for (int cx = (centre.getX() - RADIUS) >> 4; cx <= (centre.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (centre.getZ() - RADIUS) >> 4; cz <= (centre.getZ() + RADIUS) >> 4; cz++) {
                for (BlockPos p : WorkstationIndex.workstationsIn(world, cx, cz)) {
                    Block b = world.getBlockState(p).getBlock();
                    String name = b.getRegistryName().getPath();
                    blocks.put(name, blocks.containsKey(name) ? blocks.get(name) + 1 : 1);
                    sites++;
                    String career = JobSiteRegistry.careerForBlock(b);
                    if ("armor".equals(career) || "weapon".equals(career) || "tool".equals(career)) {
                        smith++;
                        if (chestNear(world, p)) {
                            smithChests++;
                        }
                    }
                }
                beds += WorkstationIndex.bedsIn(world, cx, cz).size();
            }
        }
        List<EntityVillager> adults = new ArrayList<EntityVillager>();
        AxisAlignedBB box = new AxisAlignedBB(centre).grow(RADIUS, 80, RADIUS);
        for (EntityVillager v : world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            if (!v.isChild()) {
                adults.add(v);
            }
        }
        log("VILLAGE " + centre + " " + biome + " sites=" + sites + " beds=" + beds + " smithSites=" + smith
                + " smithChests=" + smithChests + " blocks=" + blocks);
        for (Totals t : new Totals[]{before}) {
            add(t, sites, beds, smith, smithChests);
        }
        people(world, "  generated", adults, before);
        pieces(world, centre);

        long time = world.getWorldTime();
        world.setWorldTime(time - time % 24000L + 1000L);
        int t = 0;
        for (int c = 0; c < CHECKPOINTS.length; c++) {
            for (; t < CHECKPOINTS[c]; t++) {
                world.getWorldInfo().setWorldTotalTime(world.getTotalWorldTime() + 1L);
                // Daytime throughout: 1000..11000, so nobody goes to bed.
                world.setWorldTime(world.getWorldTime() - world.getWorldTime() % 24000L + 1000L + (t % 10000));
                for (EntityVillager v : adults) {
                    if (!v.isDead) {
                        world.updateEntity(v);
                    }
                }
            }
            add(after[c], sites, beds, smith, smithChests);
            people(world, "  after " + CHECKPOINTS[c], adults, after[c]);
        }
        leftovers(world, centre, adults);
    }

    /** Logs the house templates of the village start covering this position, by reflection: the pieces are package-private. */
    private static void pieces(WorldServer world, BlockPos centre) {
        try {
            Object gen = com.exiledradio.villagerbackport.village.modern.MapGenModernVillage.of(world);
            java.lang.reflect.Field f = net.minecraft.world.gen.structure.MapGenStructure.class.getDeclaredField("structureMap");
            f.setAccessible(true);
            Map<?, ?> starts = (Map<?, ?>) f.get(gen);
            for (Object o : starts.values()) {
                net.minecraft.world.gen.structure.StructureStart start = (net.minecraft.world.gen.structure.StructureStart) o;
                if (!start.getBoundingBox().isVecInside(new BlockPos(centre.getX(), start.getBoundingBox().minY, centre.getZ()))) {
                    continue;
                }
                Map<String, Integer> names = new TreeMap<String, Integer>();
                int placed = 0;
                for (net.minecraft.world.gen.structure.StructureComponent c : start.getComponents()) {
                    String name = c.getClass().getSimpleName();
                    try {
                        java.lang.reflect.Method m = c.getClass().getDeclaredMethod("element");
                        m.setAccessible(true);
                        Object e = m.invoke(c);
                        java.lang.reflect.Field loc = e.getClass().getDeclaredField("location");
                        loc.setAccessible(true);
                        name = String.valueOf(loc.get(e));
                        name = name.substring(name.lastIndexOf('/') + 1);
                    } catch (ReflectiveOperationException ignored) {
                    }
                    if (name.contains("street") || name.contains("decoration") || name.contains("lamp") || name.startsWith("Empty")) {
                        continue;
                    }
                    placed++;
                    names.put(name, names.containsKey(name) ? names.get(name) + 1 : 1);
                }
                log("  pieces(" + placed + ") " + names);
            }
        } catch (Exception e) {
            log("  pieces: " + e);
        }
    }

    /** For each workstation nobody holds, the nearest unemployed villager and whether it can walk there. */
    private static void leftovers(WorldServer world, BlockPos centre, List<EntityVillager> adults) {
        java.util.Set<BlockPos> held = new java.util.HashSet<BlockPos>();
        for (EntityVillager v : adults) {
            if (com.exiledradio.villagerbackport.job.JobSite.get(v) != null) {
                held.add(com.exiledradio.villagerbackport.job.JobSite.get(v));
            }
        }
        for (int cx = (centre.getX() - RADIUS) >> 4; cx <= (centre.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (centre.getZ() - RADIUS) >> 4; cz <= (centre.getZ() + RADIUS) >> 4; cz++) {
                for (BlockPos p : WorkstationIndex.workstationsIn(world, cx, cz)) {
                    if (held.contains(p)) {
                        continue;
                    }
                    EntityVillager best = null;
                    for (EntityVillager v : adults) {
                        if (!v.isDead && Employment.isUnemployed(v) && !Employment.isNitwit(v)
                                && (best == null || v.getDistanceSq(p) < best.getDistanceSq(p))) {
                            best = v;
                        }
                    }
                    String who = "nobody";
                    if (best != null) {
                        net.minecraft.pathfinding.Path path = com.exiledradio.villagerbackport.job.WorkPathing.pathTo(best, p);
                        who = String.format("villager %d at %s dist %.1f claim %s path %s arrives %s", best.getEntityId(),
                                new BlockPos(best), Math.sqrt(best.getDistanceSq(p)),
                                com.exiledradio.villagerbackport.job.JobSite.get(best),
                                path == null ? "null" : path.getCurrentPathLength() + "->" + path.getFinalPathPoint(),
                                com.exiledradio.villagerbackport.job.WorkPathing.arrives(path, p));
                    }
                    if (best != null && !com.exiledradio.villagerbackport.job.WorkPathing.arrives(
                            com.exiledradio.villagerbackport.job.WorkPathing.pathTo(best, p), p) && System.getenv("CENSUS_MAP") != null) {
                        map(world, p);
                    }
                    if (best != null) {
                        for (BlockPos q : BlockPos.getAllInBox(p.add(-8, -2, -8), p.add(8, 2, 8))) {
                            net.minecraft.block.state.IBlockState ds = world.getBlockState(q);
                            if (ds.getBlock() instanceof net.minecraft.block.BlockDoor
                                    && ds.getValue(net.minecraft.block.BlockDoor.HALF) == net.minecraft.block.BlockDoor.EnumDoorHalf.LOWER) {
                                net.minecraft.pathfinding.Path dp = com.exiledradio.villagerbackport.job.WorkPathing.pathTo(best, q);
                                net.minecraft.pathfinding.Path vp = best.getNavigator().getPathToPos(q);
                                log("    door " + q + " " + ds + " long " + (dp == null ? "null" : dp.getFinalPathPoint())
                                        + " arrives " + com.exiledradio.villagerbackport.job.WorkPathing.arrives(dp, q)
                                        + " vanillaNav " + (vp == null ? "null" : vp.getFinalPathPoint())
                                        + " type " + best.getNavigator().getNodeProcessor().getPathNodeType(world, q.getX(), q.getY(), q.getZ())
                                        + " canOpen " + best.getNavigator().getNodeProcessor().getCanOpenDoors()
                                        + " enter " + best.getNavigator().getNodeProcessor().getCanEnterDoors());
                                net.minecraft.util.EnumFacing fc = ds.getValue(net.minecraft.block.BlockDoor.FACING);
                                for (BlockPos side : new BlockPos[]{q.offset(fc), q.offset(fc.getOpposite())}) {
                                    EntityVillager probe = new EntityVillager(world);
                                    probe.setPosition(side.getX() + 0.5D, side.getY(), side.getZ() + 0.5D);
                                    probe.onGround = true;
                                    net.minecraft.pathfinding.Path sp = com.exiledradio.villagerbackport.job.WorkPathing.pathTo(probe, p);
                                    StringBuilder pts = new StringBuilder();
                                    for (int k = 0; sp != null && k < sp.getCurrentPathLength(); k++) {
                                        pts.append(sp.getPathPointFromIndex(k)).append(' ');
                                    }
                                    EntityVillager far = new EntityVillager(world);
                                    far.setPosition(best.posX, best.posY, best.posZ);
                                    far.onGround = true;
                                    net.minecraft.pathfinding.Path fp = com.exiledradio.villagerbackport.job.WorkPathing.pathTo(far, side);
                                    StringBuilder fpts = new StringBuilder();
                                    for (int k = Math.max(0, fp == null ? 0 : fp.getCurrentPathLength() - 6); fp != null && k < fp.getCurrentPathLength(); k++) {
                                        fpts.append(fp.getPathPointFromIndex(k)).append(' ');
                                    }
                                    log("      villager->" + side + " " + world.getBlockState(side.down()).getBlock().getRegistryName().getPath()
                                            + " arrives " + com.exiledradio.villagerbackport.job.WorkPathing.arrives(fp, side) + " tail " + fpts);
                                    log("      from " + side + " " + world.getBlockState(side).getBlock().getRegistryName().getPath()
                                            + " arrives " + com.exiledradio.villagerbackport.job.WorkPathing.arrives(sp, p) + " path " + pts);
                                }
                            }
                        }
                    }
                    if (best != null) {
                        EntityVillager probe = new EntityVillager(world);
                        probe.setPosition(best.posX, best.posY, best.posZ);
                        probe.onGround = true;
                        net.minecraft.pathfinding.Path pp = com.exiledradio.villagerbackport.job.WorkPathing.pathTo(probe, p);
                        log("    probe at villager: " + (pp == null ? "null" : pp.getCurrentPathLength() + "->" + pp.getFinalPathPoint())
                                + " arrives " + com.exiledradio.villagerbackport.job.WorkPathing.arrives(pp, p)
                                + " real onGround " + best.onGround + " box " + best.getEntityBoundingBox() + " riding " + best.isRiding());
                    }
                    log("  FREE " + world.getBlockState(p).getBlock().getRegistryName().getPath() + " " + p
                            + " dCentre=" + (int) Math.sqrt(p.distanceSq(centre)) + " nearest " + who);
                }
            }
        }
        for (EntityVillager v : adults) {
            BlockPos claim = com.exiledradio.villagerbackport.job.JobSite.get(v);
            if (!v.isDead && Employment.isUnemployed(v) && claim != null) {
                net.minecraft.pathfinding.Path path = com.exiledradio.villagerbackport.job.WorkPathing.pathTo(v, claim);
                StringBuilder goals = new StringBuilder();
                for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry e : v.tasks.taskEntries) {
                    if (e.using) {
                        goals.append(e.action.getClass().getSimpleName()).append(' ');
                    }
                }
                log(String.format("  HELD villager %d exact %.2f %.2f %.2f claim %s %s centreDist %.2f atSite %s data %s path %s arrives %s nav %s goals %s",
                        v.getEntityId(), v.posX, v.posY, v.posZ, claim, world.getBlockState(claim).getBlock().getRegistryName().getPath(),
                        Math.sqrt(v.getDistanceSqToCenter(claim)), com.exiledradio.villagerbackport.job.JobSite.isAtSite(v),
                        com.exiledradio.villagerbackport.data.VillagerTradeData.root(v),
                        path == null ? "null" : path.getCurrentPathLength() + "->" + path.getFinalPathPoint(),
                        com.exiledradio.villagerbackport.job.WorkPathing.arrives(path, claim),
                        v.getNavigator().noPath() ? "idle" : "moving", goals));
                if (System.getenv("CENSUS_MAP") != null) {
                    map(world, claim);
                }
            }
            if (!v.isDead && Employment.isUnemployed(v) && !Employment.isNitwit(v)) {
                log("  IDLE villager " + v.getEntityId() + " at " + new BlockPos(v) + " claim "
                        + com.exiledradio.villagerbackport.job.JobSite.get(v) + " dCentre=" + (int) Math.sqrt(new BlockPos(v).distanceSq(centre)));
            }
        }
    }

    private static void map(WorldServer world, BlockPos p) {
        int r = Integer.parseInt(System.getenv("CENSUS_MAP"));
        for (int y = p.getY() - 1; y <= p.getY() + 2; y++) {
            log("    map y=" + y + " around " + p);
            for (int z = p.getZ() - r; z <= p.getZ() + r; z++) {
                StringBuilder row = new StringBuilder("     ");
                for (int x = p.getX() - r; x <= p.getX() + r; x++) {
                    net.minecraft.block.state.IBlockState st = world.getBlockState(new BlockPos(x, y, z));
                    String n = st.getBlock().getRegistryName().getPath();
                    if (st.getMaterial() == net.minecraft.block.material.Material.AIR) n = ".";
                    if (x == p.getX() && z == p.getZ()) n = "[" + n + "]";
                    row.append(String.format("%-9s", n.length() > 8 ? n.substring(0, 8) : n));
                }
                log(row.toString());
            }
        }
    }

    private static void add(Totals t, int sites, int beds, int smith, int smithChests) {
        t.villages++;
        t.sites += sites;
        t.beds += beds;
        t.smithSites += smith;
        t.smithChests += smithChests;
    }

    private static boolean chestNear(WorldServer world, BlockPos p) {
        for (BlockPos q : BlockPos.getAllInBoxMutable(p.add(-4, -2, -4), p.add(4, 2, 4))) {
            if (world.getBlockState(q).getBlock() == Blocks.CHEST) {
                return true;
            }
        }
        return false;
    }

    private static void people(WorldServer world, String label, List<EntityVillager> adults, Totals t) {
        Map<String, Integer> careers = new TreeMap<String, Integer>();
        int employed = 0, nitwits = 0, unemployed = 0;
        for (EntityVillager v : adults) {
            if (v.isDead) {
                continue;
            }
            t.adults++;
            if (Employment.isNitwit(v)) {
                nitwits++;
            } else if (Employment.isUnemployed(v)) {
                unemployed++;
            } else {
                employed++;
                String name = career(v);
                careers.put(name, careers.containsKey(name) ? careers.get(name) + 1 : 1);
            }
        }
        t.employed += employed;
        t.nitwits += nitwits;
        t.unemployed += unemployed;
        log(label + ": adults=" + (employed + nitwits + unemployed) + " employed=" + employed + " nitwits=" + nitwits
                + " unemployed=" + unemployed + " careers=" + careers);
    }

    private static String career(EntityVillager v) {
        VillagerRegistry.VillagerProfession prof = v.getProfessionForge();
        int id = VillagerAccess.getCareerId(v);
        try {
            return prof.getCareer(id - 1).getName();
        } catch (RuntimeException e) {
            return prof.getRegistryName() + "#" + id;
        }
    }
}
