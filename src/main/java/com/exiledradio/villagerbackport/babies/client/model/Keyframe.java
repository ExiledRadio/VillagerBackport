package com.exiledradio.villagerbackport.babies.client.model;

/** 26.3's {@code Keyframe}. */
public final class Keyframe {

    final float timestamp;
    final float[] preTarget;
    final float[] postTarget;
    final AnimationChannel.Interpolation interpolation;

    public Keyframe(float timestamp, float[] target, AnimationChannel.Interpolation interpolation) {
        this.timestamp = timestamp;
        this.preTarget = target;
        this.postTarget = target;
        this.interpolation = interpolation;
    }
}
