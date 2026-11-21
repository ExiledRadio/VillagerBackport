package com.exiledradio.villagerbackport.babies.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 26.3's {@code KeyframeAnimation}: an animation definition bound to a model's parts. */
public final class KeyframeAnimation {

    private final AnimationDefinition definition;
    private final List<Entry> entries = new ArrayList<Entry>();
    private final float[] scratch = new float[3];

    KeyframeAnimation(ModelPart root, AnimationDefinition definition) {
        this.definition = definition;
        Map<String, ModelPart> parts = new LinkedHashMap<String, ModelPart>();
        root.collect("root", parts);
        for (Map.Entry<String, List<AnimationChannel>> bone : definition.boneAnimations.entrySet()) {
            ModelPart part = parts.get(bone.getKey());
            if (part == null) {
                throw new IllegalArgumentException("Cannot animate " + bone.getKey() + ", which does not exist in model");
            }
            for (AnimationChannel channel : bone.getValue()) {
                entries.add(new Entry(part, channel));
            }
        }
    }

    /** Applies the animation if its state is running, at the given age in ticks. */
    public void apply(AnimationState state, float ageInTicks) {
        if (state.isStarted()) {
            apply(state.getTimeInMillis(ageInTicks), 1.0F);
        }
    }

    /** 26.3's {@code applyWalk}: a walk cycle driven by the limb swing, faded in with its speed. */
    public void applyWalk(float animationPos, float animationSpeed, float speedFactor, float scaleFactor) {
        apply((long) (animationPos * 50.0F * speedFactor), Math.min(animationSpeed * scaleFactor, 1.0F));
    }

    public void apply(long millis, float targetScale) {
        float seconds = millis / 1000.0F;
        if (definition.looping) {
            seconds %= definition.lengthInSeconds;
        }
        for (Entry entry : entries) {
            entry.apply(seconds, targetScale, scratch);
        }
    }

    private static final class Entry {
        private final ModelPart part;
        private final AnimationChannel channel;

        Entry(ModelPart part, AnimationChannel channel) {
            this.part = part;
            this.channel = channel;
        }

        void apply(float seconds, float targetScale, float[] out) {
            Keyframe[] frames = channel.keyframes;
            int prev = Math.max(0, firstAtOrAfter(frames, seconds) - 1);
            int next = Math.min(frames.length - 1, prev + 1);
            Keyframe a = frames[prev];
            Keyframe b = frames[next];
            float alpha = 0.0F;
            if (next != prev) {
                alpha = Math.max(0.0F, Math.min(1.0F, (seconds - a.timestamp) / (b.timestamp - a.timestamp)));
            }
            b.interpolation.apply(out, alpha, frames, prev, next, targetScale);
            channel.target.apply(part, out);
        }

        /** {@code Mth.binarySearch}: the first frame whose timestamp is at or after the time. */
        private static int firstAtOrAfter(Keyframe[] frames, float seconds) {
            int from = 0;
            int len = frames.length;
            while (len > 0) {
                int half = len / 2;
                int middle = from + half;
                if (seconds <= frames[middle].timestamp) {
                    len = half;
                } else {
                    from = middle + 1;
                    len -= half + 1;
                }
            }
            return from;
        }
    }
}
