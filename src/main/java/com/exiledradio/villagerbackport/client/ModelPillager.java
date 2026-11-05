package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.illager.EntityPillager;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14's {@code PillagerModel}: the illager, arms always free, with 1.14's two crossbow poses -
 * held up to the eye, and drawn back while loading.
 */
public final class ModelPillager extends ModelBase {

    private final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;
    private final ModelRenderer rightArm;
    private final ModelRenderer leftArm;

    public ModelPillager() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.head.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, 0.0F);
        ModelRenderer nose = new ModelRenderer(this);
        nose.setRotationPoint(0.0F, -2.0F, 0.0F);
        nose.setTextureOffset(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2, 0.0F);
        this.head.addChild(nose);

        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.body.setTextureOffset(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6, 0.0F);
        this.body.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, 0.5F);

        this.rightLeg = new ModelRenderer(this, 0, 22);
        this.rightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
        this.rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
        this.leftLeg = new ModelRenderer(this, 0, 22);
        this.leftLeg.mirror = true;
        this.leftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
        this.leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);

        this.rightArm = new ModelRenderer(this, 40, 46);
        this.rightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F);
        this.rightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        this.leftArm = new ModelRenderer(this, 40, 46);
        this.leftArm.mirror = true;
        this.leftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.0F);
        this.leftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.head.render(scale);
        this.body.render(scale);
        this.rightLeg.render(scale);
        this.leftLeg.render(scale);
        this.rightArm.render(scale);
        this.leftArm.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        this.head.rotateAngleX = headPitch * 0.017453292F;

        if (this.isRiding) {
            this.rightArm.rotateAngleX = -(float) Math.PI / 5F;
            this.rightArm.rotateAngleY = 0.0F;
            this.rightArm.rotateAngleZ = 0.0F;
            this.leftArm.rotateAngleX = -(float) Math.PI / 5F;
            this.leftArm.rotateAngleY = 0.0F;
            this.leftArm.rotateAngleZ = 0.0F;
            this.rightLeg.rotateAngleX = -1.4137167F;
            this.rightLeg.rotateAngleY = (float) Math.PI / 10F;
            this.rightLeg.rotateAngleZ = 0.07853982F;
            this.leftLeg.rotateAngleX = -1.4137167F;
            this.leftLeg.rotateAngleY = -(float) Math.PI / 10F;
            this.leftLeg.rotateAngleZ = -0.07853982F;
        } else {
            this.rightArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
            this.rightArm.rotateAngleY = 0.0F;
            this.rightArm.rotateAngleZ = 0.0F;
            this.leftArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
            this.leftArm.rotateAngleY = 0.0F;
            this.leftArm.rotateAngleZ = 0.0F;
            this.rightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
            this.rightLeg.rotateAngleY = 0.0F;
            this.rightLeg.rotateAngleZ = 0.0F;
            this.leftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
            this.leftLeg.rotateAngleY = 0.0F;
            this.leftLeg.rotateAngleZ = 0.0F;
        }

        if (!(entity instanceof EntityPillager)) {
            return;
        }
        EntityPillager pillager = (EntityPillager) entity;

        if (pillager.isCharging()) {
            // CROSSBOW_CHARGE: the left hand drawn back over the load.
            this.rightArm.rotateAngleY = -0.8F;
            this.rightArm.rotateAngleX = -0.97079635F;
            float progress = MathHelper.clamp(pillager.clientChargeTicks(), 0.0F, 25.0F) / 25.0F;
            this.leftArm.rotateAngleY = 0.4F + (0.85F - 0.4F) * progress;
            this.leftArm.rotateAngleX = -0.97079635F + (-(float) Math.PI / 2F + 0.97079635F) * progress;
        } else if (pillager.isHoldingCrossbow()) {
            // CROSSBOW_HOLD: up to the eye, following the head.
            this.rightArm.rotateAngleY = -0.3F + this.head.rotateAngleY;
            this.leftArm.rotateAngleY = 0.6F + this.head.rotateAngleY;
            this.rightArm.rotateAngleX = -(float) Math.PI / 2F + this.head.rotateAngleX + 0.1F;
            this.leftArm.rotateAngleX = -1.5F + this.head.rotateAngleX;
        }
    }

    public ModelRenderer getArm(EnumHandSide side) {
        return side == EnumHandSide.LEFT ? this.leftArm : this.rightArm;
    }
}
