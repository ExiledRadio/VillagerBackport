# Screenshots to take

Five `PASTE_URL` placeholders in `DESCRIPTION.md`, in order. Upload each to the CurseForge
project's Images tab first, then paste the forgecdn URL in.

1. **The 1.14 trade screen** — a villager with several trades, scrolled, level badge and
   experience bar visible. This is the header image, so pick the busiest merchant available.
2. **Workstations** — all twelve blocks placed together, or a village square with several in
   use. Villagers standing at them reads better than an empty row.
3. **Demand pricing** — a trade whose price has risen, with the struck-out original showing.
   Trade the same offer four or five times first.
4. **Villager skins and level badges** — three or four villagers of different professions and
   levels side by side, close enough that the badges are legible.
5. **The village command** — `/village` run in a village at night, showing the green and red
   bed boxes and the white outline, with the chat counts in shot.

Worth having beyond these, for the Images tab: a villager asleep in a bed, an iron golem
spawning, and the grindstone or stonecutter screen.

## Icon

`project-avatar.png` in the project root is the 512x512 CurseForge avatar: a bed with the
emerald above it, on the same dark squircle with the lavender sparkles the other mods use.

The gem is traced off the one on the Tomes icon rather than drawn by eye - its silhouette,
facet vertices and colours were measured out of that image, so the two are the same gem at the
same size. The background colours, corner radius and sparkle colour come from there too, and
the bed's red and cream from the Death Overhaul heart and the Enchant Recipes book.

Sparkles are placed against the subject's own silhouette rather than a bounding box, so none
of them end up half-hidden behind the bed.

`docs/curseforge/icon.png` is the same file and `icon-400.png` is CurseForge's stated 400x400.
Run `python docs/curseforge/avatar.py` to regenerate all three.
