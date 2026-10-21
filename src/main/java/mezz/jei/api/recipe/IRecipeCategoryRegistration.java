package mezz.jei.api.recipe;

import mezz.jei.api.IJeiHelpers;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IRecipeCategoryRegistration {

    void addRecipeCategories(IRecipeCategory... recipeCategories);

    IJeiHelpers getJeiHelpers();
}
