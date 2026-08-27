package com.exiledradio.villagerbackport.village;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Random;

/**
 * An open market stall: two posts, a striped awning, and a workstation under it.
 *
 * <h2>Why an open building as well as a closed one</h2>
 * 1.14's villages are not all houses - a good deal of their character comes from job sites out in
 * the open, worked at from the street. A village made only of closed workshops reads as a housing
 * estate, and hides the very blocks this is all meant to put on show.
 *
 * <p>Cheaper than a workshop in every sense: three by two, no door, no villager of its own. It does
 * not enlarge the village the way a workshop does, which is the point - stalls fill in a village
 * rather than inflating it.
 *
 * <p>The awning takes its two colours from the piece's own layout roll, so stalls in one village
 * differ from each other but each one stays the colour it was built.
 */
public class VillageMarketStall extends VillageWorkstationPiece {

    private static final int WIDTH = 3;
    private static final int HEIGHT = 5;
    private static final int DEPTH = 2;

    /** The awnings 1.14 uses on its stalls: a plain white and a colour beside it. */
    private static final EnumDyeColor[] AWNINGS = {
            EnumDyeColor.WHITE,
            EnumDyeColor.YELLOW,
            EnumDyeColor.LIGHT_BLUE,
            EnumDyeColor.ORANGE,
            EnumDyeColor.LIME,
    };

    public VillageMarketStall() {
    }

    public VillageMarketStall(StructureVillagePieces.Start start, int type, Random random,
                              StructureBoundingBox box, EnumFacing facing) {
        super(start, type, random, box, facing);
    }

    @Nullable
    public static VillageMarketStall createPiece(StructureVillagePieces.Start start,
                                                 List<StructureComponent> pieces, Random random,
                                                 int x, int y, int z, EnumFacing facing, int type) {
        StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(
                x, y, z, 0, 0, 0, WIDTH, HEIGHT, DEPTH, facing);

        if (StructureComponent.findIntersecting(pieces, box) != null) {
            return null;
        }

        return new VillageMarketStall(start, type, random, box, facing);
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
        IBlockState fence = getBiomeSpecificBlockState(Blocks.OAK_FENCE.getDefaultState());

        // The awning is picked here rather than at layout time because it is decoration - nothing
        // depends on it being the same across a reload, and it keeps the saved data to the one
        // field that does matter.
        EnumDyeColor front = AWNINGS[random.nextInt(AWNINGS.length)];
        IBlockState canopy = Blocks.WOOL.getStateFromMeta(front.getMetadata());
        IBlockState canopyEdge = Blocks.WOOL.getStateFromMeta(EnumDyeColor.WHITE.getMetadata());

        fillWithBlocks(world, box, 0, 0, 0, 2, 0, 1, cobble, cobble, false);
        fillWithBlocks(world, box, 0, 1, 0, 2, 3, 1,
                Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);

        // Posts at the two front corners, so the stall is open on three sides and can be walked into.
        for (int y = 1; y <= 3; y++) {
            setBlockState(world, fence, 0, y, 0, box);
            setBlockState(world, fence, 2, y, 0, box);
        }

        fillWithBlocks(world, box, 0, 4, 0, 2, 4, 0, canopy, canopy, false);
        fillWithBlocks(world, box, 0, 4, 1, 2, 4, 1, canopyEdge, canopyEdge, false);

        // Facing south: the stall's back is at z=1, so it is served from the street in front of it.
        placeWorkstation(world, box, 1, 1, 1, EnumFacing.SOUTH);

        placeTorch(world, EnumFacing.NORTH, 0, 3, 1, box);

        for (int z = 0; z < DEPTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                clearCurrentPositionBlocksUpwards(world, x, HEIGHT, z, box);
                replaceAirAndLiquidDownwards(world, cobble, x, -1, z, box);
            }
        }

        return true;
    }
}
