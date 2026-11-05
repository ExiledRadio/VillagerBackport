package com.exiledradio.villagerbackport.fox.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.fox.EntityFox;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a fox: red or snow, with its eyes shut while asleep, tipped forward through a pounce, and
 * holding whatever is in its mouth.
 */
@SideOnly(Side.CLIENT)
public class RenderFox extends RenderLiving<EntityFox> {

    private static final ResourceLocation RED = texture("fox");
    private static final ResourceLocation RED_SLEEP = texture("fox_sleep");
    private static final ResourceLocation SNOW = texture("snow_fox");
    private static final ResourceLocation SNOW_SLEEP = texture("snow_fox_sleep");

    public RenderFox(RenderManager manager) {
        super(manager, new ModelFox(), 0.4F);
        addLayer(new LayerFoxHeldItem(this));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/fox/" + name + ".png");
    }

    @Override
    protected void applyRotations(EntityFox fox, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(fox, ageInTicks, rotationYaw, partialTicks);
        if (fox.isPouncing() || fox.isFaceplanted()) {
            float pitch = fox.prevRotationPitch + (fox.rotationPitch - fox.prevRotationPitch) * partialTicks;
            GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityFox fox) {
        if (fox.getFoxType() == EntityFox.Type.RED) {
            return fox.isSleeping() ? RED_SLEEP : RED;
        }
        return fox.isSleeping() ? SNOW_SLEEP : SNOW;
    }
}
