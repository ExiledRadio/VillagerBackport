package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.GuiHandler;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A cartography table: copies and widens maps.
 *
 * <p>Like the grindstone it keeps nothing between uses, so it has no tile entity - the container
 * holds the two inputs while the screen is open and hands them back when it closes.
 */
public class BlockCartographyTable extends BlockWorkstation {

    public BlockCartographyTable(Material material, SoundType sound, float hardness) {
        super(material, sound, hardness);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(VillagerBackport.instance, GuiHandler.CARTOGRAPHY, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
