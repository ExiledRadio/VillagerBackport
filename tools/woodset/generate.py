"""Generates the village blocks' blockstates, models and language entries (package woodset).

The source of truth for the woods is Wood.java, read here as text. After changing it, run from the
repository root:

    python tools/woodset/generate.py

It overwrites every file it owns and nothing else, and rewrites the "# --- village-blocks ---" section
at the end of en_us.lang in place. Then run tools/woodset/check_blockstates.py.

Assets from 1.14.4: the stripped log, trapdoor, blue ice and sea pickle textures and the sea pickle
models are copied from the 1.14.4 client jar when it is found at JAR_114 (they are committed, so the
jar is only needed to refresh them). Buttons and pressure plates use 1.12.2's own planks textures, so
they match the planks of a 1.12.2 world; trapdoor, button and pressure plate models are 1.12.2's
vanilla ones (block/trapdoor_bottom and so on) with the texture swapped, and their blockstates copy
the rotations of vanilla 1.12.2's trapdoor.json, wooden_button.json and wooden_pressure_plate.json.
"""
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "exiledradio", "villagerbackport", "woodset")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "villagerbackport")
MOD = "villagerbackport"
JAR_114 = "G:/Sort this shit/Minecraft/Install/versions/1.14.4/1.14.4.jar"

WOOD = re.compile(r'^\s*[A-Z_]+\("(\w+)",\s*BlockPlanks\.EnumType\.\w+,\s*"(\w+)"\)', re.M)

# 1.14's names for the vanilla blocks it renamed. Vanilla's wooden button shares "tile.button.name"
# with the stone button, so it cannot be renamed without renaming that too, and is left alone.
VANILLA_RENAMES = [
    ("tile.trapdoor.name", "Oak Trapdoor"),
    ("tile.pressurePlateWood.name", "Oak Pressure Plate"),
]

PICKLE_MODELS = ["sea_pickle", "two_sea_pickles", "three_sea_pickles", "four_sea_pickles",
                 "dead_sea_pickle", "two_dead_sea_pickles", "three_dead_sea_pickles", "four_dead_sea_pickles"]


def read_woods():
    src = open(os.path.join(JAVA, "Wood.java"), encoding="utf-8").read()
    woods = [{"name": n, "planks": p} for n, p in WOOD.findall(src)]
    if len(woods) != 6:
        sys.exit("Could not read Wood.java: found %d woods" % len(woods))
    return woods


def write(path, data):
    path = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def model(name):
    return MOD + ":" + name


def tex(name):
    return MOD + ":blocks/" + name


def title(name):
    return " ".join(w.capitalize() for w in name.split("_"))


def name_of(lang, n):
    lang.append(("tile.%s.%s.name" % (MOD, n), title(n)))


def stripped(wood, lang):
    w = wood["name"]
    log, bark = "stripped_%s_log" % w, "stripped_%s_wood" % w
    write("models/block/%s.json" % log, {"parent": "block/cube_column", "textures": {
        "end": tex(log + "_top"), "side": tex(log)}})
    write("models/block/%s.json" % bark, {"parent": "block/cube_all", "textures": {"all": tex(log)}})
    # The log's rotations are vanilla's oak_log.json; axis=none is drawn as stripped wood, as 1.12.2
    # draws a log's axis=none as bark.
    write("blockstates/%s.json" % log, {"variants": {
        "axis=y": {"model": model(log)},
        "axis=z": {"model": model(log), "x": 90},
        "axis=x": {"model": model(log), "x": 90, "y": 90},
        "axis=none": {"model": model(bark)}}})
    write("blockstates/%s.json" % bark, {"variants": {
        "axis=y": {"model": model(bark)},
        "axis=z": {"model": model(bark), "x": 90},
        "axis=x": {"model": model(bark), "x": 90, "y": 90},
        "axis=none": {"model": model(bark)}}})
    for n in (log, bark):
        write("models/item/%s.json" % n, {"parent": model("block/" + n)})
        name_of(lang, n)


def trapdoor(wood, lang):
    n = wood["name"] + "_trapdoor"
    for part in ("bottom", "top", "open"):
        write("models/block/%s_%s.json" % (n, part), {"parent": "block/trapdoor_" + part, "textures": {"texture": tex(n)}})
    variants = {}
    for open_ in ("false", "true"):
        for half in ("bottom", "top"):
            for facing, y in (("north", 0), ("south", 180), ("east", 90), ("west", 270)):
                if open_ == "false":
                    v = {"model": model("%s_%s" % (n, half))}
                else:
                    v = {"model": model(n + "_open")}
                    if y:
                        v["y"] = y
                variants["facing=%s,half=%s,open=%s" % (facing, half, open_)] = v
    write("blockstates/%s.json" % n, {"variants": variants})
    write("models/item/%s.json" % n, {"parent": model("block/%s_bottom" % n)})
    name_of(lang, n)


# Vanilla's wooden_button.json rotations, by facing.
BUTTON_ROT = {"up": (0, 0), "down": (180, 0), "east": (90, 90), "west": (90, 270), "south": (90, 180), "north": (90, 0)}


