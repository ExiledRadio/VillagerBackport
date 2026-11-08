package com.exiledradio.villagerbackport.lookfeel.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14.1's F3+Esc screen: 1.14.4's {@code PauseScreen(false)}. It pauses the game like the pause menu, with no
 * buttons and no darkened background; "Game paused" is drawn at the top. Esc closes it, as any screen.
 */
@SideOnly(Side.CLIENT)
public final class GuiPauseOnly extends GuiScreen {

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawCenteredString(this.fontRenderer, I18n.format("menu.paused"), this.width / 2, 10, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
