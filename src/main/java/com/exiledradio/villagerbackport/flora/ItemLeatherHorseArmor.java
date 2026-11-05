package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.EnumHelper;

import javax.annotation.Nullable;

/**
 * Leather horse armor: 1.14's cheapest horse armor, worth three armor points, and dyeable.
 *
 * <h2>Being horse armor</h2>
 * 1.12.2's horses know three armors, as the constants of {@link HorseArmorType}. Forge lets a mod
 * add more through {@link EnumHelper#addHorseArmor}, and an item becomes horse armor by answering
 * {@link #getHorseArmorType} with one of them: the horse's armor slot then takes it, the horse gains
 * its armor points, and it is saved and synced like iron, gold or diamond. That is all done here.
 *
 * <h2>Color</h2>
 * Stored exactly where leather armor keeps it - {@code display.color} - so it behaves like leather
 * armor everywhere: the crafting grid dyes it (see {@link LeatherHorseArmorDyeRecipe}), a filled
 * cauldron washes it ({@link CauldronWashing}), and undyed it is leather's own brown, {@code A06540}.
 *
 * <h2>Drawing it</h2>
 * 1.12.2 draws horse armor by baking it into the horse's own texture, and a baked texture cannot be
 * tinted. So {@link #getHorseArmorTexture} answers null, which leaves the armor out of the baked
 * texture, and a render layer on the client draws it over the horse in its color instead
 * ({@code flora.client.LayerLeatherHorseArmor}). The pattern is 1.12.2's iron horse armor redrawn
 * in grey for tinting, since 1.14's leather texture is laid out for 1.14's horse model, not 1.12.2's.
 */
public class ItemLeatherHorseArmor extends Item {

    /** Leather's undyed color, as {@code ItemArmor} uses for leather armor. */
    public static final int DEFAULT_COLOR = 0xA06540;

    /** 1.14's protection for leather horse armor. */
    private static final int ARMOR_POINTS = 3;

    private static HorseArmorType type;

    public ItemLeatherHorseArmor() {
        setMaxStackSize(1);
        // Added now, during item registration, so it takes the same ordinal on server and client.
        type();
    }

    /** @return the horse armor type this item is, added to the enum on first call */
    public static synchronized HorseArmorType type() {
        if (type == null) {
            type = EnumHelper.addHorseArmor("VILLAGERBACKPORT_LEATHER",
                    VillagerBackport.MOD_ID + ":textures/entity/horse/armor/horse_armor_leather.png", ARMOR_POINTS);
        }
        return type;
    }

    @Override
    public HorseArmorType getHorseArmorType(ItemStack stack) {
        return type();
    }

    /** None: the client layer draws it, tinted. See the class description. */
    @Override
    @Nullable
    public String getHorseArmorTexture(EntityLiving wearer, ItemStack stack) {
        return null;
    }

    public static boolean hasColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("display", 10) && tag.getCompoundTag("display").hasKey("color", 3);
    }

    /** @return the armor's color, or leather's own when undyed */
    public static int getColor(ItemStack stack) {
        if (!hasColor(stack)) {
            return DEFAULT_COLOR;
        }
        return stack.getTagCompound().getCompoundTag("display").getInteger("color");
    }

    public static void setColor(ItemStack stack, int color) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!tag.hasKey("display", 10)) {
            tag.setTag("display", new NBTTagCompound());
        }
        tag.getCompoundTag("display").setInteger("color", color);
    }

    public static void removeColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("display", 10)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            display.removeTag("color");
            if (display.isEmpty()) {
                tag.removeTag("display");
            }
            if (tag.isEmpty()) {
                stack.setTagCompound(null);
            }
        }
    }
}
