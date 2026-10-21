package mezz.jei.api.gui;

import mezz.jei.api.ingredients.IIngredients;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IGuiIngredientGroup<T> {

    void set(IIngredients ingredients);

    void init(int slotIndex, boolean input, int xPosition, int yPosition);
}
