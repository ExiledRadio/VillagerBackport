"""Generates the building blocks' blockstates, models and language entries.

The source of truth is the Java table, BuildingTable.java (rows and full blocks) and SignWood.java
(the five sign woods). This script reads those files as text, so after adding or changing a row there,
run it from the repository root:

    python tools/building/generate.py

It overwrites every file it owns and nothing else, and rewrites the "# --- building ---" section at the
end of en_us.lang in place. Textures are vanilla 1.12.2's own (named in the table); the sign textures
under textures/entity/signs and textures/items are 1.14.4's, copied once from the 1.14.4 client jar.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "exiledradio", "villagerbackport", "building")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "villagerbackport")
MOD = "villagerbackport"

ROW = re.compile(r'row\("(\w+)",\s*"([\w:]+)",\s*(\d+),\s*"([\w|]+)",\s*([A-Z, ]+)\)')
FULL = re.compile(r'full\("(\w+)",\s*"(\w+)",')
WOOD = re.compile(r'^\s*[A-Z_]+\("(\w+)",\s*(\d+),\s*"(\w+)"\)', re.M)

# Names 1.14 spells differently from a plain title-casing of the registry name.
NAME_FIXES = {}

# Vanilla names 1.14 changed, so two blocks never share one name.
VANILLA_RENAMES = [
    ("tile.stoneSlab.stone.name", "Smooth Stone Slab"),
    ("tile.sandStone.smooth.name", "Cut Sandstone"),
    ("tile.redSandStone.smooth.name", "Cut Red Sandstone"),
    ("item.sign.name", "Oak Sign"),
    ("tile.sign.name", "Oak Sign"),
]


def read_table():
    table = open(os.path.join(JAVA, "BuildingTable.java"), encoding="utf-8").read()
    rows = []
    for name, base, meta, tex, shapes in ROW.findall(table):
        side, top, bottom = tex.split("|")
        rows.append({"name": name, "side": side, "top": top, "bottom": bottom,
                     "shapes": {s.strip() for s in shapes.split(",")}})
    fulls = [{"name": n, "texture": t} for n, t in FULL.findall(table)]
    woods_src = open(os.path.join(JAVA, "SignWood.java"), encoding="utf-8").read()
    woods = [{"name": n, "planks": p} for n, _, p in WOOD.findall(woods_src)]
    if not rows or not fulls or not woods:
        sys.exit("Could not read the table: rows=%d fulls=%d woods=%d" % (len(rows), len(fulls), len(woods)))
    return rows, fulls, woods


def write(path, data):
    path = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def tex(name):
    return "blocks/" + name


def model(name):
    return MOD + ":" + name


def title(name):
    return NAME_FIXES.get(name) or " ".join(w.capitalize() for w in name.split("_"))


def slab(row, lang):
    n = row["name"] + "_slab"
    t = {"bottom": tex(row["bottom"]), "top": tex(row["top"]), "side": tex(row["side"])}
    write("models/block/%s.json" % n, {"parent": "block/half_slab", "textures": t})
    write("models/block/%s_top.json" % n, {"parent": "block/upper_slab", "textures": t})
    write("models/block/%s.json" % (row["name"] + "_double_slab"), {"parent": "block/cube_bottom_top", "textures": t})
    write("blockstates/%s.json" % n, {"variants": {
        "half=bottom": {"model": model(n)},
        "half=top": {"model": model(n + "_top")}}})
    write("blockstates/%s.json" % (row["name"] + "_double_slab"),
          {"variants": {"normal": {"model": model(row["name"] + "_double_slab")}}})
    write("models/item/%s.json" % n, {"parent": model("block/" + n)})
    lang.append(("tile.%s.%s.name" % (MOD, n), title(n)))
    lang.append(("tile.%s.%s_double_slab.name" % (MOD, row["name"]), title(n)))


# Vanilla's own stairs rotations: y for a straight bottom stair facing each way.
FACING_Y = {"east": 0, "south": 90, "west": 180, "north": 270}


def stairs(row, lang):
    n = row["name"] + "_stairs"
    t = {"bottom": tex(row["bottom"]), "top": tex(row["top"]), "side": tex(row["side"])}
    write("models/block/%s.json" % n, {"parent": "block/stairs", "textures": t})
    write("models/block/%s_inner.json" % n, {"parent": "block/inner_stairs", "textures": t})
    write("models/block/%s_outer.json" % n, {"parent": "block/outer_stairs", "textures": t})
    variants = {}
    for half in ("bottom", "top"):
        for shape in ("straight", "outer_right", "outer_left", "inner_right", "inner_left"):
            for facing in ("east", "west", "south", "north"):
                y = FACING_Y[facing]
                if shape != "straight":
                    if half == "bottom" and shape.endswith("left"):
                        y -= 90
                    if half == "top" and shape.endswith("right"):
                        y += 90
                m = n if shape == "straight" else n + ("_outer" if shape.startswith("outer") else "_inner")
                v = {"model": model(m)}
                if half == "top":
                    v["x"] = 180
                y %= 360
                if y:
                    v["y"] = y
                if y or half == "top":
                    v["uvlock"] = True
                variants["facing=%s,half=%s,shape=%s" % (facing, half, shape)] = v
    write("blockstates/%s.json" % n, {"variants": variants})
    write("models/item/%s.json" % n, {"parent": model("block/" + n)})
    lang.append(("tile.%s.%s.name" % (MOD, n), title(n)))


def wall(row, lang):
    n = row["name"] + "_wall"
    t = {"wall": tex(row["side"])}
    for part in ("post", "side", "inventory"):
        write("models/block/%s_%s.json" % (n, part), {"parent": "block/wall_" + part, "textures": t})
    parts = [{"when": {"up": "true"}, "apply": {"model": model(n + "_post")}}]
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": model(n + "_side"), "uvlock": True}
        if y:
            apply["y"] = y
        parts.append({"when": {facing: "true"}, "apply": apply})
    write("blockstates/%s.json" % n, {"multipart": parts})
    write("models/item/%s.json" % n, {"parent": model("block/" + n + "_inventory")})
    lang.append(("tile.%s.%s.name" % (MOD, n), title(n)))


def full(block, lang):
    n = block["name"]
    write("models/block/%s.json" % n, {"parent": "block/cube_all", "textures": {"all": tex(block["texture"])}})
    write("blockstates/%s.json" % n, {"variants": {"normal": {"model": model(n)}}})
    write("models/item/%s.json" % n, {"parent": model("block/" + n)})
    lang.append(("tile.%s.%s.name" % (MOD, n), title(n)))


def sign(wood, lang):
    n = wood["name"] + "_sign"
    # Signs are drawn by their renderer; the block model only names the breaking particles.
    write("models/block/%s.json" % n, {"textures": {"particle": tex(wood["planks"])}})
    for block in (n, wood["name"] + "_wall_sign"):
        write("blockstates/%s.json" % block, {"variants": {"normal": {"model": model(n)}}})
        lang.append(("tile.%s.%s.name" % (MOD, block), title(n)))
    write("models/item/%s.json" % n, {"parent": "item/generated", "textures": {"layer0": MOD + ":items/" + n}})
    lang.append(("item.%s.%s.name" % (MOD, n), title(n)))


def write_lang(entries):
    path = os.path.join(ASSETS, "lang", "en_us.lang")
    text = open(path, encoding="utf-8").read()
    marker = "# --- building ---"
    block = [marker, "# Generated by tools/building/generate.py from BuildingTable.java; rerun it, don't edit."]
    block += ["%s=%s" % e for e in entries]
    block += ["# 1.14's names for the vanilla blocks it renamed."]
    block += ["%s=%s" % e for e in VANILLA_RENAMES]
    block += ["# --- end building ---"]
    section = re.compile(re.escape(marker) + r".*?# --- end building ---\n?", re.S)
    if section.search(text):
        text = section.sub(lambda _: "\n".join(block) + "\n", text)
    else:
        text = text.rstrip("\n") + "\n\n" + "\n".join(block) + "\n"
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def main():
    rows, fulls, woods = read_table()
    lang = []
    for block in fulls:
        full(block, lang)
    for row in rows:
        if "SLAB" in row["shapes"]:
            slab(row, lang)
        if "STAIRS" in row["shapes"]:
            stairs(row, lang)
        if "WALL" in row["shapes"]:
            wall(row, lang)
    for wood in woods:
        sign(wood, lang)
    write_lang(lang)
    print("%d full blocks, %d rows, %d woods, %d lang entries" % (len(fulls), len(rows), len(woods), len(lang)))


if __name__ == "__main__":
    main()
