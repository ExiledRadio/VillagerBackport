package com.exiledradio.villagerbackport.flora;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.DyeUtils;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Dyeing leather horse armor in the crafting grid: one piece of armor and any number of dyes.
 *
 * <p>The mix is 1.12.2's {@code RecipesArmorDyes} - and 1.14's, which did not change it: every dye
 * and the armor's current color (if it has one) are averaged, then brightened back up to the average
 * of their brightest channels, so mixing does not just make everything muddier.
 *
 * <p>Any dye counts, by the ore dictionary: 1.12.2's dye items, this mod's four new ones, and every
 * other mod's that registers its dyes properly.
 */
public class LeatherHorseArmorDyeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return !armor(inv).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack armor = armor(inv);
        if (armor.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<EnumDyeColor> dyes = new ArrayList<EnumDyeColor>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && DyeUtils.isDye(stack)) {
                dyes.add(DyeUtils.colorFromStack(stack).get());
            }
        }

        ItemStack result = armor.copy();
        result.setCount(1);
        ItemLeatherHorseArmor.setColor(result, mix(armor, dyes));
        return result;
    }

    /** @return the one leather horse armor in the grid, if the rest is all dyes and there is at least one */
    private static ItemStack armor(InventoryCrafting inv) {
        ItemStack armor = ItemStack.EMPTY;
        int dyes = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ItemLeatherHorseArmor) {
                if (!armor.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                armor = stack;
            } else if (DyeUtils.isDye(stack) && DyeUtils.colorFromStack(stack).isPresent()) {
                dyes++;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return dyes > 0 ? armor : ItemStack.EMPTY;
    }

    /** {@code RecipesArmorDyes.getCraftingResult}'s arithmetic. */
    static int mix(ItemStack armor, List<EnumDyeColor> dyes) {
        int[] sum = new int[3];
        int brightest = 0;
        int count = 0;

        if (ItemLeatherHorseArmor.hasColor(armor)) {
            int color = ItemLeatherHorseArmor.getColor(armor);
            float r = (color >> 16 & 255) / 255.0F;
            float g = (color >> 8 & 255) / 255.0F;
            float b = (color & 255) / 255.0F;
            brightest = (int) (brightest + Math.max(r, Math.max(g, b)) * 255.0F);
            sum[0] = (int) (sum[0] + r * 255.0F);
            sum[1] = (int) (sum[1] + g * 255.0F);
            sum[2] = (int) (sum[2] + b * 255.0F);
            count++;
        }

        for (EnumDyeColor dye : dyes) {
            float[] rgb = dye.getColorComponentValues();
            int r = (int) (rgb[0] * 255.0F);
            int g = (int) (rgb[1] * 255.0F);
            int b = (int) (rgb[2] * 255.0F);
            brightest += Math.max(r, Math.max(g, b));
            sum[0] += r;
            sum[1] += g;
            sum[2] += b;
            count++;
        }

        int r = sum[0] / count;
        int g = sum[1] / count;
        int b = sum[2] / count;
        float average = (float) brightest / (float) count;
        float max = Math.max(r, Math.max(g, b));
        r = (int) (r * average / max);
        g = (int) (g * average / max);
        b = (int) (b * average / max);
        return (r << 16) + (g << 8) + b;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
