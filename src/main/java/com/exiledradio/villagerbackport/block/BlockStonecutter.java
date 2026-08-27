package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.GuiHandler;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A stonecutter: cuts one stone block into a shape, without a crafting grid.
 *
 * <p>Holds nothing between uses, so no tile entity - what is in it when the screen closes goes back
 * to the player.
 *
 * <p>1.14's saw does not hurt anything that stands on it, despite the blade, and neither does this
 * one. The block is shaped rather than a cube only so it looks right.
 */
public class BlockStonecutter extends BlockWorkstationShaped {

    public BlockStonecutter(Material material, SoundType sound, float hardness,
                            AxisAlignedBB shape, boolean cutout) {
        super(material, sound, hardness, shape, cutout);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(VillagerBackport.instance, GuiHandler.STONECUTTER, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
