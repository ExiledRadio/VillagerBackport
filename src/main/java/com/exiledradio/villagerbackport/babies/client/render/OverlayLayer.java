package com.exiledradio.villagerbackport.babies.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/**
 * A copy of the body model drawn again with another texture, optionally tinted: 26.3's sheep wool, wolf collar,
 * horse markings and villager type layers ({@code coloredCutoutModelCopyLayerRender}). Skipped when invisible.
 */
public final class OverlayLayer<T extends EntityLivingBase> implements LayerRenderer<T> {

    /** The overlay's texture and tint for an entity, or null for none. */
    public interface Overlay<T> {
        /** @return the texture, or null to draw nothing */
        ResourceLocation texture(T entity);

        /** @return red, green, blue */
        float[] color(T entity, float partialTicks);
    }

    private static final float[] WHITE = {1.0F, 1.0F, 1.0F};

    private final RenderLivingBase<T> renderer;
    private final ModelBase model;
    private final Overlay<T> overlay;
    private final boolean translucent;

    public OverlayLayer(RenderLivingBase<T> renderer, ModelBase model, Overlay<T> overlay, boolean translucent) {
        this.renderer = renderer;
        this.model = model;
        this.overlay = overlay;
        this.translucent = translucent;
    }

    /** An untinted overlay. */
    public static <T extends EntityLivingBase> Overlay<T> plain(final ResourceLocation texture) {
        return new Overlay<T>() {
            @Override
            public ResourceLocation texture(T entity) {
                return texture;
            }

            @Override
            public float[] color(T entity, float partialTicks) {
                return WHITE;
            }
        };
    }

    @Override
    public void doRenderLayer(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch, float scale) {
        if (entity.isInvisible()) {
            return;
        }
        ResourceLocation texture = overlay.texture(entity);
        if (texture == null) {
            return;
        }
        float[] rgb = overlay.color(entity, partialTicks);
        renderer.bindTexture(texture);
        GlStateManager.color(rgb[0], rgb[1], rgb[2], 1.0F);
        if (translucent) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        model.setModelAttributes(renderer.getMainModel());
        model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (translucent) {
            GlStateManager.disableBlend();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
}
