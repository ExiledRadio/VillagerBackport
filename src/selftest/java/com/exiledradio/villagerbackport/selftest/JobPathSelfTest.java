package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.job.JobSite;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * An unemployed villager outside a small house walks in through the door and takes the workstation
 * inside, within the minute 1.14 gives a villager to reach its job site.
 *
 * <p>Each case is a house of planks on a stone platform in the sky, far from everything else, with one
 * villager. The villagers are ticked here by hand, a server tick at a time with the world clock moving,
 * so the run takes no real time and every case sees the same thing on every run.
 * <ul>
 *   <li>{@code door}: the workstation against the back wall, the villager in front of the door.</li>
 *   <li>{@code corner}: the workstation in a back corner.</li>
 *   <li>{@code behind}: the villager behind the house, so the nearest way in is round to the door.</li>
 *   <li>{@code lectern}: a lectern (not a full cube) in a front corner, beside the door.</li>
 *   <li>{@code lroom}: an L-shaped room, the workstation round the bend, out of sight of the door.</li>
 *   <li>{@code homeedge}: the house just outside the villager's 1.12 village (its home radius).</li>
 * </ul>
 */
public final class JobPathSelfTest implements SelfTest {

    /** 1.14's {@code StayNearPointTask} limit: a villager that cannot get to a site in this long gives up. */
    private static final int LIMIT = 1200;

    /** Every case here is a short walk; well under 1.14's minute, which pacing used to run out. */
    private static final int EXPECTED = 600;

    private static final int Y = 200;
    private static final int X0 = 9000;
    private static final int Z0 = 9000;
    /** Far enough apart that no villager can see another case's workstation (search radius 48). */
    private static final int SPACING = 160;

    private WorldServer world;

    @Override
    public String name() {
        return "jobpath";
    }

    private static final class Case {
        final String name;
        final BlockPos site;
        final EntityVillager villager;
        int employedAt = -1;
        int claimedAt = -1;
        BlockPos home;

        Case(String name, BlockPos site, EntityVillager villager) {
            this.name = name;
            this.site = site;
            this.villager = villager;
        }
    }

