package com.exiledradio.villagerbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.SliderOption;
import com.exiledradio.backportcore.presentation.option.ToggleOption;
import com.exiledradio.villagerbackport.VillagerBackport;
import net.minecraft.util.ResourceLocation;

/**
 * 1.14's look and feel: the changes 1.14 made beyond its content. Backport Core owns every piece only one
 * version can have at a time (panorama, splash list, world-loading screen, options screens, F3 keys) and shows
 * the newest installed version's; this class hands Core 1.14's pieces.
 *
 * <ul>
 *   <li>1.14: the village panorama (1.14.4's six faces from the asset index, which override the jar's 1.13 ones)
 *       and the jar's {@code panorama_overlay.png} (1.13's gradient, unchanged);</li>
 *   <li>the splash changes between 1.13.2's and 1.14.4's {@code splashes.txt} (16 added, 5 removed), plus 1.14's
 *       "&lt;name&gt; IS YOU", which {@code SplashManager} draws as one more pick out of the list;</li>
 *   <li>Mouse Settings' Scroll Sensitivity and Discrete Scrolling (1.14-pre2), which the hotbar scroll follows
 *       ({@code lookfeel.client.LookAndFeelClient});</li>
 *   <li>1.14.1's F3+N (survival goes to creative, as 1.14.4's {@code KeyboardHandler}) and F3+Esc (pause without
 *       the menu);</li>
 *   <li>the world-loading screen: 1.14's chunk-status grid comes from Core's table for 1.14.</li>
 * </ul>
 * Core builds 1.14's Accessibility Settings and Mouse Settings screens itself once 1.14 is installed. Menu music
 * did not change in 1.14 ({@code music.menu} is menu1 to menu4 in 1.13.2 and 1.14.4). Also, from 1.14.4's jar, as
 * Core's single slots: the title screen's Accessibility icon (19w13a), the startup screen's {@code mojang.png}
 * (19w06a; Core draws 1.14's screen and bar), and vanilla's own text as 1.14.4 words it ({@code vanilla_text/}:
 * "Chat Text Opacity", "Smooth Stone Slab" ...). The pause menu's feedback row, Use VBOs' removal and Text
 * Background Opacity are Core's own, by version.
 */
public final class LookAndFeel {

    private static final String ID = VillagerBackport.MOD_ID;

    /** 1.14's Scroll Sensitivity (Mouse Settings), 0.01 to 10: how far one wheel notch scrolls the hotbar. */
    public static final SliderOption SCROLL_SENSITIVITY = new SliderOption(ID + ":mouse_wheel_sensitivity",
            "options.mouseWheelSensitivity", OptionScreen.MOUSE, 0.01, 10.0, 0.01, 1.0).decimals(2);

    /** 1.14's Discrete Scrolling (Mouse Settings): each wheel event counts as one notch, whatever its size. */
    public static final ToggleOption DISCRETE_SCROLLING = new ToggleOption(ID + ":discrete_mouse_scroll",
            "options.discrete_mouse_scroll", OptionScreen.MOUSE, false);

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
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"))
                .addSplashes(new ResourceLocation(ID, "texts/splashes_added.txt"))
                .removeSplashes(new ResourceLocation(ID, "texts/splashes_removed.txt"))
                .addSplashes("{PLAYER} IS YOU")
                // Mouse Settings in 1.14.4: Sensitivity, Invert Mouse, Scroll Sensitivity, Discrete Scrolling, Touchscreen.
                .option(SCROLL_SENSITIVITY.after("INVERT_MOUSE"))
                .option(DISCRETE_SCROLLING.after(SCROLL_SENSITIVITY.id()))
                .titleAccessibilityIcon(new ResourceLocation(ID, "textures/gui/accessibility.png"))
                .startupLogo("textures/gui/title/mojang.png", new ResourceLocation(ID, "textures/gui/title/mojang.png"))
                .vanillaText(new ResourceLocation(ID, "vanilla_text"));
        // 1.14.1's F3+N and F3+Esc are client code (LookAndFeelClient); the version is declared here so the
        // newest installed version is the same on both sides.
        Presentation.version("1.14.1");
        // 1.14.2's F3 server chunk line, also client code.
        Presentation.version("1.14.2");
    }
}
