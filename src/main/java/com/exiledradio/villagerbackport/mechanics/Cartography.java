package com.exiledradio.villagerbackport.mechanics;

import com.exiledradio.backportcore.map.MapIcons;

import net.minecraft.world.storage.MapDecoration;

/**
 * The map icons current Java's cartographer maps carry (today's {@code MapDecorationTypes}), added through Backport
 * Core in pre-init on both sides so a server and its clients number them the same.
 */
public final class Cartography {

    private static final String[] ICONS = {"plains_village", "desert_village", "savanna_village", "snowy_village",
            "taiga_village", "swamp_hut", "jungle_temple", "trial_chambers"};

    private Cartography() {
    }

    static void preInit() {
        for (String name : ICONS) {
            MapIcons.get(name, "trial_chambers".equals(name) ? MapIcons.TRIAL_CHAMBERS_COLOR : MapIcons.LIGHT_GRAY);
        }
    }

    /** @return the decoration for one of today's icons (Core's), or the red marker if it is unknown */
    public static MapDecoration.Type icon(String name) {
        MapDecoration.Type type = MapIcons.find(name);
        return type != null ? type : MapDecoration.Type.RED_MARKER;
    }
}
