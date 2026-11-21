package com.exiledradio.villagerbackport.babies.client.model;

/** 26.3's {@code KeyframeAnimations} value helpers. */
public final class KeyframeAnimations {

    private static final float RAD = (float) (Math.PI / 180.0D);

    private KeyframeAnimations() {
    }

    public static float[] posVec(float x, float y, float z) {
        return new float[]{x, -y, z};
    }

    public static float[] degreeVec(float x, float y, float z) {
        return new float[]{x * RAD, y * RAD, z * RAD};
    }

    public static float[] scaleVec(double x, double y, double z) {
        return new float[]{(float) (x - 1.0D), (float) (y - 1.0D), (float) (z - 1.0D)};
    }
}
