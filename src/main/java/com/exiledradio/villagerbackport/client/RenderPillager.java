package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.illager.EntityPillager;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;

/** 1.14's pillager renderer: the illager scale, and the crossbow drawn in its hand. */
public final class RenderPillager extends RenderLiving<EntityPillager> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/illager/pillager.png");

    public RenderPillager(RenderManager manager) {
        super(manager, new ModelPillager(), 0.5F);
        addLayer(new LayerHeldItem(this) {
            @Override
            protected void translateToHand(EnumHandSide side) {
                ((ModelPillager) this.livingEntityRenderer.getMainModel()).getArm(side).postRender(0.0625F);
            }
        });
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPillager entity) {
        return TEXTURE;
    }

    /** Illagers are drawn at fifteen sixteenths, as vanilla's are. */
    @Override
    protected void preRenderCallback(EntityPillager entity, float partialTicks) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
