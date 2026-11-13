"""Converts 1.18.2's village and outpost structures where they differ from 1.14.4's.

usage: convert118.py <v14 data root> <v18 data root> <mod resources/assets/villagerbackport>

Both versions go through convert.py's template conversion. A 1.18.2 file whose converted blocks, size, jigsaws or
entities differ from the converted 1.14.4 file is written to structures/v1_18/<kind>/, which villages laid out as
1.18.2 lays them out read first (ModernTemplate.get(String, boolean)). 1.16 and later's jigsaw names and targets
are compared as 1.14's attachment type, since Mojang's upgrade of a 1.14 jigsaw sets both to it.
"""
import glob
import json
import os
import shutil
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import convert
import nbt

KINDS = ("village", "pillager_outpost")


def canon(path):
    t = nbt.plain(nbt.load(path))
    pal = t["palette"]

    def st(i):
        e = pal[i]
        return json.dumps([e["Names"], e["Props"], e["Src"], e["SrcProps"]], sort_keys=True)

    blocks = sorted(json.dumps([b["pos"], st(b["state"]), b.get("nbt")], sort_keys=True, default=str)
                    for b in t["blocks"])
    jigsaws = sorted(json.dumps([j["pos"], j["facing"], j["pool"], j.get("name", j.get("attach")),
                                 j.get("target", j.get("attach")), st(j["state"])]) for j in t["jigsaws"])
    entities = sorted(json.dumps(e, sort_keys=True, default=str) for e in t["entities"])
    return {"size": t["size"], "blocks": blocks, "jigsaws": jigsaws, "entities": entities}


def main(argv):
    v14, v18, out_root = argv[1], argv[2], argv[3]
    dest_root = os.path.join(out_root, "structures", "v1_18")
    if os.path.isdir(dest_root):
        shutil.rmtree(dest_root)
    tmp = tempfile.mkdtemp()
    written = 0
    try:
        for kind in KINDS:
            convert.kind = kind
            for src, tag in ((v14, "14"), (v18, "18")):
                root = os.path.join(src, "structures", kind)
                for p in sorted(glob.glob(os.path.join(root, "**", "*.nbt"), recursive=True)):
                    rel = os.path.relpath(p, root).replace("\\", "/")
                    convert.convert_template(p, rel, os.path.join(tmp, tag, kind))
            for p in sorted(glob.glob(os.path.join(tmp, "18", kind, "**", "*.nbt"), recursive=True)):
                rel = os.path.relpath(p, os.path.join(tmp, "18", kind)).replace("\\", "/")
                old = os.path.join(tmp, "14", kind, rel)
                if os.path.exists(old) and canon(old) == canon(p):
                    continue
                dest = os.path.join(dest_root, kind, rel)
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                shutil.copyfile(p, dest)
                written += 1
                print("changed", kind + "/" + rel)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print("templates written", written)
    print("unmapped", dict(convert.unmapped))
    print("skipped entities", dict(convert.skipped_entities))


if __name__ == "__main__":
    main(sys.argv)
