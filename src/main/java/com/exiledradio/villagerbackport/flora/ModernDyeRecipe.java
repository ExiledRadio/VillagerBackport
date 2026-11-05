package com.exiledradio.villagerbackport.flora;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeFireworks;
import net.minecraft.item.crafting.RecipesArmorDyes;
import net.minecraft.item.crafting.RecipesBanners;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the new dyes into the three vanilla recipes that look for {@code Items.DYE} by hand.
 *
 * <h2>The problem</h2>
 * Forge rewrites vanilla's shaped and shapeless recipes to take dyes by ore dictionary name, so the
 * new dyes work in those without help. Three recipes are not shaped or shapeless but code, and each
 * tests {@code item == Items.DYE} directly: dyeing leather armor ({@code RecipesArmorDyes}), coloring
 * a firework star ({@code RecipeFireworks}) and adding a pattern to a banner
 * ({@code RecipesBanners.RecipeAddPattern}). To them a white dye is not a dye.
 *
 * <h2>What this does</h2>
 * It stands in front of them. When the grid holds one of the new dyes, it makes a copy of the grid
 * with each new dye replaced by 1.12.2's dye of the same color - white dye by bone meal, and so on -
 * and asks each of the three whether that copy is a recipe. If one says yes, its answer is this
 * recipe's answer. The player's grid itself is never changed, and the dye they put in is the one
 * that is used up.
 *
 * <p>Only those three are asked. A wider net would catch recipes where bone meal, an ink sac or
 * cocoa beans mean the item rather than the color - a bone block, a book and quill, a cookie - and
 * 1.14 deliberately keeps the new dyes out of those.
 */
public class ModernDyeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    /** A grid needs a container to report changes to; this one has nobody to tell. */
    private static final Container NOBODY = new Container() {
        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    };

    @Nullable
    private static List<IRecipe> delegates;

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        InventoryCrafting copy = substitute(inv);
        return copy != null && delegate(copy, world) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        InventoryCrafting copy = substitute(inv);
        if (copy == null) {
            return ItemStack.EMPTY;
        }
        // Matched again here rather than remembered: RecipeFireworks works its result out in matches,
        // and a remembered delegate could belong to another player's grid.
        IRecipe delegate = delegate(copy, null);
        return delegate == null ? ItemStack.EMPTY : delegate.getCraftingResult(copy);
    }

    /** @return a copy of the grid with the new dyes swapped for 1.12.2's, or null if it holds none */
    @Nullable
    private static InventoryCrafting substitute(InventoryCrafting inv) {
        InventoryCrafting copy = null;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (inv.getStackInSlot(i).getItem() instanceof ItemModernDye) {
                copy = new InventoryCrafting(NOBODY, inv.getWidth(), inv.getHeight());
                break;
            }
        }
        if (copy == null) {
            return null;
        }

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() instanceof ItemModernDye) {
                stack = new ItemStack(Items.DYE, stack.getCount(), ((ItemModernDye) stack.getItem()).getColor().getDyeDamage());
            } else {
                stack = stack.copy();
            }
            copy.setInventorySlotContents(i, stack);
        }
        return copy;
    }

    @Nullable
    private static IRecipe delegate(InventoryCrafting copy, @Nullable World world) {
        for (IRecipe recipe : delegates()) {
            if (recipe.matches(copy, world)) {
                return recipe;
            }
        }
        return null;
    }

    /** The three hand-written dye recipes, as registered - found by class, whatever they were named. */
    private static synchronized List<IRecipe> delegates() {
        if (delegates == null) {
            List<IRecipe> found = new ArrayList<IRecipe>();
            for (IRecipe recipe : ForgeRegistries.RECIPES) {
                if (recipe instanceof RecipesArmorDyes || recipe instanceof RecipeFireworks
                        || recipe instanceof RecipesBanners.RecipeAddPattern) {
                    found.add(recipe);
                }
            }
            delegates = found;
        }
        return delegates;
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
