package com.exiledradio.villagerbackport.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether a block holds up the middle of one of its faces, as current Java's {@code canSupportCenter} asks
 * for candles and lanterns: the block's shape has to reach that face over the middle two pixels.
 *
 * <p>1.12.2's face shapes answer that for vanilla blocks, but furniture mods (MrCrayfish's tables among
 * them) answer UNDEFINED for every face of a table with a full top. So a block whose face shape says no is
 * asked for its collision boxes: one has to reach the face and cover its middle. Forge's {@code isSideSolid}
 * is not asked, since it says yes for a hopper's open top.
 */
public final class CenterSupport {

    private static final double LO = 7 / 16.0;
    private static final double HI = 9 / 16.0;
    private static final double EPS = 1.0E-4;

    private CenterSupport() {
    }

    /** @return true if the block at {@code at} holds up the middle of its {@code face} by its shape alone */
    public static boolean byShape(IBlockAccess world, BlockPos at, EnumFacing face) {
        if (face.getAxis() != EnumFacing.Axis.Y) {
            return false;
        }
        IBlockState state = world.getBlockState(at);
        try {
            boolean top = face == EnumFacing.UP;
            double x = at.getX();
            double y = at.getY();
            double z = at.getZ();
            List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
            if (world instanceof World) {
                AxisAlignedBB probe = new AxisAlignedBB(x + LO, top ? y + 1 - 0.01 : y, z + LO,
                        x + HI, top ? y + 1 : y + 0.01, z + HI);
                state.addCollisionBoxToList((World) world, at, probe, boxes, null, false);
            } else {
                AxisAlignedBB box = state.getCollisionBoundingBox(world, at);
                if (box != null && box != Block.NULL_AABB) {
                    boxes.add(box.offset(at));
                }
            }
            for (AxisAlignedBB box : boxes) {
                boolean reaches = top ? box.maxY >= y + 1 - EPS : box.minY <= y + EPS;
                if (reaches && box.minX <= x + LO + EPS && box.maxX >= x + HI - EPS
                        && box.minZ <= z + LO + EPS && box.maxZ >= z + HI - EPS) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            // A modded block that cannot answer without an entity or a real world does not hold anything up.
        }
        return false;
    }
}
