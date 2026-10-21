package mezz.jei.api;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IJeiRuntime {

    IRecipeRegistry getRecipeRegistry();
}
