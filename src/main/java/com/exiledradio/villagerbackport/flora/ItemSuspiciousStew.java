package com.exiledradio.villagerbackport.flora;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Suspicious stew: mushroom stew with a flower in it, and the flower's effect when eaten.
 *
 * <h2>Food</h2>
 * 1.14's: six hunger, 0.6 saturation, edible when full, does not stack, and leaves the bowl behind,
 * as mushroom stew does. The effect is a hidden surprise - the item says nothing about it, which is
 * the point of the name.
 *
 * <h2>Where the effect is kept</h2>
 * In 1.14's own NBT layout, an {@code Effects} list of {@code {EffectId: byte, EffectDuration: int}},
 * so a stew means the same thing to anything that reads 1.14's format. A stew with no list (the one
 * in the creative menu, or one another mod made) is plain mushroom stew in all but name.
 *
 * <h2>Where it comes from</h2>
 * The crafting grid (bowl, red and brown mushroom, and a small flower - see {@link FloraRecipes}),
 * a brown mooshroom fed a flower and milked with a bowl ({@link Mooshrooms}), and an expert farmer.
 */
public class ItemSuspiciousStew extends ItemSoup {

    private static final String EFFECTS = "Effects";
    private static final String EFFECT_ID = "EffectId";
    private static final String DURATION = "EffectDuration";

    public ItemSuspiciousStew() {
        super(6);
        setAlwaysEdible();
    }

    /** @return a stew carrying one effect */
    public static ItemStack create(@Nullable Potion effect, int ticks) {
        ItemStack stack = new ItemStack(FloraContent.suspiciousStew);
        if (effect != null) {
            addEffect(stack, effect, ticks);
        }
        return stack;
    }

    /** 1.14's {@code SuspiciousStewItem.addEffect}: appends one entry to the list. */
    public static void addEffect(ItemStack stack, Potion effect, int ticks) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        NBTTagList list = tag.getTagList(EFFECTS, 10);
        NBTTagCompound entry = new NBTTagCompound();
        entry.setByte(EFFECT_ID, (byte) Potion.getIdFromPotion(effect));
        entry.setInteger(DURATION, ticks);
        list.appendTag(entry);
        tag.setTag(EFFECTS, list);
    }

    /** Applies every listed effect, then hands back to {@code ItemFood} for the hunger itself. */
    @Override
    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        super.onFoodEaten(stack, world, player);

        if (world.isRemote || !stack.hasTagCompound()) {
            return;
        }

        NBTTagList list = stack.getTagCompound().getTagList(EFFECTS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int ticks = entry.hasKey(DURATION, 3) ? entry.getInteger(DURATION) : 160;
            Potion effect = Potion.getPotionById(entry.getByte(EFFECT_ID) & 255);
            if (effect != null) {
                player.addPotionEffect(new PotionEffect(effect, ticks));
            }
        }
    }
}
