package com.exiledradio.villagerbackport.babies.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 26.3's {@code AnimationDefinition}: keyframe channels per bone name. */
public final class AnimationDefinition {

    final float lengthInSeconds;
    final boolean looping;
    final Map<String, List<AnimationChannel>> boneAnimations;

    private AnimationDefinition(float lengthInSeconds, boolean looping, Map<String, List<AnimationChannel>> boneAnimations) {
        this.lengthInSeconds = lengthInSeconds;
        this.looping = looping;
        this.boneAnimations = boneAnimations;
    }

    public KeyframeAnimation bake(ModelPart root) {
        return new KeyframeAnimation(root, this);
    }

    public static final class Builder {
        private final float length;
        private final Map<String, List<AnimationChannel>> animationByBone = new LinkedHashMap<String, List<AnimationChannel>>();
        private boolean looping;

        private Builder(float length) {
            this.length = length;
        }

        public static Builder withLength(float lengthInSeconds) {
            return new Builder(lengthInSeconds);
        }

        public Builder looping() {
            looping = true;
            return this;
        }

        public Builder addAnimation(String bone, AnimationChannel channel) {
            List<AnimationChannel> list = animationByBone.get(bone);
            if (list == null) {
                list = new ArrayList<AnimationChannel>();
                animationByBone.put(bone, list);
            }
            list.add(channel);
            return this;
        }

        public AnimationDefinition build() {
            return new AnimationDefinition(length, looping, animationByBone);
        }
    }
}
