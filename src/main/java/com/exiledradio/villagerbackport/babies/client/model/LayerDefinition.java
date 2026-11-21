package com.exiledradio.villagerbackport.babies.client.model;

/** 26.3's {@code LayerDefinition}: a mesh with its texture size. */
public final class LayerDefinition {

    private final MeshDefinition mesh;
    private final int texWidth;
    private final int texHeight;

    private LayerDefinition(MeshDefinition mesh, int texWidth, int texHeight) {
        this.mesh = mesh;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    public static LayerDefinition create(MeshDefinition mesh, int texWidth, int texHeight) {
        return new LayerDefinition(mesh, texWidth, texHeight);
    }

    public ModelPart bakeRoot() {
        return mesh.getRoot().bake(texWidth, texHeight);
    }
}
