package com.exiledradio.villagerbackport.campfire.client;

import com.exiledradio.villagerbackport.campfire.BlockCampfire;
import com.exiledradio.villagerbackport.campfire.TileEntityCampfire;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws the food lying on a campfire, as 1.14's {@code CampfireTileEntityRenderer} does: each item flat
 * on the logs, 0.375 of its usual size, a little over 7/16 of a block up, set in from its own side and
 * turned to face out from it. The first spot is on the side the campfire faces, the rest follow round
 * clockwise.
 */
@SideOnly(Side.CLIENT)
public class TileEntityCampfireRenderer extends TileEntitySpecialRenderer<TileEntityCampfire> {

    @Override
    public void render(TileEntityCampfire tile, double x, double y, double z, float partialTicks, int destroyStage,
                       float alpha) {
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof BlockCampfire)) {
            return;
        }
        EnumFacing facing = state.getValue(BlockCampfire.FACING);
        NonNullList<ItemStack> items = tile.items();
        RenderHelper.enableStandardItemLighting();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            EnumFacing side = TileEntityCampfire.slotDirection(facing, i);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5D, y + 0.44921875D, z + 0.5D);
            GlStateManager.rotate(-side.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(-0.3125D, -0.3125D, 0.0D);
            GlStateManager.scale(0.375F, 0.375F, 0.375F);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
        }
    }
}
