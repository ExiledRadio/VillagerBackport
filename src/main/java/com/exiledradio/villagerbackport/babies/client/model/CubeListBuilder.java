package com.exiledradio.villagerbackport.babies.client.model;

import java.util.ArrayList;
import java.util.List;

/** 26.3's {@code CubeListBuilder}: texture offset and mirror carry over to the boxes added after them. */
public final class CubeListBuilder {

    /** One box before baking. */
    static final class Def {
        final int u;
        final int v;
        final float x;
        final float y;
        final float z;
        final float w;
        final float h;
        final float d;
        final CubeDeformation grow;
        final boolean mirror;

        Def(int u, int v, float x, float y, float z, float w, float h, float d, CubeDeformation grow, boolean mirror) {
            this.u = u;
            this.v = v;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.d = d;
            this.grow = grow;
            this.mirror = mirror;
        }
    }

    final List<Def> cubes = new ArrayList<Def>();
    private int u;
    private int v;
    private boolean mirror;

    public static CubeListBuilder create() {
        return new CubeListBuilder();
    }

    public CubeListBuilder texOffs(int u, int v) {
        this.u = u;
        this.v = v;
        return this;
    }

    public CubeListBuilder mirror() {
        return mirror(true);
    }

    public CubeListBuilder mirror(boolean mirror) {
        this.mirror = mirror;
        return this;
    }

    public CubeListBuilder addBox(float x, float y, float z, float w, float h, float d) {
        return addBox(x, y, z, w, h, d, CubeDeformation.NONE);
    }

    public CubeListBuilder addBox(float x, float y, float z, float w, float h, float d, CubeDeformation grow) {
        cubes.add(new Def(u, v, x, y, z, w, h, d, grow, mirror));
        return this;
    }

    /** Mirrors this box only. */
    public CubeListBuilder addBox(float x, float y, float z, float w, float h, float d, boolean mirror) {
        cubes.add(new Def(u, v, x, y, z, w, h, d, CubeDeformation.NONE, mirror));
        return this;
    }
}
