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
