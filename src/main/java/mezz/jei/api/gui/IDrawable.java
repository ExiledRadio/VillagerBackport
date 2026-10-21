package mezz.jei.api.gui;

import net.minecraft.client.Minecraft;

/** Stand-in for JEI's own, compiled against and never shipped. See build.gradle. */
public interface IDrawable {

    int getWidth();

    int getHeight();

    void draw(Minecraft minecraft, int xOffset, int yOffset);
}
