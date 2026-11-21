package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.job.JobSite;
import com.exiledradio.villagerbackport.job.WorkPathing;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Job sites and employment: {@code -Pselftest=villagejobs}.
 * <ul>
 *   <li>{@code lapse}: an unemployed villager that cannot move, holding a claim on a workstation it has a
 *   path to, lets the claim go within about a minute - the wiki's "if the villager cannot reach the block
 *   within 60 seconds". Before 2.7.2 a path that arrived kept the claim for good.</li>
 *   <li>{@code path}: the search stops at the workstation (within a block of it) rather than at the
 *   nearest point outside a wall, for a workstation inside a house whose door faces away.</li>
 *   <li>{@code village}: the nearest generated village has workstations, beds and adults, and after
 *   {@value #VILLAGE_TICKS} ticks of daytime most villagers who could have a job have one, and no
 *   unemployed villager held one claim for more than a minute.</li>
 * </ul>
 * Villagers are ticked by hand, so the run takes no real time.
 */
public final class VillageJobsSelfTest implements SelfTest {

    private static final int Y = 200;
    private static final int X0 = 12000;
    private static final int Z0 = 12000;

    private static final int VILLAGE_RADIUS = 80;
    private static final int VILLAGE_TICKS = 6000;

    /** A minute, plus one claim interval (60 ticks) for the check to come round. */
    private static final int HOLD_LIMIT = 1200 + 60 + 20;

    private WorldServer world;

    @Override
    public String name() {
        return "villagejobs";
    }

    @Override
    public void run(Check c) throws Exception {
        this.world = DimensionManager.getWorld(0);
        long time = this.world.getWorldTime();
        this.world.setWorldTime(time - time % 24000L + 2000L);
        lapse(c);
        path(c);
        village(c);
    }

    private void lapse(Check c) {
        Block lectern = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("villagerbackport:lectern"));
        BlockPos o = new BlockPos(X0, Y, Z0);
        platform(o);
        BlockPos site = o.add(4, 0, 0);
        this.world.setBlockState(site, lectern.getDefaultState(), 3);
        WorkstationIndex.invalidateAll();

        EntityVillager v = villager(o);
        // Unable to move: no speed, so it never gets anywhere, but it still searches, claims and paths.
        v.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        int claimedAt = -1;
        int releasedAt = -1;
        for (int t = 1; t <= 2000 && releasedAt < 0; t++) {
            tickClock();
            this.world.updateEntity(v);
            if (claimedAt < 0 && site.equals(JobSite.get(v))) {
                claimedAt = t;
                if (c.ok(WorkPathing.arrives(WorkPathing.pathTo(v, site), site), "lapse: a path reaches the lectern")) {
                    VillagerBackport.LOGGER.info("SELFTEST villagejobs lapse: claimed at {}", t);
                }
            }
            if (claimedAt >= 0 && !site.equals(JobSite.get(v))) {
                releasedAt = t;
            }
        }
        c.ok(claimedAt >= 0, "lapse: the villager claimed the lectern 4 blocks away");
        c.ok(Employment.isUnemployed(v), "lapse: the villager never reached the lectern, so it is still unemployed");
        c.ok(releasedAt > 0 && releasedAt - claimedAt <= HOLD_LIMIT,
                "lapse: the claim was let go within a minute (claimed " + claimedAt + ", released " + releasedAt + ")");
        v.setDead();
    }

    private void path(Check c) {
        Block lectern = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("villagerbackport:lectern"));
        BlockPos o = new BlockPos(X0 + 200, Y, Z0);
        platform(o);
        // A 7x7 stone house, door on the south side; the lectern against the north wall inside, the villager
        // twelve blocks north of the house, so the straight line ends at the back wall.
        fill(o, o.add(6, 2, 6), Blocks.STONE.getDefaultState());
        fill(o.add(1, 0, 1), o.add(5, 2, 5), Blocks.AIR.getDefaultState());
        fill(o.add(0, 3, 0), o.add(6, 3, 6), Blocks.STONE.getDefaultState());
        fill(o.add(3, 0, 6), o.add(3, 1, 6), Blocks.AIR.getDefaultState());
        BlockPos site = o.add(3, 0, 1);
        this.world.setBlockState(site, lectern.getDefaultState(), 3);
        EntityVillager v = villager(o.add(3, 0, -12));
        v.onGround = true;
        c.ok(WorkPathing.arrives(WorkPathing.pathTo(v, site), site),
                "path: the search goes round the house and in at the door to the lectern");
        v.setDead();
    }

    private void village(Check c) {
        BlockPos centre = this.world.findNearestStructure("Village", this.world.getSpawnPoint(), false);
        if (!c.ok(centre != null, "village: a village was found")) {
            return;
        }
        int r = (VILLAGE_RADIUS >> 4) + 1;
        for (int cx = -r; cx <= r; cx++) {
            for (int cz = -r; cz <= r; cz++) {
                this.world.getChunk((centre.getX() >> 4) + cx, (centre.getZ() >> 4) + cz);
            }
        }
        WorkstationIndex.invalidateAll();
        int sites = 0;
        int beds = 0;
        for (int cx = (centre.getX() - VILLAGE_RADIUS) >> 4; cx <= (centre.getX() + VILLAGE_RADIUS) >> 4; cx++) {
            for (int cz = (centre.getZ() - VILLAGE_RADIUS) >> 4; cz <= (centre.getZ() + VILLAGE_RADIUS) >> 4; cz++) {
                sites += WorkstationIndex.workstationsIn(this.world, cx, cz).size();
                beds += WorkstationIndex.bedsIn(this.world, cx, cz).size();
            }
        }
        List<EntityVillager> adults = new ArrayList<EntityVillager>();
        for (EntityVillager v : this.world.getEntitiesWithinAABB(EntityVillager.class,
                new AxisAlignedBB(centre).grow(VILLAGE_RADIUS, 80, VILLAGE_RADIUS))) {
            if (!v.isChild()) {
                adults.add(v);
            }
        }
        VillagerBackport.LOGGER.info("SELFTEST villagejobs village {}: {} workstations, {} beds, {} adults",
                centre, sites, beds, adults.size());
        c.ok(sites > 0, "village: the village has workstations (" + sites + ")");
        c.ok(beds > 0, "village: the village has beds (" + beds + ")");
        c.ok(!adults.isEmpty(), "village: the village has adult villagers (" + adults.size() + ")");

        Map<EntityVillager, BlockPos> claim = new HashMap<EntityVillager, BlockPos>();
        Map<EntityVillager, Integer> since = new HashMap<EntityVillager, Integer>();
        int longest = 0;
        for (int t = 1; t <= VILLAGE_TICKS; t++) {
            tickClock();
            this.world.setWorldTime(this.world.getWorldTime() - this.world.getWorldTime() % 24000L + 1000L + (t % 10000));
            for (EntityVillager v : adults) {
                if (v.isDead) {
                    continue;
                }
                this.world.updateEntity(v);
                BlockPos now = Employment.isUnemployed(v) ? JobSite.get(v) : null;
                if (now == null || !now.equals(claim.get(v))) {
                    claim.put(v, now);
                    since.put(v, t);
                } else {
                    longest = Math.max(longest, t - since.get(v));
                }
            }
        }
        int employable = 0;
        int employed = 0;
        for (EntityVillager v : adults) {
            if (v.isDead || Employment.isNitwit(v)) {
                continue;
            }
            employable++;
            if (!Employment.isUnemployed(v)) {
                employed++;
            }
        }
        int could = Math.min(employable, sites);
        VillagerBackport.LOGGER.info("SELFTEST villagejobs village: {} of {} employable adults employed ({} workstations), longest unemployed claim {} ticks",
                employed, employable, sites, longest);
        c.ok(employed * 10 >= could * 7, "village: at least 70% of the villagers who could have a job have one ("
                + employed + " of min(" + employable + " villagers, " + sites + " workstations))");
        c.ok(longest <= HOLD_LIMIT, "village: no unemployed villager held one claim for more than a minute (" + longest + " ticks)");
    }

    private void tickClock() {
        this.world.getWorldInfo().setWorldTotalTime(this.world.getTotalWorldTime() + 1L);
        this.world.setWorldTime(this.world.getWorldTime() + 1L);
    }

    private void platform(BlockPos o) {
        for (int x = -48; x <= 64; x += 16) {
            for (int z = -48; z <= 64; z += 16) {
                this.world.getChunk(o.add(x, 0, z));
            }
        }
        fill(o.add(-16, -1, -16), o.add(16, -1, 16), Blocks.STONE.getDefaultState());
        fill(o.add(-16, 0, -16), o.add(16, 5, 16), Blocks.AIR.getDefaultState());
    }

    private EntityVillager villager(BlockPos at) {
        EntityVillager villager = new EntityVillager(this.world, 0);
        villager.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        villager.enablePersistence();
        this.world.spawnEntity(villager);
        if (!Employment.isUnemployed(villager)) {
            Employment.unassign(villager);
        }
        return villager;
    }

    private void fill(BlockPos a, BlockPos b, net.minecraft.block.state.IBlockState state) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(a, b)) {
            this.world.setBlockState(pos, state, 2);
        }
    }
}
