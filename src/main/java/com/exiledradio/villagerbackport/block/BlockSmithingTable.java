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
 * The smithing table: the toolsmith's workstation, and 1.16's netherite upgrade when there is
 * netherite to upgrade with.
 *
 * <p>1.14's table did nothing, and with nothing to upgrade neither does this one - a screen that
 * could never produce anything would only be confusing. With a netherite mod installed, or smithing
 * recipes added to Future MC (CraftTweaker's included), it opens 1.16's upgrade screen; see
 * {@link SmithingUpgrades} for how they are found.
 */
public class BlockSmithingTable extends BlockWorkstation {

    public BlockSmithingTable(Material material, SoundType sound, float hardness) {
        super(material, sound, hardness);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!SmithingUpgrades.isActive()) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(VillagerBackport.instance, GuiHandler.SMITHING, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
