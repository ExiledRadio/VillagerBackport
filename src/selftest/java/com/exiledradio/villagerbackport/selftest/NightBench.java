package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.home.HomeSite;

import net.minecraft.block.BlockDoor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A measurement, not a pass/fail test: how villagers get to bed at night.
 *
 * <p>{@code -Pselftest=nightfind}: finds the nearest village, makes it the world spawn (so its chunks stay
 * loaded and ticking) and stops. {@code -Pselftest=nightwatch}: watches {@value #NIGHTS} nights there and
 * logs, per night, how long each villager with a bed took to fall asleep after dusk and how many seconds
 * villagers spent stuck, wedged at a door, or wandering away from their bed. Mobs behave as if a player
 * were near (no idle freeze), without a fake player in the world.
 */
public final class NightBench {

    private static final int RADIUS = 96;
    private static final int NIGHTS = 3;
    private static final long DUSK = 12000L;
    /** Each night is watched this long from dusk; vanilla's villagers are in bed well before. */
    private static final int WATCH = 5000;
    private static final int SAMPLE = 20;

    private final String mode;
    private int ticks;
    private int night;
    private int nightTick = -1;

    private final Map<Integer, BlockPos> lastPos = new HashMap<Integer, BlockPos>();
    private final Map<Integer, double[]> lastExact = new HashMap<Integer, double[]>();
    private final Map<Integer, Integer> asleepAt = new HashMap<Integer, Integer>();
    private final Map<Integer, Double> lastBedDistance = new HashMap<Integer, Double>();
    private int stuck;
    private int atDoor;
    private int wandering;
    private int samples;
    private int homed;

    private static Field idleTime;
    private static Field updateEntityTick;

    private NightBench(String mode) {
        this.mode = mode;
    }

    public static void start(String mode) {
        MinecraftForge.EVENT_BUS.register(new NightBench(mode));
    }

    private static void log(String line) {
        VillagerBackport.LOGGER.info("NIGHTBENCH " + line);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        this.ticks++;

        if ("nightfind".equals(this.mode)) {
            if (this.ticks == 20) {
                BlockPos village = world.findNearestStructure("Village", world.getSpawnPoint(), false);
                log("village " + village);
                if (village != null) {
                    world.setSpawnPoint(world.getTopSolidOrLiquidBlock(village));
                }
                server.initiateShutdown();
            }
            return;
        }

        keepAwake(world);
        if (this.ticks == 20) {
            // A day first, so villagers claim beds and jobs as they would by the first evening.
            world.setWorldTime(1000L);
        }
        if (this.ticks == 20 + 4000) {
            beginNight(world);
        }
        if (this.nightTick >= 0) {
            this.nightTick++;
            if (this.nightTick % SAMPLE == 0) {
                sample(world);
            }
            if (this.nightTick == 1000 || this.nightTick == 3000) {
                diagnose(world);
            }
            if (this.nightTick >= WATCH) {
                endNight(world);
                if (this.night >= NIGHTS) {
                    log("DONE");
                    server.initiateShutdown();
                    return;
                }
                // Morning, then a short day, then the next dusk.
                world.setWorldTime(world.getWorldTime() / 24000L * 24000L + 24000L + 1000L);
                this.nightTick = -2000;
            }
        } else if (this.nightTick < -1) {
            this.nightTick++;
            if (this.nightTick == -1) {
                beginNight(world);
            }
        }
    }

    private void beginNight(WorldServer world) {
        this.night++;
        world.setWorldTime(world.getWorldTime() / 24000L * 24000L + DUSK);
        this.nightTick = 0;
        this.asleepAt.clear();
        this.lastPos.clear();
        this.lastExact.clear();
        this.lastBedDistance.clear();
        this.stuck = 0;
        this.atDoor = 0;
        this.wandering = 0;
        this.samples = 0;
        this.homed = 0;
    }

    private void sample(WorldServer world) {
        BlockPos centre = world.getSpawnPoint();
        List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class,
                new AxisAlignedBB(centre).grow(RADIUS, 64, RADIUS));
        int homedNow = 0;
        for (EntityVillager v : villagers) {
            BlockPos bed = HomeSite.validated(v);
            if (bed == null) {
                continue;
            }
            homedNow++;
            int id = v.getEntityId();
            if (HomeSite.isSleeping(v)) {
                if (!this.asleepAt.containsKey(id)) {
                    this.asleepAt.put(id, this.nightTick);
                }
                continue;
            }
            this.samples++;
            double[] was = this.lastExact.get(id);
            if (was != null) {
                double dx = v.posX - was[0];
                double dz = v.posZ - was[1];
                if (dx * dx + dz * dz < 0.05 * 0.05 && v.getDistanceSqToCenter(bed) > 4.0D) {
                    this.stuck++;
                }
            }
            if (nearDoor(world, new BlockPos(v))) {
                this.atDoor++;
            }
            double toBed = Math.sqrt(v.getDistanceSqToCenter(bed));
            Double before = this.lastBedDistance.get(id);
            if (before != null && toBed > before + 0.5D) {
                this.wandering++;
            }
            this.lastBedDistance.put(id, toBed);
            this.lastExact.put(id, new double[]{v.posX, v.posZ});
        }
        this.homed = Math.max(this.homed, homedNow);
    }

    private static boolean nearDoor(WorldServer world, BlockPos at) {
        for (BlockPos p : BlockPos.getAllInBoxMutable(at.add(-1, -1, -1), at.add(1, 1, 1))) {
            if (world.getBlockState(p).getBlock() instanceof BlockDoor) {
                return true;
            }
        }
        return false;
    }

    private void endNight(WorldServer world) {
        List<Integer> times = new ArrayList<Integer>(this.asleepAt.values());
        Collections.sort(times);
        int never = this.homed - times.size();
        int median = times.isEmpty() ? -1 : times.get(times.size() / 2);
        int max = times.isEmpty() ? -1 : times.get(times.size() - 1);
        int by500 = 0;
        int by1500 = 0;
        for (int t : times) {
            if (t <= 500) {
                by500++;
            }
            if (t <= 1500) {
                by1500++;
            }
        }
        // Each sample is a second; these totals are villager-seconds while awake with a bed.
        log(String.format("night=%d homed=%d asleep=%d never=%d medianTicks=%d maxTicks=%d asleepBy500=%d asleepBy1500=%d "
                        + "awakeSeconds=%d stuckSeconds=%d atDoorSeconds=%d awayFromBedSeconds=%d",
                this.night, this.homed, times.size(), never, median, max, by500, by1500,
                this.samples, this.stuck, this.atDoor, this.wandering));
    }

    /** As if a player were near: 1.12.2 stops idle mobs' wandering and the whole entity tick without one. */
    private static void keepAwake(WorldServer world) {
        try {
            if (updateEntityTick == null) {
                updateEntityTick = ReflectionHelper.findField(WorldServer.class, "updateEntityTick", "field_80004_Q");
                idleTime = ReflectionHelper.findField(EntityLivingBase.class, "idleTime", "field_70708_bq");
            }
            updateEntityTick.setInt(world, 0);
            BlockPos centre = world.getSpawnPoint();
            for (EntityVillager v : world.getEntitiesWithinAABB(EntityVillager.class,
                    new AxisAlignedBB(centre).grow(RADIUS + 32, 64, RADIUS + 32))) {
                idleTime.setInt(v, 0);
            }
        } catch (Exception e) {
            log("could not keep the world awake: " + e);
        }
    }

    /** Prints, for every villager with a bed that is not asleep, what it is doing and why it is not getting home. */
    private void diagnose(WorldServer world) {
        BlockPos centre = world.getSpawnPoint();
        for (EntityVillager v : world.getEntitiesWithinAABB(EntityVillager.class, new AxisAlignedBB(centre).grow(RADIUS, 64, RADIUS))) {
            BlockPos bed = HomeSite.validated(v);
            if (bed == null || HomeSite.isSleeping(v)) {
                continue;
            }
            StringBuilder running = new StringBuilder();
            try {
                Field f = ReflectionHelper.findField(net.minecraft.entity.ai.EntityAITasks.class, "executingTaskEntries", "field_75780_b");
                for (Object entry : (java.util.Set<?>) f.get(v.tasks)) {
                    Field a = ReflectionHelper.findField(entry.getClass(), "action", "field_75733_a");
                    running.append(a.get(entry).getClass().getSimpleName()).append(' ');
                }
            } catch (Exception e) {
                running.append(e);
            }
            net.minecraft.pathfinding.Path path = com.exiledradio.villagerbackport.job.WorkPathing.pathTo(v, bed);
            net.minecraft.pathfinding.PathPoint end = path == null ? null : path.getFinalPathPoint();
            BlockPos at = new BlockPos(v);
            StringBuilder around = new StringBuilder();
            for (int dy = 0; dy <= 1; dy++) {
                around.append(" y+").append(dy).append(':');
                for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.Plane.HORIZONTAL) {
                    around.append(side.getName().charAt(0)).append('=')
                            .append(world.getBlockState(at.up(dy).offset(side)).getBlock().getRegistryName().getPath()).append(' ');
                }
            }
            log(String.format("DIAG night=%d t=%d #%d at %s (%.2f,%.2f,%.2f) bed=%s dist=%.1f onGround=%s inWater=%s child=%s "
                            + "navPath=%s pathEnd=%s pathLen=%d below=%s running=[%s]%s",
                    this.night, this.nightTick, v.getEntityId(), at, v.posX, v.posY, v.posZ, bed, Math.sqrt(v.getDistanceSqToCenter(bed)),
                    v.onGround, v.isInWater(), v.isChild(), !v.getNavigator().noPath(),
                    end == null ? "none" : end.x + "," + end.y + "," + end.z, path == null ? 0 : path.getCurrentPathLength(),
                    world.getBlockState(at.down()).getBlock().getRegistryName().getPath(), running, around));
        }
    }
}
