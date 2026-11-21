package com.exiledradio.villagerbackport.babies.client.model;

/** 26.3's {@code CubeDeformation}: growth per axis (1.12's single {@code inflate}). */
public final class CubeDeformation {

    public static final CubeDeformation NONE = new CubeDeformation(0.0F);

    final float growX;
    final float growY;
    final float growZ;

    public CubeDeformation(float growX, float growY, float growZ) {
        this.growX = growX;
        this.growY = growY;
        this.growZ = growZ;
    }

    public CubeDeformation(float grow) {
        this(grow, grow, grow);
    }

    public CubeDeformation extend(float factor) {
        return new CubeDeformation(growX + factor, growY + factor, growZ + factor);
    }
}
