package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.illager.EntityRavager;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/** 1.14's {@code RavagerModel}, box for box, with its bite, stun and roar animations. */
public final class ModelRavager extends ModelBase {

    private final ModelRenderer head;
    private final ModelRenderer jaw;
    private final ModelRenderer body;
    private final ModelRenderer rightHindLeg;
    private final ModelRenderer leftHindLeg;
    private final ModelRenderer rightFrontLeg;
    private final ModelRenderer leftFrontLeg;
    private final ModelRenderer neck;

    public ModelRavager() {
        this.textureWidth = 128;
        this.textureHeight = 128;

        this.neck = new ModelRenderer(this);
        this.neck.setRotationPoint(0.0F, -7.0F, -1.5F);
        this.neck.setTextureOffset(68, 73).addBox(-5.0F, -1.0F, -18.0F, 10, 10, 18, 0.0F);

        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 16.0F, -17.0F);
        this.head.setTextureOffset(0, 0).addBox(-8.0F, -20.0F, -14.0F, 16, 20, 16, 0.0F);
        this.head.setTextureOffset(0, 0).addBox(-2.0F, -6.0F, -18.0F, 4, 8, 4, 0.0F);

        ModelRenderer rightHorn = new ModelRenderer(this);
        rightHorn.setRotationPoint(-10.0F, -14.0F, -8.0F);
        rightHorn.setTextureOffset(74, 55).addBox(0.0F, -14.0F, -2.0F, 2, 14, 4, 0.0F);
        rightHorn.rotateAngleX = 1.0995574F;
        this.head.addChild(rightHorn);

        ModelRenderer leftHorn = new ModelRenderer(this);
        leftHorn.mirror = true;
        leftHorn.setRotationPoint(8.0F, -14.0F, -8.0F);
        leftHorn.setTextureOffset(74, 55).addBox(0.0F, -14.0F, -2.0F, 2, 14, 4, 0.0F);
        leftHorn.rotateAngleX = 1.0995574F;
        this.head.addChild(leftHorn);

        this.jaw = new ModelRenderer(this);
        this.jaw.setRotationPoint(0.0F, -2.0F, 2.0F);
        this.jaw.setTextureOffset(0, 36).addBox(-8.0F, 0.0F, -16.0F, 16, 3, 16, 0.0F);
        this.head.addChild(this.jaw);
        this.neck.addChild(this.head);

        this.body = new ModelRenderer(this);
        this.body.setTextureOffset(0, 55).addBox(-7.0F, -10.0F, -7.0F, 14, 16, 20, 0.0F);
        this.body.setTextureOffset(0, 91).addBox(-6.0F, 6.0F, -7.0F, 12, 13, 18, 0.0F);
        this.body.setRotationPoint(0.0F, 1.0F, 2.0F);

        this.rightHindLeg = new ModelRenderer(this, 96, 0);
        this.rightHindLeg.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.rightHindLeg.setRotationPoint(-8.0F, -13.0F, 18.0F);
        this.leftHindLeg = new ModelRenderer(this, 96, 0);
        this.leftHindLeg.mirror = true;
        this.leftHindLeg.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.leftHindLeg.setRotationPoint(8.0F, -13.0F, 18.0F);
        this.rightFrontLeg = new ModelRenderer(this, 64, 0);
        this.rightFrontLeg.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.rightFrontLeg.setRotationPoint(-8.0F, -13.0F, -5.0F);
        this.leftFrontLeg = new ModelRenderer(this, 64, 0);
        this.leftFrontLeg.mirror = true;
        this.leftFrontLeg.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.leftFrontLeg.setRotationPoint(8.0F, -13.0F, -5.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.neck.render(scale);
        this.body.render(scale);
        this.rightHindLeg.render(scale);
        this.leftHindLeg.render(scale);
        this.rightFrontLeg.render(scale);
        this.leftFrontLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        this.body.rotateAngleX = (float) Math.PI / 2F;
        float swing = 0.4F * limbSwingAmount;
        this.rightHindLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * swing;
        this.leftHindLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * swing;
        this.rightFrontLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * swing;
        this.leftFrontLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * swing;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTick);
        if (!(entity instanceof EntityRavager)) {
            return;
        }
        EntityRavager ravager = (EntityRavager) entity;
        int stun = ravager.stunTick();
        int roar = ravager.roarTick();
        int attack = ravager.attackTick();

        if (attack > 0) {
            float wave = triangle(attack - partialTick, 10.0F);
            float lunge = (1.0F + wave) * 0.5F;
            float reach = lunge * lunge * lunge * 12.0F;
            float lift = reach * MathHelper.sin(this.neck.rotateAngleX);
            this.neck.rotationPointZ = -6.5F + reach;
            this.neck.rotationPointY = -7.0F - lift;
            if (attack > 5) {
                this.jaw.rotateAngleX = MathHelper.sin((-4 + attack - partialTick) / 4.0F) * (float) Math.PI * 0.4F;
            } else {
                this.jaw.rotateAngleX = 0.15707964F * MathHelper.sin((float) Math.PI * (attack - partialTick) / 10.0F);
            }
        } else {
            float lift = -1.0F * MathHelper.sin(this.neck.rotateAngleX);
            this.neck.rotationPointX = 0.0F;
            this.neck.rotationPointY = -7.0F - lift;
            this.neck.rotationPointZ = 5.5F;
            boolean stunned = stun > 0;
            this.neck.rotateAngleX = stunned ? 0.21991149F : 0.0F;
            this.jaw.rotateAngleX = (float) Math.PI * (stunned ? 0.05F : 0.01F);
            if (stunned) {
                this.neck.rotationPointX = (float) Math.sin(stun / 40.0D * 10.0D) * 3.0F;
            } else if (roar > 0) {
                float open = MathHelper.sin((20 - roar - partialTick) / 20.0F * (float) Math.PI * 0.25F);
                this.jaw.rotateAngleX = (float) Math.PI / 2F * open;
            }
        }
    }

    private static float triangle(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }
}
