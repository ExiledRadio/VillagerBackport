package com.exiledradio.villagerbackport.babies.client.model;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/** 26.3's {@code ModelPart.Cube}: six faces with float sizes, per-axis growth and 26.3's UV layout. */
final class Cube {

    // Face normals: down, up, west, north, east, south.
    private static final float[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}};

    /** Per face: 4 vertices of x, y, z, u, v. */
    private final float[][][] faces = new float[6][][];
    private final float[][] normals = new float[6][];

    Cube(int texU, int texV, float minX, float minY, float minZ, float width, float height, float depth,
            float growX, float growY, float growZ, boolean mirror, float texWidth, float texHeight) {
        float maxX = minX + width;
        float maxY = minY + height;
        float maxZ = minZ + depth;
        minX -= growX;
        minY -= growY;
        minZ -= growZ;
        maxX += growX;
        maxY += growY;
        maxZ += growZ;
        if (mirror) {
            float tmp = maxX;
            maxX = minX;
            minX = tmp;
        }
        float[] t0 = {minX, minY, minZ};
        float[] t1 = {maxX, minY, minZ};
        float[] t2 = {maxX, maxY, minZ};
        float[] t3 = {minX, maxY, minZ};
        float[] l0 = {minX, minY, maxZ};
        float[] l1 = {maxX, minY, maxZ};
        float[] l2 = {maxX, maxY, maxZ};
        float[] l3 = {minX, maxY, maxZ};
        float u0 = texU;
        float u1 = texU + depth;
        float u2 = texU + depth + width;
        float u22 = texU + depth + width + width;
        float u3 = texU + depth + width + depth;
        float u4 = texU + depth + width + depth + width;
        float v0 = texV;
        float v1 = texV + depth;
        float v2 = texV + depth + height;
        face(0, new float[][]{l1, l0, t0, t1}, u1, v0, u2, v1, texWidth, texHeight, mirror);
        face(1, new float[][]{t2, t3, l3, l2}, u2, v1, u22, v0, texWidth, texHeight, mirror);
        face(2, new float[][]{t0, l0, l3, t3}, u0, v1, u1, v2, texWidth, texHeight, mirror);
        face(3, new float[][]{t1, t0, t3, t2}, u1, v1, u2, v2, texWidth, texHeight, mirror);
        face(4, new float[][]{l1, t1, t2, l2}, u2, v1, u3, v2, texWidth, texHeight, mirror);
        face(5, new float[][]{l0, l1, l2, l3}, u3, v1, u4, v2, texWidth, texHeight, mirror);
    }

    private void face(int index, float[][] pos, float uA, float vA, float uB, float vB, float tw, float th, boolean mirror) {
        float[][] verts = new float[4][];
        verts[0] = new float[]{pos[0][0], pos[0][1], pos[0][2], uB / tw, vA / th};
        verts[1] = new float[]{pos[1][0], pos[1][1], pos[1][2], uA / tw, vA / th};
        verts[2] = new float[]{pos[2][0], pos[2][1], pos[2][2], uA / tw, vB / th};
        verts[3] = new float[]{pos[3][0], pos[3][1], pos[3][2], uB / tw, vB / th};
        int normal = index;
        if (mirror) {
            float[] tmp = verts[0];
            verts[0] = verts[3];
            verts[3] = tmp;
            tmp = verts[1];
            verts[1] = verts[2];
            verts[2] = tmp;
            // Mirroring swaps west and east.
            if (index == 2) {
                normal = 4;
            } else if (index == 4) {
                normal = 2;
            }
        }
        faces[index] = verts;
        normals[index] = NORMALS[normal];
    }

    void draw(float scale) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int i = 0; i < 6; i++) {
            float[] n = normals[i];
            buffer.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
            for (float[] v : faces[i]) {
                buffer.pos(v[0] * scale, v[1] * scale, v[2] * scale).tex(v[3], v[4]).normal(n[0], n[1], n[2]).endVertex();
            }
            tessellator.draw();
        }
    }
}
