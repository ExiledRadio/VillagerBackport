package com.exiledradio.villagerbackport.babies.client.model;

/** 26.3's {@code PartPose}: offset (1.12's rotation point), rotations in radians, and scale. */
public final class PartPose {

    public static final PartPose ZERO = offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public final float x;
    public final float y;
    public final float z;
    public final float xRot;
    public final float yRot;
    public final float zRot;
    public final float xScale;
    public final float yScale;
    public final float zScale;

    public PartPose(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    public static PartPose offset(float x, float y, float z) {
        return offsetAndRotation(x, y, z, 0.0F, 0.0F, 0.0F);
    }

    public static PartPose rotation(float x, float y, float z) {
        return offsetAndRotation(0.0F, 0.0F, 0.0F, x, y, z);
    }

    public static PartPose offsetAndRotation(float x, float y, float z, float xRot, float yRot, float zRot) {
        return new PartPose(x, y, z, xRot, yRot, zRot, 1.0F, 1.0F, 1.0F);
    }
}
