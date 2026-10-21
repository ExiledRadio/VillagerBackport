package mezz.jei.api;

import mezz.jei.api.ingredients.IIngredientBlacklist;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IJeiHelpers {

    IGuiHelper getGuiHelper();

    IIngredientBlacklist getIngredientBlacklist();
}
