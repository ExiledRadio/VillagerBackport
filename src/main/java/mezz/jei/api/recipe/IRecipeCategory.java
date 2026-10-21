package mezz.jei.api.recipe;

import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IRecipeCategory<T extends IRecipeWrapper> {

    String getUid();

    String getTitle();

    String getModName();

    IDrawable getBackground();

    default IDrawable getIcon() {
        return null;
    }

    void setRecipe(IRecipeLayout recipeLayout, T recipeWrapper, IIngredients ingredients);
}
