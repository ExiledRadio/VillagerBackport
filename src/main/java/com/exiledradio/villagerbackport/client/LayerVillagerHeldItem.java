package com.exiledradio.villagerbackport.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws what a villager is holding up, in front of its folded arms.
 *
 * <p>1.12.2's villager model has no hands to hold anything in, so it never drew a held item. This is
 * the game's own held-item layer for villagers: the item sits forward of the arms and is drawn the
 * way an item lying on the ground is, which is what gives it its size and angle.
 */
@SideOnly(Side.CLIENT)
public final class LayerVillagerHeldItem implements LayerRenderer<EntityVillager> {

    @Override
    public void doRenderLayer(EntityVillager villager, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                              float scale) {
        ItemStack stack = villager.getHeldItemMainhand();
        if (stack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        Block block = Block.getBlockFromItem(stack.getItem());
        boolean translucent = mc.getRenderItem().shouldRenderItemIn3D(stack)
                && block.getRenderLayer() == BlockRenderLayer.TRANSLUCENT;

        GlStateManager.pushMatrix();
        if (translucent) {
            GlStateManager.depthMask(false);
        }

        GlStateManager.translate(0.0F, 0.4F, -0.4F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        mc.getItemRenderer().renderItem(villager, stack, ItemCameraTransforms.TransformType.GROUND);

        if (translucent) {
            GlStateManager.depthMask(true);
        }
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
