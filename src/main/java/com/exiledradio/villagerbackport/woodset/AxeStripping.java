package com.exiledradio.villagerbackport.woodset;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * 1.14's axe stripping: right-click a log with an axe and it becomes the stripped log.
 *
 * <h2>From 1.14.4's {@code AxeItem.onItemUse}</h2>
 * The log keeps its axis; bark (vanilla's {@code axis=none} log, 1.14's {@code oak_wood}) becomes
 * stripped wood. {@code item.axe.strip} plays at the block, the axe takes one point of damage, and the
 * arm swings. It runs on both sides as 1.14's does: the client predicts the swing and hears the sound
 * at once, the server changes the block and damages the axe.
 *
 * <h2>Which axes, which logs</h2>
 * Any {@link ItemAxe}, or any item that says it is an axe ({@code getToolClasses} contains
 * {@code "axe"}), so modded axes and multi-tools strip too. Only logs this package has a stripped
 * form of - vanilla's ten - are stripped; another mod's log is left to that mod.
 *
 * <h2>Living with other stripping mods</h2>
 * Future MC and Quark strip logs too. This listens at {@link EventPriority#LOW} and does not receive
 * cancelled events, so if one of them handled the click first, nothing happens here; and once a log
 * is stripped by either, it is no longer a vanilla log, so the other finds nothing to do. Either way
 * one click strips one log once. With Future MC's stripped logs hidden in favor of these
 * ({@code compat.futureMcDuplicates}), Future MC's own stripping still makes its own; that is Future
 * MC's handler, and this one does not fight it.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class AxeStripping {

    private AxeStripping() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!WoodsetContent.enabled() || !ModConfig.villageBlocks.axeStripping
                || event.getUseItem() == Event.Result.DENY) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        if (!isAxe(held) || player.isSpectator()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (!player.canPlayerEdit(pos, event.getFace(), held)) {
            return;
        }
        IBlockState stripped = stripped(world.getBlockState(pos));
        if (stripped == null) {
            return;
        }

        // As 1.14: the server plays it to everyone but this player, whose client plays its own copy.
        world.playSound(player, pos, WoodsetContent.axeStrip, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!world.isRemote) {
            world.setBlockState(pos, stripped, 11);
            held.damageItem(1, player);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    /** @return true for any axe, vanilla or modded */
    public static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof ItemAxe || stack.getItem().getToolClasses(stack).contains("axe"));
    }

    /**
     * @return the stripped form of a vanilla log or bark block, keeping its axis, or null if this
     * block has none here
     */
    @Nullable
    public static IBlockState stripped(IBlockState state) {
        BlockPlanks.EnumType type;
        if (state.getBlock() == Blocks.LOG) {
            type = state.getValue(BlockOldLog.VARIANT);
        } else if (state.getBlock() == Blocks.LOG2) {
            type = state.getValue(BlockNewLog.VARIANT);
        } else {
            return null;
        }
        Wood wood = Wood.of(type);
        if (wood == null) {
            return null;
        }
        BlockLog.EnumAxis axis = state.getValue(BlockLog.LOG_AXIS);
        if (axis == BlockLog.EnumAxis.NONE) {
            BlockStrippedLog bark = WoodsetContent.strippedWood(wood);
            return bark == null ? null : bark.getDefaultState().withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y);
        }
        BlockStrippedLog log = WoodsetContent.strippedLog(wood);
        return log == null ? null : log.getDefaultState().withProperty(BlockLog.LOG_AXIS, axis);
    }
}
