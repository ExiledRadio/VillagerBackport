package mezz.jei.api.recipe;

import mezz.jei.api.ingredients.IIngredients;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IRecipeWrapper {

    void getIngredients(IIngredients ingredients);
}
