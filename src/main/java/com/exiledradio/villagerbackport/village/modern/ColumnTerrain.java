package com.exiledradio.villagerbackport.village.modern;

/**
 * Another mod's world generator that can say where a column's surface will be before the chunk
 * exists. See {@link TerrainHeights}.
 */
interface ColumnTerrain {

    /** 1.14's {@code WORLD_SURFACE_WG}: one above the top block of the bare terrain, water included. */
    int height(int x, int z);
}
