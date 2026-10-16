"""Converts 1.14.4's village structures, pools and chest loot into the mod's resources.

usage: convert.py <v14 data root> <pools_raw.json> <mod resources/assets/villagerbackport>
"""
import collections
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt
import blockmap
from nbt import Tag, TagList, STRING, COMPOUND, INT, DOUBLE, FLOAT

src_root, pools_path, out_root = sys.argv[1], sys.argv[2], sys.argv[3]
struct_root = os.path.join(src_root, "structures", "village")
out_struct = os.path.join(out_root, "structures", "village")

unmapped = collections.Counter()
skipped_entities = collections.Counter()


def parse_state_string(s):
    """'minecraft:grass_path' or 'minecraft:oak_stairs[facing=north]' -> (name, props)"""
    if "[" in s:
        name, rest = s.split("[", 1)
        props = dict(kv.split("=") for kv in rest.rstrip("]").split(",") if kv)
        return name, props
    return s, {}


def strtag(d):
    return {k: str(v) for k, v in d.items()}


class Palette:
    def __init__(self):
        self.entries = []
        self.index = {}

    def add(self, src_name, src_props):
        key = (src_name, tuple(sorted(src_props.items())))
        if key in self.index:
            return self.index[key]
        try:
            names, props, te = blockmap.map_state(src_name, src_props)
        except blockmap.Unmapped:
            unmapped[src_name] += 1
            names, props, te = ["minecraft:air"], {}, None
        entry = {"Names": TagList(STRING, names), "Props": strtag(props), "Src": src_name,
                 "SrcProps": strtag(src_props)}
        self.entries.append((entry, te))
        self.index[key] = len(self.entries) - 1
        return self.index[key]

    def te(self, idx):
        return self.entries[idx][1]

    def tag(self):
        return TagList(COMPOUND, [e for e, _ in self.entries])


def item_id(i):
    return "minecraft:" + i if ":" not in i else i


def convert_te(te14, extra):
    """1.14 tile entity tag -> the fields worth carrying into 1.12.2, merged with the mapper's own."""
    out = {}
    if extra:
        for k, v in extra.items():
            out[k] = Tag(INT, v) if isinstance(v, int) else v
    if not te14:
        return out or None
    tid = te14.get("id")
    if "LootTable" in te14:
        lt = te14["LootTable"]
        if lt.startswith("minecraft:chests/village/"):
            lt = "villagerbackport:" + lt[len("minecraft:"):]
        out["LootTable"] = lt
    if tid == "minecraft:sign":
        for k in ("Text1", "Text2", "Text3", "Text4"):
            if k in te14:
                out[k] = te14[k]
    if tid == "minecraft:banner" and "Patterns" in te14:
        pats = []
        for p in te14["Patterns"]:
            pats.append({"Pattern": p["Pattern"], "Color": Tag(INT, 15 - p["Color"].value)})
        out["Patterns"] = TagList(COMPOUND, pats)
    return out or None


def convert_entity(e):
    n = e["nbt"]
    eid = n["id"]
    pos = [x.value for x in e["pos"]]
    bpos = [x.value for x in e["blockPos"]]
    rot = [x.value for x in n.get("Rotation", [])] or [0.0, 0.0]
    out = {"pos": TagList(DOUBLE, [Tag(DOUBLE, v) for v in pos]), "block": Tag(nbt.INT_ARRAY, bpos),
           "yaw": Tag(FLOAT, rot[0])}
    if eid == "minecraft:villager":
        prof = n.get("VillagerData", {}).get("profession", "minecraft:none")
        out["kind"] = "nitwit" if prof == "minecraft:nitwit" else "villager"
        out["baby"] = Tag(nbt.BYTE, 1 if n.get("Age", Tag(INT, 0)).value < 0 else 0)
    elif eid == "minecraft:zombie_villager":
        out["kind"] = "zombie_villager"
    elif eid == "minecraft:iron_golem":
        out["kind"] = "iron_golem"
    elif eid in ("minecraft:cow", "minecraft:pig"):
        out["kind"] = eid.split(":")[1]
    elif eid == "minecraft:sheep":
        out["kind"] = "sheep"
        out["color"] = Tag(nbt.BYTE, n.get("Color", Tag(nbt.BYTE, 0)).value)
    elif eid == "minecraft:horse":
        out["kind"] = "horse"
        out["variant"] = Tag(INT, n.get("Variant", Tag(INT, 0)).value)
    elif eid == "minecraft:armor_stand":
        out["kind"] = "armor_stand"
        armour = []
        for it in n.get("ArmorItems", []):
            armour.append(item_id(it["id"]) if it and "id" in it else "")
        out["armor"] = TagList(STRING, armour)
    else:
        # Cats: 1.12.2's only cat is a tamed ocelot, and a wild ocelot is not a village cat.
        skipped_entities[eid] += 1
        return None
    return out


