package com.exiledradio.villagerbackport.fox.client;

import com.exiledradio.villagerbackport.fox.EntityFox;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14.4's fox model, box for box, with its poses for sitting, sleeping, crouching and the
 * face-plant. The numbers are 1.14.4's {@code FoxModel}, read from its bytecode.
 *
 * <p>The cub is drawn as 1.14 draws it: the head at three quarters scale and the body at half, rather
 * than the whole model shrunk.
 */
@SideOnly(Side.CLIENT)
public class ModelFox extends ModelBase {

    public final ModelRenderer head;
    private final ModelRenderer earLeft;
    private final ModelRenderer earRight;
    private final ModelRenderer nose;
    private final ModelRenderer body;
    private final ModelRenderer leg0;
    private final ModelRenderer leg1;
    private final ModelRenderer leg2;
    private final ModelRenderer leg3;
    private final ModelRenderer tail;
    private float legMotionPos;

    public ModelFox() {
        textureWidth = 48;
        textureHeight = 32;

        head = new ModelRenderer(this, 1, 5);
        head.addBox(-3.0F, -2.0F, -5.0F, 8, 6, 6);
        head.setRotationPoint(-1.0F, 16.5F, -3.0F);
        earLeft = new ModelRenderer(this, 8, 1);
        earLeft.addBox(-3.0F, -4.0F, -4.0F, 2, 2, 1);
        earRight = new ModelRenderer(this, 15, 1);
        earRight.addBox(3.0F, -4.0F, -4.0F, 2, 2, 1);
        nose = new ModelRenderer(this, 6, 18);
        nose.addBox(-1.0F, 2.01F, -8.0F, 4, 2, 3);
        head.addChild(earLeft);
        head.addChild(earRight);
        head.addChild(nose);

        body = new ModelRenderer(this, 24, 15);
        body.addBox(-3.0F, 3.999F, -3.5F, 6, 11, 6);
        body.setRotationPoint(0.0F, 16.0F, -6.0F);

        leg0 = new ModelRenderer(this, 13, 24);
        leg0.addBox(2.0F, 0.5F, -1.0F, 2, 6, 2, 0.001F);
        leg0.setRotationPoint(-5.0F, 17.5F, 7.0F);
        leg1 = new ModelRenderer(this, 4, 24);
        leg1.addBox(2.0F, 0.5F, -1.0F, 2, 6, 2, 0.001F);
        leg1.setRotationPoint(-1.0F, 17.5F, 7.0F);
        leg2 = new ModelRenderer(this, 13, 24);
        leg2.addBox(2.0F, 0.5F, -1.0F, 2, 6, 2, 0.001F);
        leg2.setRotationPoint(-5.0F, 17.5F, 0.0F);
        leg3 = new ModelRenderer(this, 4, 24);
        leg3.addBox(2.0F, 0.5F, -1.0F, 2, 6, 2, 0.001F);
        leg3.setRotationPoint(-1.0F, 17.5F, 0.0F);

        tail = new ModelRenderer(this, 30, 0);
        tail.addBox(2.0F, 0.0F, -1.0F, 4, 9, 5);
        tail.setRotationPoint(-4.0F, 15.0F, -1.0F);
        body.addChild(tail);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        EntityFox fox = (EntityFox) entity;
        body.rotateAngleX = (float) Math.PI / 2.0F;
        tail.rotateAngleX = -0.05235988F;
        leg0.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        leg3.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        head.setRotationPoint(-1.0F, 16.5F, -3.0F);
        head.rotateAngleY = 0.0F;
        head.rotateAngleZ = fox.getHeadRollAngle(partialTicks);
        leg0.showModel = true;
        leg1.showModel = true;
        leg2.showModel = true;
        leg3.showModel = true;
        body.setRotationPoint(0.0F, 16.0F, -6.0F);
        body.rotateAngleZ = 0.0F;
        leg0.setRotationPoint(-5.0F, 17.5F, 7.0F);
        leg1.setRotationPoint(-1.0F, 17.5F, 7.0F);

        if (fox.isCrouching()) {
            body.rotateAngleX = 1.6755161F;
            float crouch = fox.getCrouchAmount(partialTicks);
            body.setRotationPoint(0.0F, 16.0F + crouch, -6.0F);
            head.setRotationPoint(-1.0F, 16.5F + crouch, -3.0F);
            head.rotateAngleY = 0.0F;
        } else if (fox.isSleeping()) {
            body.rotateAngleZ = -(float) Math.PI / 2.0F;
            body.setRotationPoint(0.0F, 21.0F, -6.0F);
            tail.rotateAngleX = -2.6179938F;
            if (isChild) {
                tail.rotateAngleX = -2.1816616F;
                body.setRotationPoint(0.0F, 21.0F, -2.0F);
            }
            head.setRotationPoint(1.0F, 19.49F, -3.0F);
            head.rotateAngleX = 0.0F;
            head.rotateAngleY = -2.0943952F;
            head.rotateAngleZ = 0.0F;
            leg0.showModel = false;
            leg1.showModel = false;
            leg2.showModel = false;
            leg3.showModel = false;
        } else if (fox.isSitting()) {
            body.rotateAngleX = 0.5235988F;
            body.setRotationPoint(0.0F, 9.0F, -3.0F);
            tail.rotateAngleX = 0.7853982F;
            tail.setRotationPoint(-4.0F, 15.0F, -2.0F);
            head.setRotationPoint(-1.0F, 10.0F, -0.25F);
            head.rotateAngleX = 0.0F;
            head.rotateAngleY = 0.0F;
            if (isChild) {
                head.setRotationPoint(-1.0F, 13.0F, -3.75F);
            }
            leg0.rotateAngleX = -1.3089969F;
            leg0.setRotationPoint(-5.0F, 21.5F, 6.75F);
            leg1.rotateAngleX = -1.3089969F;
            leg1.setRotationPoint(-1.0F, 21.5F, 6.75F);
            leg2.rotateAngleX = -0.2617994F;
            leg3.rotateAngleX = -0.2617994F;
        }
        if (!fox.isSitting()) {
            tail.setRotationPoint(-4.0F, 15.0F, -1.0F);
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        EntityFox fox = (EntityFox) entity;
        if (!fox.isSleeping() && !fox.isFaceplanted() && !fox.isCrouching()) {
            head.rotateAngleX = headPitch * 0.017453292F;
            head.rotateAngleY = netHeadYaw * 0.017453292F;
        }
        if (fox.isSleeping()) {
            head.rotateAngleX = 0.0F;
            head.rotateAngleY = -2.0943952F;
            head.rotateAngleZ = MathHelper.cos(ageInTicks * 0.027F) / 22.0F;
        }
        if (fox.isCrouching()) {
            float sway = MathHelper.cos(ageInTicks) * 0.01F;
            body.rotateAngleY = sway;
            leg0.rotateAngleZ = sway;
            leg1.rotateAngleZ = sway;
            leg2.rotateAngleZ = sway / 2.0F;
            leg3.rotateAngleZ = sway / 2.0F;
        } else {
            body.rotateAngleY = 0.0F;
            leg0.rotateAngleZ = 0.0F;
            leg1.rotateAngleZ = 0.0F;
            leg2.rotateAngleZ = 0.0F;
            leg3.rotateAngleZ = 0.0F;
        }
        if (fox.isFaceplanted()) {
            legMotionPos += 0.67F;
            leg0.rotateAngleX = MathHelper.cos(legMotionPos * 0.4662F) * 0.1F;
            leg1.rotateAngleX = MathHelper.cos(legMotionPos * 0.4662F + (float) Math.PI) * 0.1F;
            leg2.rotateAngleX = MathHelper.cos(legMotionPos * 0.4662F + (float) Math.PI) * 0.1F;
            leg3.rotateAngleX = MathHelper.cos(legMotionPos * 0.4662F) * 0.1F;
        }
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(0.0F, 8.0F * scale, 3.35F * scale);
            head.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            body.render(scale);
            leg0.render(scale);
            leg1.render(scale);
            leg2.render(scale);
            leg3.render(scale);
            GlStateManager.popMatrix();
        } else {
            GlStateManager.pushMatrix();
            head.render(scale);
            body.render(scale);
            leg0.render(scale);
            leg1.render(scale);
            leg2.render(scale);
            leg3.render(scale);
            GlStateManager.popMatrix();
        }
    }
}
