package com.exiledradio.villagerbackport.building;

import net.minecraft.util.ResourceLocation;

import com.exiledradio.villagerbackport.VillagerBackport;

/**
 * The five woods 1.14 gave signs of their own. Oak keeps vanilla's sign.
 */
public enum SignWood {
    SPRUCE("spruce", 1, "planks_spruce"),
    BIRCH("birch", 2, "planks_birch"),
    JUNGLE("jungle", 3, "planks_jungle"),
    ACACIA("acacia", 4, "planks_acacia"),
    DARK_OAK("dark_oak", 5, "planks_big_oak");

    /** The 1.14 name, so {@code spruce_sign} and {@code spruce_wall_sign}. */
    public final String name;

    /** Which {@code minecraft:planks} metadata the sign is crafted from. */
    public final int planksMeta;

    /** The planks texture, used for the block-breaking particles. */
    public final String planksTexture;

    /** 1.14's sign texture for this wood, the same 64 by 32 layout as 1.12.2's. */
    public final ResourceLocation texture;

    SignWood(String name, int planksMeta, String planksTexture) {
        this.name = name;
        this.planksMeta = planksMeta;
        this.planksTexture = planksTexture;
        this.texture = new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/signs/" + name + ".png");
    }
}
