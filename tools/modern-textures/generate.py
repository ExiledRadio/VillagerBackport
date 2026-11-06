"""Builds the built-in "modern textures" packs from the vanilla client jars.

    python -I tools/modern-textures/generate.py <1.12.2.jar> <1.14.4.jar> <1.15.jar>

Writes src/main/resources/villagerbackport_packs/:
  modern/   1.14.4 (and the one real 1.15) textures, saved under their 1.12.2 names, for every
            vanilla block, item, mob and armor texture whose pixels changed. On by default.
  classic/  1.12.2 textures for the few of this mod's own textures that are 1.14 copies of
            something 1.12.2 already had. Only used with the option off.
Each pack gets an index.txt listing its files, so the game never has to probe the classpath.

How a 1.12.2 name finds its 1.13+ name, most trusted first:
  1. manual_map.py
  2. the same path with blocks/ -> block/, items/ -> item/
  3. models of the same name in both versions, paired by texture variable
  4. a pixel-identical image (the texture only moved)
Anything still unmapped stops the script.
"""
import glob
import hashlib
import io
import json
import os
import shutil
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from manual_map import MANUAL, EXCLUDE, EXCLUDE_EXACT, LAYOUT_ONLY_115, EXTRA_115, CLASSIC_MOD  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(REPO, 'src', 'main', 'resources', 'villagerbackport_packs')
SCOPES = ('blocks/', 'items/', 'entity/', 'models/armor/')
TEX = 'assets/minecraft/textures/'
MOD_TEX = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'villagerbackport', 'textures')


class Jar:
    def __init__(self, path):
        self.z = zipfile.ZipFile(path)
        self.names = set(self.z.namelist())
        self.tex = {n[len(TEX):-4] for n in self.names if n.startswith(TEX) and n.endswith('.png')}
        self._h = {}

    def read(self, name):
        return self.z.read(name)

    def png(self, tex):
        return self.read(TEX + tex + '.png')

    def mcmeta(self, tex):
        n = TEX + tex + '.png.mcmeta'
        return self.read(n) if n in self.names else None

    def hash(self, tex):
        if tex not in self._h:
            self._h[tex] = pixel_hash(self.png(tex))
        return self._h[tex]

    def model_textures(self):
        res = {}
        for n in self.names:
            if n.startswith('assets/minecraft/models/') and n.endswith('.json'):
                try:
                    j = json.loads(self.read(n))
                except ValueError:
                    continue
                t = j.get('textures') or {}
                res[n[len('assets/minecraft/models/'):-5]] = {
                    k: v.replace('minecraft:', '') for k, v in t.items()
                    if isinstance(v, str) and not v.startswith('#')}
        return res


def pixel_hash(data):
    im = Image.open(io.BytesIO(data)).convert('RGBA')
    px = bytearray(im.tobytes())
    # fully transparent pixels can hold any color; blank them so they compare equal
    for i in range(3, len(px), 4):
        if px[i] == 0:
            px[i - 3] = px[i - 2] = px[i - 1] = 0
    return hashlib.sha1(bytes(px) + repr(im.size).encode()).hexdigest()


def rule(n):
    if n.startswith('blocks/'):
        return 'block/' + n[7:]
    if n.startswith('items/'):
        return 'item/' + n[6:]
    return n


def excluded(n):
    return n in EXCLUDE_EXACT or n.startswith(EXCLUDE)


def build_map(old, new):
    mo, mn = old.model_textures(), new.model_textures()
    model_pairs = {}
    for name, tex in mo.items():
        for var, val in tex.items():
            nv = mn.get(name, {}).get(var)
            if nv and val in old.tex and nv in new.tex:
                model_pairs.setdefault(val, set()).add(nv)
    by_hash = {}
    for t in new.tex:
        if t.startswith(('block/', 'item/', 'entity/', 'models/')):
            by_hash.setdefault(new.hash(t), []).append(t)

    mapping, unmapped = {}, []
    for n in sorted(old.tex):
        if not n.startswith(SCOPES) or excluded(n):
            continue
        if n in MANUAL:
            m = MANUAL[n]
            if m not in new.tex:
                raise SystemExit('manual_map names a texture 1.14.4 does not have: %s -> %s' % (n, m))
        elif rule(n) in new.tex:
            m = rule(n)
        elif len(model_pairs.get(n, ())) == 1:
            m = next(iter(model_pairs[n]))
        else:
            same = [c for c in by_hash.get(old.hash(n), []) if c.split('/')[0] == rule(n).split('/')[0]]
            if not same:
                unmapped.append(n)
                continue
            m = sorted(same)[0]
        mapping[n] = m
    if unmapped:
        raise SystemExit('No 1.14.4 name for: ' + ', '.join(unmapped))
    return mapping


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(data)


def main(jar112, jar114, jar115):
    old, new, v115 = Jar(jar112), Jar(jar114), Jar(jar115)
    mapping = build_map(old, new)
    mapping.update(EXTRA_115)

    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    modern = os.path.join(OUT, 'modern')
    files, report = [], []
    # 1.15 moved a few files; look its version up by the 1.14 name, then the 1.15 folder-per-mob name
    moved_115 = {'entity/iron_golem': 'entity/iron_golem/iron_golem'}
    for o, n in sorted(mapping.items()):
        src, ver = new, '1.14.4'
        n115 = moved_115.get(n, n)
        if n115 in v115.tex and not n.startswith(LAYOUT_ONLY_115) and v115.hash(n115) != new.hash(n):
            src, ver, n = v115, '1.15', n115
        if src.hash(n) == old.hash(o):
            continue
        rel = 'assets/minecraft/textures/' + o + '.png'
        write(os.path.join(modern, rel), src.png(n))
        files.append(rel)
        meta = src.mcmeta(n)
        if meta is None and old.mcmeta(o) is not None:
            # 1.12.2's animation would otherwise still apply - the game takes the .mcmeta from the
            # highest pack that has one, not from the pack the image came from
            meta = b'{}'
        if meta is not None:
            write(os.path.join(modern, rel + '.mcmeta'), meta)
            files.append(rel + '.mcmeta')
        report.append('%s <- %s %s' % (o, ver, n))

    classic = os.path.join(OUT, 'classic')
    cfiles = []
    for mine, van in sorted(CLASSIC_MOD.items()):
        if not os.path.exists(os.path.join(MOD_TEX, mine + '.png')):
            raise SystemExit('CLASSIC_MOD names a texture this mod does not have: ' + mine)
        rel = 'assets/villagerbackport/textures/' + mine + '.png'
        write(os.path.join(classic, rel), old.png(van))
        cfiles.append(rel)

    for root, fl in ((modern, files), (classic, cfiles)):
        write(os.path.join(root, 'index.txt'), ('\n'.join(sorted(fl)) + '\n').encode())
    write(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'textures-changed.txt'),('\n'.join(report) + '\n').encode())
    print('modern: %d textures (%d from 1.15), classic: %d' % (
        len(report), sum(' 1.15 ' in r for r in report), len(cfiles)))


if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    main(*sys.argv[1:])
