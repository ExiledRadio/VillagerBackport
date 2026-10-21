package com.exiledradio.villagerbackport.village;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

/**
 * Workstations inside 1.12.2's own village buildings, for villages generated in the legacy style.
 *
 * <p>1.14 builds its workstations into its houses. 1.12.2's houses have none, so each building that
 * plainly belongs to a trade gets that trade's workstation, at a fixed spot read off the building's
 * own layout in {@code StructureVillagePieces}: a lectern in the library, a smoker in the butcher's
 * hall, a blast furnace, smithing table and grindstone at the blacksmith's, a brewing stand in the
 * church, a composter on the corner of each field and a bell beside the well. The small houses each
 * get one workstation from the village pool, chosen from the building's position so the same seed
 * always furnishes them the same way.
 *
 * <p>Runs after a chunk's village pieces are placed, and only places into a spot that is still
 * empty, with something solid underneath, inside the chunk being populated.
 */
public final class LegacyHouseWorkstations {

    private static Field averageGroundLevel;

    private LegacyHouseWorkstations() {
    }

    /** A workstation at a building-relative spot. Local z = 0 is the building's front. */
    private static final class Spot {
        final int x;
        final int y;
        final int z;
        final String block;

        Spot(int x, int y, int z, String block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
        }
    }

    private static final String RANDOM = "random";

    private static Spot[] spotsFor(StructureComponent piece) {
        if (piece instanceof StructureVillagePieces.House1) {
            return new Spot[]{new Spot(3, 1, 1, "villagerbackport:lectern")};
        }
        if (piece instanceof StructureVillagePieces.House2) {
            return new Spot[]{new Spot(7, 1, 3, "villagerbackport:blast_furnace"),
                    new Spot(8, 1, 3, "villagerbackport:smithing_table"),
                    new Spot(9, 1, 2, "villagerbackport:grindstone")};
        }
        if (piece instanceof StructureVillagePieces.Hall) {
            return new Spot[]{new Spot(7, 1, 3, "villagerbackport:smoker")};
        }
        if (piece instanceof StructureVillagePieces.Church) {
            return new Spot[]{new Spot(1, 1, 2, "minecraft:brewing_stand")};
        }
        if (piece instanceof StructureVillagePieces.Field1 || piece instanceof StructureVillagePieces.Field2) {
            return new Spot[]{new Spot(0, 1, 0, "villagerbackport:composter")};
        }
        if (piece instanceof StructureVillagePieces.Well) {
            return new Spot[]{new Spot(0, 12, 0, "villagerbackport:bell")};
        }
        if (piece instanceof StructureVillagePieces.House3) {
            return new Spot[]{new Spot(6, 1, 8, RANDOM)};
        }
        if (piece instanceof StructureVillagePieces.House4Garden) {
            return new Spot[]{new Spot(3, 1, 3, RANDOM)};
        }
        if (piece instanceof StructureVillagePieces.WoodHut) {
            return new Spot[]{new Spot(2, 1, 1, RANDOM)};
        }
        return null;
    }

    /** Furnishes every legacy village building in this start that the chunk being populated reaches. */
    public static void furnish(World world, StructureStart start, StructureBoundingBox area) {
        if (!ModConfig.villages.legacyHouseWorkstations) {
            return;
        }
        for (StructureComponent piece : start.getComponents()) {
            if (!piece.getBoundingBox().intersectsWith(area) || !isBuilt(piece)) {
                continue;
            }
            Spot[] spots = spotsFor(piece);
            if (spots == null) {
                continue;
            }
            for (Spot spot : spots) {
                place(world, piece, spot, area);
            }
        }
    }

    /**
     * A 1.12.2 village building settles its height the first time any part of it is placed. Until then
     * its box is still at the height it was laid out at, and local coordinates would land in the air.
     */
    private static boolean isBuilt(StructureComponent piece) {
        if (!(piece instanceof StructureVillagePieces.Village)) {
            return false;
        }
        try {
            if (averageGroundLevel == null) {
                averageGroundLevel = ReflectionHelper.findField(StructureVillagePieces.Village.class,
                        "averageGroundLvl", "field_143015_k");
                averageGroundLevel.setAccessible(true);
            }
            return averageGroundLevel.getInt(piece) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void place(World world, StructureComponent piece, Spot spot, StructureBoundingBox area) {
        BlockPos at = local(piece, spot.x, spot.y, spot.z);
        if (!area.isVecInside(at) || !world.isAirBlock(at) || !world.getBlockState(at.down()).isSideSolid(world, at.down(), EnumFacing.UP)) {
            return;
        }

        Block block = RANDOM.equals(spot.block) ? randomWorkstation(piece) : ForgeRegistries.BLOCKS.getValue(new ResourceLocation(spot.block));
        if (block == null || block == Blocks.AIR) {
            return;
        }
        // Future MC's twin when that is the one kept.
        block = com.exiledradio.villagerbackport.compat.FutureMc.preferred(block);

        IBlockState state = block.getDefaultState();
        EnumFacing front = piece.getCoordBaseMode() == null ? EnumFacing.NORTH : piece.getCoordBaseMode().getOpposite();
        state = face(state, front);
        world.setBlockState(at, state, 2);
    }

    /** Turns a directional workstation to face the building's door. */
    private static IBlockState face(IBlockState state, EnumFacing front) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if ("facing".equals(property.getName()) && property.getAllowedValues().contains(front)) {
                @SuppressWarnings("unchecked")
                IProperty<EnumFacing> facing = (IProperty<EnumFacing>) property;
                return state.withProperty(facing, front);
            }
        }
        if (state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            return state.withProperty(BlockHorizontal.FACING, front);
        }
        return state;
    }

    private static Block randomWorkstation(StructureComponent piece) {
        List<Block> pool = WorkstationPool.weighted();
        if (pool.isEmpty()) {
            return null;
        }
        StructureBoundingBox box = piece.getBoundingBox();
        Random random = new Random(((long) box.minX * 341873128712L) ^ ((long) box.minZ * 132897987541L));
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * {@code StructureComponent.getXWithOffset} and friends, which are protected: building-relative
     * coordinates to world coordinates for the building's facing.
     */
    private static BlockPos local(StructureComponent piece, int x, int y, int z) {
        StructureBoundingBox box = piece.getBoundingBox();
        EnumFacing facing = piece.getCoordBaseMode();
        int wx;
        int wz;
        if (facing == null) {
            wx = x;
            wz = z;
        } else {
            switch (facing) {
                case NORTH:
                    wx = box.minX + x;
                    wz = box.maxZ - z;
                    break;
                case SOUTH:
                    wx = box.minX + x;
                    wz = box.minZ + z;
                    break;
                case WEST:
                    wx = box.maxX - z;
                    wz = box.minZ + x;
                    break;
                default:
                    wx = box.minX + z;
                    wz = box.minZ + x;
                    break;
            }
        }
        return new BlockPos(wx, box.minY + y, wz);
    }
}