def convert_template(path, rel):
    t = nbt.load(path)
    pal14 = [nbt.plain(p) for p in t["palette"]]
    pal = Palette()
    blocks = []
    jigsaws = []
    for bl in t["blocks"]:
        st = pal14[bl["state"].value]
        name = st["Name"]
        props = st.get("Properties", {})
        pos = [x.value for x in bl["pos"]]
        te14 = bl.get("nbt")
        te14p = nbt.plain(te14) if te14 is not None else None
        if name == "minecraft:jigsaw":
            fname, fprops = parse_state_string(te14p.get("final_state", "minecraft:air"))
            idx = pal.add(fname, fprops)
            jigsaws.append({"pos": Tag(nbt.INT_ARRAY, pos), "facing": props["facing"],
                            "pool": te14p["target_pool"].replace("minecraft:", ""),
                            "attach": te14p["attachement_type"], "state": Tag(INT, idx)})
            blocks.append({"pos": Tag(nbt.INT_ARRAY, pos), "state": Tag(INT, idx)})
            continue
        if name == "minecraft:structure_block":
            continue
        idx = pal.add(name, props)
        entry = {"pos": Tag(nbt.INT_ARRAY, pos), "state": Tag(INT, idx)}
        te = convert_te(te14, pal.te(idx))
        if te:
            entry["nbt"] = te
        blocks.append(entry)
    ents = []
    for e in t.get("entities", []):
        c = convert_entity(e)
        if c:
            ents.append(c)
    size = [x.value for x in t["size"]]
    out = {"size": Tag(nbt.INT_ARRAY, size), "palette": pal.tag(),
           "blocks": TagList(COMPOUND, blocks), "jigsaws": TagList(COMPOUND, jigsaws),
           "entities": TagList(COMPOUND, ents)}
    dest = os.path.join(out_struct, rel)
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    nbt.save(dest, out)
    return len(blocks), len(jigsaws), len(ents)


# The TE tags come through as raw nbt values (Tag objects); convert_te reads them as plain values for
# names and keeps the Tag objects where they are copied through.
def _plain_get(d, k):
    return d.get(k)


count = 0
for path in sorted(glob.glob(os.path.join(struct_root, "**", "*.nbt"), recursive=True)):
    rel = os.path.relpath(path, struct_root).replace("\\", "/")
    convert_template(path, rel)
    count += 1


# ---- pools: map processor outputs and location tests to 1.12.2; input tests keep 1.14 names, since
# templates carry each block's 1.14 source name for exactly this.
WORLD_NAMES = {"minecraft:water": ["minecraft:water", "minecraft:flowing_water"],
               "minecraft:grass_block": ["minecraft:grass"], "minecraft:dirt": ["minecraft:dirt"],
               "minecraft:air": ["minecraft:air"], "minecraft:sand": ["minecraft:sand"]}


def map_output(state):
    names, props, te = blockmap.map_state(state["name"], state["props"])
    return {"names": names, "props": props}


def map_test(test, where):
    if where == "location" and test["kind"] == "block":
        if test["block"] not in WORLD_NAMES:
            raise SystemExit("no world mapping for location test " + test["block"])
        return {"kind": "world_block", "blocks": WORLD_NAMES[test["block"]]}
    return test


