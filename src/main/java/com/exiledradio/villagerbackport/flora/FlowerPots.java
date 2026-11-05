package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Putting this package's flowers into 1.12.2's flower pot.
 *
 * <p>1.12.2's pot refuses anything not on its own list, so a player right-clicking an empty pot
 * with a cornflower is caught here first: the pot becomes {@link BlockFloraPot potted_cornflower},
 * the flower is spent (not in creative), and the "flowers potted" statistic counts it, as vanilla's
 * pot does. Taking it back out is the potted block's own business.
 *
 * <p>The event is cancelled on both sides, so the client does not go on to try placing the flower on
 * top of the pot; the server still receives the click, as Forge sends it regardless.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FlowerPots {

    private FlowerPots() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!FloraContent.enabled()) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockFlowerPot) || state.getBlock() != Blocks.FLOWER_POT) {
            return;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityFlowerPot) || !((TileEntityFlowerPot) tile).getFlowerItemStack().isEmpty()) {
            return;
        }

        ItemStack held = event.getItemStack();
        BlockFloraPot potted = pottedFor(held);
        if (potted == null) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (!world.isRemote) {
            world.setBlockState(pos, potted.getDefaultState(), 3);
            player.addStat(StatList.FLOWER_POTTED);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    /** @return the potted block for a held flower of this package's, or null */
    @Nullable
    static BlockFloraPot pottedFor(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBlock)) {
            return null;
        }
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        for (BlockFloraPot pot : new BlockFloraPot[]{FloraContent.pottedCornflower,
                FloraContent.pottedLilyOfTheValley, FloraContent.pottedWitherRose}) {
            if (pot != null && pot.getFlower() == block) {
                return pot;
            }
        }
        return null;
    }

    /**
     * @return true if a stack is a plant any flower pot takes: 1.12.2's own list, or this package's
     */
    static boolean isPottable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (pottedFor(stack) != null) {
            return true;
        }
        if (!(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        // 1.12.2's BlockFlowerPot.canBePotted, which is private.
        if (block == Blocks.YELLOW_FLOWER || block == Blocks.RED_FLOWER || block == Blocks.CACTUS
                || block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM || block == Blocks.SAPLING
                || block == Blocks.DEADBUSH) {
            return true;
        }
        return block == Blocks.TALLGRASS && stack.getMetadata() == 2;
    }
}
