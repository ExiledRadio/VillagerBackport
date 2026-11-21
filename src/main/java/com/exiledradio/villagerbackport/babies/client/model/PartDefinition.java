package com.exiledradio.villagerbackport.babies.client.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 26.3's {@code PartDefinition}: a part's boxes, pose and named children, before baking. */
public final class PartDefinition {

    private final List<CubeListBuilder.Def> cubes;
    private final PartPose pose;
    private final Map<String, PartDefinition> children = new LinkedHashMap<String, PartDefinition>();

    PartDefinition(List<CubeListBuilder.Def> cubes, PartPose pose) {
        this.cubes = cubes;
        this.pose = pose;
    }

    public PartDefinition addOrReplaceChild(String name, CubeListBuilder cubes, PartPose pose) {
        return addOrReplaceChild(name, new PartDefinition(new ArrayList<CubeListBuilder.Def>(cubes.cubes), pose));
    }

    private PartDefinition addOrReplaceChild(String name, PartDefinition child) {
        PartDefinition previous = children.put(name, child);
        if (previous != null) {
            child.children.putAll(previous.children);
        }
        return child;
    }

    public PartDefinition getChild(String name) {
        return children.get(name);
    }

    public PartDefinition clearRecursively() {
        for (String name : new ArrayList<String>(children.keySet())) {
            clearChild(name).clearRecursively();
        }
        return this;
    }

    public PartDefinition clearChild(String name) {
        PartDefinition child = children.get(name);
        if (child == null) {
            throw new IllegalArgumentException("No child with name: " + name);
        }
        return addOrReplaceChild(name, CubeListBuilder.create(), child.pose);
    }

    /** Keeps the listed parts with their children; empties the rest. */
    public void retainPartsAndChildren(Set<String> parts) {
        for (Map.Entry<String, PartDefinition> entry : new ArrayList<Map.Entry<String, PartDefinition>>(children.entrySet())) {
            if (!parts.contains(entry.getKey())) {
                addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), entry.getValue().pose).retainPartsAndChildren(parts);
            }
        }
    }

    /** Keeps only the listed parts' own boxes; empties everything else. */
    public void retainExactParts(Set<String> parts) {
        for (Map.Entry<String, PartDefinition> entry : new ArrayList<Map.Entry<String, PartDefinition>>(children.entrySet())) {
            if (parts.contains(entry.getKey())) {
                entry.getValue().clearRecursively();
            } else {
                addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), entry.getValue().pose).retainExactParts(parts);
            }
        }
    }

    ModelPart bake(int texWidth, int texHeight) {
        Map<String, ModelPart> baked = new LinkedHashMap<String, ModelPart>();
        for (Map.Entry<String, PartDefinition> entry : children.entrySet()) {
            baked.put(entry.getKey(), entry.getValue().bake(texWidth, texHeight));
        }
        List<Cube> bakedCubes = new ArrayList<Cube>();
        for (CubeListBuilder.Def def : cubes) {
            bakedCubes.add(new Cube(def.u, def.v, def.x, def.y, def.z, def.w, def.h, def.d,
                    def.grow.growX, def.grow.growY, def.grow.growZ, def.mirror, texWidth, texHeight));
        }
        ModelPart part = new ModelPart(bakedCubes, baked);
        part.setInitialPose(pose);
        part.loadPose(pose);
        return part;
    }
}
