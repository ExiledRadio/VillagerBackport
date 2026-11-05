package com.exiledradio.villagerbackport.trader;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 1.14's {@code WanderingTraderSpawner}: now and then, a trader turns up near a player.
 *
 * <h2>The timing</h2>
 * From the 1.14.4 bytecode, matching what minecraft.wiki describes for current Java:
 * <ul>
 *   <li>Once a minute (1200 ticks) the world's countdown drops by 1200. It starts at 24000, one day.</li>
 *   <li>At zero it resets, and - if {@code doMobSpawning} and {@code doTraderSpawning} are on - an
 *       attempt is made with the current chance, which then rises by 25 for next time, to at most 75.
 *       The roll uses the chance <em>before</em> the rise: 25%, then 50%, then 75% from then on.</li>
 *   <li>An attempt picks a random player. With no player it counts as a success and the chance
 *       resets; that is 1.14's code, and keeps an empty server from saving up a 75% chance.</li>
 *   <li>With a player there is a further one in ten roll, so the real odds are 2.5%, 5% and 7.5%.
 *       A trader that appears resets the chance to 25.</li>
 * </ul>
 * The countdown and chance are kept per world (here, in the overworld's saved data) and survive a
 * restart, as 1.14 keeps them in {@code level.dat}.
 *
 * <h2>Where</h2>
 * The nearest bell within 48 blocks of the player if there is one - 1.14 looks for a meeting point,
 * and a bell is what makes one - and the player's own position otherwise. Then ten tries at a random
 * column within 48 blocks of that, standing on the surface where a mob could spawn, and never in the
 * void biome. The trader is sent to walk to that bell or player position, and told to stay within 16
 * blocks of it once there. Two trader llamas are put on leads within 4 blocks of it.
 *
 * <p>Bells are found among loaded tile entities, by block name, so this mod's bell and any other
 * mod's {@code bell} both count. A bell in an unloaded chunk is too far for 48 blocks anyway.
 *
 * <h2>One at a time</h2>
 * 1.14 remembers the last trader's id but does not stop a second one coming. With
 * {@code trader.oneAtATime} on, an attempt is skipped (and counts as a failure, so the chance still
 * climbs) while the last trader is loaded and alive, or while it has not been gone long enough to
 * have left - its clock only runs while it is loaded, so a trader in an unloaded chunk could still be
 * out there. Overworld only, as in 1.14.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class TraderSpawner {

    /** The game rule current Java uses for this; added to worlds that do not have it. */
    public static final String GAME_RULE = "doTraderSpawning";

    /** How often the countdown is updated, and by how much. */
    private static final int STEP = 1200;

    /** 1.14's home distance round the wander target. */
    private static final int HOME_DISTANCE = 16;

    /** How close to the trader its llamas are placed. */
    private static final int LLAMA_RADIUS = 4;

    private static int stepTimer = STEP;

    private TraderSpawner() {
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!TraderContent.enabled() || event.getWorld().isRemote) {
            return;
        }
        GameRules rules = event.getWorld().getGameRules();
        if (!rules.hasRule(GAME_RULE)) {
            rules.addGameRule(GAME_RULE, "true", GameRules.ValueType.BOOLEAN_VALUE);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)
                || event.world.provider.getDimension() != 0) {
            return;
        }
        if (!TraderContent.enabled() || !ModConfig.trader.naturalSpawning) {
            return;
        }
        if (--stepTimer > 0) {
            return;
        }
        stepTimer = STEP;
        tick((WorldServer) event.world);
    }

    /** One minute of 1.14's {@code WanderingTraderSpawner.tick}. */
    private static void tick(WorldServer world) {
        Data data = Data.get(world);
        data.delay -= STEP;
        data.markDirty();
        if (data.delay > 0) {
            return;
        }
        data.delay = ModConfig.trader.spawnDelay;

        GameRules rules = world.getGameRules();
        if (!rules.getBoolean("doMobSpawning") || (rules.hasRule(GAME_RULE) && !rules.getBoolean(GAME_RULE))) {
            return;
        }

        int chance = data.chance;
        data.chance = clampChance(data.chance + ModConfig.trader.spawnChanceStep);
        if (world.rand.nextInt(100) > chance) {
            return;
        }
        if (attempt(world)) {
            data.chance = clampChance(ModConfig.trader.minSpawnChance);
        }
    }

    private static int clampChance(int chance) {
        int min = ModConfig.trader.minSpawnChance;
        int max = Math.max(min, ModConfig.trader.maxSpawnChance);
        return Math.max(min, Math.min(max, chance));
    }

    /**
     * 1.14's {@code spawn}: true when the attempt is spent - a trader appeared, or there was nobody to
     * send one to.
     */
    private static boolean attempt(WorldServer world) {
        List<EntityPlayer> players = new ArrayList<EntityPlayer>();
        for (EntityPlayer player : world.playerEntities) {
            if (!player.isSpectator()) {
                players.add(player);
            }
        }
        if (players.isEmpty()) {
            return true;
        }
        if (world.rand.nextInt(10) != 0) {
            return false;
        }
        if (ModConfig.trader.oneAtATime && previousStillAround(world)) {
            return false;
        }
        EntityPlayer player = players.get(world.rand.nextInt(players.size()));
        return spawnNear(world, new BlockPos(player), true) != null;
    }

    /** @return true if the last natural trader may still be in the world */
    public static boolean previousStillAround(WorldServer world) {
        Data data = Data.get(world);
        if (data.lastTrader == null) {
            return false;
        }
        net.minecraft.entity.Entity loaded = world.getEntityFromUuid(data.lastTrader);
        if (loaded != null) {
            return loaded.isEntityAlive();
        }
        int lifetime = ModConfig.trader.despawnTicks;
        return lifetime <= 0 || world.getTotalWorldTime() - data.spawnedAt < lifetime;
    }

    /**
     * Sends a trader to a position: a bell near it if there is one, then a spot within the spawn
     * radius, then the llamas.
     *
     * @param natural true for the timer's own traders, which leave after {@code trader.despawnTicks}
     *                and are remembered for {@code trader.oneAtATime}; false for one a command asked
     *                for, which also leaves but is not remembered
     * @return the trader, or null if no spot in ten tries would do
     */
    @Nullable
    public static EntityWanderingTrader spawnNear(WorldServer world, BlockPos around, boolean natural) {
        int radius = ModConfig.trader.spawnRadius;
        BlockPos bell = nearestBell(world, around, radius);
        BlockPos target = bell != null ? bell : around;

        BlockPos spot = findSpawnSpot(world, target, radius, world.rand);
        if (spot == null || world.getBiome(spot) == Biomes.VOID) {
            return null;
        }

        EntityWanderingTrader trader = new EntityWanderingTrader(world);
        trader.setLocationAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                world.rand.nextFloat() * 360.0F, 0.0F);
        trader.onInitialSpawn(world.getDifficultyForLocation(spot), null);
        trader.setDespawnDelay(ModConfig.trader.despawnTicks);
        trader.setWanderTarget(target);
        trader.setHomePosAndDistance(target, HOME_DISTANCE);
        world.spawnEntity(trader);

        for (int i = 0; i < ModConfig.trader.llamas; i++) {
            spawnLlama(world, trader);
        }

        if (natural) {
            Data data = Data.get(world);
            data.lastTrader = trader.getUniqueID();
            data.spawnedAt = world.getTotalWorldTime();
            data.markDirty();
        }
        VillagerBackport.LOGGER.debug("A wandering trader arrived at {}, heading for {}{}.", spot, target,
                bell != null ? " (a bell)" : "");
        return trader;
    }

    private static void spawnLlama(WorldServer world, EntityWanderingTrader trader) {
        BlockPos spot = findSpawnSpot(world, new BlockPos(trader), LLAMA_RADIUS, world.rand);
        if (spot == null) {
            return;
        }
        EntityTraderLlama llama = new EntityTraderLlama(world);
        llama.setLocationAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                world.rand.nextFloat() * 360.0F, 0.0F);
        llama.onInitialSpawn(world.getDifficultyForLocation(spot), null);
        // Spawned with the trader, never as a foal.
        llama.setGrowingAge(0);
        world.spawnEntity(llama);
        llama.setLeashHolder(trader, true);
    }

    /**
     * 1.14's {@code findSpawnPositionNear}: ten tries at a random column within {@code radius}, on the
     * surface, where a ground mob could spawn.
     */
    @Nullable
    static BlockPos findSpawnSpot(World world, BlockPos around, int radius, Random random) {
        for (int i = 0; i < 10; i++) {
            int x = around.getX() + random.nextInt(radius * 2) - radius;
            int z = around.getZ() + random.nextInt(radius * 2) - radius;
            BlockPos column = new BlockPos(x, 0, z);
            if (!world.isBlockLoaded(column)) {
                continue;
            }
            BlockPos spot = new BlockPos(x, world.getHeight(x, z), z);
            if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, spot)) {
                return spot;
            }
        }
        return null;
    }

    /** The nearest loaded bell within {@code radius} of a position, or null. */
    @Nullable
    static BlockPos nearestBell(World world, BlockPos around, int radius) {
        BlockPos best = null;
        double bestDistance = (double) radius * radius;
        for (TileEntity tile : new ArrayList<TileEntity>(world.loadedTileEntityList)) {
            BlockPos pos = tile.getPos();
            double distance = pos.distanceSq(around);
            if (distance > bestDistance || tile.isInvalid()) {
                continue;
            }
            ResourceLocation name = world.getBlockState(pos).getBlock().getRegistryName();
            if (name != null && "bell".equals(name.getPath())) {
                best = pos;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Ticks until the next attempt, and the chance it will have. */
    public static int[] status(WorldServer world) {
        Data data = Data.get(world);
        return new int[] {data.delay, data.chance};
    }

    /** Sets the countdown, for testing. */
    public static void setDelay(WorldServer world, int ticks) {
        Data data = Data.get(world);
        data.delay = ticks;
        data.markDirty();
    }

    /** The countdown, the chance and the last trader, kept with the overworld. */
    public static final class Data extends WorldSavedData {

        private static final String NAME = VillagerBackport.MOD_ID + "_wandering_trader";

        int delay;
        int chance;
        @Nullable
        UUID lastTrader;
        long spawnedAt;

        public Data(String name) {
            super(name);
            this.delay = ModConfig.trader.spawnDelay;
            this.chance = ModConfig.trader.minSpawnChance;
        }

        static Data get(World world) {
            MapStorage storage = world.getMapStorage();
            Data data = (Data) storage.getOrLoadData(Data.class, NAME);
            if (data == null) {
                data = new Data(NAME);
                storage.setData(NAME, data);
            }
            return data;
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            this.delay = nbt.getInteger("SpawnDelay");
            this.chance = nbt.getInteger("SpawnChance");
            if (nbt.hasUniqueId("Last")) {
                this.lastTrader = nbt.getUniqueId("Last");
            }
            this.spawnedAt = nbt.getLong("SpawnedAt");
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            nbt.setInteger("SpawnDelay", this.delay);
            nbt.setInteger("SpawnChance", this.chance);
            if (this.lastTrader != null) {
                nbt.setUniqueId("Last", this.lastTrader);
            }
            nbt.setLong("SpawnedAt", this.spawnedAt);
            return nbt;
        }
    }
}
