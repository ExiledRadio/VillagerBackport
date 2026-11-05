package com.exiledradio.villagerbackport.bamboo;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * The thin scattering of bamboo 1.14 adds to ordinary jungles and jungle hills.
 *
 * <p>1.14's numbers: sixteen attempts a chunk, each at a random column and a random height between
 * the bottom of the world and twice the height of the ground there. Only an attempt that happens to
 * land on open ground grows a stalk, so most chunks get none and a stand turns up every few chunks.
 * No podzol.
 *
 * <p>1.14 places this bamboo before the jungle's trees and grass, which would otherwise take the
 * ground first, so it runs at the start of decoration ({@link DecorateBiomeEvent.Pre}) rather than
 * as a world generator afterward. It draws from a random of its own, seeded per chunk, so a jungle's
 * trees and plants come out where they would without this mod.
 *
 * <p>Future MC already puts its own bamboo in jungles, so with it installed this does nothing.
 */
final class JungleBamboo {

    private static final int ATTEMPTS = 16;

    private JungleBamboo() {
    }

    @SubscribeEvent
    public static void onDecorate(DecorateBiomeEvent.Pre event) {
        if (!ModConfig.bamboo.jungleBamboo || BambooFutureMc.generatesJungleBamboo()) {
            return;
        }
        World world = event.getWorld();
        BlockPos corner = event.getChunkPos().getBlock(0, 0, 0);
        Biome biome = world.getBiome(corner.add(16, 0, 16));
        if (biome != Biomes.JUNGLE && biome != Biomes.JUNGLE_HILLS) {
            return;
        }

        int areaX = corner.getX() + 8;
        int areaZ = corner.getZ() + 8;
        Random random = new Random(world.getSeed() ^ (corner.getX() * 341873128712L + corner.getZ() * 132897987541L)
                ^ 0x62616D626FL);
        for (int i = 0; i < ATTEMPTS; i++) {
            int x = areaX + random.nextInt(16);
            int z = areaZ + random.nextInt(16);
            int top = world.getHeight(x, z);
            if (top <= 0) {
                continue;
            }
            int y = random.nextInt(top * 2);
            BambooFeature.place(world, random, new BlockPos(x, y, z), 0.0F, areaX, areaZ);
        }
    }
}
