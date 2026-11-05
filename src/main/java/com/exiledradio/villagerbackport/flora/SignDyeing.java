package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.DyeUtils;

/**
 * Right-clicking a sign with a dye recolors its text, as 1.14's {@code SignBlock.onBlockActivated}.
 *
 * <p>Any dye: 1.12.2's dye items, the four new ones, and other mods' that are in the ore dictionary
 * as dyes. The dye is spent unless the player is in creative, and only when the color actually
 * changes - dyeing a red sign red costs nothing. The player must be allowed to edit the world there.
 *
 * <p>Works on any sign whose tile entity is a {@code TileEntitySign}, which covers 1.12.2's sign
 * and the wood signs of mods that build on it. The color is drawn by
 * {@code flora.client.TileEntityDyedSignRenderer} for 1.12.2's own sign; a mod drawing its own reads
 * {@link SignColor#textColor}.
 *
 * <p>Cancelled on both sides so the dye is not also used on the block (bone meal, cocoa beans),
 * and so the client swings its arm. The server still hears the click: Forge sends it first.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class SignDyeing {

    private SignDyeing() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!FloraContent.enabled() || !ModConfig.flora.dyeableSigns) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !DyeUtils.isDye(held)) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntitySign)) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (!player.capabilities.allowEdit) {
            return;
        }

        EnumDyeColor color = DyeUtils.colorFromStack(held).orElse(null);
        if (color == null) {
            return;
        }

        if (!world.isRemote && SignColor.set(tile, color)) {
            tile.markDirty();
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }
}
