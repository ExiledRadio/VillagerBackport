package mezz.jei.api.ingredients;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IIngredientBlacklist {

    <V> void addIngredientToBlacklist(V ingredient);
}
