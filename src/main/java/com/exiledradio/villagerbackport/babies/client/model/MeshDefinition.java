package com.exiledradio.villagerbackport.babies.client.model;

import java.util.ArrayList;

/** 26.3's {@code MeshDefinition}: the unposed root part. */
public final class MeshDefinition {

    private final PartDefinition root = new PartDefinition(new ArrayList<CubeListBuilder.Def>(), PartPose.ZERO);

    public PartDefinition getRoot() {
        return root;
    }
}
