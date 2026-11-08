package com.exiledradio.villagerbackport.worldgen;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.bamboo.BambooContent;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * 1.14's placement of the structures 1.12.2 already has, so a world matches 1.14's seed maps (the villages
 * and outposts are in {@code village/modern}, the biome map in {@code bamboo/Layers114}).
 *
 * <ul>
 *   <li><b>Temples</b>: since 1.13 the desert pyramid, jungle temple, witch hut and igloo each have their own
 *       grid (salts 14357617, 14357619, 14357620 and 14357618; spacing 32, separation 8) where 1.12.2 shares
 *       one, and read the block-scale biome at the chunk's block (9, 9); 1.14 adds the bamboo jungles to the
 *       jungle temple's biomes.</li>
 *   <li><b>Strongholds</b>: placed in every biome that has them in 1.14; next to 1.12.2's "above sea level"
 *       rule, that adds the mushroom field shore (the bamboo jungles are above sea level already).</li>
 * </ul>
 * Update Aquatic does the same for 1.13 (whichever replaces vanilla's temple generator first, the result is
 * the same). Its biome search patch, which strongholds and the spawn point need for 1.13 and later, has no
 * counterpart here: this mod has no bytecode patches.
 */
public final class Placement114 {

    static final int DESERT_PYRAMID = 14357617, IGLOO = 14357618, JUNGLE_TEMPLE = 14357619, SWAMP_HUT = 14357620;
    private static final int[] SALTS = {DESERT_PYRAMID, IGLOO, JUNGLE_TEMPLE, SWAMP_HUT};
    private static final int SEPARATION = 8;

    private Placement114() {
    }

    public static void init() {
        BiomeManager.addStrongholdBiome(Biomes.MUSHROOM_ISLAND_SHORE);
        MinecraftForge.TERRAIN_GEN_BUS.register(Placement114.class);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInitMapGen(InitMapGenEvent event) {
        if (event.getType() == InitMapGenEvent.EventType.SCATTERED_FEATURE
                && event.getNewGen().getClass() == MapGenScatteredFeature.class) {
            event.setNewGen(new Temples((MapGenScatteredFeature) event.getNewGen()));
        }
    }

    static Biome biomeAt(World world, int chunkX, int chunkZ) {
        return world.getBiomeProvider().getBiome(new BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9));
    }

    static boolean isBamboo(Biome biome) {
        return biome != null && (biome == BambooContent.bambooJungle || biome == BambooContent.bambooJungleHills);
    }

    /** The grid of the temple a biome has in 1.14, or 0 for none. */
    static int templeSalt(Biome biome) {
        if (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS) {
            return DESERT_PYRAMID;
        }
        if (biome == Biomes.JUNGLE || biome == Biomes.JUNGLE_HILLS || isBamboo(biome)) {
            return JUNGLE_TEMPLE;
        }
        if (biome == Biomes.SWAMPLAND) {
            return SWAMP_HUT;
        }
        if (biome == Biomes.ICE_PLAINS || biome == Biomes.COLD_TAIGA) {
            return IGLOO;
        }
        return 0;
    }

    /** 1.12.2's temple generator on 1.14's four grids. Saved and loaded as vanilla's ("Temple"). */
    public static final class Temples extends MapGenScatteredFeature {

        private static final Field SPACING = ReflectionHelper.findField(MapGenScatteredFeature.class,
                "maxDistanceBetweenScatteredFeatures", "field_82669_g");

        private final int spacing;

        Temples(MapGenScatteredFeature replaced) {
            int spacing = 32;
            try {
                spacing = SPACING.getInt(replaced);
            } catch (IllegalAccessException e) {
                VillagerBackport.LOGGER.warn("Temples: could not read the temple spacing; using 32", e);
            }
            this.spacing = spacing;
        }

        /** Each temple's grid first, as vanilla checks, then the biome at the chunk's (9, 9). */
        @Override
        protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
            int cellX = Math.floorDiv(chunkX, this.spacing);
            int cellZ = Math.floorDiv(chunkZ, this.spacing);
            int biomeSalt = -1;
            for (int salt : SALTS) {
                Random random = this.world.setRandomSeed(cellX, cellZ, salt);
                if (chunkX == cellX * this.spacing + random.nextInt(this.spacing - SEPARATION)
                        && chunkZ == cellZ * this.spacing + random.nextInt(this.spacing - SEPARATION)) {
                    if (biomeSalt == -1) {
                        biomeSalt = templeSalt(biomeAt(this.world, chunkX, chunkZ));
                    }
                    if (biomeSalt == salt) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** The temple of the biome its grid was chosen by; a bamboo jungle builds a jungle temple. */
        @Override
        protected StructureStart getStructureStart(int chunkX, int chunkZ) {
            Biome biome = biomeAt(this.world, chunkX, chunkZ);
            return new MapGenScatteredFeature.Start(this.world, this.rand, chunkX, chunkZ, isBamboo(biome) ? Biomes.JUNGLE : biome);
        }
    }
}
