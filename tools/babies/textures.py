"""26.1's baby textures, for the baby models shown while Game Drops Backport is installed (babies.client.BabyModels).

Copies each texture from the 26.3 client jar (the newest 26.x release) into
assets/villagerbackport/textures/entity/<same path>, keyed (tRNS) transparency rewritten as alpha for 1.12.2.

Run: python -I tools/babies/textures.py   (idempotent)
"""
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png_rgba  # noqa: E402

JAR = "G:/MCDownloads/minecraft-26.3/client.jar"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..",
                   "src", "main", "resources", "assets", "villagerbackport", "textures", "entity")

CATS = ("tabby", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "white", "jellie",
        "all_black")
PANDAS = ("panda", "lazy_panda", "worried_panda", "playful_panda", "brown_panda", "weak_panda", "aggressive_panda")
TYPES = ("desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga")

TEXTURES = ["fox/fox_baby", "fox/fox_sleep_baby", "fox/fox_snow_baby", "fox/fox_snow_sleep_baby",
            "cat/cat_collar_baby", "villager/villager_baby"] \
    + ["cat/cat_%s_baby" % c for c in CATS] \
    + ["panda/%s_baby" % p for p in PANDAS] \
    + ["villager/baby/%s" % t for t in TYPES]


def main():
    jar = zipfile.ZipFile(JAR)
    for t in TEXTURES:
        data = png_rgba.keyed_to_rgba(jar.read("assets/minecraft/textures/entity/%s.png" % t))
        path = os.path.join(OUT, *(t + ".png").split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(data)
    print("%d textures" % len(TEXTURES))


if __name__ == "__main__":
    main()
