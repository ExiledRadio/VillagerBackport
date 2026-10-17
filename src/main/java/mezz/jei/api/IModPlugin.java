package mezz.jei.api;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IModPlugin {

    default void registerCategories(mezz.jei.api.recipe.IRecipeCategoryRegistration registry) {
    }

    default void register(IModRegistry registry) {
    }

    default void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    }
}
