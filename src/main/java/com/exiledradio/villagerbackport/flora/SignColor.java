package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/**
 * The color a sign's text is drawn in, and where it is kept.
 *
 * <h2>Storage</h2>
 * In the sign tile's Forge data ({@link TileEntity#getTileData()}), under
 * {@code villagerbackport:Color}, holding the dye's 1.14 name ({@code "red"}, {@code "light_blue"} -
 * 1.14's own {@code Color} tag uses the same names). 1.12.2's sign does not save fields it does not
 * know, but Forge saves and loads that compound for every tile entity, and a sign's update packet is
 * its whole save, so the color reaches clients when the sign loads and whenever it changes, with no
 * packet of this mod's own.
 *
 * <h2>For other signs</h2>
 * Anything that extends {@code TileEntitySign} can be dyed (see {@link SignDyeing}). A mod drawing
 * its own sign - another wood's sign, say - only needs to call {@link #textColor} for the color to
 * draw text in; 0 is black, as an undyed sign is.
 *
 * <h2>Drawn color</h2>
 * 1.14's: each dye has a text color brighter than its wool color, and a sign draws it at 40%, so the
 * text stays dark enough to read on the wood. Black stays black.
 */
public final class SignColor {

    private static final String KEY = VillagerBackport.MOD_ID + ":Color";

    /** 1.14's {@code DyeColor} text colors, by 1.12.2 metadata (white first). */
    private static final int[] TEXT_COLORS = {
            0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD,
            0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080,
            0xD3D3D3, 0x00FFFF, 0xA020F0, 0x0000FF,
            0x8B4513, 0x00FF00, 0xFF0000, 0x000000,
    };

    private SignColor() {
    }

    /** @return the sign's dye color; black when it has never been dyed */
    public static EnumDyeColor get(TileEntity sign) {
        EnumDyeColor color = parse(sign.getTileData().getString(KEY));
        return color == null ? EnumDyeColor.BLACK : color;
    }

    /** @return true if the color changed */
    public static boolean set(TileEntity sign, EnumDyeColor color) {
        if (get(sign) == color) {
            return false;
        }
        NBTTagCompound data = sign.getTileData();
        if (color == EnumDyeColor.BLACK) {
            data.removeTag(KEY);
        } else {
            data.setString(KEY, color == EnumDyeColor.SILVER ? "light_gray" : color.getName());
        }
        return true;
    }

    /** @return the RGB to draw this sign's text in, already darkened as 1.14 draws it */
    public static int textColor(TileEntity sign) {
        EnumDyeColor color = get(sign);
        if (color == EnumDyeColor.BLACK) {
            return 0;
        }
        int rgb = TEXT_COLORS[color.getMetadata()];
        int r = (int) ((rgb >> 16 & 255) * 0.4D);
        int g = (int) ((rgb >> 8 & 255) * 0.4D);
        int b = (int) ((rgb & 255) * 0.4D);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * 1.12.2 names light gray "silver"; 1.14 and this tag call it "light_gray". Both are read.
     */
    @Nullable
    private static EnumDyeColor parse(String name) {
        if (name.isEmpty()) {
            return null;
        }
        if ("light_gray".equals(name)) {
            return EnumDyeColor.SILVER;
        }
        for (EnumDyeColor color : EnumDyeColor.values()) {
            if (color.getName().equals(name)) {
                return color;
            }
        }
        return null;
    }
}
