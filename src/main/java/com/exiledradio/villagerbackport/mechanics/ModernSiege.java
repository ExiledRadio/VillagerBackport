package com.exiledradio.villagerbackport.mechanics;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.raid.RaidVillages;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillageSiege;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * 1.14's zombie siege ({@code VillageSiege}, unchanged through today's game), in place of 1.12.2's in the overworld.
 *
 * <p>1.12.2's siege needs a door village of at least 10 doors and 20 villagers, and spawns inside its radius. 1.14's
 * comes for any player standing in a village ({@code isVillage}: within a section of a claimed bed, workstation or
 * bell; here {@link RaidVillages#isVillage}, which also counts 1.12.2's door villages), outside mushroom fields: at
 * midnight one night in ten, it picks a spot 32 blocks from the player and spawns 20 zombies there, one every 3
 * ticks, each on the surface within 8 blocks of the spot, in the village, where a zombie could spawn.
 *
 * <p>Swapped in for {@code WorldServer.villageSiege} when the overworld loads; with the setting off it runs 1.12.2's.
 */
public class ModernSiege extends VillageSiege {

    private enum State { TONIGHT, DONE }

    private final World world;
    private boolean hasSetupSiege;
    private State siegeState = State.DONE;
    private int zombiesToSpawn;
    private int nextSpawnTime;
    private int spawnX;
    private int spawnY;
    private int spawnZ;

    public ModernSiege(World world) {
        super(world);
        this.world = world;
    }

    static void init() {
        MinecraftForge.EVENT_BUS.register(new ModernSiege.Events());
    }

    @Override
    public void tick() {
        if (!ModConfig.mechanics.modernSieges) {
            super.tick();
            return;
        }
        // 1.14 ticks its custom spawners only with mob spawning on, and sieges only with monsters allowed.
        boolean spawnEnemies = this.world.getDifficulty() != EnumDifficulty.PEACEFUL
                && this.world.getGameRules().getBoolean("doMobSpawning");
        if (!this.world.isDaytime() && spawnEnemies) {
            long time = this.world.getWorldTime() % 24000L;
            if (time == 18000L) {
                this.siegeState = this.world.rand.nextInt(10) == 0 ? State.TONIGHT : State.DONE;
            }
            if (this.siegeState != State.DONE) {
                if (!this.hasSetupSiege) {
                    if (!this.tryToSetupSiege()) {
                        return;
                    }
                    this.hasSetupSiege = true;
                }
                if (this.nextSpawnTime > 0) {
                    --this.nextSpawnTime;
                } else {
                    this.nextSpawnTime = 2;
                    if (this.zombiesToSpawn > 0) {
                        this.trySpawn();
                        --this.zombiesToSpawn;
                    } else {
                        this.siegeState = State.DONE;
                    }
                }
            }
        } else {
            this.siegeState = State.DONE;
            this.hasSetupSiege = false;
        }
    }

    /** For the self-tests: starts tonight's siege as midnight's one-in-ten would. */
    public void forceTonight() {
        this.siegeState = State.TONIGHT;
        this.hasSetupSiege = false;
    }

    /** @return zombies still to come tonight */
    public int zombiesToSpawn() {
        return this.zombiesToSpawn;
    }

    private boolean tryToSetupSiege() {
        for (EntityPlayer player : this.world.playerEntities) {
            if (player.isSpectator()) {
                continue;
            }
            BlockPos pos = new BlockPos(player);
            if (RaidVillages.isVillage(this.world, pos) && !mushrooms(this.world.getBiome(pos))) {
                for (int i = 0; i < 10; ++i) {
                    float angle = this.world.rand.nextFloat() * ((float) Math.PI * 2.0F);
                    this.spawnX = pos.getX() + MathHelper.floor(MathHelper.cos(angle) * 32.0F);
                    this.spawnY = pos.getY();
                    this.spawnZ = pos.getZ() + MathHelper.floor(MathHelper.sin(angle) * 32.0F);
                    if (this.findRandomSpawnPos(new BlockPos(this.spawnX, this.spawnY, this.spawnZ)) != null) {
                        this.nextSpawnTime = 0;
                        this.zombiesToSpawn = 20;
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean mushrooms(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.MUSHROOM);
    }

    private void trySpawn() {
        Vec3d at = this.findRandomSpawnPos(new BlockPos(this.spawnX, this.spawnY, this.spawnZ));
        if (at == null) {
            return;
        }
        EntityZombie zombie;
        try {
            zombie = new EntityZombie(this.world);
            zombie.onInitialSpawn(this.world.getDifficultyForLocation(new BlockPos(zombie)), null);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Failed to create zombie for village siege at {}", at, e);
            return;
        }
        zombie.setLocationAndAngles(at.x, at.y, at.z, this.world.rand.nextFloat() * 360.0F, 0.0F);
        this.world.spawnEntity(zombie);
    }

    @Nullable
    private Vec3d findRandomSpawnPos(BlockPos center) {
        for (int i = 0; i < 10; ++i) {
            int x = center.getX() + this.world.rand.nextInt(16) - 8;
            int z = center.getZ() + this.world.rand.nextInt(16) - 8;
            int y = this.world.getHeight(x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (RaidVillages.isVillage(this.world, pos) && canZombieSpawn(pos)) {
                return new Vec3d(x + 0.5D, y, z + 0.5D);
            }
        }
        return null;
    }

    /** {@code Monster.checkMonsterSpawnRules} for a zombie: dark enough, a block to stand on, room. */
    private boolean canZombieSpawn(BlockPos pos) {
        if (!this.world.isBlockLoaded(pos)) {
            return false;
        }
        EntityZombie probe = new EntityZombie(this.world);
        probe.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        return probe.getCanSpawnHere() && probe.isNotColliding();
    }

    public static final class Events {

        /** The overworld's siege becomes 1.14's (1.14 runs sieges in the overworld only). */
        @SubscribeEvent
        public void onLoad(WorldEvent.Load event) {
            World world = event.getWorld();
            if (world.isRemote || !(world instanceof WorldServer) || world.provider.getDimension() != 0) {
                return;
            }
            try {
                ObfuscationReflectionHelper.setPrivateValue(WorldServer.class, (WorldServer) world, new ModernSiege(world), "field_175740_d");
            } catch (RuntimeException e) {
                VillagerBackport.LOGGER.warn("Cannot replace the village siege; 1.12.2's stays", e);
            }
        }
    }

    /** @return the overworld's siege, if it is this one */
    @Nullable
    public static ModernSiege of(WorldServer world) {
        Object siege = ObfuscationReflectionHelper.getPrivateValue(WorldServer.class, world, "field_175740_d");
        return siege instanceof ModernSiege ? (ModernSiege) siege : null;
    }
}
