package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.raid.RaidVillages;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Where cats come from: 1.14's {@code CatSpawner}, run on the overworld.
 *
 * <h2>The rule</h2>
 * Once a minute (1200 ticks), if animals and mob spawning are on, one random player is picked and a
 * spot is chosen 8 to 31 blocks away from them on each horizontal axis, at the player's own height.
 * If the ground there would take an animal:
 * <ul>
 *   <li><b>In a village</b> - one with more than four occupied homes within 48 blocks of the spot, and
 *       fewer than five cats within 48 blocks across and 8 up or down - a stray cat appears, of a
 *       random coat (black only on a full moon).</li>
 *   <li><b>In a witch hut</b> with no cat within 16 blocks across, a black cat appears, and stays.</li>
 * </ul>
 *
 * <h2>"Occupied homes"</h2>
 * 1.14 counts beds that a villager has claimed. This mod keeps those claims on the villagers
 * ({@link HomeSite}), so villagers near the spot are asked which beds are theirs. With this mod's homes
 * turned off ({@code homes.enabled}) no villager claims a bed, and 1.12.2's own door village is
 * asked instead: its door count stands in for its homes, which is what a door village's doors are.
 *
 * <p>Whether the spot is in a village at all is {@link RaidVillages#isVillage}'s answer - the same one
 * raids use.
 *
 * <h2>Witch huts</h2>
 * 1.12.2 files witch huts with desert and jungle temples as {@code Temple}; a temple in a swamp is a
 * witch hut. Structures from other mods are not found.
 */
public final class CatSpawner {

    private static final int INTERVAL = 1200;

    /** 1.14's limits. */
    private static final int HOMES_NEEDED = 4;
    private static final int VILLAGE_CAT_LIMIT = 5;
    private static final double HOME_RANGE = 48.0D;

    /** How far a villager may sleep from where it stands, so its bed is not missed. */
    private static final double CLAIM_REACH = 48.0D;

    private final Map<World, Integer> countdown = new WeakHashMap<World, Integer>();

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.provider.getDimension() != 0) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        if (!world.getMinecraftServer().getCanSpawnAnimals() || !world.getGameRules().getBoolean("doMobSpawning")) {
            return;
        }
        Integer left = this.countdown.get(world);
        int next = (left == null ? INTERVAL : left) - 1;
        if (next > 0) {
            this.countdown.put(world, next);
            return;
        }
        this.countdown.put(world, INTERVAL);
        tick(world);
    }

    /** One attempt, as 1.14 makes every minute. @return the cat, or null if none appeared */
    @Nullable
    static EntityCat tick(WorldServer world) {
        EntityPlayer player = randomPlayer(world);
        if (player == null) {
            return null;
        }
        int dx = (8 + world.rand.nextInt(24)) * (world.rand.nextBoolean() ? -1 : 1);
        int dz = (8 + world.rand.nextInt(24)) * (world.rand.nextBoolean() ? -1 : 1);
        return trySpawnAt(world, new BlockPos(player).add(dx, 0, dz));
    }

    /** The second half of an attempt, at a chosen spot. Package-visible for the headless test. */
    @Nullable
    static EntityCat trySpawnAt(WorldServer world, BlockPos pos) {
        if (!world.isAreaLoaded(pos.add(-10, -10, -10), pos.add(10, 10, 10))) {
            return null;
        }
        if (!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, pos)) {
            return null;
        }
        if (RaidVillages.isVillage(world, pos)) {
            return ModConfig.cats.spawnInVillages ? spawnInVillage(world, pos) : null;
        }
        if (ModConfig.cats.spawnAtWitchHuts && isWitchHut(world, pos)) {
            return spawnInHut(world, pos);
        }
        return null;
    }

    @Nullable
    private static EntityCat spawnInVillage(WorldServer world, BlockPos pos) {
        if (occupiedHomes(world, pos) <= HOMES_NEEDED) {
            return null;
        }
        List<EntityCat> cats = world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(pos).grow(48.0D, 8.0D, 48.0D));
        return cats.size() < VILLAGE_CAT_LIMIT ? spawnCat(world, pos) : null;
    }

    @Nullable
    private static EntityCat spawnInHut(WorldServer world, BlockPos pos) {
        List<EntityCat> cats = world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(pos).grow(16.0D, 8.0D, 16.0D));
        // onInitialSpawn sees the hut and makes it the black, persistent witch's cat.
        return cats.isEmpty() ? spawnCat(world, pos) : null;
    }

    private static EntityCat spawnCat(WorldServer world, BlockPos pos) {
        EntityCat cat = new EntityCat(world);
        cat.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        cat.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        world.spawnEntity(cat);
        return cat;
    }

    /** Beds claimed by villagers within 48 blocks of the spot - 1.14's occupied homes. */
    static int occupiedHomes(World world, BlockPos pos) {
        if (!ModConfig.homes.enabled) {
            Village village = world.getVillageCollection().getNearestVillage(pos, 32);
            return village == null ? 0 : village.getNumVillageDoors();
        }
        Set<BlockPos> beds = new HashSet<BlockPos>();
        double range = HOME_RANGE * HOME_RANGE;
        for (EntityVillager villager : world.getEntitiesWithinAABB(EntityVillager.class,
                new AxisAlignedBB(pos).grow(HOME_RANGE + CLAIM_REACH))) {
            BlockPos bed = HomeSite.get(villager);
            if (!villager.isDead && bed != null && bed.distanceSq(pos) <= range) {
                beds.add(bed);
            }
        }
        return beds.size();
    }

    /** @return true if the position is inside a witch hut: a vanilla temple, in a swamp */
    static boolean isWitchHut(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) {
            return false;
        }
        Biome biome = world.getBiome(pos);
        if (!BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)) {
            return false;
        }
        try {
            return ((WorldServer) world).getChunkProvider().isInsideStructure(world, "Temple", pos);
        } catch (RuntimeException e) {
            // A generator from another mod that does not know the name.
            return false;
        }
    }

    @Nullable
    private static EntityPlayer randomPlayer(WorldServer world) {
        List<EntityPlayer> players = new ArrayList<EntityPlayer>();
        for (EntityPlayer player : world.playerEntities) {
            if (player.isEntityAlive() && !player.isSpectator()) {
                players.add(player);
            }
        }
        return players.isEmpty() ? null : players.get(world.rand.nextInt(players.size()));
    }
}