    @Override
    public void run(Check c) throws Exception {
        this.world = DimensionManager.getWorld(0);
        Block fletching = block("villagerbackport:fletching_table");
        Block lectern = block("villagerbackport:lectern");
        if (!c.ok(fletching != null && lectern != null, "workstation blocks registered")) {
            return;
        }

        long time = this.world.getWorldTime();
        this.world.setWorldTime(time - time % 24000L + 3000L);

        List<Case> cases = new ArrayList<Case>();
        int i = 0;

        // A 7x7 house, door in the middle of the south wall (z + 6).
        BlockPos o = origin(i++);
        house(o, 7, 7);
        door(o.add(3, 0, 6));
        cases.add(new Case("door", put(o.add(3, 0, 1), fletching), villager(o.add(3, 0, 13))));

        o = origin(i++);
        house(o, 7, 7);
        door(o.add(3, 0, 6));
        cases.add(new Case("corner", put(o.add(5, 0, 1), fletching), villager(o.add(3, 0, 13))));

        o = origin(i++);
        house(o, 7, 7);
        door(o.add(3, 0, 6));
        cases.add(new Case("behind", put(o.add(3, 0, 1), fletching), villager(o.add(3, 0, -3))));

        o = origin(i++);
        house(o, 7, 7);
        door(o.add(3, 0, 6));
        cases.add(new Case("lectern", put(o.add(5, 0, 5), lectern), villager(o.add(3, 0, 13))));

        // An L: a corridor north from the door (x 1..3), then a leg east along the back (z 1..3).
        o = origin(i++);
        house(o, 9, 9);
        fill(o.add(1, 0, 1), o.add(7, 2, 7), Blocks.PLANKS.getDefaultState());
        fill(o.add(1, 0, 1), o.add(3, 2, 7), Blocks.AIR.getDefaultState());
        fill(o.add(4, 0, 1), o.add(7, 2, 3), Blocks.AIR.getDefaultState());
        door(o.add(2, 0, 8));
        cases.add(new Case("lroom", put(o.add(7, 0, 1), fletching), villager(o.add(2, 0, 14))));

        // The house just outside the 1.12 village the villager belongs to: 1.12.2 sets a villager's home
        // to its door-made village's centre and radius, and its "stay near home" goal pulls it back in.
        o = origin(i++);
        house(o, 7, 7);
        door(o.add(3, 0, 6));
        Case edge = new Case("homeedge", put(o.add(3, 0, 1), fletching), villager(o.add(3, 0, 30)));
        edge.home = o.add(3, 0, 60);
        cases.add(edge);

        WorkstationIndex.invalidateAll();

        for (int tick = 1; tick <= LIMIT; tick++) {
            this.world.getWorldInfo().setWorldTotalTime(this.world.getTotalWorldTime() + 1L);
            for (Case k : cases) {
                if (k.employedAt >= 0 || k.villager.isDead) {
                    continue;
                }
                if (k.home != null) {
                    k.villager.setHomePosAndDistance(k.home, 32);
                }
                this.world.updateEntity(k.villager);
                if (k.claimedAt < 0 && k.site.equals(JobSite.get(k.villager))) {
                    k.claimedAt = tick;
                }
                if (!Employment.isUnemployed(k.villager)) {
                    k.employedAt = tick;
                }
                if (tick % 100 == 0) {
                    Path path = k.villager.getNavigator().getPath();
                    VillagerBackport.LOGGER.info("SELFTEST jobpath {} t={} at {} claim={} unemployed={} path={}",
                            k.name, tick, String.format("%.2f %.2f %.2f", k.villager.posX - k.site.getX(),
                                    k.villager.posY - k.site.getY(), k.villager.posZ - k.site.getZ()),
                            JobSite.get(k.villager), Employment.isUnemployed(k.villager),
                            path == null ? "none" : (path.getCurrentPathIndex() + "/" + path.getCurrentPathLength()
                                    + " end " + path.getFinalPathPoint()));
                }
            }
        }

        for (Case k : cases) {
            VillagerBackport.LOGGER.info("SELFTEST jobpath {}: claimed at {}, employed at {}", k.name, k.claimedAt, k.employedAt);
            c.ok(k.employedAt >= 0 && k.employedAt <= EXPECTED, k.name + ": the villager took the workstation at " + k.site + " within " + EXPECTED
                    + " ticks (claimed at " + k.claimedAt + ", ended at " + new BlockPos(k.villager) + ")");
            k.villager.setDead();
        }
    }

    private static BlockPos origin(int i) {
        return new BlockPos(X0 + i * SPACING, Y, Z0);
    }

    private static Block block(String id) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        return b == Blocks.AIR ? null : b;
    }

    /** A stone platform, and a plank house of the given outer size with a plank roof, 3 blocks of room inside. */
    private void house(BlockPos o, int sizeX, int sizeZ) {
        for (int x = -48; x < sizeX + 64; x += 16) {
            for (int z = -48; z < sizeZ + 64; z += 16) {
                this.world.getChunk(o.add(x, 0, z));
            }
        }
        fill(o.add(-14, -1, -14), o.add(sizeX + 14, -1, sizeZ + 40), Blocks.STONE.getDefaultState());
        fill(o.add(-14, 0, -14), o.add(sizeX + 14, 5, sizeZ + 40), Blocks.AIR.getDefaultState());
        fill(o, o.add(sizeX - 1, 2, sizeZ - 1), Blocks.PLANKS.getDefaultState());
        fill(o.add(1, 0, 1), o.add(sizeX - 2, 2, sizeZ - 2), Blocks.AIR.getDefaultState());
        fill(o.add(0, 3, 0), o.add(sizeX - 1, 3, sizeZ - 1), Blocks.PLANKS.getDefaultState());
    }

    private void door(BlockPos lower) {
        IBlockState door = Blocks.OAK_DOOR.getDefaultState().withProperty(BlockDoor.FACING, EnumFacing.NORTH);
        this.world.setBlockState(lower, door.withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.LOWER), 2);
        this.world.setBlockState(lower.up(), door.withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER), 2);
    }

    private BlockPos put(BlockPos pos, Block block) {
        IBlockState state = block.getDefaultState();
        if (state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            state = state.withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH);
        }
        this.world.setBlockState(pos, state, 3);
        return pos;
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

    private void fill(BlockPos a, BlockPos b, IBlockState state) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(a, b)) {
            this.world.setBlockState(pos, state, 2);
        }
    }
}
