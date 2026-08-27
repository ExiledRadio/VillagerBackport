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
 * A loom: applies one banner pattern at a time, chosen rather than crafted.
 *
 * <p>Holds nothing between uses, so no tile entity - whatever is in it when the screen closes goes
 * back to the player, the same as the grindstone, cartography table and stonecutter.
 */
public class BlockLoom extends BlockWorkstationFacing {

    public BlockLoom(Material material, SoundType sound, float hardness) {
        super(material, sound, hardness);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(VillagerBackport.instance, GuiHandler.LOOM, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
