package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Where cornflowers and lilies of the valley grow.
 *
 * <h2>Which biomes</h2>
 * minecraft.wiki's, mapped onto 1.12.2's biomes:
 * <ul>
 * <li><b>Cornflower:</b> plains, sunflower plains and flower forest.</li>
 * <li><b>Lily of the valley:</b> forest (and its hills), birch forest and old growth birch forest
 * (and their hills), dark forest (and its hills), and flower forest.</li>
 * </ul>
 * A modded biome joins by its biome dictionary types: a plains that is not snowy, savanna or dead
 * grows cornflowers, and a forest that is not coniferous, snowy, jungle or dead grows lilies - which
 * is how the dozens of plains and woods in a big pack pick them up without being listed.
 *
 * <h2>How much</h2>
 * 1.14 places flowers as scattered patches; this does the same, after the chunk's own decoration, as
 * a world generator - so it runs under any chunk generator, OTG and RTG included, because Forge calls
 * world generators for every one of them. A patch is up to 32 tries within seven blocks of a point,
 * and each try plants only where the flower could stand anyway: on grass or dirt, in an empty block.
 * Patches are centered in the middle of the four chunks being populated so no try reaches a chunk
 * that might not be loaded.
 *
 * <h2>Bone meal</h2>
 * 1.12.2's grass picks the flower bone meal grows from the biome's list ({@code Biome.addFlower}),
 * so each flower is added to its biomes' lists - cornflowers in plains, sunflower plains and flower
 * forests at the weight of the oxeye daisy, lilies of the valley in flower forests only, as the wiki
 * says. Done when the first world loads, when every mod's biomes and dictionary types are in.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FloraWorldGen implements IWorldGenerator {

    /** A patch's tries, and the chance per chunk of one, for each kind of biome. */
    private static final int TRIES = 32;
    private static final int SPREAD = 7;

    private static final float CORNFLOWER_PLAINS = 0.5F;
    private static final float LILY_FOREST = 0.33F;
    private static final float FLOWER_FOREST = 1.0F;

    private static final Map<Biome, float[]> CHANCES = new HashMap<Biome, float[]>();

    private static boolean registered;
    private static boolean flowersAdded;

    private FloraWorldGen() {
    }

    /** Registers the generator once. Called from block registration, before any world exists. */
    static void register() {
        if (!registered && FloraContent.enabled() && ModConfig.flora.generateFlowers) {
            registered = true;
            GameRegistry.registerWorldGenerator(new FloraWorldGen(), 10);
        }
    }

    static boolean active() {
        return FloraContent.enabled() && ModConfig.flora.generateFlowers && !FloraFutureMc.oursHidden();
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator,
                         IChunkProvider provider) {
        if (!world.provider.isSurfaceWorld() || FloraContent.cornflower == null || !active()) {
            return;
        }

        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;
        Biome biome = world.getBiome(new BlockPos(centerX + 8, 0, centerZ + 8));
        float[] chances = chances(biome);

        if (chances[0] > 0.0F && random.nextFloat() < chances[0]) {
            patch(world, random, centerX, centerZ, FloraContent.cornflower);
        }
        if (chances[1] > 0.0F && random.nextFloat() < chances[1]) {
            patch(world, random, centerX, centerZ, FloraContent.lilyOfTheValley);
        }
    }

    private static void patch(World world, Random random, int centerX, int centerZ, Block flower) {
        int x = centerX + random.nextInt(16);
        int z = centerZ + random.nextInt(16);

        for (int i = 0; i < TRIES; i++) {
            int px = x + random.nextInt(SPREAD + 1) - random.nextInt(SPREAD + 1);
            int pz = z + random.nextInt(SPREAD + 1) - random.nextInt(SPREAD + 1);
            BlockPos pos = world.getTopSolidOrLiquidBlock(new BlockPos(px, 0, pz));
            if (world.isAirBlock(pos) && ((BlockFloraFlower) flower).canBlockStay(world, pos, flower.getDefaultState())) {
                world.setBlockState(pos, flower.getDefaultState(), 2);
            }
        }
    }

    /** @return {cornflower, lily} patch chances for a biome */
    private static float[] chances(Biome biome) {
        float[] chances = CHANCES.get(biome);
        if (chances == null) {
            chances = classify(biome);
            CHANCES.put(biome, chances);
        }
        return chances;
    }

    private static final List<Biome> CORNFLOWER_BIOMES = Arrays.asList(Biomes.PLAINS, Biomes.MUTATED_PLAINS);
    private static final List<Biome> LILY_BIOMES = Arrays.asList(Biomes.FOREST, Biomes.FOREST_HILLS,
            Biomes.BIRCH_FOREST, Biomes.BIRCH_FOREST_HILLS, Biomes.MUTATED_BIRCH_FOREST,
            Biomes.MUTATED_BIRCH_FOREST_HILLS, Biomes.ROOFED_FOREST, Biomes.MUTATED_ROOFED_FOREST);

    private static float[] classify(Biome biome) {
        if (biome == Biomes.MUTATED_FOREST) {
            return new float[]{FLOWER_FOREST, FLOWER_FOREST};
        }
        if (CORNFLOWER_BIOMES.contains(biome)) {
            return new float[]{CORNFLOWER_PLAINS, 0.0F};
        }
        if (LILY_BIOMES.contains(biome)) {
            return new float[]{0.0F, LILY_FOREST};
        }
        if (biome.getRegistryName() == null || "minecraft".equals(biome.getRegistryName().getNamespace())) {
            return new float[]{0.0F, 0.0F};
        }
        return new float[]{moddedPlains(biome) ? CORNFLOWER_PLAINS : 0.0F, moddedForest(biome) ? LILY_FOREST : 0.0F};
    }

    private static boolean moddedPlains(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.PLAINS)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SAVANNA)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.DEAD);
    }

    private static boolean moddedForest(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.DEAD);
    }

    /** Adds the flowers to bone meal's lists, once, when the first world loads. */
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (flowersAdded || event.getWorld().isRemote || !active() || FloraContent.cornflower == null) {
            return;
        }
        flowersAdded = true;

        Set<Biome> cornflower = new HashSet<Biome>(CORNFLOWER_BIOMES);
        cornflower.add(Biomes.MUTATED_FOREST);
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (biome.getRegistryName() != null && !"minecraft".equals(biome.getRegistryName().getNamespace())
                    && moddedPlains(biome)) {
                cornflower.add(biome);
            }
        }

        for (Biome biome : cornflower) {
            // The oxeye daisy's weight in 1.12.2's plains; flower forests list each flower at 10.
            biome.addFlower(FloraContent.cornflower.getDefaultState(), biome == Biomes.MUTATED_FOREST ? 10 : 20);
        }
        Biomes.MUTATED_FOREST.addFlower(FloraContent.lilyOfTheValley.getDefaultState(), 10);

        VillagerBackport.LOGGER.info("Bone meal can now grow cornflowers in {} biomes and lilies of the valley in flower forests.",
                cornflower.size());
    }
}