raw = json.load(open(pools_path))
pools = {}
for name, pool in raw.items():
    elems = []
    for e in pool["elements"]:
        el = e["element"]

        def fix(el):
            if el["type"] == "single":
                procs = []
                for rules in el["processors"]:
                    procs.append([{"input": map_test(r["input"], "input"),
                                   "location": map_test(r["location"], "location"),
                                   "output": map_output(r["output"])} for r in rules])
                el["processors"] = procs
                if not os.path.exists(os.path.join(struct_root, el["location"][len("village/"):] + ".nbt")):
                    raise SystemExit("missing template " + el["location"])
            if el["type"] == "list":
                for sub in el["elements"]:
                    fix(sub)
            return el

        elems.append({"element": fix(el), "weight": e["weight"]})
    pools[name] = {"fallback": pool["fallback"], "projection": pool["projection"], "elements": elems}

json.dump(pools, open(os.path.join(out_struct, "pools.json"), "w"), indent=1, sort_keys=True)


# ---- loot tables
ITEMS = {
    "acacia_sapling": ("minecraft:sapling", 4), "oak_sapling": ("minecraft:sapling", 0),
    "spruce_sapling": ("minecraft:sapling", 1), "beetroot_seeds": ("minecraft:beetroot_seeds", 0),
    "black_wool": ("minecraft:wool", 15), "brown_wool": ("minecraft:wool", 12), "gray_wool": ("minecraft:wool", 7),
    "light_gray_wool": ("minecraft:wool", 8), "white_wool": ("minecraft:wool", 0),
    "blue_ice": ("minecraft:packed_ice", 0), "cod": ("minecraft:fish", 0), "salmon": ("minecraft:fish", 1),
    "dandelion": ("minecraft:yellow_flower", 0), "poppy": ("minecraft:red_flower", 0),
    "dead_bush": ("minecraft:deadbush", 0), "fern": ("minecraft:tallgrass", 2), "grass": ("minecraft:tallgrass", 1),
    "large_fern": ("minecraft:double_plant", 3), "tall_grass": ("minecraft:double_plant", 2),
    "green_dye": ("minecraft:dye", 2), "yellow_dye": ("minecraft:dye", 11), "lapis_lazuli": ("minecraft:dye", 4),
    "golden_horse_armor": ("minecraft:golden_horse_armor", 0), "iron_horse_armor": ("minecraft:iron_horse_armor", 0),
    "diamond_horse_armor": ("minecraft:diamond_horse_armor", 0),
    "smooth_stone": ("minecraft:stone_slab", 0), "stone_bricks": ("minecraft:stonebrick", 0),
    "snow_block": ("minecraft:snow", 0), "spruce_log": ("minecraft:log", 1), "spruce_sign": ("minecraft:sign", 0),
    "barrel": ("villagerbackport:barrel", 0),
}
DROP = {"sweet_berries"}


def convert_loot(path, dest):
    j = json.load(open(path))
    j.pop("type", None)
    for pi, pool in enumerate(j.get("pools", [])):
        pool.setdefault("name", "pool%d" % pi)
        entries = []
        for entry in pool.get("entries", []):
            if entry.get("type") == "minecraft:item":
                short = entry["name"].split(":")[1]
                if short in DROP:
                    continue
                if short in ITEMS:
                    name, data = ITEMS[short]
                    entry["name"] = name
                    if data:
                        entry.setdefault("functions", []).append({"function": "minecraft:set_data", "data": data})
                entry["type"] = "item"
                entry.setdefault("entryName", entry["name"] + "#" + str(len(entries)))
            entries.append(entry)
        pool["entries"] = entries
        for fn in pool.get("functions", []):
            pass
        for entry in entries:
            for fn in entry.get("functions", []):
                fn["function"] = fn["function"].replace("minecraft:", "")
                cnt = fn.get("count")
                if isinstance(cnt, dict) and cnt.get("type") == "minecraft:uniform":
                    cnt.pop("type")
        roll = pool.get("rolls")
        if isinstance(roll, dict) and roll.get("type") == "minecraft:uniform":
            roll.pop("type")
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    json.dump(j, open(dest, "w"), indent=2)


loot_src = os.path.join(src_root, "loot_tables", "chests", "village")
for f in glob.glob(os.path.join(loot_src, "*.json")):
    convert_loot(f, os.path.join(out_root, "loot_tables", "chests", "village", os.path.basename(f)))

print("templates", count)
print("unmapped", dict(unmapped))
print("skipped entities", dict(skipped_entities))
