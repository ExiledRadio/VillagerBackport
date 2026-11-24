package com.exiledradio.villagerbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.villagerbackport.VillagerBackport;
import net.minecraft.util.ResourceLocation;

/**
 * 1.14's look and feel: the changes 1.14 made beyond its content. Backport Core owns every piece only one
 * version can have at a time (panorama, splash list, world-loading screen, options screens, F3 keys) and shows
 * the newest installed version's; this class declares 1.14, 1.14.1 and 1.14.2 installed and hands Core what stays
 * this mod's: the village panorama (1.14.4's six faces from the asset index, which override the jar's 1.13 ones)
 * and the jar's {@code panorama_overlay.png} (1.13's gradient, unchanged).
 *
 * <p>Since Backport Core 1.4.7, Core itself has 1.14's other pieces, whichever backport is installed: the splash
 * changes, Mouse Settings' Scroll Sensitivity and Discrete Scrolling (still {@code villagerbackport:} ids, so saved
 * values carry over) with the hotbar scroll, 1.14.1's F3 + N and F3 + Esc, 1.14.2's F3 server chunk line, the title
 * screen's Accessibility icon, the startup screen's {@code mojang.png} and vanilla's text as 1.14.4 words it. Core
 * builds 1.14's Accessibility Settings and Mouse Settings screens, the world-loading grid, the pause menu's feedback
 * row, Use VBOs' removal and Text Background Opacity by version, as before.
 */
public final class LookAndFeel {

    private static final String ID = VillagerBackport.MOD_ID;

    private static boolean registered;

    private LookAndFeel() {
    }

    /** Pre-init, both sides. */
    public static void preInit() {
        if (registered) {
            return;
        }
        registered = true;
        Contribution v114 = Presentation.version("1.14");
        v114.panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"));
        // 1.14.1's F3 + N and F3 + Esc and 1.14.2's F3 server chunk line are Backport Core's; the versions are
        // declared here, so the newest installed version is the same on both sides.
        Presentation.version("1.14.1");
        Presentation.version("1.14.2");
    }
}
