package com.exiledradio.villagerbackport.woodset;

import net.minecraft.block.BlockPlanks;

import javax.annotation.Nullable;

/**
 * The six woods 1.12.2 has, as 1.14 names them.
 *
 * <p>The order is vanilla's planks metadata order, so {@code ordinal()} is also the planks metadata
 * and the {@link BlockPlanks.EnumType} of the same wood. {@link #OAK} is listed but has no trapdoor,
 * button or pressure plate here: vanilla's oak ones already are those (see {@link #hasOwnRedstone}).
 *
 * <p>{@code tools/woodset/generate.py} reads this file as text for the names and planks textures, so
 * a new constant needs the same one-line shape as the others.
 */
public enum Wood {
    OAK("oak", BlockPlanks.EnumType.OAK, "planks_oak"),
    SPRUCE("spruce", BlockPlanks.EnumType.SPRUCE, "planks_spruce"),
    BIRCH("birch", BlockPlanks.EnumType.BIRCH, "planks_birch"),
    JUNGLE("jungle", BlockPlanks.EnumType.JUNGLE, "planks_jungle"),
    ACACIA("acacia", BlockPlanks.EnumType.ACACIA, "planks_acacia"),
    DARK_OAK("dark_oak", BlockPlanks.EnumType.DARK_OAK, "planks_big_oak");

    /** The 1.14 name: {@code stripped_spruce_log}, {@code spruce_trapdoor}. */
    public final String name;

    /** The planks this wood crafts into and is crafted from. */
    public final BlockPlanks.EnumType planks;

    /**
     * 1.12.2's planks texture, under {@code minecraft:blocks/}. Buttons and pressure plates are drawn
     * with it rather than 1.14's repainted planks, so a spruce button matches the spruce planks it
     * sits on in a 1.12.2 world.
     */
    public final String planksTexture;

    Wood(String name, BlockPlanks.EnumType planks, String planksTexture) {
        this.name = name;
        this.planks = planks;
        this.planksTexture = planksTexture;
    }

    /** @return the {@code minecraft:planks} metadata of this wood */
    public int planksMeta() {
        return this.planks.getMetadata();
    }

    /**
     * @return true if 1.14 added this wood's trapdoor, button and pressure plate; false for oak,
     * whose are vanilla's own
     */
    public boolean hasOwnRedstone() {
        return this != OAK;
    }

    /** @return the wood of a planks variant, never null for a vanilla variant */
    @Nullable
    public static Wood of(BlockPlanks.EnumType type) {
        for (Wood wood : values()) {
            if (wood.planks == type) {
                return wood;
            }
        }
        return null;
    }
}
