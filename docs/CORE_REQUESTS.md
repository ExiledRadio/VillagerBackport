# Requests for Backport Core

Things this mod needs from Backport Core (`E:\MincraftModding\Workspaces\BackportCore`). This repo never edits
Core; each request says what is wanted and what this mod does until it exists.

## 1. Look and feel: 1.14 pieces the `presentation` module does not cover yet (Core 1.2.0)

This mod registers 1.14 and 1.14.1 with `Presentation.version` (panorama, splashes, Scroll Sensitivity and
Discrete Scrolling, F3+N). These 1.14 items need a single owner or a screen Core builds:

*   **Title screen Accessibility button (19w13a)**: the small icon button beside Language. Core's docs note it needs
    1.14's `textures/gui/accessibility.png`; this mod can ship it. Wanted: `Contribution.titleAccessibilityButton(rl)`.
*   **Startup screen loading bar (19w06a)**: the Mojang screen's progress bar and its fade into the title screen.
    Wanted with the startup-logo slot (also asked for by the 1.16 backport).
*   **Pause menu (18w43a)**: "Give Feedback" and "Report Bugs" buttons. Wanted: a pause-menu owner, newest wins.
    F3+Esc's pause without the menu (1.14.1) is done here meanwhile (`lookfeel/client/GuiPauseOnly`), by swapping the
    menu as it opens while F3 is held.
*   **Text Background Opacity and Text Background (Accessibility, 1.14)**: the chat's background darkness apart
    from its text, and "Chat Only / Everywhere". Both need one owner of the chat renderer (`GuiNewChat`). Not shown
    until then, since an option that does nothing would mislead.
*   **Video Settings: Use VBOs removed (18w44a).** Vanilla's own entries can't be removed through Core yet. Wanted:
    `Contribution.removeVanillaOption("USE_VBO")` (VBOs then always on).
*   **Scroll Sensitivity's slider is logarithmic** in 1.14.4 (`LogaritmicProgressOption`, 0.01 to 10): the middle
    of the bar is 0.32, not 5. Wanted: `SliderOption.logarithmic()`. Until then the bar is linear; the values and
    the "%.2f" label are 1.14.4's.
*   **Scrolling in screens**: 1.14 also scales scrolling inside screens (lists, the creative inventory) by Scroll
    Sensitivity. 1.12.2's screens read the wheel themselves; this mod only does the hotbar.
*   **Debug screen**: Alt+F3's server tick chart (18w43c) and the server chunk count (1.14.2) need a debug-overlay
    owner (also asked for by the 1.16 backport).
*   **Keys that later versions keep.** F3+Esc and F3+N's 1.14.1 behavior only exist while this mod is installed;
    with only a newer backport installed they are missing although vanilla still has them. Wanted later: Core
    carrying vanilla's own key table per version, so the newest version's full map applies.

## Done in Backport Core 1.2.0 (core-finish, 2026-11-08)

Used here on branch `vp-lookfeel2`: the title-screen Accessibility button (this mod's icon), the startup screen
(this mod's 1.14.4 `mojang.png`; Core draws the bar), the pause menu's feedback row and Use VBOs' removal (Core's
1.14 table), Text Background Opacity and Text Background (Core's chat owner), the F3 text pipeline (1.14.2's server
chunk line, registered here), and vanilla text by version (`vanilla_text/`). Still open: Scroll Sensitivity's
logarithmic slider, scrolling inside screens, Alt+F3's tick chart (a chart, not text), and keys later versions
keep.
