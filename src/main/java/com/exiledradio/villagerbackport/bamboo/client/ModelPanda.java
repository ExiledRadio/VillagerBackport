package com.exiledradio.villagerbackport.bamboo.client;

import com.exiledradio.villagerbackport.bamboo.EntityPanda;

import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's panda model, box for box: a 13x10x9 head with its muzzle and ears, a 19x26x13 body turned
 * on its side, and four 6x9x6 legs, on a 64x64 texture.
 *
 * <p>The poses - head shaking when unhappy, the sneeze, sitting up to eat, cowering, lying on its
 * back and rolling - are 1.14's {@code setupAnim}, numbers included. The whole-body turns that go
 * with sitting, lying back and rolling are the renderer's: see {@link RenderPanda}.
 */
@SideOnly(Side.CLIENT)
public class ModelPanda extends ModelQuadruped {

    private float sitAmount;
    private float lieOnBackAmount;
    private float rollAmount;

    public ModelPanda() {
        super(9, 0.0F);
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.head = new ModelRenderer(this, 0, 6);
        this.head.addBox(-6.5F, -5.0F, -4.0F, 13, 10, 9);
        this.head.setRotationPoint(0.0F, 11.5F, -17.0F);
        this.head.setTextureOffset(45, 16).addBox(-3.5F, 0.0F, -6.0F, 7, 5, 2);
        this.head.setTextureOffset(52, 25).addBox(-8.5F, -8.0F, -1.0F, 5, 4, 1);
        this.head.setTextureOffset(52, 25).addBox(3.5F, -8.0F, -1.0F, 5, 4, 1);

        this.body = new ModelRenderer(this, 0, 25);
        this.body.addBox(-9.5F, -13.0F, -6.5F, 19, 26, 13);
        this.body.setRotationPoint(0.0F, 10.0F, 0.0F);

        this.leg1 = leg(-5.5F, 9.0F);
        this.leg2 = leg(5.5F, 9.0F);
        this.leg3 = leg(-5.5F, -9.0F);
        this.leg4 = leg(5.5F, -9.0F);
    }

