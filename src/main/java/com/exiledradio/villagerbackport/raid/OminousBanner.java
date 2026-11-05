package com.exiledradio.villagerbackport.raid;

import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;

/**
 * 1.14's ominous banner, which raid and patrol captains wear.
 *
 * <p>Every pattern it uses is already in 1.12.2: rhombus, bottom stripe, centre stripe, border,
 * middle stripe, top half and roundel, on white, by their 1.12.2 hash names.
 */
public final class OminousBanner {

    /** Marks a banner as ominous, so it is recognised whatever name a player gives it. */
    public static final String TAG = "villagerbackport:ominous";

    private OminousBanner() {
    }

    public static ItemStack create() {
        NBTTagList patterns = new NBTTagList();
        add(patterns, "mr", EnumDyeColor.CYAN);
        add(patterns, "bs", EnumDyeColor.SILVER);
        add(patterns, "cs", EnumDyeColor.GRAY);
        add(patterns, "bo", EnumDyeColor.SILVER);
        add(patterns, "ms", EnumDyeColor.BLACK);
        add(patterns, "hh", EnumDyeColor.SILVER);
        add(patterns, "mc", EnumDyeColor.SILVER);
        add(patterns, "bo", EnumDyeColor.BLACK);

        NBTTagCompound blockEntity = new NBTTagCompound();
        blockEntity.setInteger("Base", EnumDyeColor.WHITE.getDyeDamage());
        blockEntity.setTag("Patterns", patterns);

        ItemStack banner = new ItemStack(Items.BANNER, 1, EnumDyeColor.WHITE.getDyeDamage());
        banner.setTagInfo("BlockEntityTag", blockEntity);
        banner.getTagCompound().setBoolean(TAG, true);
        // 1.14 hides the list of patterns (HideFlags 32), leaving just the name.
        banner.getTagCompound().setInteger("HideFlags", 32);
        // 1.12.2 cannot store a translated name, so it is written out, in 1.14's gold. The italic
        // is 1.14's mark of a named item, which 1.12.2's tooltip loses after a colour code.
        banner.setStackDisplayName(TextFormatting.GOLD + "" + TextFormatting.ITALIC + "Ominous Banner");
        return banner;
    }

    public static boolean isOminous(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG);
    }

    /** Banner colours are saved as dye damage in 1.12.2, not as 1.14's colour id. */
    private static void add(NBTTagList patterns, String pattern, EnumDyeColor colour) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("Pattern", pattern);
        entry.setInteger("Color", colour.getDyeDamage());
        patterns.appendTag(entry);
    }
}
