package com.exiledradio.villagerbackport.village.modern;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockHay;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTallGrass;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

/**
 * The features 1.14's village decoration pools place: trees, crop piles, flowers.
 *
 * <p>Trees use 1.12.2's own generators for the same trees. The piles are 1.14's
 * {@code BlockPileFeature}, which 1.12.2 has no equivalent of.
 */
final class VillageFeatures {

    private VillageFeatures() {
    }

    static void place(String feature, World world, Random random, BlockPos pos) {
        if ("normal_tree".equals(feature)) {
            new WorldGenTrees(false).generate(world, random, pos);
        } else if ("savanna_tree".equals(feature)) {
            new WorldGenSavannaTree(false).generate(world, random, pos);
        } else if ("spruce_tree".equals(feature)) {
            new WorldGenTaiga2(false).generate(world, random, pos);
        } else if ("pine_tree".equals(feature)) {
            new WorldGenTaiga1().generate(world, random, pos);
        } else if ("cactus".equals(feature)) {
            cactus(world, random, pos);
        } else if ("hay_pile".equals(feature)) {
            pile(world, random, pos, Pile.HAY);
        } else if ("melon_pile".equals(feature)) {
            pile(world, random, pos, Pile.MELON);
        } else if ("pumpkin_pile".equals(feature)) {
            pile(world, random, pos, Pile.PUMPKIN);
        } else if ("snow_pile".equals(feature)) {
            pile(world, random, pos, Pile.SNOW);
        } else if ("ice_pile".equals(feature)) {
            pile(world, random, pos, Pile.ICE);
        } else if ("plain_flower".equals(feature)) {
            flowers(world, random, pos);
        } else if ("taiga_grass".equals(feature)) {
            BlockTallGrass.EnumType type = random.nextInt(5) == 0 ? BlockTallGrass.EnumType.GRASS : BlockTallGrass.EnumType.FERN;
            new WorldGenTallGrass(type).generate(world, random, pos);
        } else if ("sweet_berry_bush".equals(feature)) {
            berries(world, random, pos);
        }
    }

    private enum Pile {
        HAY, MELON, PUMPKIN, SNOW, ICE;

        IBlockState pick(Random random) {
            switch (this) {
                case HAY:
                    return Blocks.HAY_BLOCK.getDefaultState().withProperty(BlockHay.AXIS,
                            random.nextBoolean() ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
                case MELON:
                    return Blocks.MELON_BLOCK.getDefaultState();
                case PUMPKIN:
                    // 1.14's pumpkin pile is one jack o'lantern in twenty.
                    Block block = random.nextFloat() < 0.95F ? Blocks.PUMPKIN : Blocks.LIT_PUMPKIN;
                    return block.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.Plane.HORIZONTAL.random(random));
                case SNOW:
                    return Blocks.SNOW.getDefaultState();
                default:
                    // 1.14 makes one block in three blue ice, which 1.12.2 only has from a backport mod.
                    IBlockState blue = BackportBlocks.resolve("minecraft:blue_ice", new NBTTagCompound(), new NBTTagCompound());
                    return blue != null && random.nextInt(3) == 0 ? blue : Blocks.PACKED_ICE.getDefaultState();
            }
        }
    }

    /** 1.14's {@code BlockPileFeature}. */
    private static void pile(World world, Random random, BlockPos pos, Pile pile) {
        if (pos.getY() < 5) {
            return;
        }
        int rx = 2 + random.nextInt(2);
        int rz = 2 + random.nextInt(2);
        for (BlockPos at : BlockPos.getAllInBoxMutable(pos.add(-rx, 0, -rz), pos.add(rx, 1, rz))) {
            int dx = pos.getX() - at.getX();
            int dz = pos.getZ() - at.getZ();
            if ((float) (dx * dx + dz * dz) <= random.nextFloat() * 10.0F - random.nextFloat() * 6.0F
                    || random.nextFloat() < 0.031D) {
                BlockPos fixed = at.toImmutable();
                IBlockState below = world.getBlockState(fixed.down());
                if (world.isAirBlock(fixed) && below.isSideSolid(world, fixed.down(), EnumFacing.UP)) {
                    world.setBlockState(fixed, pile.pick(random), 2);
                }
            }
        }
    }

    private static void cactus(World world, Random random, BlockPos pos) {
        if (!Blocks.CACTUS.canBlockStay(world, pos)) {
            return;
        }
        int height = 1 + random.nextInt(random.nextInt(3) + 1);
        for (int i = 0; i < height && world.isAirBlock(pos.up(i)); i++) {
            world.setBlockState(pos.up(i), Blocks.CACTUS.getDefaultState(), 2);
        }
    }

    /** 1.14's flower patch: up to 64 tries in a 15 by 7 by 15 area around the spot. */
    private static void flowers(World world, Random random, BlockPos pos) {
        for (int i = 0; i < 64; i++) {
            BlockPos at = pos.add(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
            if (!world.isAirBlock(at) || at.getY() >= 255) {
                continue;
            }
            BlockFlower.EnumFlowerType type = world.getBiome(at).pickRandomFlower(random, at);
            BlockFlower block = type.getBlockType().getBlock();
            IBlockState state = block.getDefaultState().withProperty(block.getTypeProperty(), type);
            if (block.canBlockStay(world, at, state)) {
                world.setBlockState(at, state, 2);
            }
        }
    }

    /** Only with a mod that adds sweet berry bushes; 1.12.2 has none. */
    private static void berries(World world, Random random, BlockPos pos) {
        Block bush = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("futuremc", "sweet_berry_bush"));
        if (bush == null || bush == Blocks.AIR) {
            return;
        }
        for (int i = 0; i < 32; i++) {
            BlockPos at = pos.add(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
            if (world.isAirBlock(at) && world.getBlockState(at.down()).getMaterial() == Material.GRASS) {
                world.setBlockState(at, bush.getDefaultState(), 2);
            }
        }
    }
}
