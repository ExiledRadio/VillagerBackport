package com.exiledradio.villagerbackport.babies.client.model;

import com.exiledradio.villagerbackport.bamboo.EntityPanda;

/** 26.3's {@code BabyPandaModel} with {@code PandaModel.setupAnim}. Babies never roll there ({@code rollAmount} 0). */
public final class BabyPandaModel extends BabyModel<EntityPanda> {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public BabyPandaModel() {
        super(createBodyLayer());
        head = root.getChild("head");
        body = root.getChild("body");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 11).addBox(-4.5F, -3.5F, -5.5F, 9.0F, 7.0F, 11.0F),
                PartPose.offset(0.0F, 18.5F, 2.5F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -3.0F, -5.0F, 7.0F, 6.0F, 5.0F)
                        .texOffs(24, 6).addBox(-2.0F, 1.0F, -6.0F, 4.0F, 2.0F, 1.0F)
                        .texOffs(24, 0).addBox(-4.5F, -4.0F, -3.5F, 3.0F, 3.0F, 1.0F)
                        .texOffs(33, 0).addBox(1.5F, -4.0F, -3.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 19.0F, -3.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-3.0F, 22.0F, 6.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(3.0F, 22.0F, 6.5F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 29).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-3.0F, 22.0F, -1.5F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 29).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(3.0F, 22.0F, -1.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected void setupAnim(EntityPanda panda, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        quadruped(head, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg, pos, speed, yRot, xRot);
        if (panda.getUnhappyCounter() > 0) {
            head.yRot = 0.35F * sin(0.6F * ageInTicks);
            head.zRot = 0.35F * sin(0.6F * ageInTicks);
            rightFrontLeg.xRot = -0.75F * sin(0.3F * ageInTicks);
            leftFrontLeg.xRot = 0.75F * sin(0.3F * ageInTicks);
        } else {
            head.zRot = 0.0F;
        }
        if (panda.isSneezing()) {
            int sneezeTime = panda.getSneezeCounter();
            if (sneezeTime < 15) {
                head.xRot = (float) (-Math.PI / 4) * sneezeTime / 14.0F;
            } else if (sneezeTime < 20) {
                // 26.3 divides as ints: 0 until the twentieth tick.
                float internal = (float) ((sneezeTime - 15) / 5);
                head.xRot = (float) (-Math.PI / 4) + (float) (Math.PI / 4) * internal;
            }
        }
        float sit = panda.getSitAmount(partialTicks);
        if (sit > 0.0F) {
            // BabyPandaModel.animateSitting
            body.xRot = rotLerpRad(sit, body.xRot, (float) (Math.PI / 18));
            body.z = lerp(sit, body.z, -1.5F);
            head.z = lerp(sit, head.z, -11.5F);
            head.y = lerp(sit, head.y, 17.5F);
            rightFrontLeg.z = lerp(sit, rightFrontLeg.z, -5.0F);
            leftFrontLeg.z = lerp(sit, leftFrontLeg.z, -5.0F);
            rightHindLeg.z = lerp(sit, rightHindLeg.z, 3.0F);
            leftHindLeg.z = lerp(sit, leftHindLeg.z, 3.0F);
            rightFrontLeg.zRot = -0.27079642F;
            leftFrontLeg.zRot = 0.27079642F;
            rightHindLeg.zRot = 0.5707964F;
            leftHindLeg.zRot = -0.5707964F;
            if (panda.isEating()) {
                head.xRot = (float) (Math.PI / 2) + 0.2F * sin(ageInTicks * 0.6F);
                rightFrontLeg.xRot = -0.4F - 0.2F * sin(ageInTicks * 0.6F);
                leftFrontLeg.xRot = -0.4F - 0.2F * sin(ageInTicks * 0.6F);
            }
            if (panda.isScared()) {
                head.xRot = 2.1707964F;
                rightFrontLeg.xRot = -0.9F;
                leftFrontLeg.xRot = -0.9F;
            }
        } else {
            rightHindLeg.zRot = 0.0F;
            leftHindLeg.zRot = 0.0F;
            rightFrontLeg.zRot = 0.0F;
            leftFrontLeg.zRot = 0.0F;
        }
        float back = panda.getLieOnBackAmount(partialTicks);
        if (back > 0.0F) {
            rightHindLeg.xRot = -0.6F * sin(ageInTicks * 0.15F);
            leftHindLeg.xRot = 0.6F * sin(ageInTicks * 0.15F);
            rightFrontLeg.xRot = 0.3F * sin(ageInTicks * 0.25F);
            leftFrontLeg.xRot = -0.3F * sin(ageInTicks * 0.25F);
            head.xRot = rotLerpRad(back, head.xRot, (float) (Math.PI / 2));
        }
    }

    /** {@code Mth.rotLerpRad}. */
    private static float rotLerpRad(float delta, float from, float to) {
        float diff = to - from;
        while (diff < -(float) Math.PI) {
            diff += (float) (Math.PI * 2);
        }
        while (diff >= (float) Math.PI) {
            diff -= (float) (Math.PI * 2);
        }
        return from + delta * diff;
    }
}
