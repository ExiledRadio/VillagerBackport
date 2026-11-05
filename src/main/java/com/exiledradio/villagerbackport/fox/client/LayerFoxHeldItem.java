package com.exiledradio.villagerbackport.fox.client;

import com.exiledradio.villagerbackport.fox.EntityFox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The item in a fox's mouth, drawn across its snout and following its head. Positions are 1.14.4's
 * {@code FoxHeldItemLayer}: one set for an adult and one for a cub, each awake or asleep.
 */
@SideOnly(Side.CLIENT)
public class LayerFoxHeldItem implements LayerRenderer<EntityFox> {

    private final RenderFox renderer;

    public LayerFoxHeldItem(RenderFox renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityFox fox, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack held = fox.getHeldItemMainhand();
        if (held.isEmpty()) {
            return;
        }
        boolean sleeping = fox.isSleeping();
        boolean cub = fox.isChild();
        ModelFox model = (ModelFox) renderer.getMainModel();

        GlStateManager.pushMatrix();
        if (cub) {
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(0.0F, 8.0F * scale, 3.35F * scale);
        }
        GlStateManager.translate(model.head.rotationPointX / 16.0F, model.head.rotationPointY / 16.0F,
                model.head.rotationPointZ / 16.0F);
        GlStateManager.rotate(fox.getHeadRollAngle(partialTicks) * 57.295776F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(netHeadYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(headPitch, 1.0F, 0.0F, 0.0F);
        if (cub) {
            if (sleeping) {
                GlStateManager.translate(0.4F, 0.26F, 0.15F);
            } else {
                GlStateManager.translate(0.06F, 0.26F, -0.5F);
            }
        } else if (sleeping) {
            GlStateManager.translate(0.46F, 0.26F, 0.22F);
        } else {
            GlStateManager.translate(0.06F, 0.27F, -0.5F);
        }
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        if (sleeping) {
            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        }
        Minecraft.getMinecraft().getItemRenderer().renderItem(fox, held, ItemCameraTransforms.TransformType.GROUND);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
