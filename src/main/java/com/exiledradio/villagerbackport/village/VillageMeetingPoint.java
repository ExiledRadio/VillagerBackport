package com.exiledradio.villagerbackport.village;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.BlockWorkstation;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Random;

/**
 * A small paved square with the village bell on it.
 *
 * <h2>Not a workstation</h2>
 * The bell is 1.14's meeting point, not a job site - no profession works at one, and it is
 * deliberately absent from the workstation mapping for that reason. So it gets a building of its own
 * rather than a place in the pool, and only one per village.
 *
 * <p>1.14 puts the bell at the centre, on the well. Here it cannot be: 1.12.2's village is laid out
 * outwards from a well that <em>is</em> the starting piece, and there is no supported way to alter
 * it. A square of its own placed like any other building is the closest arrangement that does not
 * involve rewriting how villages begin.
 */
public class VillageMeetingPoint extends StructureVillagePieces.Village {

    private static final int WIDTH = 5;
    private static final int HEIGHT = 4;
    private static final int DEPTH = 5;

    private static final ResourceLocation BELL =
            new ResourceLocation(VillagerBackport.MOD_ID, "bell");

    public VillageMeetingPoint() {
    }

    public VillageMeetingPoint(StructureVillagePieces.Start start, int type, Random random,
                               StructureBoundingBox box, EnumFacing facing) {
        super(start, type);

        setCoordBaseMode(facing);
        this.boundingBox = box;
    }

    @Nullable
    public static VillageMeetingPoint createPiece(StructureVillagePieces.Start start,
                                                  List<StructureComponent> pieces, Random random,
                                                  int x, int y, int z, EnumFacing facing, int type) {
        StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(
                x, y, z, 0, 0, 0, WIDTH, HEIGHT, DEPTH, facing);

        if (StructureComponent.findIntersecting(pieces, box) != null) {
            return null;
        }

        return new VillageMeetingPoint(start, type, random, box, facing);
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
        IBlockState gravel = getBiomeSpecificBlockState(Blocks.GRAVEL.getDefaultState());

        fillWithBlocks(world, box, 0, 0, 0, 4, 0, 4, cobble, cobble, false);
        fillWithBlocks(world, box, 1, 0, 1, 3, 0, 3, gravel, gravel, false);
        fillWithBlocks(world, box, 0, 1, 0, 4, 3, 4,
                Blocks.AIR.getDefaultState(), Blocks.AIR.getDefaultState(), false);

        // Lanterns on posts at the corners, so the square is lit and reads as somewhere to gather.
        IBlockState standingTorch = Blocks.TORCH.getDefaultState().withProperty(BlockTorch.FACING, EnumFacing.UP);

        setBlockState(world, fence, 0, 1, 0, box);
        setBlockState(world, standingTorch, 0, 2, 0, box);
        setBlockState(world, fence, 4, 1, 0, box);
        setBlockState(world, standingTorch, 4, 2, 0, box);
        setBlockState(world, fence, 0, 1, 4, box);
        setBlockState(world, standingTorch, 0, 2, 4, box);
        setBlockState(world, fence, 4, 1, 4, box);
        setBlockState(world, standingTorch, 4, 2, 4, box);

        Block bell = ForgeRegistries.BLOCKS.getValue(BELL);
        if (bell != null) {
            IBlockState state = bell.getDefaultState();

            if (state.getPropertyKeys().contains(BlockWorkstation.FACING)) {
                state = state.withProperty(BlockWorkstation.FACING, EnumFacing.NORTH);
            }

            setBlockState(world, state, 2, 1, 2, box);
        }

        for (int z = 0; z < DEPTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                clearCurrentPositionBlocksUpwards(world, x, HEIGHT, z, box);
                replaceAirAndLiquidDownwards(world, cobble, x, -1, z, box);
            }
        }

        return true;
    }
}
