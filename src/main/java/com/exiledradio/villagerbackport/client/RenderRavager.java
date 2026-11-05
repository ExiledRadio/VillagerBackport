package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.illager.EntityRavager;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/** 1.14's ravager renderer. */
public final class RenderRavager extends RenderLiving<EntityRavager> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/illager/ravager.png");

    public RenderRavager(RenderManager manager) {
        super(manager, new ModelRavager(), 1.1F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityRavager entity) {
        return TEXTURE;
    }
}
