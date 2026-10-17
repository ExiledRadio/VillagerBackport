package com.exiledradio.villagerbackport.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredientBlacklist;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Keeps the hidden half of each duplicated workstation out of JEI's item list.
 *
 * <p>JEI finds this class by its annotation and is the only thing that ever loads it, so without
 * JEI none of the types named here are looked for. They are compiled against stand-ins that are
 * left out of the jar - see the note in build.gradle.
 *
 * @see FutureMc
 */
@JEIPlugin
public final class JeiPlugin implements IModPlugin {

    /**
     * Runs for every plugin before any plugin's {@code register}, which is where JEI Villagers reads
     * the careers' trades. See {@link JeiTradeLists}.
     */
    @Override
    public void registerCategories(mezz.jei.api.recipe.IRecipeCategoryRegistration registry) {
        JeiTradeLists.swapIn();
    }

    /** Every plugin has registered by now, so the careers' own trades go back. */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.IJeiRuntime runtime) {
        JeiTradeLists.restore();
    }

    @Override
    public void register(IModRegistry registry) {
        IIngredientBlacklist blacklist = registry.getJeiHelpers().getIngredientBlacklist();

        for (Item item : FutureMc.hiddenItems()) {
            blacklist.addIngredientToBlacklist(new ItemStack(item));
        }
    }
}
