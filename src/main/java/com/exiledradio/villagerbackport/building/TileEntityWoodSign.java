package com.exiledradio.villagerbackport.building;

import net.minecraft.tileentity.TileEntitySign;

/**
 * The tile behind a spruce, birch, jungle, acacia or dark oak sign.
 *
 * <p>Deliberately nothing but a vanilla sign under its own registry name. Everything that works on
 * signs checks {@code instanceof TileEntitySign} - the editing screen and its packets, the server's
 * update handling, commands run from sign text, dyeing the text, other mods' sign tools - so being one
 * is what makes all of it work here without a line of its own.
 *
 * <p>The separate class exists for one reason: a tile entity class can only have one renderer, and
 * vanilla's draws the oak texture. Which wood to draw is read from the block, not stored here.
 */
public class TileEntityWoodSign extends TileEntitySign {
}
