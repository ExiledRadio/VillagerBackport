package mezz.jei.api;

import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import net.minecraft.util.ResourceLocation;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IGuiHelper {

    IDrawableStatic createDrawable(ResourceLocation resourceLocation, int u, int v, int width, int height);

    <V> IDrawable createDrawableIngredient(V ingredient);
}
