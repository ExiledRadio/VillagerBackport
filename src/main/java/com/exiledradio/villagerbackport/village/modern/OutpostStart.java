package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.backportcore.terrain.TerrainHeights;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A 1.14 pillager outpost: its base plate, watchtower and scattered features, laid out by the same
 * jigsaw assembler as the villages, seven pieces deep.
 */
public class OutpostStart extends StructureStart {

    /** 1.14's {@code PillagerOutpostPieces}: depth 7. */
    private static final int DEPTH = 7;

    /** 1.14's {@code MarginedStructureStart}: pillagers spawn up to twelve blocks outside the pieces. */
    private static final int MARGIN = 12;

    /** For loading from disk. */
    public OutpostStart() {
    }

    public OutpostStart(Random random, int chunkX, int chunkZ, @Nullable TerrainHeights heights) {
        super(chunkX, chunkZ);
        if (heights != null) {
            new ModernVillageStart.Assembler(heights, random, this.components, DEPTH)
                    .assemble("pillager_outpost/base_plates", new BlockPos(chunkX * 16, 0, chunkZ * 16));
        }
        this.updateBoundingBox();
        if (!this.components.isEmpty()) {
            this.boundingBox.minX -= MARGIN;
            this.boundingBox.minY -= MARGIN;
            this.boundingBox.minZ -= MARGIN;
            this.boundingBox.maxX += MARGIN;
            this.boundingBox.maxY += MARGIN;
            this.boundingBox.maxZ += MARGIN;
        }
    }

    @Override
    public boolean isSizeableStructure() {
        return !this.components.isEmpty();
    }
}
