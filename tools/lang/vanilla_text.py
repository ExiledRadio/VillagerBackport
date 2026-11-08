#!/usr/bin/env python3
"""
vanilla_text.py - writes a backport's vanilla-text files (Backport Core's Contribution.vanillaText) in every
language the mod has, from Mojang's own language files of the backport's version.

Vanilla text a version reworded is not ours to translate: it is Mojang's, at that version. This reads
<folder>/en_us.lang (vanilla keys only, the version's English), checks each line against Mojang's en_us of that
version, and writes <folder>/<code>.lang for every code the mod's lang folder has, with Mojang's translation of
the same key at that version. A key Mojang's file lacks in a language gets the English, as the game would show.

Settings come from tools/lang/langs.json:  "vanilla_text": {"dir": "src/main/resources/assets/<modid>/vanilla_text",
"version": "1.16.5"}. Mojang's files are read from <cache>/<version>/lang/<code>.json (the same cache as
build_langs.py; `fetch` downloads a missing version from piston-meta).

  python -I tools/lang/vanilla_text.py fetch    download the version's language files into the cache
  python -I tools/lang/vanilla_text.py build    write every language, then check
  python -I tools/lang/vanilla_text.py check    check only; exit code 1 on any error
"""

import io
import json
import os
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))


def settings():
    cfg = json.load(io.open(os.path.join(HERE, 'langs.json'), encoding='utf-8'))
    vt = cfg['vanilla_text']
    cache = os.environ.get('BACKPORT_LANG_CACHE', cfg.get('cache', 'G:/MojangLang'))
    return (os.path.join(ROOT, vt['dir']), vt['version'], cache, os.path.join(ROOT, cfg['lang_dir']))


def read_lang(path):
    out = []
    for line in io.open(path, encoding='utf-8'):
        line = line.rstrip('\r\n')
        if not line or line.startswith('#') or '=' not in line:
            continue
        k, v = line.split('=', 1)
        out.append((k, v))
    return out


def mojang(cache, version, code):
    path = os.path.join(cache, version, 'lang', code + '.json')
    if not os.path.isfile(path):
        return None
    return json.load(io.open(path, encoding='utf-8'))


def to_lang(value):
    # 1.12.2's .lang files are one line per key: a line break is written as \n and Core's tooltips split on it.
    return value.replace('\r', '').replace('\n', '\\n')


def codes(lang_dir):
    return sorted(f[:-5] for f in os.listdir(lang_dir) if f.endswith('.lang'))


def fetch(cache, version):
    manifest = json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
    url = [v['url'] for v in manifest['versions'] if v['id'] == version][0]
    meta = json.load(urllib.request.urlopen(url))
    index = json.load(urllib.request.urlopen(meta['assetIndex']['url']))
    dest = os.path.join(cache, version, 'lang')
    os.makedirs(dest, exist_ok=True)
    n = 0
    for name, obj in index['objects'].items():
        if name.startswith('minecraft/lang/') and name.endswith('.json'):
            h = obj['hash']
            data = urllib.request.urlopen('https://resources.download.minecraft.net/%s/%s' % (h[:2], h)).read()
            io.open(os.path.join(dest, os.path.basename(name)), 'wb').write(data)
            n += 1
    # en_us is in the client jar, not the asset index.
    jar = urllib.request.urlopen(meta['downloads']['client']['url']).read()
    import zipfile
    z = zipfile.ZipFile(io.BytesIO(jar))
    io.open(os.path.join(dest, 'en_us.json'), 'wb').write(z.read('assets/minecraft/lang/en_us.json'))
    print('fetch: %d language files of %s into %s' % (n + 1, version, dest))


def resolver(cache, version):
    """1.12.2 keys the Flattening renamed (entity.PigZombie.name) are found by their English at the version."""
    moj_en = mojang(cache, version, 'en_us') or {}
    by_text = {}
    for k in sorted(moj_en):
        by_text.setdefault(to_lang(moj_en[k]), []).append(k)

    def resolve(key, english):
        if key in moj_en and to_lang(moj_en[key]) == english:
            return key
        same = by_text.get(english)
        if same:
            # The key itself when it is among them, else the first.
            return key if key in same else same[0]
        return None
    return resolve


def build(folder, version, cache, lang_dir):
    en = read_lang(os.path.join(folder, 'en_us.lang'))
    resolve = resolver(cache, version)
    for code in codes(lang_dir):
        if code == 'en_us':
            continue
        moj = mojang(cache, version, code) or {}
        lines = []
        for k, v in en:
            mk = resolve(k, v)
            lines.append('%s=%s' % (k, to_lang(moj[mk]) if mk in moj else v))
        io.open(os.path.join(folder, code + '.lang'), 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
    print('build: wrote %d languages to %s' % (len(codes(lang_dir)) - 1, folder))


def check(folder, version, cache, lang_dir):
    errors = 0
    warnings = 0
    en = read_lang(os.path.join(folder, 'en_us.lang'))
    keys = [k for k, _ in en]
    moj_en = mojang(cache, version, 'en_us')
    if moj_en is None:
        print('ERROR: no Mojang en_us for %s in %s (run fetch)' % (version, cache))
        return 1
    resolve = resolver(cache, version)
    for k, v in en:
        if resolve(k, v) is None:
            said = to_lang(moj_en[k]) if k in moj_en else '(no such key)'
            print('ERROR: en_us: %s is "%s"; %s has no key with that text (its %s: %s)' % (k, v, version, k, said))
            errors += 1
    for code in codes(lang_dir):
        if code == 'en_us':
            continue
        path = os.path.join(folder, code + '.lang')
        if not os.path.isfile(path):
            print('ERROR: %s missing' % path)
            errors += 1
            continue
        have = read_lang(path)
        hk = [k for k, _ in have]
        if hk != keys:
            print('ERROR: %s: keys differ from en_us' % code)
            errors += 1
        moj = mojang(cache, version, code) or {}
        for k, v in have:
            if resolve(k, dict(en).get(k, '')) not in moj:
                warnings += 1
    print('check: %d languages, %d keys each, %d errors, %d English fallbacks (Mojang had no translation)'
          % (len(codes(lang_dir)) - 1, len(keys), errors, warnings))
    return 1 if errors else 0


def main():
    folder, version, cache, lang_dir = settings()
    cmd = sys.argv[1] if len(sys.argv) > 1 else 'check'
    if cmd == 'fetch':
        fetch(cache, version)
        return 0
    if cmd == 'build':
        build(folder, version, cache, lang_dir)
    return check(folder, version, cache, lang_dir)


if __name__ == '__main__':
    sys.exit(main())
