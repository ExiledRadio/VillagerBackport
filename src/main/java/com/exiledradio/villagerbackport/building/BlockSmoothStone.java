package com.exiledradio.villagerbackport.building;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

/**
 * Smooth stone, smooth sandstone, smooth red sandstone and smooth quartz.
 *
 * <p>Plain cubes with 1.14's numbers: hardness 2 and blast resistance 6, which 1.12.2 spells
 * {@code setResistance(10)} because it stores resistance at three fifths of what it reports. They
 * need a pickaxe, as every stone does.
 */
public final class BlockSmoothStone extends Block {

    BlockSmoothStone(BuildingTable.MapTint tint) {
        super(Material.ROCK, mapColor(tint));
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
    }

    private static MapColor mapColor(BuildingTable.MapTint tint) {
        switch (tint) {
            case SAND:
                return MapColor.SAND;
            case ADOBE:
                return MapColor.ADOBE;
            case QUARTZ:
                return MapColor.QUARTZ;
            default:
                return MapColor.STONE;
        }
    }
}
