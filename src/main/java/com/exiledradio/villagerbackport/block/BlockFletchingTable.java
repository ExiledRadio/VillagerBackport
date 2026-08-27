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
 * A fletching table: the same saw as the stonecutter, working wood instead of rock.
 *
 * <h2>Why it does anything at all</h2>
 * 1.14 ships the fletching table as furniture. It gives a villager a job and has no use of its own -
 * right through to the versions after it, opening one does nothing. That leaves the one workstation
 * in the set that a player has no reason ever to touch.
 *
 * <p>So this one cuts wood. It is the stonecutter's machine with a different question asked of each
 * block - is this wood rather than is this rock - which means a log becomes planks and planks become
 * stairs, slabs and the rest in a single click, at the cost of the offcuts you would have got from
 * doing it in a crafting grid. The saw was already written and did not care what it was cutting; see
 * {@link CutterKind}.
 *
 * <p>Holds nothing between uses, so no tile entity - what is in it when the screen closes goes back
 * to the player, as with the grindstone, cartography table and stonecutter.
 */
public class BlockFletchingTable extends BlockWorkstation {

    public BlockFletchingTable(Material material, SoundType sound, float hardness) {
        super(material, sound, hardness);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(VillagerBackport.instance, GuiHandler.FLETCHING, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
