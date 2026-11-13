# 1.14 village port

Regenerates `src/main/resources/assets/villagerbackport/structures/village/` (structure files and
`pools.json`) and `loot_tables/chests/village/` from Minecraft 1.14.4's own data.

1. Extract `data/minecraft/structures/village/` and `data/minecraft/loot_tables/chests/village/` from
   the 1.14.4 client jar into a folder, say `v14/data/minecraft/`.
2. Parse the pool definitions out of 1.14.4's decompiled `*VillagePools.java` (in
   `net/minecraft/world/gen/feature/structure/`):

   ```
   python -I -c "import sys,json,glob; sys.path.insert(0,'tools/village-port'); import pools; P={}; [pools.parse_file(f,P) for f in glob.glob('<src>/net/minecraft/world/gen/feature/structure/*VillagePools.java')]; json.dump(P,open('pools_raw.json','w'))"
   ```
3. Convert:

   ```
   python -I tools/village-port/convert.py v14/data/minecraft pools_raw.json src/main/resources/assets/villagerbackport
   ```

`blockmap.py` is the 1.14 to 1.12.2 block table. The converter reports any 1.14 block it has no
mapping for; there should be none. Each converted palette entry keeps its original 1.14 name and
properties beside the 1.12.2 ones, because 1.14's pool processors match on the 1.14 names.

## Pillager outposts

The same converter handles 1.14's outposts. Extract `data/minecraft/structures/pillager_outpost/` and
`data/minecraft/loot_tables/chests/pillager_outpost.json` alongside the village data, parse the pools
from `PillagerOutpostPieces.java` with `pools.parse_file` as above, and pass the kind:

```
python -I tools/village-port/convert.py v14/data/minecraft outpost_pools.json src/main/resources/assets/villagerbackport pillager_outpost
```

The chest's crossbow is dropped from the JSON and added at load time by `OutpostGenerator`, from
whichever mod adds a crossbow.

## 1.18.2's changed templates

Mojang changed some of these files after 1.14.4: more jigsaws on the outpost's feature plate and on some houses
and streets, terminators that ask for nothing, a resized savanna mason, fixed beds, doors and snow. Villages and
outposts laid out as 1.18.2 lays them out (Backport Core's 1.18 rule starts) read the 1.18.2 file where it differs.
Extract the same folders from the 1.18.2 jar as from the 1.14.4 one, then:

```
python -I tools/village-port/convert118.py v14/data/minecraft v18/data/minecraft src/main/resources/assets/villagerbackport
```

It converts both versions and writes the 1.18.2 file to `structures/v1_18/` wherever the converted blocks, size,
jigsaws or entities differ, after putting 1.18.2's renamed blocks and new properties back to 1.14's. A 1.18.2
jigsaw keeps its name, target, top and joint; a 1.14 one keeps its attachment type, which is both its name and its
target.
