package com.exiledradio.villagerbackport.building;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/**
 * One of 1.14's new stairs.
 *
 * <p>Vanilla's {@link BlockStairs} already does everything - corners, upside-down placement, copying
 * its strength and sound from the block it is made of - and only hides its constructor. This exists
 * to open it.
 */
public final class BlockBuildingStairs extends BlockStairs implements com.exiledradio.backportcore.waterlog.Waterloggable {

    BlockBuildingStairs(IBlockState base) {
        super(base);
        this.useNeighborBrightness = true;
    }
}
