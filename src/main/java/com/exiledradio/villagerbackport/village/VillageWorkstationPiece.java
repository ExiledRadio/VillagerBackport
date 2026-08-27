package com.exiledradio.villagerbackport.village;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraft.world.gen.structure.template.TemplateManager;

import javax.annotation.Nullable;

import java.util.Random;

/**
 * A village building put there to hold a workstation.
 *
 * <h2>Why the choice is recorded rather than re-rolled</h2>
 * A village's layout is decided in one pass and written to disk; the buildings themselves are only
 * constructed as the chunks under them generate, which can be a different session entirely. So the
 * workstation is chosen when the piece is laid out and stored in the piece's own NBT, exactly as
 * vanilla stores whether a small house got a roof terrace.
 *
 * <p>It is recorded by registry name, not by index into the pool. An index would quietly come to
 * mean a different block the moment a pack edited its workstation mapping, and the building would be
 * rebuilt wrong. A name that no longer resolves simply leaves the building empty, which is the right
 * failure: a house with a missing workstation, rather than the wrong one.
 */
public abstract class VillageWorkstationPiece extends StructureVillagePieces.Village {

    private static final String WORKSTATION = "Workstation";

    private String workstation = "";

    /** Required by {@code MapGenStructureIO}, which instantiates pieces reflectively when loading. */
    public VillageWorkstationPiece() {
    }

    protected VillageWorkstationPiece(StructureVillagePieces.Start start, int type, Random random,
                                      StructureBoundingBox box, EnumFacing facing) {
        super(start, type);

        setCoordBaseMode(facing);
        this.boundingBox = box;
        this.workstation = WorkstationPool.nameOf(WorkstationPool.pick(random));
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        super.writeStructureToNBT(tag);
        tag.setString(WORKSTATION, this.workstation);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager templates) {
        super.readStructureFromNBT(tag, templates);
        this.workstation = tag.getString(WORKSTATION);
    }

    /** @return the block this building was laid out to hold, or null if it is not installed. */
    @Nullable
    protected Block workstation() {
        return WorkstationPool.byName(this.workstation);
    }

    /**
     * Puts the workstation down, facing the given way in the building's own coordinates.
     *
     * <p>The piece's rotation is applied to the block state by {@code setBlockState}, so a
     * workstation laid out facing the door faces the door whichever way the building ended up
     * turned - which is why these are written as if every building faced north.
     */
    protected boolean placeWorkstation(World world, StructureBoundingBox box, int x, int y, int z,
                                       EnumFacing facing) {
        Block block = workstation();
        if (block == null) {
            return false;
        }

        IBlockState state = block.getDefaultState();

        // Only the directional workstations carry a facing; asking the rest for one would throw.
        if (state.getPropertyKeys().contains(com.exiledradio.villagerbackport.block.BlockWorkstation.FACING)) {
            state = state.withProperty(
                    com.exiledradio.villagerbackport.block.BlockWorkstation.FACING, facing);
        }

        setBlockState(world, state, x, y, z, box);
        return true;
    }
}
