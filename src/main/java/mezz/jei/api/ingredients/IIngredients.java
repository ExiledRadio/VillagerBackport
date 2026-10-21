package mezz.jei.api.ingredients;

import mezz.jei.api.recipe.IIngredientType;

import java.util.List;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IIngredients {

    <T> void setInputLists(IIngredientType<T> ingredientType, List<List<T>> inputs);

    <T> void setOutput(IIngredientType<T> ingredientType, T output);
}
