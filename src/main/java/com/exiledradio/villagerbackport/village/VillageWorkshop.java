package com.exiledradio.villagerbackport.village;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Random;

/**
 * A one-room workshop with a door, a window and somebody's job inside it.
 *
 * <h2>Shape</h2>
 * Five by five, flat-roofed, built from the same cobblestone-and-planks vocabulary as vanilla's own
 * small house - and through {@code getBiomeSpecificBlockState}, so it turns to sandstone in a desert
 * and spruce in a taiga along with everything around it. A pack that swaps village materials through
 * Forge's terrain-gen event gets this building swapped with the rest.
 *
 * <p>The workstation sits at the back wall facing the door, with the villager beside it. That is
 * deliberately the arrangement 1.14 uses in its own job-site houses: you see what the building is
 * for from the doorway.
 *
 * <p>It has a door, which matters for more than access - 1.12.2 counts doors to decide how large a
 * village is and how many villagers it supports, so a workshop enlarges the village that contains it
 * rather than just occupying space in it.
 */
public class VillageWorkshop extends VillageWorkstationPiece {

    private static final int WIDTH = 5;
    private static final int HEIGHT = 6;
    private static final int DEPTH = 5;

    public VillageWorkshop() {
    }

    public VillageWorkshop(StructureVillagePieces.Start start, int type, Random random,
                           StructureBoundingBox box, EnumFacing facing) {
        super(start, type, random, box, facing);
    }

    @Nullable
    public static VillageWorkshop createPiece(StructureVillagePieces.Start start,
                                              List<StructureComponent> pieces, Random random,
                                              int x, int y, int z, EnumFacing facing, int type) {
        StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(
                x, y, z, 0, 0, 0, WIDTH, HEIGHT, DEPTH, facing);

        if (StructureComponent.findIntersecting(pieces, box) != null) {
            return null;
        }

        return new VillageWorkshop(start, type, random, box, facing);
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox box) {
        if (this.averageGroundLvl < 0) {
            this.averageGroundLvl = getAverageGroundLevel(world, box);

            if (this.averageGroundLvl < 0) {
                return true;
            }

            this.boundingBox.offset(0, this.averageGroundLvl - this.boundingBox.maxY + HEIGHT - 1, 0);
        }

        IBlockState cobble = getBiomeSpecificBlockState(Blocks.COBBLESTONE.getDefaultState());
        IBlockState planks = getBiomeSpecificBlockState(Blocks.PLANKS.getDefaultState());
        IBlockState log = getBiomeSpecificBlockState(Blocks.LOG.getDefaultState());
        IBlockState stairs = getBiomeSpecificBlockState(
                Blocks.STONE_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));

        // Floor, then the whole shell, then the room hollowed back out of it - fewer, larger fills
        // than placing each wall, and it cannot leave a gap at a corner.
        fillWithBlocks(world, box, 0, 0, 0, 4, 0, 4, cobble, cobble, false);
        fillWithBlocks(world, box, 0, 1, 0, 4, 3, 4, planks, planks, false);
        fillWithBlocks(world, box, 0, 4, 0, 4, 4, 4, log, log, false);
        fillWithBlocks(world, box, 1, 4, 1, 3, 4, 3, planks, planks, false);

        // Corner posts, which is what stops five-by-five of planks reading as a crate.
        for (int y = 1; y <= 3; y++) {
            setBlockState(world, log, 0, y, 0, box);
            setBlockState(world, log, 4, y, 0, box);
            setBlockState(world, log, 0, y, 4, box);
            setBlockState(world, log, 4, y, 4, box);
        }

        fillWithBlocks(world, box, 1, 1, 1, 3, 3, 3,
                Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);

        setBlockState(world, Blocks.GLASS_PANE.getDefaultState(), 0, 2, 2, box);
        setBlockState(world, Blocks.GLASS_PANE.getDefaultState(), 4, 2, 2, box);
        setBlockState(world, Blocks.GLASS_PANE.getDefaultState(), 2, 2, 4, box);

        setBlockState(world, Blocks.AIR.getDefaultState(), 2, 1, 0, box);
        setBlockState(world, Blocks.AIR.getDefaultState(), 2, 2, 0, box);
        createVillageDoor(world, box, random, 2, 1, 0, EnumFacing.NORTH);

        // A step up to the threshold, but only where the ground actually needs one - the same test
        // vanilla's small house makes, including putting grass back under a path it would cover.
        if (getBlockStateFromPos(world, 2, 0, -1, box).getMaterial() == Material.AIR
                && getBlockStateFromPos(world, 2, -1, -1, box).getMaterial() != Material.AIR) {
            setBlockState(world, stairs, 2, 0, -1, box);

            if (getBlockStateFromPos(world, 2, -1, -1, box).getBlock() == Blocks.GRASS_PATH) {
                setBlockState(world, Blocks.GRASS.getDefaultState(), 2, -1, -1, box);
            }
        }

        placeWorkstation(world, box, 2, 1, 3, EnumFacing.NORTH);

        placeTorch(world, EnumFacing.EAST, 1, 3, 2, box);
        placeTorch(world, EnumFacing.WEST, 3, 3, 2, box);

        for (int z = 0; z < DEPTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                clearCurrentPositionBlocksUpwards(world, x, HEIGHT, z, box);
                replaceAirAndLiquidDownwards(world, cobble, x, -1, z, box);
            }
        }

        spawnVillagers(world, box, 1, 1, 2, 1);
        return true;
    }
}