    /**
     * 1.14.4's {@code PandaModel.render} (read from its client jar): a baby's head is scaled by 0.5555555 and moved by
     * (0, 23 * scale, 0.3), its body and legs scaled to 1/3 and moved down by 49 * scale. 1.12's {@code ModelQuadruped}
     * would use its defaults instead (full-size head moved by (0, 8, 4), body at 1/2), which left a baby's head
     * floating in front of its body.
     */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        if (!this.isChild) {
            super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5555555F, 0.5555555F, 0.5555555F);
        GlStateManager.translate(0.0F, 23.0F * scale, 0.3F);
        this.head.render(scale);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.33333334F, 0.33333334F, 0.33333334F);
        GlStateManager.translate(0.0F, 49.0F * scale, 0.0F);
        this.body.render(scale);
        this.leg1.render(scale);
        this.leg2.render(scale);
        this.leg3.render(scale);
        this.leg4.render(scale);
        GlStateManager.popMatrix();
    }

    private ModelRenderer leg(float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, 40, 0);
        leg.addBox(-3.0F, 0.0F, -3.0F, 6, 9, 6);
        leg.setRotationPoint(x, 15.0F, z);
        return leg;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                    float partialTicks) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        EntityPanda panda = (EntityPanda) entity;
        sitAmount = panda.getSitAmount(partialTicks);
        lieOnBackAmount = panda.getLieOnBackAmount(partialTicks);
        rollAmount = panda.isChild() ? 0.0F : panda.getRollAmount(partialTicks);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        EntityPanda panda = (EntityPanda) entity;
        boolean unhappy = panda.getUnhappyCounter() > 0;
        boolean sneezing = panda.isSneezing();
        int sneezeTicks = panda.getSneezeCounter();
        boolean eating = panda.isEating();
        boolean scared = panda.isScared();

        // leg1/leg2 are the back legs and leg3/leg4 the front, right then left, as in 1.14.
        if (unhappy) {
            head.rotateAngleY = 0.35F * MathHelper.sin(0.6F * ageInTicks);
            head.rotateAngleZ = 0.35F * MathHelper.sin(0.6F * ageInTicks);
            leg3.rotateAngleX = -0.75F * MathHelper.sin(0.3F * ageInTicks);
            leg4.rotateAngleX = 0.75F * MathHelper.sin(0.3F * ageInTicks);
        } else {
            head.rotateAngleZ = 0.0F;
        }

        if (sneezing) {
            if (sneezeTicks < 15) {
                head.rotateAngleX = (-(float) Math.PI / 4F) * sneezeTicks / 14.0F;
            } else if (sneezeTicks < 20) {
                float f = (float) ((sneezeTicks - 15) / 5);
                head.rotateAngleX = (-(float) Math.PI / 4F) + ((float) Math.PI / 4F) * f;
            }
        }

        if (sitAmount > 0.0F) {
            body.rotateAngleX = rotlerp(body.rotateAngleX, 1.7407963F, sitAmount);
            head.rotateAngleX = rotlerp(head.rotateAngleX, (float) Math.PI / 2F, sitAmount);
            leg3.rotateAngleZ = -0.27079642F;
            leg4.rotateAngleZ = 0.27079642F;
            leg1.rotateAngleZ = 0.5707964F;
            leg2.rotateAngleZ = -0.5707964F;
            if (eating) {
                head.rotateAngleX = (float) Math.PI / 2F + 0.2F * MathHelper.sin(ageInTicks * 0.6F);
                leg3.rotateAngleX = -0.4F - 0.2F * MathHelper.sin(ageInTicks * 0.6F);
                leg4.rotateAngleX = -0.4F - 0.2F * MathHelper.sin(ageInTicks * 0.6F);
            }
            if (scared) {
                head.rotateAngleX = 2.1707964F;
                leg3.rotateAngleX = -0.9F;
                leg4.rotateAngleX = -0.9F;
            }
        } else {
            leg1.rotateAngleZ = 0.0F;
            leg2.rotateAngleZ = 0.0F;
            leg3.rotateAngleZ = 0.0F;
            leg4.rotateAngleZ = 0.0F;
        }

        if (lieOnBackAmount > 0.0F) {
            leg1.rotateAngleX = -0.6F * MathHelper.sin(ageInTicks * 0.15F);
            leg2.rotateAngleX = 0.6F * MathHelper.sin(ageInTicks * 0.15F);
            leg3.rotateAngleX = 0.3F * MathHelper.sin(ageInTicks * 0.25F);
            leg4.rotateAngleX = -0.3F * MathHelper.sin(ageInTicks * 0.25F);
            head.rotateAngleX = rotlerp(head.rotateAngleX, (float) Math.PI / 2F, lieOnBackAmount);
        }

        if (rollAmount > 0.0F) {
            head.rotateAngleX = rotlerp(head.rotateAngleX, 2.0561945F, rollAmount);
            leg1.rotateAngleX = -0.5F * MathHelper.sin(ageInTicks * 0.5F);
            leg2.rotateAngleX = 0.5F * MathHelper.sin(ageInTicks * 0.5F);
            leg3.rotateAngleX = 0.5F * MathHelper.sin(ageInTicks * 0.5F);
            leg4.rotateAngleX = -0.5F * MathHelper.sin(ageInTicks * 0.5F);
        }
    }

    /** Interpolates an angle the short way round. */
    private static float rotlerp(float from, float to, float amount) {
        float delta = to - from;
        while (delta < -(float) Math.PI) {
            delta += (float) Math.PI * 2F;
        }
        while (delta >= (float) Math.PI) {
            delta -= (float) Math.PI * 2F;
        }
        return from + amount * delta;
    }
}