def button(wood, lang):
    n = wood["name"] + "_button"
    t = {"texture": "blocks/" + wood["planks"]}
    write("models/block/%s.json" % n, {"parent": "block/button", "textures": t})
    write("models/block/%s_pressed.json" % n, {"parent": "block/button_pressed", "textures": t})
    write("models/block/%s_inventory.json" % n, {"parent": "block/button_inventory", "textures": t})
    variants = {}
    for powered in ("false", "true"):
        for facing, (x, y) in BUTTON_ROT.items():
            v = {"model": model(n if powered == "false" else n + "_pressed"), "uvlock": True}
            if x:
                v["x"] = x
            if y:
                v["y"] = y
            variants["facing=%s,powered=%s" % (facing, powered)] = v
    write("blockstates/%s.json" % n, {"variants": variants})
    write("models/item/%s.json" % n, {"parent": model("block/%s_inventory" % n)})
    name_of(lang, n)


def pressure_plate(wood, lang):
    n = wood["name"] + "_pressure_plate"
    t = {"texture": "blocks/" + wood["planks"]}
    write("models/block/%s_up.json" % n, {"parent": "block/pressure_plate_up", "textures": t})
    write("models/block/%s_down.json" % n, {"parent": "block/pressure_plate_down", "textures": t})
    write("blockstates/%s.json" % n, {"variants": {
        "powered=false": {"model": model(n + "_up")},
        "powered=true": {"model": model(n + "_down")}}})
    write("models/item/%s.json" % n, {"parent": model("block/%s_up" % n)})
    name_of(lang, n)


def blue_ice(lang):
    write("models/block/blue_ice.json", {"parent": "block/cube_all", "textures": {"all": tex("blue_ice")}})
    write("blockstates/blue_ice.json", {"variants": {"normal": {"model": model("blue_ice")}}})
    write("models/item/blue_ice.json", {"parent": model("block/blue_ice")})
    name_of(lang, "blue_ice")


def sea_pickle(lang, jar):
    if jar is not None:
        for m in PICKLE_MODELS:
            data = json.loads(jar.read("assets/minecraft/models/block/%s.json" % m).decode("utf-8"))
            if data.get("parent") != "block/block":
                sys.exit("1.14's %s.json has parent %s; check it by hand" % (m, data.get("parent")))
            for key, value in list(data["textures"].items()):
                if value != "block/sea_pickle":
                    sys.exit("1.14's %s.json uses texture %s; check it by hand" % (m, value))
                data["textures"][key] = tex("sea_pickle")
            write("models/block/%s.json" % m, data)
    variants = {}
    for wet in ("false", "true"):
        for count, live in enumerate(PICKLE_MODELS[:4], 1):
            m = live if wet == "true" else PICKLE_MODELS[3 + count]
            variants["pickles=%d,waterlogged=%s" % (count, wet)] = [
                dict({"model": model(m)}, **({"y": y} if y else {})) for y in (0, 90, 180, 270)]
    write("blockstates/sea_pickle.json", {"variants": variants})
    write("models/item/sea_pickle.json", {"parent": "item/generated", "textures": {"layer0": MOD + ":items/sea_pickle"}})
    name_of(lang, "sea_pickle")


def copy_textures(jar, woods):
    names = []
    for wood in woods:
        names += ["stripped_%s_log" % wood["name"], "stripped_%s_log_top" % wood["name"]]
        if wood["name"] != "oak":
            names.append("%s_trapdoor" % wood["name"])
    names += ["blue_ice", "sea_pickle"]
    for n in names:
        out = os.path.join(ASSETS, "textures", "blocks", n + ".png")
        with open(out, "wb") as f:
            f.write(jar.read("assets/minecraft/textures/block/%s.png" % n))
    with open(os.path.join(ASSETS, "textures", "items", "sea_pickle.png"), "wb") as f:
        f.write(jar.read("assets/minecraft/textures/item/sea_pickle.png"))
    return len(names) + 1


def write_lang(entries):
    path = os.path.join(ASSETS, "lang", "en_us.lang")
    text = open(path, encoding="utf-8").read()
    marker = "# --- village-blocks ---"
    end = "# --- end village-blocks ---"
    block = [marker, "# Generated by tools/woodset/generate.py from Wood.java; rerun it, don't edit."]
    block += ["%s=%s" % e for e in entries]
    block += ["subtitles.%s.item.axe.strip=Axe strips" % MOD]
    block += ["# 1.14's names for the vanilla blocks it renamed."]
    block += ["%s=%s" % e for e in VANILLA_RENAMES]
    block += [end]
    section = re.compile(re.escape(marker) + r".*?" + re.escape(end) + r"\n?", re.S)
    if section.search(text):
        text = section.sub(lambda _: "\n".join(block) + "\n", text)
    else:
        text = text.rstrip("\n") + "\n\n" + "\n".join(block) + "\n"
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def main():
    woods = read_woods()
    jar = zipfile.ZipFile(JAR_114) if os.path.exists(JAR_114) else None
    lang = []
    for wood in woods:
        stripped(wood, lang)
    for kind in (trapdoor, button, pressure_plate):
        for wood in woods:
            if wood["name"] != "oak":
                kind(wood, lang)
    blue_ice(lang)
    sea_pickle(lang, jar)
    copied = copy_textures(jar, woods) if jar is not None else 0
    write_lang(lang)
    print("%d woods, %d lang entries, %d textures copied%s" % (
        len(woods), len(lang), copied, "" if jar else " (1.14.4 jar not found; kept the committed ones)"))


if __name__ == "__main__":
    main()
