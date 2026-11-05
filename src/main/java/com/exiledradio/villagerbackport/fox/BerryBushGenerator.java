package com.exiledradio.villagerbackport.fox;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Sweet berry bush patches in taigas, at 1.14's rates.
 *
 * <h2>1.14's numbers</h2>
 * An ordinary taiga tries one patch per chunk; a snowy taiga one patch in twelve chunks. Each patch
 * picks a column in the chunk and a height anywhere from bedrock to twice the surface - 1.14's
 * "heightmap double" placement - and then makes 64 attempts within 7 blocks across and 3 up or down,
 * each placing a ripe bush where there is air above grass, dirt or podzol. Because the height is
 * drawn up to twice the surface, roughly half the patches start in the sky and place nothing; that is
 * why 1.14's bushes are scattered rather than everywhere, and it is copied as it is.
 *
 * <p>1.14.4 put none in giant tree taigas; current Java does, rarely, so they share the snowy taiga's
 * one in twelve.
 *
 * <h2>Which biomes</h2>
 * Anything the Forge biome dictionary calls {@code CONIFEROUS}, which is every vanilla taiga and the
 * taigas of Biomes O' Plenty, OTG presets and the like. Snowy ones ({@code SNOWY}) and the vanilla
 * giant (redwood) taigas use the rare rate.
 *
 * <h2>Chunk edges</h2>
 * Patch columns are picked in the square starting 8 blocks into the chunk, as vanilla decoration
 * does, so the 7-block spread stays within the four chunks population is allowed to touch. Bushes are placed without neighbor updates
 * and need no support beyond the block under them, which is already generated.
 */
public class BerryBushGenerator implements IWorldGenerator {

    private static final int ATTEMPTS = 64;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator,
                         IChunkProvider provider) {
        if (!ModConfig.foxes.generateBushes || FoxContent.bush == null || !world.provider.isSurfaceWorld()) {
            return;
        }
        int baseX = chunkX * 16 + 8;
        int baseZ = chunkZ * 16 + 8;
        Biome biome = world.getBiome(new BlockPos(baseX, 0, baseZ));
        if (!BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS) || blacklisted(biome)) {
            return;
        }

        if (rare(biome)) {
            int chance = ModConfig.foxes.rarePatchChance;
            if (chance > 0 && random.nextInt(chance) == 0) {
                patch(world, random, baseX, baseZ);
            }
        } else {
            for (int i = 0; i < ModConfig.foxes.taigaPatchesPerChunk; i++) {
                patch(world, random, baseX, baseZ);
            }
        }
    }

    /** @return true for snowy taigas and the vanilla giant ones, which get the rare rate */
    static boolean rare(Biome biome) {
        return FoxContent.isSnowy(biome) || biome == Biomes.REDWOOD_TAIGA || biome == Biomes.REDWOOD_TAIGA_HILLS
                || biome == Biomes.MUTATED_REDWOOD_TAIGA || biome == Biomes.MUTATED_REDWOOD_TAIGA_HILLS;
    }

    private static boolean blacklisted(Biome biome) {
        String[] list = ModConfig.foxes.bushBiomeBlacklist;
        if (list.length == 0) {
            return false;
        }
        Set<String> set = new HashSet<String>(Arrays.asList(list));
        return set.contains(String.valueOf(biome.getRegistryName()));
    }

    /** One 1.14 patch: a random column and a height up to twice the surface, then 64 tries around it. */
    static int patch(World world, Random random, int baseX, int baseZ) {
        // A column in the 16x16 square population decorates, which starts 8 blocks into this chunk;
        // the 7-block spread around it then stays inside the 2x2 chunks that are loaded.
        int x = baseX + random.nextInt(16);
        int z = baseZ + random.nextInt(16);
        int surface = world.getHeight(x, z);
        if (surface <= 0) {
            return 0;
        }
        BlockPos center = new BlockPos(x, random.nextInt(surface * 2), z);
        IBlockState ripe = FoxContent.bush.getDefaultState().withProperty(BlockSweetBerryBush.AGE, BlockSweetBerryBush.MAX_AGE);

        int placed = 0;
        for (int i = 0; i < ATTEMPTS; i++) {
            BlockPos at = center.add(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
            if (at.getY() > 0 && at.getY() < 255 && world.isAirBlock(at)
                    && FoxContent.bush.canGrowOn(world.getBlockState(at.down()))) {
                world.setBlockState(at, ripe, 2);
                placed++;
            }
        }
        return placed;
    }
}
