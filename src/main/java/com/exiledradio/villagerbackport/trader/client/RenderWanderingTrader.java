package com.exiledradio.villagerbackport.trader.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.trader.EntityWanderingTrader;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's {@code WanderingTraderRenderer}: the villager model with its hat boxes, 1.14's trader
 * texture, villager scale, and whatever it is drinking held up in front of its folded arms.
 */
@SideOnly(Side.CLIENT)
public final class RenderWanderingTrader extends RenderLiving<EntityWanderingTrader> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/wandering_trader.png");

    public RenderWanderingTrader(RenderManager manager) {
        super(manager, new ModelWanderingTrader(), 0.5F);
        addLayer(new HeldItem());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWanderingTrader entity) {
        return TEXTURE;
    }

    /** Villagers and the trader are drawn at fifteen sixteenths. */
    @Override
    protected void preRenderCallback(EntityWanderingTrader entity, float partialTicks) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }

    /**
     * 1.14's {@code CrossedArmsItemLayer}: the held item forward of the arms, drawn as an item lying
     * on the ground is, which gives it its size and angle. The trader only ever holds the potion or
     * the milk it is drinking.
     */
    private static final class HeldItem implements LayerRenderer<EntityWanderingTrader> {

        @Override
        public void doRenderLayer(EntityWanderingTrader trader, float limbSwing, float limbSwingAmount,
                                  float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale) {
            ItemStack stack = trader.getHeldItemMainhand();
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
            mc.getItemRenderer().renderItem(trader, stack, ItemCameraTransforms.TransformType.GROUND);
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
}
