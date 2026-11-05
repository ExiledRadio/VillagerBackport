package com.exiledradio.villagerbackport.trader.client;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.model.ModelLlama;
import net.minecraft.client.renderer.entity.RenderLlama;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A llama, drawn as vanilla draws one, plus 1.14's trader decor whenever it wears no carpet.
 *
 * <p>1.14 folds this into {@code LlamaDecorLayer}: the carpet's color if there is one, the trader's
 * decor otherwise. Vanilla's layer here already draws the carpet, so this one only fills the other case.
 */
@SideOnly(Side.CLIENT)
public final class RenderTraderLlama extends RenderLlama {

    private static final ResourceLocation DECOR =
            new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/llama/decor/trader_llama.png");

    public RenderTraderLlama(RenderManager manager) {
        super(manager);
        addLayer(new Decor());
    }

    private final class Decor implements LayerRenderer<EntityLlama> {

        /** Slightly inflated, as vanilla's decor model is, so it sits over the coat. */
        private final ModelLlama model = new ModelLlama(0.5F);

        @Override
        public void doRenderLayer(EntityLlama llama, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (llama.hasColor()) {
                return;
            }
            bindTexture(DECOR);
            this.model.setModelAttributes(getMainModel());
            this.model.render(llama, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
