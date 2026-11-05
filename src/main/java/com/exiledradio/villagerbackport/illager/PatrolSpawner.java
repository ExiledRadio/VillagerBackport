package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.raid.RaidVillages;
import com.exiledradio.villagerbackport.raid.Raids;

import net.minecraft.block.BlockLeaves;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;

/**
 * 1.14's {@code PatrolSpawner}: bands of pillagers out on the land, led by a captain carrying the
 * ominous banner. Killing that captain away from a raid is how a player is meant to come by Bad Omen.
 *
 * <p>1.14's numbers: a check every ten to eleven minutes, from the fifth in-game day, by day, one
 * chance in five; 24 to 48 blocks from a random player who is not in a village; never on mushroom
 * islands; one pillager more for each step of local difficulty, scattered a few blocks about the
 * captain. Overworld only, and only with this mod's raids.
 */
public final class PatrolSpawner {

    private int cooldown;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)
                || event.world.provider.getDimension() != 0) {
            return;
        }
        if (!ModConfig.raids.patrols || !Raids.usingOwnRaids()) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        if (world.getDifficulty() == net.minecraft.world.EnumDifficulty.PEACEFUL
                || !world.getGameRules().getBoolean("doMobSpawning")) {
            return;
        }
        tick(world);
    }

    private void tick(WorldServer world) {
        Random random = world.rand;
        if (--this.cooldown > 0) {
            return;
        }
        this.cooldown += 12000 + random.nextInt(1200);

        if (world.getWorldTime() / 24000L < 5L || !world.isDaytime() || random.nextInt(5) != 0) {
            return;
        }

        List<EntityPlayer> players = world.playerEntities;
        if (players.isEmpty()) {
            return;
        }
        EntityPlayer player = players.get(random.nextInt(players.size()));
        if (player.isSpectator() || RaidVillages.isVillage(world, new BlockPos(player))) {
            return;
        }

        spawnNear(world, new BlockPos(player), random);
    }

    /**
     * Sends a patrol out 24 to 48 blocks from a position, as 1.14 does round a player.
     *
     * @return how many pillagers it had, 0 if the spot chosen would not do
     */
    public static int spawnNear(WorldServer world, BlockPos around, Random random) {
        int dx = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
        int dz = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(around.add(dx, 0, dz));
        if (!world.isAreaLoaded(pos, 10)) {
            return 0;
        }

        Biome biome = world.getBiome(pos);
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.MUSHROOM)) {
            return 0;
        }

        int spawned = 0;
        int members = (int) Math.ceil(world.getDifficultyForLocation(pos).getAdditionalDifficulty()) + 1;
        for (int i = 0; i < members; i++) {
            pos.setY(groundNoLeaves(world, pos));
            if (i == 0) {
                if (!spawn(world, pos, random, true)) {
                    break;
                }
                spawned++;
            } else if (spawn(world, pos, random, false)) {
                spawned++;
            }
            pos.setPos(pos.getX() + random.nextInt(5) - random.nextInt(5), pos.getY(),
                    pos.getZ() + random.nextInt(5) - random.nextInt(5));
        }
        return spawned;
    }

    /** 1.14's {@code MOTION_BLOCKING_NO_LEAVES} height: the top block, looking through leaves. */
    private static int groundNoLeaves(World world, BlockPos pos) {
        BlockPos top = world.getHeight(pos);
        while (top.getY() > 0) {
            BlockPos below = top.down();
            if (!(world.getBlockState(below).getBlock() instanceof BlockLeaves) && !world.isAirBlock(below)) {
                break;
            }
            top = below;
        }
        return top.getY();
    }

    private static boolean spawn(World world, BlockPos pos, Random random, boolean leader) {
        EntityPillager pillager = new EntityPillager(world);
        pillager.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (!pillager.getCanSpawnHere() || !pillager.isNotColliding()) {
            return false;
        }
        pillager.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        if (leader) {
            pillager.becomePatrolLeader();
        } else {
            pillager.joinPatrol();
        }
        world.spawnEntity(pillager);
        return true;
    }
}
