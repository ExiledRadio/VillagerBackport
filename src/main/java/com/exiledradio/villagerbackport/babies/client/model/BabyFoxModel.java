package com.exiledradio.villagerbackport.babies.client.model;

import com.exiledradio.villagerbackport.fox.EntityFox;

/** 26.3's {@code BabyFoxModel} with {@code FoxModel.setupAnim}. */
public final class BabyFoxModel extends BabyModel<EntityFox> {

    public final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;
    private final KeyframeAnimation walk;
    private float legMotionPos;

    public BabyFoxModel() {
        super(createBodyLayer());
        head = root.getChild("head");
        body = root.getChild("body");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        tail = body.getChild("tail");
        walk = FoxBabyAnimation.FOX_BABY_WALK.bake(root);
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -2.125F, -5.125F, 6.0F, 5.0F, 5.0F)
                        .texOffs(18, 20).addBox(-1.0F, 0.875F, -7.125F, 2.0F, 2.0F, 2.0F)
                        .texOffs(22, 8).addBox(-3.0F, -4.125F, -4.125F, 2.0F, 2.0F, 1.0F)
                        .texOffs(22, 11).addBox(1.0F, -4.125F, -4.125F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 18.125F, 0.125F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(22, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-1.5F, 22.0F, 4.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(1.5F, 22.0F, 4.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(22, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-1.5F, 22.0F, 0.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(1.5F, 22.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 10).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 6.0F), PartPose.offset(0.0F, 20.0F, 2.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 20).addBox(-1.5F, -1.48F, -1.0F, 3.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, -0.5F, 3.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void setupAnim(EntityFox fox, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        float crouchAmount = fox.getCrouchAmount(partialTicks);
        // setWalkingPose
        head.zRot = fox.getHeadRollAngle(partialTicks);
        rightHindLeg.visible = true;
        leftHindLeg.visible = true;
        rightFrontLeg.visible = true;
        leftFrontLeg.visible = true;
        walk.applyWalk(pos, speed, 1.0F, 2.5F);
        boolean sleeping = fox.isSleeping();
        if (fox.isCrouching()) {
            body.xRot += 0.10471976F;
            head.y += crouchAmount * AGE_SCALE;
            float wiggle = cos(ageInTicks) * 0.05F;
            body.yRot = wiggle;
            rightHindLeg.zRot = wiggle;
            leftHindLeg.zRot = wiggle;
            rightFrontLeg.zRot = wiggle / 2.0F;
            leftFrontLeg.zRot = wiggle / 2.0F;
            body.y += crouchAmount / 6.0F;
        } else if (sleeping) {
            rightHindLeg.visible = false;
            leftHindLeg.visible = false;
            rightFrontLeg.visible = false;
            leftFrontLeg.visible = false;
            body.zRot = (float) (-Math.PI / 2);
            body.xRot = (float) (-Math.PI / 18);
            body.y++;
            body.z--;
            body.x--;
            tail.xRot = -2.1816616F;
            tail.x -= 0.7F;
            tail.z += 0.6F;
            tail.y += 0.9F;
            head.x -= 2.0F;
            head.y += 2.8F;
            head.z -= 4.0F;
            head.yRot = (float) (-Math.PI * 2.0 / 3.0);
            head.zRot = 0.0F;
        } else if (fox.isSitting()) {
            head.xRot = 0.0F;
            head.yRot = 0.0F;
            body.xRot = -0.959931F;
            body.z -= 4.5F * AGE_SCALE;
            body.y += 3.0F * AGE_SCALE;
            tail.y -= 0.6F;
            tail.z -= 2.0F * AGE_SCALE;
            tail.xRot = 0.95993114F;
            head.y -= 0.75F;
            rightFrontLeg.xRot = (float) (-Math.PI / 12);
            leftFrontLeg.xRot = (float) (-Math.PI / 12);
            rightFrontLeg.z--;
            leftFrontLeg.z--;
            rightFrontLeg.x += 0.01F;
            leftFrontLeg.x -= 0.01F;
            rightHindLeg.z -= 3.75F;
            leftHindLeg.z -= 3.75F;
            rightHindLeg.x += 0.01F;
            leftHindLeg.x -= 0.01F;
        }
        boolean faceplanted = fox.isFaceplanted();
        if (!sleeping && !faceplanted && !fox.isCrouching()) {
            head.xRot = xRot * RAD;
            head.yRot = yRot * RAD;
        }
        if (sleeping) {
            head.xRot = 0.0F;
            head.yRot = (float) (-Math.PI * 2.0 / 3.0);
            head.zRot = cos(ageInTicks * 0.027F) / 22.0F;
        }
        if (faceplanted) {
            legMotionPos += 0.67F;
            rightHindLeg.xRot = cos(legMotionPos * 0.4662F) * 0.1F;
            leftHindLeg.xRot = cos(legMotionPos * 0.4662F + (float) Math.PI) * 0.1F;
            rightFrontLeg.xRot = cos(legMotionPos * 0.4662F + (float) Math.PI) * 0.1F;
            leftFrontLeg.xRot = cos(legMotionPos * 0.4662F) * 0.1F;
        }
    }
}
