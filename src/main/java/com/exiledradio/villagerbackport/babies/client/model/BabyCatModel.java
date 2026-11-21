package com.exiledradio.villagerbackport.babies.client.model;

import com.exiledradio.villagerbackport.cat.EntityCat;

/** 26.3's {@code BabyCatModel}: {@code BabyFelineModel}'s layer and {@code setupAnim}. */
public final class BabyCatModel extends BabyModel<EntityCat> {

    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart head;
    private final ModelPart body;

    public BabyCatModel() {
        super(createBabyLayer());
        head = root.getChild("head");
        body = root.getChild("body");
        tail1 = root.getChild("tail1");
        tail2 = root.getChild("tail2");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
    }

    static LayerDefinition createBabyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -3.0F, -2.875F, 5.0F, 4.0F, 4.0F)
                        .texOffs(18, 0).addBox(-2.0F, -4.0F, -0.875F, 1.0F, 1.0F, 2.0F)
                        .texOffs(24, 0).addBox(1.0F, -4.0F, -0.875F, 1.0F, 1.0F, 2.0F)
                        .texOffs(18, 3).addBox(-1.5F, -1.0F, -3.875F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 20.0F, -3.125F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(18, 18).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(1.0F, 22.0F, -1.5F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(12, 18).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(-1.0F, 22.0F, -1.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(18, 22).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(1.0F, 22.0F, 2.5F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0F, -1.5F, -3.5F, 4.0F, 3.0F, 7.0F),
                PartPose.offset(0.0F, 20.5F, 0.5F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(12, 22).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(-1.0F, 22.0F, 2.5F));
        root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 18).addBox(-0.5F, -0.107F, 0.0849F, 1.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 19.107F, 3.9151F, -0.567232F, 0.0F, 0.0F));
        root.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void setupAnim(EntityCat cat, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        boolean crouching = cat.isSneaking();
        boolean sprinting = cat.isSprinting();
        boolean sitting = cat.isSitting();
        if (crouching) {
            body.y += 1.0F * AGE_SCALE;
            head.y += 2.0F * AGE_SCALE;
            tail1.y += 1.0F * AGE_SCALE;
            tail2.y += -4.0F * AGE_SCALE;
            tail2.z += 2.0F * AGE_SCALE;
            tail1.xRot = (float) (Math.PI / 2);
            tail2.xRot = (float) (Math.PI / 2);
        } else if (sprinting) {
            tail2.y = tail1.y;
            tail2.z += 2.0F * AGE_SCALE;
            tail1.xRot = (float) (Math.PI / 2);
            tail2.xRot = (float) (Math.PI / 2);
        }
        head.xRot = xRot * RAD;
        head.yRot = yRot * RAD;
        if (!sitting) {
            if (sprinting) {
                leftHindLeg.xRot = cos(pos * 0.6662F) * speed;
                rightHindLeg.xRot = cos(pos * 0.6662F + 0.3F) * speed;
                leftFrontLeg.xRot = cos(pos * 0.6662F + (float) Math.PI + 0.3F) * speed;
                rightFrontLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * speed;
                tail2.xRot = 1.7278761F + (float) (Math.PI / 10) * cos(pos) * speed;
            } else {
                leftHindLeg.xRot = cos(pos * 0.6662F) * speed;
                rightHindLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * speed;
                leftFrontLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * speed;
                rightFrontLeg.xRot = cos(pos * 0.6662F) * speed;
                if (!crouching) {
                    tail2.xRot = 1.7278761F + (float) (Math.PI / 4) * cos(pos) * speed;
                } else {
                    tail2.xRot = 1.7278761F + 0.47123894F * cos(pos) * speed;
                }
            }
        } else {
            body.xRot += -0.43633232F;
            body.y++;
            head.z += 0.75F;
            tail1.xRot += 0.5454154F;
            tail1.y += 4.0F;
            tail1.z -= 0.9F;
            leftHindLeg.z -= 0.9F;
            rightHindLeg.z -= 0.9F;
        }
        float lie = cat.getLieDownAmount(partialTicks);
        if (lie > 0.0F) {
            float lieTail = cat.getLieDownAmountTail(partialTicks);
            body.x++;
            head.xRot = rotLerp(lie, head.xRot, (float) (Math.PI / 18));
            head.zRot = rotLerp(lie, head.zRot, (float) (-Math.PI * 5.0 / 12.0));
            head.x++;
            head.y += 0.75F;
            head.z -= 0.5F;
            rightFrontLeg.xRot = (float) (-Math.PI / 4);
            rightFrontLeg.x += 3.5F;
            rightFrontLeg.y -= 0.5F;
            leftFrontLeg.xRot = (float) (-Math.PI / 2);
            leftFrontLeg.x++;
            leftFrontLeg.y--;
            leftFrontLeg.z -= 2.0F;
            rightHindLeg.xRot = (float) (Math.PI * 2.0 / 9.0);
            rightHindLeg.yRot = (float) (Math.PI / 9);
            rightHindLeg.zRot = (float) (-Math.PI / 9);
            rightHindLeg.x += 2.5F;
            rightHindLeg.y -= 0.25F;
            rightHindLeg.z += 0.5F;
            leftHindLeg.x++;
            leftHindLeg.z--;
            tail1.xRot = tail1.xRot + rotLerp(lieTail, tail1.xRot, (float) (-Math.PI / 6));
            tail1.yRot = tail1.yRot + rotLerp(lieTail, tail1.yRot, 0.0F);
            tail1.zRot = tail1.zRot + rotLerp(lieTail, tail1.zRot, (float) (-Math.PI / 18));
            tail1.x++;
            tail1.y += 0.5F;
            tail1.z -= 0.25F;
        }
        float relax = cat.getRelaxAmount(partialTicks);
        if (relax > 0.0F) {
            head.xRot = rotLerp(relax, head.xRot, -0.58177644F);
        }
    }

    /** {@code Mth.rotLerp}, which wraps in degrees: 26.3's cat hands it radians, so it is a plain lerp in practice. */
    static float rotLerp(float delta, float from, float to) {
        return from + delta * net.minecraft.util.math.MathHelper.wrapDegrees(to - from);
    }
}
