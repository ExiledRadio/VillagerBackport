package com.exiledradio.villagerbackport.babies.client.model;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 26.3's {@code ModelPart} on 1.12's GL pipeline: translate by the offset, rotate Z then Y then X (as 1.12's
 * {@code ModelRenderer}), then scale, which 1.12's parts lack. Boxes go to a display list on first draw.
 */
public final class ModelPart {

    private static final float DEG = (float) (180.0D / Math.PI);

    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    public boolean visible = true;

    private final List<Cube> cubes;
    private final Map<String, ModelPart> children;
    private PartPose initialPose = PartPose.ZERO;
    private int displayList = -1;

    ModelPart(List<Cube> cubes, Map<String, ModelPart> children) {
        this.cubes = cubes;
        this.children = children;
    }

    void setInitialPose(PartPose pose) {
        this.initialPose = pose;
    }

    public void loadPose(PartPose pose) {
        x = pose.x;
        y = pose.y;
        z = pose.z;
        xRot = pose.xRot;
        yRot = pose.yRot;
        zRot = pose.zRot;
        xScale = pose.xScale;
        yScale = pose.yScale;
        zScale = pose.zScale;
    }

    /** Back to the baked pose, this part and every descendant ({@code Model.resetPose}). */
    public void resetPoseAll() {
        loadPose(initialPose);
        for (ModelPart child : children.values()) {
            child.resetPoseAll();
        }
    }

    /** Copies the pose of a part of another model (armor following the body). */
    public void copyFrom(ModelPart other) {
        x = other.x;
        y = other.y;
        z = other.z;
        xRot = other.xRot;
        yRot = other.yRot;
        zRot = other.zRot;
        xScale = other.xScale;
        yScale = other.yScale;
        zScale = other.zScale;
    }

    public boolean hasChild(String name) {
        return children.containsKey(name);
    }

    public ModelPart getChild(String name) {
        ModelPart part = children.get(name);
        if (part == null) {
            throw new NoSuchElementException("Can't find part " + name);
        }
        return part;
    }

    /** This part and all descendants, depth first, with their names (for keyframe animations). */
    void collect(String name, Map<String, ModelPart> out) {
        if (!out.containsKey(name)) {
            out.put(name, this);
        }
        for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
            if (!out.containsKey(entry.getKey())) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        for (ModelPart child : new ArrayList<ModelPart>(children.values())) {
            child.collectChildren(out);
        }
    }

    private void collectChildren(Map<String, ModelPart> out) {
        for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
            if (!out.containsKey(entry.getKey())) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        for (ModelPart child : children.values()) {
            child.collectChildren(out);
        }
    }

    public void render(float scale) {
        if (!visible || (cubes.isEmpty() && children.isEmpty())) {
            return;
        }
        GlStateManager.pushMatrix();
        translateAndRotate(scale);
        if (!cubes.isEmpty()) {
            if (displayList < 0) {
                compile(scale);
            }
            GlStateManager.callList(displayList);
        }
        for (ModelPart child : children.values()) {
            child.render(scale);
        }
        GlStateManager.popMatrix();
    }

    public void translateAndRotate(float scale) {
        GlStateManager.translate(x * scale, y * scale, z * scale);
        if (zRot != 0.0F) {
            GlStateManager.rotate(zRot * DEG, 0.0F, 0.0F, 1.0F);
        }
        if (yRot != 0.0F) {
            GlStateManager.rotate(yRot * DEG, 0.0F, 1.0F, 0.0F);
        }
        if (xRot != 0.0F) {
            GlStateManager.rotate(xRot * DEG, 1.0F, 0.0F, 0.0F);
        }
        if (xScale != 1.0F || yScale != 1.0F || zScale != 1.0F) {
            GlStateManager.scale(xScale, yScale, zScale);
        }
    }

    private void compile(float scale) {
        displayList = GLAllocation.generateDisplayLists(1);
        GlStateManager.glNewList(displayList, GL11.GL_COMPILE);
        for (Cube cube : cubes) {
            cube.draw(scale);
        }
        GlStateManager.glEndList();
    }
}
