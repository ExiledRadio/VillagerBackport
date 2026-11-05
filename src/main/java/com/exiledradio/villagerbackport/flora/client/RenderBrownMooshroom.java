package com.exiledradio.villagerbackport.flora.client;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelCow;
import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a brown mooshroom: 1.14's brown mooshroom texture on 1.12.2's cow model, with brown
 * mushrooms on its back and head where a red one has red ones.
 *
 * <p>The mushroom layer is 1.12.2's {@code LayerMooshroomMushroom} line for line, with the block
 * swapped - same three mushrooms, same angles - so a brown mooshroom and a red one stand side by side
 * as 1.14's do.
 */
@SideOnly(Side.CLIENT)
public class RenderBrownMooshroom extends RenderLiving<EntityMooshroom> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/cow/brown_mooshroom.png");

    public RenderBrownMooshroom(RenderManager manager) {
        super(manager, new ModelCow(), 0.7F);
        addLayer(new BrownMushrooms(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityMooshroom entity) {
        return TEXTURE;
    }

    /** {@code LayerMooshroomMushroom}, with brown mushrooms. */
    private static final class BrownMushrooms implements LayerRenderer<EntityMooshroom> {

        private final RenderBrownMooshroom renderer;

        BrownMushrooms(RenderBrownMooshroom renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityMooshroom entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (entity.isChild() || entity.isInvisible()) {
                return;
            }

            BlockRendererDispatcher blocks = Minecraft.getMinecraft().getBlockRendererDispatcher();
            this.renderer.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.enableCull();
            GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.0F, -1.0F, 1.0F);
            GlStateManager.translate(0.2F, 0.35F, 0.5F);
            GlStateManager.rotate(42.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.pushMatrix();
            GlStateManager.translate(-0.5F, -0.5F, 0.5F);
            blocks.renderBlockBrightness(Blocks.BROWN_MUSHROOM.getDefaultState(), 1.0F);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.1F, 0.0F, -0.6F);
            GlStateManager.rotate(42.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(-0.5F, -0.5F, 0.5F);
            blocks.renderBlockBrightness(Blocks.BROWN_MUSHROOM.getDefaultState(), 1.0F);
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            ((ModelQuadruped) this.renderer.getMainModel()).head.postRender(0.0625F);
            GlStateManager.scale(1.0F, -1.0F, 1.0F);
            GlStateManager.translate(0.0F, 0.7F, -0.2F);
            GlStateManager.rotate(12.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(-0.5F, -0.5F, 0.5F);
            blocks.renderBlockBrightness(Blocks.BROWN_MUSHROOM.getDefaultState(), 1.0F);
            GlStateManager.popMatrix();
            GlStateManager.cullFace(GlStateManager.CullFace.BACK);
            GlStateManager.disableCull();
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
