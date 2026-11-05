package com.exiledradio.villagerbackport.building;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSign;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * A spruce, birch, jungle, acacia or dark oak sign, as held.
 *
 * <p>Vanilla's {@link ItemSign} with its two block names swapped: placing on top of a block stands the
 * sign up facing the player, placing on a side hangs it there, and either way the editing screen opens
 * straight after. It stays an {@code ItemSign} so anything that recognizes signs by their item - a
 * sorting mod, a recipe that takes "any sign" - recognizes these.
 *
 * <p>Stacks to 16, as vanilla's does.
 */
public final class ItemWoodSign extends ItemSign {

    final SignWood wood;

    ItemWoodSign(SignWood wood) {
        this.wood = wood;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState clicked = world.getBlockState(pos);
        boolean replaceable = clicked.getBlock().isReplaceable(world, pos);

        if (facing == EnumFacing.DOWN || !(clicked.getMaterial().isSolid() || replaceable)
                || (replaceable && facing != EnumFacing.UP)) {
            return EnumActionResult.FAIL;
        }

        pos = pos.offset(facing);
        ItemStack stack = player.getHeldItem(hand);
        BlockWoodSign.Standing standing = BuildingContent.standingSign(this.wood);
        BlockWoodSign.Wall wall = BuildingContent.wallSign(this.wood);

        if (!player.canPlayerEdit(pos, facing, stack) || !standing.canPlaceBlockAt(world, pos)) {
            return EnumActionResult.FAIL;
        }

        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        pos = replaceable ? pos.down() : pos;

        if (facing == EnumFacing.UP) {
            int rotation = MathHelper.floor((double) ((player.rotationYaw + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
            world.setBlockState(pos, standing.getDefaultState().withProperty(BlockStandingSign.ROTATION, rotation), 11);
        } else {
            world.setBlockState(pos, wall.getDefaultState().withProperty(BlockWallSign.FACING, facing), 11);
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntitySign && !ItemBlock.setTileEntityNBT(world, player, pos, stack)) {
            player.openEditSign((TileEntitySign) tile);
        }

        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
        }

        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }
}
