package com.exiledradio.villagerbackport.babies.client.model;

/** 26.3's {@code AnimationChannel}: what a bone's keyframes drive, and how they blend. */
public final class AnimationChannel {

    final Target target;
    final Keyframe[] keyframes;

    public AnimationChannel(Target target, Keyframe... keyframes) {
        this.target = target;
        this.keyframes = keyframes;
    }

    public interface Interpolation {
        void apply(float[] out, float alpha, Keyframe[] keyframes, int prev, int next, float targetScale);
    }

    public interface Target {
        void apply(ModelPart part, float[] value);
    }

    public static final class Interpolations {
        public static final Interpolation LINEAR = new Interpolation() {
            @Override
            public void apply(float[] out, float alpha, Keyframe[] keyframes, int prev, int next, float targetScale) {
                float[] a = keyframes[prev].postTarget;
                float[] b = keyframes[next].preTarget;
                for (int i = 0; i < 3; i++) {
                    out[i] = (a[i] + (b[i] - a[i]) * alpha) * targetScale;
                }
            }
        };
        public static final Interpolation CATMULLROM = new Interpolation() {
            @Override
            public void apply(float[] out, float alpha, Keyframe[] keyframes, int prev, int next, float targetScale) {
                float[] p0 = keyframes[Math.max(0, prev - 1)].postTarget;
                float[] p1 = keyframes[prev].postTarget;
                float[] p2 = keyframes[next].postTarget;
                float[] p3 = keyframes[Math.min(keyframes.length - 1, next + 1)].postTarget;
                for (int i = 0; i < 3; i++) {
                    out[i] = catmullrom(alpha, p0[i], p1[i], p2[i], p3[i]) * targetScale;
                }
            }
        };

        private Interpolations() {
        }

        private static float catmullrom(float a, float p0, float p1, float p2, float p3) {
            return 0.5F * (2.0F * p1 + (p2 - p0) * a + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * a * a
                    + (3.0F * p1 - p0 - 3.0F * p2 + p3) * a * a * a);
        }
    }

    public static final class Targets {
        public static final Target POSITION = new Target() {
            @Override
            public void apply(ModelPart part, float[] v) {
                part.x += v[0];
                part.y += v[1];
                part.z += v[2];
            }
        };
        public static final Target ROTATION = new Target() {
            @Override
            public void apply(ModelPart part, float[] v) {
                part.xRot += v[0];
                part.yRot += v[1];
                part.zRot += v[2];
            }
        };
        public static final Target SCALE = new Target() {
            @Override
            public void apply(ModelPart part, float[] v) {
                part.xScale += v[0];
                part.yScale += v[1];
                part.zScale += v[2];
            }
        };

        private Targets() {
        }
    }
}
