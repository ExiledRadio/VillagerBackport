package com.exiledradio.villagerbackport.bamboo.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.bamboo.EntityPanda;
import com.exiledradio.villagerbackport.bamboo.PandaGene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.14's panda renderer: one texture per personality, and the whole-body turns of 1.14's
 * {@code setupRotations} - the somersault of a roll in four quarter-turns with a hop between each,
 * sitting up, cowering with a tremble, and lying on its back.
 */
@SideOnly(Side.CLIENT)
public class RenderPanda extends RenderLiving<EntityPanda> {

    private static final Map<PandaGene, ResourceLocation> TEXTURES = new EnumMap<PandaGene, ResourceLocation>(PandaGene.class);

    static {
        TEXTURES.put(PandaGene.NORMAL, texture("panda"));
        TEXTURES.put(PandaGene.LAZY, texture("lazy_panda"));
        TEXTURES.put(PandaGene.WORRIED, texture("worried_panda"));
        TEXTURES.put(PandaGene.PLAYFUL, texture("playful_panda"));
        TEXTURES.put(PandaGene.BROWN, texture("brown_panda"));
        TEXTURES.put(PandaGene.WEAK, texture("weak_panda"));
        TEXTURES.put(PandaGene.AGGRESSIVE, texture("aggressive_panda"));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/panda/" + name + ".png");
    }

    public RenderPanda(RenderManager manager) {
        super(manager, new ModelPanda(), 0.9F);
        addLayer(new HeldItemLayer());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPanda panda) {
        ResourceLocation texture = TEXTURES.get(panda.getPersonality());
        return texture != null ? texture : TEXTURES.get(PandaGene.NORMAL);
    }

    @Override
    protected void applyRotations(EntityPanda panda, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(panda, ageInTicks, rotationYaw, partialTicks);

        if (panda.rollCounter > 0) {
            int i = panda.rollCounter;
            int j = i + 1;
            float lift = panda.isChild() ? 0.3F : 0.8F;
            if (i < 8) {
                float angle = angle(90 * i / 7.0F, 90 * j / 7.0F, j, partialTicks, 8.0F);
                GlStateManager.translate(0.0F, (lift + 0.2F) * (angle / 90.0F), 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            } else if (i < 16) {
                float from = 90.0F + 90.0F * ((i - 8.0F) / 7.0F);
                float to = 90.0F + 90.0F * ((j - 8.0F) / 7.0F);
                float angle = angle(from, to, j, partialTicks, 16.0F);
                GlStateManager.translate(0.0F, lift + 0.2F + (lift - 0.2F) * (angle - 90.0F) / 90.0F, 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            } else if (i < 24) {
                float from = 180.0F + 90.0F * ((i - 16.0F) / 7.0F);
                float to = 180.0F + 90.0F * ((j - 16.0F) / 7.0F);
                float angle = angle(from, to, j, partialTicks, 24.0F);
                GlStateManager.translate(0.0F, lift + lift * (270.0F - angle) / 90.0F, 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            } else if (i < 32) {
                float from = 270.0F + 90.0F * ((i - 24.0F) / 7.0F);
                float to = 270.0F + 90.0F * ((j - 24.0F) / 7.0F);
                float angle = angle(from, to, j, partialTicks, 32.0F);
                GlStateManager.translate(0.0F, lift * ((360.0F - angle) / 90.0F), 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            }
        }

        float sit = panda.getSitAmount(partialTicks);
        if (sit > 0.0F) {
            GlStateManager.translate(0.0F, 0.8F * sit, 0.0F);
            GlStateManager.rotate(lerp(sit, panda.rotationPitch, panda.rotationPitch + 90.0F), 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(0.0F, -1.0F * sit, 0.0F);
            if (panda.isScared()) {
                float tremble = (float) (Math.cos(panda.ticksExisted * 1.25D) * Math.PI * 0.05F);
                GlStateManager.rotate(tremble, 0.0F, 1.0F, 0.0F);
                if (panda.isChild()) {
                    GlStateManager.translate(0.0F, 0.8F, 0.55F);
                }
            }
        }

        float back = panda.getLieOnBackAmount(partialTicks);
        if (back > 0.0F) {
            float lift = panda.isChild() ? 0.5F : 1.3F;
            GlStateManager.translate(0.0F, lift * back, 0.0F);
            GlStateManager.rotate(lerp(back, panda.rotationPitch, panda.rotationPitch + 180.0F), 1.0F, 0.0F, 0.0F);
        }
    }

    private static float angle(float from, float to, int next, float partialTicks, float limit) {
        return next < limit ? lerp(partialTicks, from, to) : from;
    }

    private static float lerp(float amount, float from, float to) {
        return from + amount * (to - from);
    }

    /** What a sitting panda holds, raised to its mouth and bobbing while it eats. */
    private static final class HeldItemLayer implements LayerRenderer<EntityPanda> {

        @Override
        public void doRenderLayer(EntityPanda panda, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            ItemStack held = panda.getHeldItemMainhand();
            if (!panda.isSitting() || held.isEmpty() || panda.isScared()) {
                return;
            }
            float z = -0.6F;
            float y = 1.4F;
            if (panda.isEating()) {
                z -= 0.2F * MathHelper.sin(ageInTicks * 0.6F) + 0.2F;
                y -= 0.09F * MathHelper.sin(ageInTicks * 0.6F);
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.1F, y, z);
            Minecraft.getMinecraft().getItemRenderer().renderItem(panda, held, ItemCameraTransforms.TransformType.GROUND);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
