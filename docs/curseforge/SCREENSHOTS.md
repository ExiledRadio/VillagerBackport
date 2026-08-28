# Images

Uploaded to the project's Images tab and linked from `DESCRIPTION.md` by URL. Listed here so
the page can be rebuilt without hunting for them.

| Shot | Where it sits | URL |
| --- | --- | --- |
| Trade screen | header | `1900/425/villagergui-png.png` |
| Twelve workstations | Jobs | `1900/426/workstations-png.png` |
| Stonecutter | The blocks actually work | `1900/423/stonecutter-png.png` |
| Fletching table | The blocks actually work | `1900/418/fletching-png.png` |
| Cartography table | The blocks actually work | `1900/421/map-png.png` |
| Loom | The blocks actually work | `1900/420/loom-png.png` |
| Rolled: Punch II | The blocks actually work | `1900/419/lecternmessage-png.png` |
| Demand pricing | Prices move | `1900/417/demandpricing-png.png` |
| Villager skin | Levels | `1900/424/villager-png.png` |
| Market stall | Villages | `1900/422/stalls-png.png` |
| Config screen | Config | `1900/416/config-png.png` |

All prefixed with `https://media.forgecdn.net/attachments/`.

Still worth capturing for the Images tab: a villager asleep in a bed, an iron golem spawning,
and `/village` run at night with the bed boxes drawn.

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
