package com.exiledradio.villagerbackport.woodset;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

import java.util.Random;

/**
 * 1.14's blue ice: packed ice made slipperier, from nine packed ice.
 *
 * <h2>From 1.14.4</h2>
 * {@code Material.PACKED_ICE}, hardness 2.8, slipperiness 0.989 (packed ice's is 0.98, so a boat
 * keeps nearly all its speed), glass sounds. It never melts: like packed ice it has no random tick, and
 * light does not reach into it.
 *
 * <h2>Dropping</h2>
 * As packed ice in 1.12.2 and 1.14: nothing, unless mined with Silk Touch. {@code PACKED_ICE} needs no
 * tool, so any tool's Silk Touch works, and a pickaxe is the tool that mines it fast (registered as
 * its harvest tool by {@code WoodsetContent}).
 */
public class BlockBlueIce extends Block {

    public BlockBlueIce() {
        super(Material.PACKED_ICE, MapColor.ICE);
        this.setHardness(2.8F);
        this.setSoundType(SoundType.GLASS);
        this.slipperiness = 0.989F;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
