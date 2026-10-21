package mezz.jei.api.recipe;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IIngredientType<T> {

    Class<? extends T> getIngredientClass();
}
