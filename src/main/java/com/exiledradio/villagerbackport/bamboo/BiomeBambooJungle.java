package com.exiledradio.villagerbackport.bamboo;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenMegaJungle;
import net.minecraft.world.gen.feature.WorldGenShrub;

import java.util.Random;

/**
 * The bamboo jungle and bamboo jungle hills.
 *
 * <p>A jungle in every respect but its plants: the same climate, terrain, grass, melons, vines,
 * temples, parrots and ocelots, with bamboo everywhere, patches of podzol under it, and pandas.
 *
 * <h2>Bamboo</h2>
 * 1.14's own numbers. Each chunk tries {@code ceil((noise + 0.3) * 160)} stalks, where the noise is
 * the same broad, 80-block-scale noise 1.12 colors grass with, so a bamboo jungle has dense thickets
 * and open glades rather than an even spread. A fifth of the stalks lay podzol. Bamboo goes in
 * before the trees, as in 1.14, so it stands on the ground rather than in the canopy.
 *
 * <h2>Trees</h2>
 * Thirty attempts a chunk (a tenth of chunks get one more), each choosing in turn: a fancy oak 5% of
 * the time, otherwise a jungle bush 15%, otherwise a giant jungle tree 70%, otherwise nothing - which
 * is how 1.14's selector reads. Fewer and bigger trees than a jungle's fifty.
 */
public class BiomeBambooJungle extends BiomeJungle {

    private static final IBlockState JUNGLE_LOG = Blocks.LOG.getDefaultState()
            .withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE);
    private static final IBlockState JUNGLE_LEAF = Blocks.LEAVES.getDefaultState()
            .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE)
            .withProperty(BlockLeaves.CHECK_DECAY, false);
    private static final IBlockState OAK_LEAF = Blocks.LEAVES.getDefaultState()
            .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.OAK)
            .withProperty(BlockLeaves.CHECK_DECAY, false);

    /** 1.14's {@code TOP_SOLID_HEIGHTMAP_NOISE_BIASED} settings for bamboo. */
    private static final int NOISE_TO_COUNT = 160;
    private static final double NOISE_SCALE = 80.0D;
    private static final double NOISE_OFFSET = 0.3D;
    private static final float PODZOL_CHANCE = 0.2F;

    /** A tree choice that grows nothing, for the selector's empty outcome. */
    private static final WorldGenAbstractTree NOTHING = new WorldGenAbstractTree(false) {
        @Override
        public boolean generate(World world, Random rand, BlockPos position) {
            return false;
        }
    };

    public BiomeBambooJungle(BiomeProperties properties) {
        super(false, properties);
        this.decorator.treesPerChunk = 30;
        this.decorator.extraTreeChance = 0.1F;
    }

    @Override
    public WorldGenAbstractTree getRandomTreeFeature(Random rand) {
        if (rand.nextFloat() < 0.05F) {
            return BIG_TREE_FEATURE;
        }
        if (rand.nextFloat() < 0.15F) {
            return new WorldGenShrub(JUNGLE_LOG, OAK_LEAF);
        }
        if (rand.nextFloat() < 0.7F) {
            return new WorldGenMegaJungle(false, 10, 20, JUNGLE_LOG, JUNGLE_LEAF);
        }
        return NOTHING;
    }

    @Override
    public void decorate(World world, Random rand, BlockPos pos) {
        int areaX = pos.getX() + 8;
        int areaZ = pos.getZ() + 8;
        double noise = GRASS_COLOR_NOISE.getValue(areaX / NOISE_SCALE, areaZ / NOISE_SCALE);
        int count = (int) Math.ceil((noise + NOISE_OFFSET) * NOISE_TO_COUNT);

        for (int i = 0; i < count; i++) {
            int x = areaX + rand.nextInt(16);
            int z = areaZ + rand.nextInt(16);
            BlockPos ground = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
            BambooFeature.place(world, rand, ground, PODZOL_CHANCE, areaX, areaZ);
        }

        super.decorate(world, rand, pos);
    }
}
