package com.exiledradio.villagerbackport.fox;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Sweet berries: a quick snack that is also the bush's seed.
 *
 * <p>Two hunger points and 0.4 saturation, eaten as fast as any food. Used on grass, dirt, podzol,
 * coarse dirt or farmland they plant a stage-0 bush in the space above; used anywhere else they are
 * eaten, as 1.14 does by trying to place first and eating when that fails.
 *
 * <p>Foxes eat them, breed on them, and pick them from bushes; composters take them at 30%.
 */
public class ItemSweetBerries extends ItemFood {

    public ItemSweetBerries() {
        super(2, 0.1F, false);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        BlockPos at = world.getBlockState(pos).getBlock().isReplaceable(world, pos) ? pos : pos.offset(facing);
        ItemStack stack = player.getHeldItem(hand);
        BlockSweetBerryBush bush = FoxContent.bush;

        if (stack.isEmpty() || !player.canPlayerEdit(at, facing, stack) || !bush.canPlaceBlockAt(world, at)) {
            return EnumActionResult.PASS;
        }

        IBlockState state = bush.getDefaultState();
        if (!world.setBlockState(at, state, 11)) {
            return EnumActionResult.FAIL;
        }
        SoundType sound = bush.getSoundType(state, world, at, player);
        world.playSound(player, at, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, at, stack);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
