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

## Waterlogging (2.5.0, through Backport Core and Fluidlogged API)

Self-test: `gradlew runServer -Pselftest=waterlog` (which blocks hold water, buckets, campfires, sea pickle light,
the stored-flag conversion, bone meal spread with a stand-in coral block). What it cannot see:

- [ ] **Rendering**: water draws inside a waterlogged lantern (standing and hanging), campfire, scaffolding and
      sea pickle, with no see-through faces or flicker.
- [ ] **Campfire**: a water bucket on a lit campfire waterlogs it with the hiss and a burst of smoke plumes;
      it can't be relit until the water is taken out with an empty bucket.
- [ ] **Sea pickle**: the live (glowing tops) model only while waterlogged, the dead one dry; the light
      changes at once when the water comes or goes.
- [ ] **Old world**: a 2.4.0 world with pickles on a sea floor loads with the pickles still lit and in water.
- [ ] **With coral blocks** (from another mod): bone meal on a waterlogged pickle on one spreads pickles.

## Creative tabs (with Backport Core 1.1.0)

The places are in `docs/CREATIVE_TABS.md`; a dev client already checked the lists, the search tab and JEI.

- [ ] There is no tab for this mod; every vanilla tab keeps its icon, name, background and scroll bar.
- [ ] A few places: stripped logs right after the dark oak log, the 1.14 walls after the mossy cobblestone wall, the workstations, bell, lantern and campfire at the end of Decoration Blocks, the fox egg after the evoker egg, the crossbow at the end of Combat with its three enchanted books.
- [ ] The search tab finds the mod's items, and JEI lists them.
- [ ] Other mods' items are still in their tabs, in their own order.
- [ ] With another update backport installed too, its items and this mod's sit together by kind (stripped logs, slabs).
