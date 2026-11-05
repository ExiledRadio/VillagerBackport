package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * The crossbow a pillager carries: whichever mod adds one.
 *
 * <h2>Held, never used</h2>
 * The pillager does its own loading and shooting, with 1.14's timings and aim. The crossbow is only
 * held and shown, and marked loaded or not so its model shows an arrow - nothing of another mod's
 * item code ever runs for a mob, so a crossbow written only for players cannot fail on one.
 *
 * <p>Future MC, Crossbows Backport and this mod's own crossbow all mark a loaded crossbow with
 * 1.14's {@code Charged} tag; Spartan Weaponry uses {@code isLoaded}. The pillagers' stand-in uses
 * {@code Charged} as well.
 */
public final class Crossbows {

    private Crossbows() {
    }

    /** @return a new crossbow, the first installed in {@code raids.crossbows} */
    public static ItemStack newCrossbow() {
        Item ours = com.exiledradio.villagerbackport.crossbow.CrossbowContent.crossbow;
        for (String name : ModConfig.raids.crossbows) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name.trim()));
            if (item != null && item != Items.AIR) {
                // A list from before this mod had a crossbow of its own ends at the stand-in.
                return new ItemStack(item == IllagerContent.pillagerCrossbow && ours != null ? ours : item);
            }
        }
        return new ItemStack(ours != null ? ours : IllagerContent.pillagerCrossbow);
    }

    /** @return true for anything a pillager will treat as its crossbow */
    public static boolean isCrossbow(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        return name != null && name.getPath().contains("crossbow");
    }

    /** @return true if this is the pillagers' stand-in crossbow, which a pillager never drops */
    public static boolean isOurs(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == IllagerContent.pillagerCrossbow;
    }

    public static void setCharged(ItemStack stack, boolean charged) {
        if (!isCrossbow(stack)) {
            return;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        if ("spartanweaponry".equals(namespace(stack))) {
            tag.setBoolean("isLoaded", charged);
        } else {
            tag.setBoolean("Charged", charged);
            NBTTagList projectiles = new NBTTagList();
            if (charged) {
                projectiles.appendTag(new ItemStack(Items.ARROW).writeToNBT(new NBTTagCompound()));
            }
            tag.setTag("ChargedProjectiles", projectiles);
        }
        stack.setTagCompound(tag);
    }

    public static boolean isCharged(ItemStack stack) {
        return stack.hasTagCompound() && (stack.getTagCompound().getBoolean("Charged") || stack.getTagCompound().getBoolean("isLoaded"));
    }

    @Nullable
    private static String namespace(ItemStack stack) {
        ResourceLocation name = stack.getItem().getRegistryName();
        return name == null ? null : name.getNamespace();
    }
}
