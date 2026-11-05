package com.exiledradio.villagerbackport.cat.client;

import com.exiledradio.villagerbackport.cat.EntityCat;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's cat model: 1.12.2's ocelot model, which it is built on, plus the cat's own sitting pose and
 * the lying-down and settling poses 1.14 added.
 *
 * <p>It is a copy rather than a subclass because 1.12.2's ocelot model casts the entity it animates
 * to an ocelot. The geometry, texture layout and walking, creeping and running animations are that
 * model's; the sitting pose is 1.14's (front legs at 16.1 rather than 15.8); the lying pose and the
 * head-lowered "relax" pose are 1.14's {@code CatModel}, blended in by amounts the cat keeps
 * ({@link EntityCat#getLieDownAmount} and friends).
 *
 * <p>{@code inflate} grows every box, for the collar layer drawn just over the body.
 */
@SideOnly(Side.CLIENT)
public class ModelCat extends ModelBase {

    private final ModelRenderer backLegL;
    private final ModelRenderer backLegR;
    private final ModelRenderer frontLegL;
    private final ModelRenderer frontLegR;
    private final ModelRenderer tail1;
    private final ModelRenderer tail2;
    private final ModelRenderer head;
    private final ModelRenderer body;

    /** 0 creeping, 1 walking, 2 running, 3 sitting - the ocelot model's states. */
    private int state = 1;

    private float lieDown;
    private float lieDownTail;
    private float relax;

    public ModelCat(float inflate) {
        this.head = new ModelRenderer(this, 0, 0);
        this.head.cubeList.add(new ModelBox(this.head, 0, 0, -2.5F, -2.0F, -3.0F, 5, 4, 5, inflate));
        this.head.cubeList.add(new ModelBox(this.head, 0, 24, -1.5F, 0.0F, -4.0F, 3, 2, 2, inflate));
        this.head.cubeList.add(new ModelBox(this.head, 0, 10, -2.0F, -3.0F, 0.0F, 1, 1, 2, inflate));
        this.head.cubeList.add(new ModelBox(this.head, 6, 10, 1.0F, -3.0F, 0.0F, 1, 1, 2, inflate));
        this.head.setRotationPoint(0.0F, 15.0F, -9.0F);

        this.body = new ModelRenderer(this, 20, 0);
        this.body.addBox(-2.0F, 3.0F, -8.0F, 4, 16, 6, inflate);
        this.body.setRotationPoint(0.0F, 12.0F, -10.0F);

        this.tail1 = new ModelRenderer(this, 0, 15);
        this.tail1.addBox(-0.5F, 0.0F, 0.0F, 1, 8, 1, inflate);
        this.tail1.rotateAngleX = 0.9F;
        this.tail1.setRotationPoint(0.0F, 15.0F, 8.0F);

        this.tail2 = new ModelRenderer(this, 4, 15);
        this.tail2.addBox(-0.5F, 0.0F, 0.0F, 1, 8, 1, inflate);
        this.tail2.setRotationPoint(0.0F, 20.0F, 14.0F);

        this.backLegL = new ModelRenderer(this, 8, 13);
        this.backLegL.addBox(-1.0F, 0.0F, 1.0F, 2, 6, 2, inflate);
        this.backLegL.setRotationPoint(1.1F, 18.0F, 5.0F);

        this.backLegR = new ModelRenderer(this, 8, 13);
        this.backLegR.addBox(-1.0F, 0.0F, 1.0F, 2, 6, 2, inflate);
        this.backLegR.setRotationPoint(-1.1F, 18.0F, 5.0F);

        this.frontLegL = new ModelRenderer(this, 40, 0);
        this.frontLegL.addBox(-1.0F, 0.0F, 0.0F, 2, 10, 2, inflate);
        this.frontLegL.setRotationPoint(1.2F, 13.8F, -5.0F);

        this.frontLegR = new ModelRenderer(this, 40, 0);
        this.frontLegR.addBox(-1.0F, 0.0F, 0.0F, 2, 10, 2, inflate);
        this.frontLegR.setRotationPoint(-1.2F, 13.8F, -5.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(0.0F, 10.0F * scale, 4.0F * scale);
            this.head.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            renderBody(scale);
            GlStateManager.popMatrix();
        } else {
            this.head.render(scale);
            renderBody(scale);
        }
    }

    private void renderBody(float scale) {
        this.body.render(scale);
        this.backLegL.render(scale);
        this.backLegR.render(scale);
        this.frontLegL.render(scale);
        this.frontLegR.render(scale);
        this.tail1.render(scale);
        this.tail2.render(scale);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase living, float limbSwing, float limbSwingAmount, float partialTicks) {
        EntityCat cat = (EntityCat) living;
        this.lieDown = cat.getLieDownAmount(partialTicks);
        this.lieDownTail = cat.getLieDownAmountTail(partialTicks);
        this.relax = cat.getRelaxAmount(partialTicks);

        // 1.14: the lying pose writes rotations the other poses never reset, so they are reset here.
        if (this.lieDown <= 0.0F) {
            this.head.rotateAngleX = 0.0F;
            this.head.rotateAngleZ = 0.0F;
            this.frontLegL.rotateAngleX = 0.0F;
            this.frontLegL.rotateAngleZ = 0.0F;
            this.frontLegR.rotateAngleX = 0.0F;
            this.frontLegR.rotateAngleZ = 0.0F;
            this.frontLegR.rotationPointX = -1.2F;
            this.backLegL.rotateAngleX = 0.0F;
            this.backLegR.rotateAngleX = 0.0F;
            this.backLegR.rotateAngleZ = 0.0F;
            this.backLegR.rotationPointX = -1.1F;
            this.backLegR.rotationPointY = 18.0F;
        }

        // The ocelot model's own pose setup.
        this.body.rotationPointY = 12.0F;
        this.body.rotationPointZ = -10.0F;
        this.head.rotationPointY = 15.0F;
        this.head.rotationPointZ = -9.0F;
        this.tail1.rotationPointY = 15.0F;
        this.tail1.rotationPointZ = 8.0F;
        this.tail2.rotationPointY = 20.0F;
        this.tail2.rotationPointZ = 14.0F;
        this.frontLegL.rotationPointY = 13.8F;
        this.frontLegL.rotationPointZ = -5.0F;
        this.frontLegR.rotationPointY = 13.8F;
        this.frontLegR.rotationPointZ = -5.0F;
        this.backLegL.rotationPointY = 18.0F;
        this.backLegL.rotationPointZ = 5.0F;
        this.backLegR.rotationPointY = 18.0F;
        this.backLegR.rotationPointZ = 5.0F;
        this.tail1.rotateAngleX = 0.9F;

        if (cat.isSneaking()) {
            this.body.rotationPointY += 1.0F;
            this.head.rotationPointY += 2.0F;
            this.tail1.rotationPointY += 1.0F;
            this.tail2.rotationPointY += -4.0F;
            this.tail2.rotationPointZ += 2.0F;
            this.tail1.rotateAngleX = ((float) Math.PI / 2F);
            this.tail2.rotateAngleX = ((float) Math.PI / 2F);
            this.state = 0;
        } else if (cat.isSprinting()) {
            this.tail2.rotationPointY = this.tail1.rotationPointY;
            this.tail2.rotationPointZ += 2.0F;
            this.tail1.rotateAngleX = ((float) Math.PI / 2F);
            this.tail2.rotateAngleX = ((float) Math.PI / 2F);
            this.state = 2;
        } else {
            this.state = 1;
        }

        if (cat.isSitting()) {
            this.body.rotateAngleX = ((float) Math.PI / 4F);
            this.body.rotationPointY += -4.0F;
            this.body.rotationPointZ += 5.0F;
            this.head.rotationPointY += -3.3F;
            this.head.rotationPointZ += 1.0F;
            this.tail1.rotationPointY += 8.0F;
            this.tail1.rotationPointZ += -2.0F;
            this.tail2.rotationPointY += 2.0F;
            this.tail2.rotationPointZ += -0.8F;
            this.tail1.rotateAngleX = 1.7278761F;
            this.tail2.rotateAngleX = 2.670354F;
            this.frontLegL.rotateAngleX = -0.15707964F;
            this.frontLegL.rotationPointY = 16.1F;
            this.frontLegL.rotationPointZ = -7.0F;
            this.frontLegR.rotateAngleX = -0.15707964F;
            this.frontLegR.rotationPointY = 16.1F;
            this.frontLegR.rotationPointZ = -7.0F;
            this.backLegL.rotateAngleX = -((float) Math.PI / 2F);
            this.backLegL.rotationPointY = 21.0F;
            this.backLegL.rotationPointZ = 1.0F;
            this.backLegR.rotateAngleX = -((float) Math.PI / 2F);
            this.backLegR.rotationPointY = 21.0F;
            this.backLegR.rotationPointZ = 1.0F;
            this.state = 3;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;

        if (this.state != 3) {
            this.body.rotateAngleX = ((float) Math.PI / 2F);
            if (this.state == 2) {
                this.backLegL.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * limbSwingAmount;
                this.backLegR.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + 0.3F) * limbSwingAmount;
                this.frontLegL.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI + 0.3F) * limbSwingAmount;
                this.frontLegR.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
                this.tail2.rotateAngleX = 1.7278761F + ((float) Math.PI / 10F) * MathHelper.cos(limbSwing) * limbSwingAmount;
            } else {
                this.backLegL.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * limbSwingAmount;
                this.backLegR.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
                this.frontLegL.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
                this.frontLegR.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * limbSwingAmount;
                if (this.state == 1) {
                    this.tail2.rotateAngleX = 1.7278761F + ((float) Math.PI / 4F) * MathHelper.cos(limbSwing) * limbSwingAmount;
                } else {
                    this.tail2.rotateAngleX = 1.7278761F + 0.47123894F * MathHelper.cos(limbSwing) * limbSwingAmount;
                }
            }
        }

        // 1.14's CatModel.setupAnim: lying on its side, and the settling head tilt.
        if (this.lieDown > 0.0F) {
            this.head.rotateAngleZ = lerpRotation(this.head.rotateAngleZ, -1.2707963F, this.lieDown);
            this.head.rotateAngleY = lerpRotation(this.head.rotateAngleY, 1.2707963F, this.lieDown);
            this.frontLegL.rotateAngleX = -1.2707963F;
            this.frontLegR.rotateAngleX = -0.47079635F;
            this.frontLegR.rotateAngleZ = -0.2F;
            this.frontLegR.rotationPointX = -0.2F;
            this.backLegL.rotateAngleX = -0.4F;
            this.backLegR.rotateAngleX = 0.5F;
            this.backLegR.rotateAngleZ = -0.5F;
            this.backLegR.rotationPointX = -0.3F;
            this.backLegR.rotationPointY = 20.0F;
            this.tail1.rotateAngleX = lerpRotation(this.tail1.rotateAngleX, 0.8F, this.lieDownTail);
            this.tail2.rotateAngleX = lerpRotation(this.tail2.rotateAngleX, -0.4F, this.lieDownTail);
        }
        if (this.relax > 0.0F) {
            this.head.rotateAngleX = lerpRotation(this.head.rotateAngleX, -0.58177644F, this.relax);
        }
    }

    /** The shortest way round from one angle to another, a fraction of the way. */
    private static float lerpRotation(float from, float to, float amount) {
        float diff = to - from;
        while (diff < -(float) Math.PI) {
            diff += ((float) Math.PI * 2F);
        }
        while (diff >= (float) Math.PI) {
            diff -= ((float) Math.PI * 2F);
        }
        return from + amount * diff;
    }
}
