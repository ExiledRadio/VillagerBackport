# In-game checklist

Things only a person in a real game can confirm. Each line says what to look at and what vanilla does.

## 1.14's look and feel (2.5.0, with Backport Core 1.1.0)

Self-tests: `gradlew runServer -Pselftest=all` (delete `eclipse/world` first: registration, assets, splash
pool, options, the scroll arithmetic, lang) and `gradlew runClient -Pselftest=client` (title screen, log only).

- [ ] **Title screen**: 1.14's village panorama (plains village, 1.14.4's images) turns slowly, sharp, with 1.13's
      gradient over it. With a newer update backport installed, that one's panorama shows instead
      (Backport Core's `presentation.panorama`; set it to `1.14` to keep this one).
- [ ] **Splashes**: 1.14 ones show over several launches ("Rainbow turtle?", "Ping the human!"; rarely
      "<YOUR NAME> IS YOU"), never "Made by Notch!" or "Woo, minecraftforum!".
- [ ] **World loading**: the chunk-status grid (43 by 43, colored by generation stage) with a percentage.
- [ ] **Options**: "Accessibility Settings..." where Snooper Settings was; Controls has "Mouse Settings...".
- [ ] **Mouse Settings**: Sensitivity, Invert Mouse, **Scroll Sensitivity** (shows "1.00"), **Discrete
      Scrolling**, Touchscreen Mode.
- [ ] **Hotbar scrolling**: at Scroll Sensitivity 1.00 one wheel notch moves one slot (as before); at 0.25 it
      takes four notches; a touchpad's small scrolls add up instead of each moving a slot; Discrete Scrolling
      counts every scroll event as one notch. Worth checking on macOS and Linux too (LWJGL 2 reports 120 per notch).
- [ ] **F3 + N** with cheats: Survival to Creative, Creative to Spectator, Spectator to Creative.
- [ ] **F3 + Esc** in singleplayer: the game pauses with only "Game paused" at the top, no buttons and no dark
      background; Esc resumes. Plain Esc still opens the pause menu.
- [ ] **Chat Text Opacity** on Chat and Accessibility Settings, also in another language (Mojang's translation).
- [ ] Menu music unchanged (1.14 kept menu1 to menu4).

## 1.14's pieces in Backport Core's finished presentation module (Core 1.1.0)

- [ ] **Startup**: as loading ends, the white Mojang screen (1.14.4's logo) with a full red bar, fading into the
      title screen; then the title screen fades in.
- [ ] **Title screen**: the Accessibility icon right of Options/Quit (1.14.4's icon, lighter when hovered) opens
      Accessibility Settings.
- [ ] **Pause menu**: 1.14's grid: Give Feedback and Report Bugs (each asks before opening the link) between
      Advancements/Statistics and Options/Open to LAN; Forge's Mod Options below Save and Quit.
- [ ] **Video Settings**: no Use VBOs (VBOs on), no 3D Anaglyph; GUI Scale counts Auto, 1, 2, 3 ...
- [ ] **Accessibility / Chat Settings**: Text Background Opacity changes only the chat's background; Text
      Background "Everywhere" darkens the chat input box too.
- [ ] **F3** in singleplayer: "ServerChunkCache: n Drop: m" right under the client's chunk cache line.
- [ ] **Text**: "Chat Text Opacity", "Smooth Stone Slab", "Oak Trapdoor" (1.14.4's wording through Core).
