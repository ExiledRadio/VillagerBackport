package mezz.jei.api;

import java.util.Collection;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IModRegistry {

    IJeiHelpers getJeiHelpers();

    void addRecipes(Collection<?> recipes, String recipeCategoryUid);

    void addRecipeCatalyst(Object catalystIngredient, String... recipeCategoryUids);

    <T> void addIngredientInfo(T ingredient, Class<? extends T> ingredientClass, String... descriptionKeys);
}
