package com.exiledradio.villagerbackport.babies.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/**
 * A 26.3 baby model as a 1.12 {@link ModelBase}: rendered at full size (no 1.12 child shrinking), the pose reset
 * every frame ({@code Model.setupAnim}) before the subclass's {@code setupAnim} port.
 */
public abstract class BabyModel<T extends EntityLivingBase> extends ModelBase {

    /** 26.3's {@code getAgeScale()} for babies, which some poses scale by. */
    protected static final float AGE_SCALE = 0.5F;
    protected static final float RAD = (float) (Math.PI / 180.0D);

    protected final ModelPart root;
    protected float partialTicks;

    protected BabyModel(LayerDefinition layer) {
        this.root = layer.bakeRoot();
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        this.partialTicks = partialTicks;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        root.render(scale);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scale, Entity entity) {
        root.resetPoseAll();
        setupAnim((T) entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    /**
     * @param pos 26.3's {@code walkAnimationPos} (1.12's limb swing, tripled for babies as 26.3 does)
     * @param speed {@code walkAnimationSpeed}
     * @param yRot head yaw relative to the body, degrees
     * @param xRot head pitch, degrees
     */
    protected abstract void setupAnim(T entity, float pos, float speed, float ageInTicks, float yRot, float xRot);

    /** 26.3's {@code QuadrupedModel} walk and head look. */
    protected static void quadruped(ModelPart head, ModelPart rightHindLeg, ModelPart leftHindLeg, ModelPart rightFrontLeg,
            ModelPart leftFrontLeg, float pos, float speed, float yRot, float xRot) {
        head.xRot = xRot * RAD;
        head.yRot = yRot * RAD;
        rightHindLeg.xRot = cos(pos * 0.6662F) * 1.4F * speed;
        leftHindLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
        rightFrontLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
        leftFrontLeg.xRot = cos(pos * 0.6662F) * 1.4F * speed;
    }

    protected static float cos(float f) {
        return MathHelper.cos(f);
    }

    protected static float sin(float f) {
        return MathHelper.sin(f);
    }

    protected static float lerp(float delta, float a, float b) {
        return a + delta * (b - a);
    }

    protected static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}
