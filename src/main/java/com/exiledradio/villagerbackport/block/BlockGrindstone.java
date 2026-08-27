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
 * A grindstone: takes the enchantments off an item and hands back some experience.
 *
 * <p>The block does almost nothing itself - it opens a screen, and the container does the work. It
 * has no tile entity because it holds nothing between uses: whatever is in it when the screen closes
 * goes back to the player, exactly as a crafting table or anvil behaves.
 */
public class BlockGrindstone extends BlockWorkstationShaped {

    public BlockGrindstone(Material material, SoundType sound, float hardness,
                           AxisAlignedBB shape, boolean cutout) {
        super(material, sound, hardness, shape, cutout);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(VillagerBackport.instance, GuiHandler.GRINDSTONE, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
