"""Checks that every state of every village block can be drawn.

A headless server cannot see a missing model: the merged scaffolding once listed only one of its two
properties in its blockstate, and every placed block rendered as the missing texture. This catches that
class of mistake without a client.

Input: the state list the self-test writes (tools/woodset/selftest/WoodsetSelfTest.java writes
eclipse/woodset-states.txt), one "registry_name<TAB>variant" line per valid state, where variant is
vanilla's DefaultStateMapper string (properties sorted by name, name=value joined by commas, "normal"
for none) built from the block's real getValidStates(). For each line it checks:

  * the block's blockstate file has that exact variant (vanilla format; this package uses no Forge
    format and no state mappers, so the default mapping is the one the client uses);
  * every model the variant names exists, and so does every parent up the chain, ours or 1.12.2's;
  * every texture variable the chain's elements use resolves, and the texture file exists;
  * the block's item model exists and resolves the same way;
  * the block has a language entry.

Vanilla parents and textures are looked up in the 1.12.2 client jar in the Gradle cache.

    python tools/woodset/check_blockstates.py [eclipse/woodset-states.txt]
"""
import json
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets")
MC_JAR = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "minecraft", "net", "minecraft",
                      "minecraft", "1.12.2", "minecraft-1.12.2.jar")

jar = zipfile.ZipFile(MC_JAR)
jar_names = set(jar.namelist())
errors = []


def split(name, default_dir):
    """'villagerbackport:foo' or 'foo' or 'block/foo' to (namespace, path)."""
    ns, _, path = name.rpartition(":")
    return (ns or "minecraft"), path


def read_asset(ns, path):
    """@return the parsed JSON or bytes of an asset, ours from the source tree, minecraft's from the jar."""
    if ns == "minecraft":
        key = "assets/minecraft/" + path
        if key in jar_names:
            return jar.read(key)
        return None
    full = os.path.join(ASSETS, ns, *path.split("/"))
    if os.path.exists(full):
        with open(full, "rb") as f:
            return f.read()
    return None


def model_chain(ns, path, where):
    """@return the list of model dicts from child to root, or None after recording an error."""
    chain = []
    seen = set()
    while True:
        raw = read_asset(ns, "models/" + path + ".json")
        if raw is None:
            if path.startswith("builtin/"):
                return chain
            errors.append("%s: missing model %s:%s" % (where, ns, path))
            return None
        data = json.loads(raw.decode("utf-8"))
        chain.append(data)
        parent = data.get("parent")
        if not parent:
            return chain
        if parent.startswith("builtin/"):
            return chain
        ns, path = split(parent, "")
        if (ns, path) in seen:
            errors.append("%s: parent loop at %s" % (where, parent))
            return None
        seen.add((ns, path))


def check_model(name, where, block_model=True):
    ns, path = split(name, "")
    if "/" not in path:
        path = ("block/" if block_model else "item/") + path
    chain = model_chain(ns, path, where)
    if chain is None:
        return
    textures = {}
    for data in reversed(chain):
        textures.update(data.get("textures", {}))
    used = set()
    for data in chain:
        for element in data.get("elements", []):
            for face in element.get("faces", {}).values():
                used.add(face["texture"])
    used.update("#" + k for k in textures)
    if any(d.get("parent") == "item/generated" or d.get("parent") == "builtin/generated" for d in chain):
        used.add("#layer0")
    for ref in used:
        value = ref
        hops = 0
        while value.startswith("#") and hops < 10:
            value = textures.get(value[1:], None)
            hops += 1
            if value is None:
                break
        if value is None or value.startswith("#"):
            # A variable nothing uses at the end of the chain is fine; one an element uses is not.
            if ref in {f for d in chain for e in d.get("elements", []) for f in [x["texture"] for x in e.get("faces", {}).values()]}:
                errors.append("%s: model %s leaves %s unresolved" % (where, name, ref))
            continue
        tns, tpath = split(value, "")
        if read_asset(tns, "textures/" + tpath + ".png") is None:
            errors.append("%s: model %s needs missing texture %s" % (where, name, value))


def main():
    states_file = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "eclipse", "woodset-states.txt")
    lines = [l.rstrip("\n").split("\t") for l in open(states_file, encoding="utf-8") if l.strip()]
    lang = open(os.path.join(ASSETS, "villagerbackport", "lang", "en_us.lang"), encoding="utf-8").read()
    blocks = {}
    for registry, variant in lines:
        blocks.setdefault(registry, []).append(variant)

    checked = 0
    for registry, variants in sorted(blocks.items()):
        ns, path = split(registry, "")
        raw = read_asset(ns, "blockstates/%s.json" % path)
        if raw is None:
            errors.append("%s: no blockstate file" % registry)
            continue
        data = json.loads(raw.decode("utf-8"))
        if "forge_marker" in data or "multipart" in data:
            errors.append("%s: expected a plain vanilla variants file" % registry)
            continue
        listed = data.get("variants", {})
        for variant in variants:
            if variant not in listed:
                errors.append("%s: blockstate has no variant '%s'" % (registry, variant))
                continue
            entries = listed[variant]
            for entry in entries if isinstance(entries, list) else [entries]:
                check_model(entry["model"] if ":" in entry["model"] else entry["model"], "%s[%s]" % (registry, variant))
            checked += 1
        extra = set(listed) - set(variants)
        if extra:
            errors.append("%s: blockstate lists states the block cannot be in: %s" % (registry, sorted(extra)))
        check_model("%s:item/%s" % (ns, path), registry + " item", block_model=False)
        if "tile.%s.%s.name=" % (ns, path) not in lang:
            errors.append("%s: no language entry" % registry)

    sounds = json.loads(open(os.path.join(ASSETS, "villagerbackport", "sounds.json"), encoding="utf-8").read())
    for sound in sounds["item.axe.strip"]["sounds"]:
        sns, spath = split(sound["name"], "")
        if read_asset(sns, "sounds/%s.ogg" % spath) is None:
            errors.append("item.axe.strip: missing %s" % sound["name"])

    for e in errors:
        print("FAIL", e)
    print("%d blocks, %d states checked, %d errors" % (len(blocks), checked, len(errors)))
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
