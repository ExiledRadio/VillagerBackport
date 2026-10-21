package mezz.jei.api;

import java.util.Collection;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IModRegistry {

    IJeiHelpers getJeiHelpers();

    void addRecipes(Collection<?> recipes, String recipeCategoryUid);

    void addRecipeCatalyst(Object catalystIngredient, String... recipeCategoryUids);
}
