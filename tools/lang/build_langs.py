#!/usr/bin/env python3
"""
build_langs.py - writes a 1.12.2 mod's lang files in the backport family's eighteen languages
(Rule 4 of docs/BACKPORT_FAMILY_PLAN.md) and checks them against en_us.lang.

Standard library only. The same file is copied into every backport; everything specific to a
mod lives in tools/lang/langs.json next to it, so copy this script unchanged.

WHERE EACH TRANSLATION COMES FROM
  * Vanilla names (blocks, items, entities, effects, enchantments, advancements, subtitles,
    death messages, ...) come from Mojang's own language files. `fetch` downloads them from
    piston-meta into a cache folder (one folder per Minecraft version). A key is matched to
    Mojang's key by structure (tile.<modid>.barrel.name -> block.minecraft.barrel), by the
    same key, or by its English text within the right namespace - and the English must agree.
    The newest version's translation is used, so renamed things get their newest name.
    Never write a vanilla name into a translations file by hand.
  * Everything else (config, commands, GUI, tooltips, chat) is ours: tools/lang/translations/
    <code>.lang holds it, one file per language, key=value like a normal lang file.
  * Config comments are shown by Forge's config GUI from "<langKey>.tooltip" when that key
    exists. `config` reads the @Config classes and writes those keys, in English, into a
    generated block at the end of en_us.lang, so they can be translated like anything else.
    Field names are left alone: they are identifiers the comments and messages refer to.

LAYOUT (per mod)
  tools/lang/build_langs.py          this script, identical in every mod
  tools/lang/langs.json              settings (see below)
  tools/lang/vanilla_map.txt         optional: "our.key = mojang.key" to force a match,
                                     "our.key = !" to force "ours"
  tools/lang/translations/<code>.lang       our own text, per language
  tools/lang/translations/en_us.source.lang the English each translation was made from
                                     (written by `snapshot`; `check` lists keys whose
                                     English has changed since - those need retranslating)

langs.json:
  {
    "modid": "examplemod",
    "lang_dir": "src/main/resources/assets/examplemod/lang",
    "mojang_versions": ["26.3", "1.14.4"],     # first = where translations come from;
                                               # the rest only help match older English
    "config_classes": ["src/main/java/.../ModConfig.java"],   # optional
    "ours_prefixes": ["config.", "commands.", "gui.", "jei.", "itemGroup."],  # optional
    "banned_words": ["Some Other Mod"],        # optional: text no lang file may contain
    "cache": "G:/MojangLang"                   # optional; env BACKPORT_LANG_CACHE overrides
  }

COMMANDS (run from anywhere; use python -I)
  python -I tools/lang/build_langs.py fetch     download Mojang's lang files (once per version)
  python -I tools/lang/build_langs.py config    regenerate the config tooltip block in en_us
  python -I tools/lang/build_langs.py todo      write tools/lang/todo/<code>.lang: our keys each
                                                language still needs, with the English
  python -I tools/lang/build_langs.py build     write every lang file, then check
  python -I tools/lang/build_langs.py check     check only; exit code 1 on any error
  python -I tools/lang/build_langs.py report    which key came from where, and Mojang renames
  python -I tools/lang/build_langs.py snapshot  record the current English as translated
  python -I tools/lang/build_langs.py verify es_mx ...   read-only check of translation files
                                                (safe while other people translate)

NEW MOD IN FIVE STEPS
  1. Copy this file into tools/lang/, write langs.json, make sure src/main/resources/pack.mcmeta
     exists with "pack_format": 3 (without it 1.12.2 looks for en_US.lang and finds nothing).
  2. fetch, then config (if the mod has @Config classes).
  3. report: check every Mojang match is the right thing; fix wrong ones in vanilla_map.txt.
  4. todo, translate tools/lang/todo/<code>.lang into tools/lang/translations/<code>.lang.
  5. build, snapshot, commit. After any en_us change: todo/translate, build, snapshot.

CHECKS (`check` and `build`)
  errors:   a key en_us lacks, a key en_us has that the file lacks, a placeholder that differs
            (%s %d %1$s ...; positions may be reordered with %n$s), a stray % that would make
            String.format throw, a section-sign formatting code that differs, duplicate keys,
            an empty value, a banned word, a missing pack.mcmeta.
  warnings: translations whose English changed since the snapshot, a different number of
            \\n line breaks in a tooltip, translation-file keys that en_us no longer has.
"""

import argparse
import hashlib
import json
import os
import re
import sys
import urllib.request
import zipfile
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent.parent

# The family's languages (Rule 4). Codes are 1.12.2's lowercase file names.
LANGUAGES = [
    "es_es", "es_mx", "pt_br", "fr_fr", "de_de", "ru_ru", "zh_cn",          # tier 1
    "ja_jp", "ko_kr", "it_it", "pl_pl", "tr_tr", "uk_ua", "zh_tw", "nl_nl",  # tier 2
]
# Written as a copy of another language's finished file.
COPIES = {"es_ar": "es_mx", "es_uy": "es_mx", "es_ve": "es_mx"}
ALL_OUTPUTS = LANGUAGES + list(COPIES)

MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
RESOURCES_URL = "https://resources.download.minecraft.net/{}/{}"

DEFAULT_OURS = ["config.", "commands.", "gui.", "jei.", "itemGroup."]

GENERATED_BEGIN = "# --- config tooltips ---"
GENERATED_NOTE = ("# Generated by tools/lang/build_langs.py config from the @Config comments;"
                  " rerun it, don't edit.")
GENERATED_END = "# --- end config tooltips ---"

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")


# ----------------------------------------------------------------------------------------------
# settings and lang files
# ----------------------------------------------------------------------------------------------

def load_settings():
    path = SCRIPT_DIR / "langs.json"
    with open(path, encoding="utf-8") as f:
        s = json.load(f)
    s.setdefault("ours_prefixes", DEFAULT_OURS)
    s.setdefault("banned_words", [])
    s.setdefault("config_classes", [])
    s["cache"] = os.environ.get("BACKPORT_LANG_CACHE") or s.get("cache") or str(Path.home() / "MojangLang")
    s["lang_dir_path"] = ROOT / s["lang_dir"]
    return s


def read_lang(path):
    """Returns (entries, lines). entries: ordered list of (key, value, line_no); lines: raw lines."""
    with open(path, encoding="utf-8-sig") as f:
        lines = f.read().split("\n")
    entries = []
    for i, line in enumerate(lines):
        line = line.rstrip("\r")
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        entries.append((k, v, i + 1))
    return entries, [l.rstrip("\r") for l in lines]


def lang_dict(path):
    if not Path(path).exists():
        return {}
    return {k: v for k, v, _ in read_lang(path)[0]}


def write_text(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


# ----------------------------------------------------------------------------------------------
# fetch: Mojang's language files
# ----------------------------------------------------------------------------------------------

def http_get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "build_langs.py"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def download(url, dest, sha1=None):
    if dest.exists() and (sha1 is None or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return
    data = http_get(url)
    if sha1 and hashlib.sha1(data).hexdigest() != sha1:
        raise SystemExit("sha1 mismatch for " + url)
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)


def resolve_version(version, cache):
    manifest = Path(cache) / "manifest" / "version_manifest_v2.json"
    download(MANIFEST_URL, manifest)
    m = json.loads(manifest.read_text(encoding="utf-8"))
    if version == "latest":
        version = m["latest"]["release"]
    for v in m["versions"]:
        if v["id"] == version:
            return version, v["url"], v.get("sha1")
    raise SystemExit("Minecraft version not in Mojang's manifest: " + version)


def cmd_fetch(s):
    for version in s["mojang_versions"]:
        version, url, sha1 = resolve_version(version, s["cache"])
        folder = Path(s["cache"]) / version
        vjson = folder / (version + ".json")
        download(url, vjson, sha1)
        meta = json.loads(vjson.read_text(encoding="utf-8"))
        index = folder / ("assets-" + meta["assetIndex"]["id"] + ".json")
        download(meta["assetIndex"]["url"], index, meta["assetIndex"]["sha1"])
        objects = json.loads(index.read_text(encoding="utf-8"))["objects"]
        wanted = set(ALL_OUTPUTS)
        for code in sorted(wanted):
            obj = objects.get("minecraft/lang/%s.json" % code)
            if obj is None:
                print("  %s: Mojang has no %s" % (version, code))
                continue
            h = obj["hash"]
            download(RESOURCES_URL.format(h[:2], h), folder / "lang" / (code + ".json"), h)
        en = folder / "lang" / "en_us.json"
        if not en.exists():
            # en_us is not an asset: it ships inside the client jar.
            client = meta["downloads"]["client"]
            jar = folder / "client.jar"
            download(client["url"], jar, client["sha1"])
            with zipfile.ZipFile(jar) as z:
                en.write_bytes(z.read("assets/minecraft/lang/en_us.json"))
        print("fetched %s -> %s" % (version, folder))


def load_mojang(s):
    """Returns list of (version, {code: {key: text}}), primary version first."""
    out = []
    for version in s["mojang_versions"]:
        if version == "latest":
            version = resolve_version(version, s["cache"])[0]
        folder = Path(s["cache"]) / version / "lang"
        if not (folder / "en_us.json").exists():
            raise SystemExit("Mojang %s is not downloaded; run: build_langs.py fetch" % version)
        langs = {}
        for code in ["en_us"] + LANGUAGES:
            p = folder / (code + ".json")
            if p.exists():
                langs[code] = json.loads(p.read_text(encoding="utf-8"))
        out.append((version, langs))
    return out


# ----------------------------------------------------------------------------------------------
# matching our keys to Mojang's
# ----------------------------------------------------------------------------------------------

def structural_candidates(key, modid):
    """Mojang keys the 1.12.2 key most likely corresponds to, best first."""
    m = re.escape(modid)
    c = []
    rules = [
        (r"^tile\.%s\.(.+)\.name$" % m, ["block.minecraft.{0}"]),
        (r"^item\.%s\.(.+)\.name$" % m, ["item.minecraft.{0}", "block.minecraft.{0}"]),
        (r"^item\.%s\.(.+)\.desc$" % m, ["item.minecraft.{0}.desc"]),
        (r"^entity\.%s\.(.+)\.name$" % m, ["entity.minecraft.{0}"]),
        (r"^effect\.%s\.(.+)$" % m, ["effect.minecraft.{0}"]),
        (r"^enchantment\.%s\.(.+)$" % m, ["enchantment.minecraft.{0}"]),
        (r"^subtitles\.%s\.(.+)$" % m, ["subtitles.{0}"]),
        (r"^container\.%s\.(.+)$" % m, ["container.{0}"]),
        (r"^event\.%s\.(.+)$" % m, ["event.minecraft.{0}"]),
        (r"^filled_map\.%s\.(.+)$" % m, ["filled_map.{0}"]),
        (r"^item\.banner\.(.+)$", ["block.minecraft.banner.{0}"]),
        (r"^entity\.Villager\.(.+)$", ["entity.minecraft.villager.{0}"]),
    ]
    for pattern, targets in rules:
        mt = re.match(pattern, key)
        if mt:
            c += [t.format(mt.group(1)) for t in targets]
    mt = re.match(r"^advancements\.%s\.(.+)\.(title|description)$" % m, key)
    if mt:
        c.append(("advancements.*.%s.%s" % mt.groups()))
    c.append(key)
    return c


def text_namespaces(key):
    """Where an English-text match may come from, by the 1.12.2 key's kind."""
    first = key.split(".", 1)[0]
    return {
        "tile": ["block.minecraft."],
        "item": ["item.minecraft.", "block.minecraft."],
        "entity": ["entity.minecraft."],
        "effect": ["effect.minecraft."],
        "enchantment": ["enchantment.minecraft."],
        "subtitles": ["subtitles."],
        "advancements": ["advancements."],
        "death": ["death."],
        "container": ["container."],
        "event": ["event.minecraft."],
        "filled_map": ["filled_map."],
    }.get(first, [])


def norm(t):
    return t.strip().casefold()


def read_vanilla_map():
    p = SCRIPT_DIR / "vanilla_map.txt"
    out = {}
    if p.exists():
        for line in p.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                a, b = line.split("=", 1)
                out[a.strip()] = b.strip()
    return out


class Matcher:
    def __init__(self, s, mojang):
        self.s = s
        self.mojang = mojang          # [(version, {code: {key: text}})]
        self.primary = mojang[0][1]
        self.forced = read_vanilla_map()
        self.by_text = []             # per version: {norm(text): [keys]}
        for _, langs in mojang:
            idx = {}
            for k, v in langs["en_us"].items():
                idx.setdefault(norm(v), []).append(k)
            self.by_text.append(idx)

    def english(self, mkey):
        """English of a Mojang key in each version, newest first (None where absent)."""
        return [langs["en_us"].get(mkey) for _, langs in self.mojang]

    def text_agrees(self, mkey, text):
        return any(e is not None and norm(e) == norm(text) for e in self.english(mkey))

    def expand(self, cand):
        if "*" not in cand:
            return [cand]
        rx = re.compile("^" + re.escape(cand).replace(r"\*", r"[^.]+") + "$")
        found = []
        for _, langs in self.mojang:
            found += [k for k in langs["en_us"] if rx.match(k) and k not in found]
        return found

    def is_ours(self, key):
        return (any(key.startswith(p) for p in self.s["ours_prefixes"])
                or key.endswith(".tooltip")
                or key.startswith(self.s["modid"] + ".general."))

    def match(self, key, text):
        """Returns (mojang_key or None, how)."""
        forced = self.forced.get(key)
        if forced == "!":
            return None, "forced ours"
        if forced:
            if all(e is None for e in self.english(forced)):
                return None, "vanilla_map names %s, which Mojang doesn't have" % forced
            return forced, "vanilla_map"
        if self.is_ours(key):
            return None, "ours"
        for cand in structural_candidates(key, self.s["modid"]):
            for mk in self.expand(cand):
                if self.text_agrees(mk, text):
                    return mk, "key"
        spaces = text_namespaces(key)
        if not spaces:
            return None, "no namespace"
        for idx in self.by_text:
            hits = [k for k in idx.get(norm(text), []) if any(k.startswith(n) for n in spaces)]
            if not hits:
                continue
            # Several Mojang keys share the English: fine if they translate alike everywhere.
            tail = key.rsplit(".name", 1)[0].rsplit(".", 1)[-1]
            hits.sort(key=lambda k: (0 if k.endswith("." + tail) else 1,
                                     next(i for i, n in enumerate(spaces) if k.startswith(n)), k))
            first = hits[0]
            if all(self.translation(h, c)[0] == self.translation(first, c)[0]
                   for h in hits[1:] for c in LANGUAGES):
                return first, "text"
            if hits[0].endswith("." + tail) and not hits[1].endswith("." + tail):
                return first, "text"
            return None, "ambiguous: " + ", ".join(hits[:6])
        return None, "no match"

    def translation(self, mkey, code):
        """(text, version) of a Mojang key in a language, newest version that has it."""
        for version, langs in self.mojang:
            v = langs.get(code, {}).get(mkey)
            if v is not None:
                return v, version
        return None, None


# ----------------------------------------------------------------------------------------------
# config: @Config comments -> "<langKey>.tooltip" keys
# ----------------------------------------------------------------------------------------------

TOKEN_RX = re.compile(r'''
    (?P<ws>\s+)
  | (?P<lc>//[^\n]*)
  | (?P<bc>/\*.*?\*/)
  | (?P<str>"(?:\\.|[^"\\])*")
  | (?P<chr>'(?:\\.|[^'\\])*')
  | (?P<id>[A-Za-z_$][A-Za-z0-9_$]*)
  | (?P<num>\d[\w.]*)
  | (?P<sym>.)
''', re.S | re.X)


def tokenize(src):
    out = []
    for m in TOKEN_RX.finditer(src):
        kind = m.lastgroup
        if kind in ("ws", "lc", "bc"):
            continue
        out.append((kind, m.group()))
    return out


def unescape_java(lit):
    body = lit[1:-1]
    out, i = [], 0
    simple = {"n": "\n", "t": "\t", "r": "\r", "b": "\b", "f": "\f", '"': '"', "'": "'", "\\": "\\"}
    while i < len(body):
        ch = body[i]
        if ch == "\\" and i + 1 < len(body):
            nx = body[i + 1]
            if nx in simple:
                out.append(simple[nx]); i += 2; continue
            if nx == "u":
                j = i + 1
                while j < len(body) and body[j] == "u":
                    j += 1
                out.append(chr(int(body[j:j + 4], 16))); i = j + 4; continue
            if nx in "01234567":
                j = i + 1
                while j < len(body) and j < i + 4 and body[j] in "01234567":
                    j += 1
                out.append(chr(int(body[i + 1:j], 8))); i = j; continue
        out.append(ch); i += 1
    return "".join(out)


def matching(tokens, i, open_s, close_s):
    """Index of the token closing the bracket opened at tokens[i]."""
    depth = 0
    for j in range(i, len(tokens)):
        if tokens[j][1] == open_s:
            depth += 1
        elif tokens[j][1] == close_s:
            depth -= 1
            if depth == 0:
                return j
    raise ValueError("unbalanced " + open_s)


def string_list(tokens):
    """Evaluates an annotation value of string literals: "a" + "b" or {"a", "b" + "c"}."""
    if tokens and tokens[0][1] == "{":
        tokens = tokens[1:-1]
    items, cur, ok = [], [], True
    for kind, text in tokens + [("sym", ",")]:
        if text == ",":
            if cur or not ok:
                items.append("".join(cur) if ok else None)
            cur, ok = [], True
        elif kind == "str":
            cur.append(unescape_java(text))
        elif text != "+":
            ok = False
    return items


class JClass:
    def __init__(self, name, annotations):
        self.name = name
        self.annotations = annotations
        self.fields = []      # (name, type, modifiers, annotations)
        self.classes = {}


def parse_annotation(tokens, i):
    """tokens[i] is '@'. Returns (name, args_tokens or None, next_index)."""
    j = i + 1
    name = tokens[j][1]
    j += 1
    while j + 1 < len(tokens) and tokens[j][1] == "." and tokens[j + 1][0] == "id":
        name += "." + tokens[j + 1][1]
        j += 2
    args = None
    if j < len(tokens) and tokens[j][1] == "(":
        end = matching(tokens, j, "(", ")")
        args = tokens[j + 1:end]
        j = end + 1
    return name.split(".")[-1], args, j


def parse_body(tokens, i, end, cls):
    """Parses class-body members between tokens[i] and tokens[end] (exclusive) into cls."""
    pending, mods = {}, []
    stmt = []
    while i < end:
        kind, text = tokens[i]
        if text == "@" and i + 1 < end and tokens[i + 1][1] != "interface":
            name, args, i = parse_annotation(tokens, i)
            pending[name] = args
            continue
        if text == "class" or text == "enum" or text == "interface":
            cname = tokens[i + 1][1]
            j = i + 2
            while tokens[j][1] != "{":
                j += 1
            close = matching(tokens, j, "{", "}")
            inner = JClass(cname, pending)
            inner.modifiers = mods
            if text == "class":
                parse_body(tokens, j + 1, close, inner)
            cls.classes[cname] = inner
            pending, mods, stmt = {}, [], []
            i = close + 1
            continue
        if text == "{":
            # method body or initializer
            close = matching(tokens, i, "{", "}")
            pending, mods, stmt = {}, [], []
            i = close + 1
            continue
        if text in ("public", "private", "protected", "static", "final", "transient", "volatile"):
            mods.append(text)
            i += 1
            continue
        if text == ";" or text == "=":
            is_method = any(t == "(" for _, t in stmt)
            ids = [t for k, t in stmt if k == "id"]
            if not is_method and len(ids) >= 2:
                fname = ids[-1]
                ftype = ids[-2]
                cls.fields.append((fname, ftype, list(mods), dict(pending)))
            if text == "=":
                # skip initializer to the ';' at this depth
                depth = 0
                while i < end:
                    t = tokens[i][1]
                    if t in "({[":
                        depth += 1
                    elif t in ")}]":
                        depth -= 1
                    elif t == ";" and depth == 0:
                        break
                    i += 1
            pending, mods, stmt = {}, [], []
            i += 1
            continue
        if text == "(":
            close = matching(tokens, i, "(", ")")
            stmt.append(("sym", "("))
            i = close + 1
            continue
        if text == "<":
            # generic type arguments
            depth = 0
            while i < end:
                if tokens[i][1] == "<":
                    depth += 1
                elif tokens[i][1] == ">":
                    depth -= 1
                    if depth == 0:
                        break
                i += 1
            i += 1
            continue
        if text not in (".", "[", "]"):
            stmt.append((kind, text))
        i += 1


def all_classes(cls, out):
    for c in cls.classes.values():
        out[c.name] = c
        all_classes(c, out)
    return out


def paragraphs(lines):
    """Joins a comment's hard-wrapped lines into paragraphs; the GUI tooltip wraps at 300px."""
    out = []
    starts_new = re.compile(r"^(?:[-*\u2022]\s|\d+[.)]\s|[A-Z][A-Z0-9_]+(?:,\s*[A-Z][A-Z0-9_]+)*:|\S+\s+=\s)")
    for line in lines:
        line = line.rstrip()
        # An indented line is a table row or list item: it keeps its own line.
        if (not out or not line or not out[-1] or line[:1].isspace()
                or starts_new.match(line.lstrip())):
            out.append(line.strip() if line.strip() else "")
        else:
            out[-1] = out[-1] + " " + line.strip()
    return "\\n".join(out)


def config_tooltips(s):
    """[(key, english)] for every commented config category and property, in file order."""
    modid = s["modid"]
    out = []
    for rel in s["config_classes"]:
        src = (ROOT / rel).read_text(encoding="utf-8")
        tokens = tokenize(src)
        top = JClass("<file>", {})
        parse_body(tokens, 0, len(tokens), top)
        classes = all_classes(top, {})
        roots = [c for c in classes.values() if "Config" in c.annotations]
        if not roots:
            raise SystemExit("no @Config class in " + rel)
        for root in roots:
            cat = "general"
            args = root.annotations.get("Config") or []
            for n, (k, t) in enumerate(args):
                if t == "category" and n + 2 < len(args) and args[n + 2][0] == "str":
                    cat = unescape_java(args[n + 2][1])

            def walk(c, path, static):
                for fname, ftype, mods, ann in c.fields:
                    if "public" not in mods or ("static" in mods) != static or "Ignore" in ann:
                        continue
                    lang = modid + "." + path + "." + fname.lower()
                    if ann.get("LangKey"):
                        lang = string_list(ann["LangKey"])[0]
                    comment = string_list(ann["Comment"]) if ann.get("Comment") else None
                    if comment is not None and None in comment:
                        print("  warning: %s.%s has a comment that isn't plain literals; skipped"
                              % (path, fname))
                        comment = None
                    if comment:
                        text = paragraphs(comment).replace("%", "%%")
                        out.append((lang + ".tooltip", text))
                    sub = classes.get(ftype)
                    if sub is not None and ftype != c.name:
                        name = string_list(ann["Name"])[0] if ann.get("Name") else fname
                        walk(sub, path + "." + name.lower(), False)

            walk(root, cat, True)
    return out


def cmd_config(s):
    en_path = s["lang_dir_path"] / "en_us.lang"
    text = en_path.read_text(encoding="utf-8")
    tips = []
    for key, value in config_tooltips(s):
        named = [w for w in s["banned_words"] if w.casefold() in value.casefold()]
        if named:
            # No key: Forge shows the English @Config.Comment instead, and no lang file names it.
            print("  skipped %s: names %s" % (key, ", ".join(named)))
        else:
            tips.append((key, value))
    block =[GENERATED_BEGIN, GENERATED_NOTE] + ["%s=%s" % kv for kv in tips] + [GENERATED_END]
    if GENERATED_BEGIN in text:
        before = text[:text.index(GENERATED_BEGIN)]
        after = text[text.index(GENERATED_END) + len(GENERATED_END):]
        text = before + "\n".join(block) + after
    else:
        text = text.rstrip("\n") + "\n\n" + "\n".join(block) + "\n"
    write_text(en_path, text)
    print("config: %d tooltip keys written to %s" % (len(tips), en_path.relative_to(ROOT)))


# ----------------------------------------------------------------------------------------------
# build, check, todo, report
# ----------------------------------------------------------------------------------------------

PLACEHOLDER_RX = re.compile(r"%(?:(\d+)\$)?([-#+ 0,(]*\d*(?:\.\d+)?)([a-zA-Z%])")


def placeholders(text):
    """Returns (multiset of (index, conversion), list of problems)."""
    found, problems, seq = [], [], 0
    i = 0
    while i < len(text):
        if text[i] != "%":
            i += 1
            continue
        m = PLACEHOLDER_RX.match(text, i)
        if not m or m.group(3) not in "sdfxXoeEgGcbBhHn%":
            problems.append("stray %% at column %d" % (i + 1))
            i += 1
            continue
        conv = m.group(3)
        if conv not in "%n":
            if m.group(1):
                idx = int(m.group(1))
            else:
                seq += 1
                idx = seq
            found.append((idx, conv.lower()))
        i = m.end()
    return sorted(found), problems


FORMAT_RX = re.compile("\u00a7[0-9a-fk-or]", re.I)


def build_all(s, matcher, en_entries):
    ours_dir = SCRIPT_DIR / "translations"
    results = {}
    for code in LANGUAGES:
        ours = lang_dict(ours_dir / (code + ".lang"))
        values, sources = {}, {}
        for key, text, _ in en_entries:
            mk, how = matcher.match(key, text)
            if key in ours:
                values[key], sources[key] = ours[key], "ours"
            elif mk is not None:
                t, version = matcher.translation(mk, code)
                if t is not None:
                    # A lang value is one line; only config tooltips turn \n back into a break.
                    t = t.replace("\n", "\\n") if key.endswith(".tooltip") else " ".join(t.split("\n"))
                    values[key], sources[key] = t, "mojang " + version
        results[code] = (values, sources)
    return results


def render(en_lines, values, code):
    out = ["# %s - generated by tools/lang/build_langs.py; edit tools/lang/translations/%s.lang"
           % (code, code)]
    for line in en_lines:
        if line and not line.startswith("#") and "=" in line:
            k = line.split("=", 1)[0]
            if k in values:
                out.append("%s=%s" % (k, values[k]))
        else:
            out.append(line)
    while out and out[-1] == "":
        out.pop()
    return "\n".join(out) + "\n"


def cmd_build(s):
    mojang = load_mojang(s)
    matcher = Matcher(s, mojang)
    en_path = s["lang_dir_path"] / "en_us.lang"
    en_entries, en_lines = read_lang(en_path)
    results = build_all(s, matcher, en_entries)
    for code, (values, _) in results.items():
        write_text(s["lang_dir_path"] / (code + ".lang"), render(en_lines, values, code))
    for copy, src in COPIES.items():
        text = (s["lang_dir_path"] / (src + ".lang")).read_text(encoding="utf-8")
        text = text.replace("# %s - " % src, "# %s (a copy of %s) - " % (copy, src), 1)
        write_text(s["lang_dir_path"] / (copy + ".lang"), text)
    print("build: wrote %d lang files to %s" % (len(ALL_OUTPUTS), s["lang_dir"]))
    return cmd_check(s)


def find_pack_mcmeta(lang_dir):
    p = lang_dir
    while p != p.parent:
        if (p / "pack.mcmeta").exists():
            return p / "pack.mcmeta"
        if p.name == "resources":
            break
        p = p.parent
    return None


def cmd_check(s):
    errors, warnings = [], []
    lang_dir = s["lang_dir_path"]
    meta = find_pack_mcmeta(lang_dir)
    if meta is None:
        errors.append("no pack.mcmeta in the resources: 1.12.2 will look for en_US.lang and find nothing")
    else:
        try:
            fmt = json.loads(meta.read_text(encoding="utf-8"))["pack"]["pack_format"]
            if fmt < 3:
                errors.append("%s has pack_format %s; needs 3" % (meta, fmt))
        except Exception as e:
            errors.append("%s unreadable: %s" % (meta, e))
    en_entries, _ = read_lang(lang_dir / "en_us.lang")
    en = {}
    for k, v, n in en_entries:
        if k in en:
            errors.append("en_us.lang:%d duplicate key %s" % (n, k))
        en[k] = v
    en_ph = {k: placeholders(v) for k, v in en.items()}
    for k, (ph, probs) in en_ph.items():
        for p in probs:
            errors.append("en_us %s: %s" % (k, p))
    banned = [w for w in s["banned_words"]]
    counts = {}
    for code in ALL_OUTPUTS:
        path = lang_dir / (code + ".lang")
        if not path.exists():
            errors.append("%s.lang missing" % code)
            continue
        try:
            entries, _ = read_lang(path)
        except UnicodeDecodeError:
            errors.append("%s.lang is not UTF-8" % code)
            continue
        seen = set()
        for k, v, n in entries:
            where = "%s.lang:%d %s" % (code, n, k)
            if k in seen:
                errors.append(where + ": duplicate key")
            seen.add(k)
            if k not in en:
                errors.append(where + ": key not in en_us")
                continue
            if not v.strip():
                errors.append(where + ": empty value")
            ph, probs = placeholders(v)
            for p in probs:
                errors.append(where + ": " + p)
            if ph != en_ph[k][0]:
                errors.append(where + ": placeholders %s, en_us has %s"
                              % (fmt_ph(ph), fmt_ph(en_ph[k][0])))
            if sorted(FORMAT_RX.findall(v.lower())) != sorted(FORMAT_RX.findall(en[k].lower())):
                errors.append(where + ": section-sign formatting codes differ from en_us")
            if k.endswith(".tooltip") and v.count("\\n") != en[k].count("\\n"):
                warnings.append(where + ": %d line breaks, en_us has %d"
                                % (v.count("\\n"), en[k].count("\\n")))
            for w in banned:
                if w.casefold() in v.casefold():
                    errors.append(where + ": names %r" % w)
        missing = [k for k in en if k not in seen]
        for k in missing:
            errors.append("%s.lang: missing %s" % (code, k))
        counts[code] = (len(seen & set(en)), len(en))
    for k, v in en.items():
        for w in banned:
            if w.casefold() in v.casefold():
                errors.append("en_us %s: names %r" % (k, w))
    # translation sources
    tdir = SCRIPT_DIR / "translations"
    snap = lang_dict(tdir / "en_us.source.lang")
    for code in LANGUAGES:
        ours = lang_dict(tdir / (code + ".lang"))
        for k in ours:
            if k not in en:
                warnings.append("translations/%s.lang: %s is no longer in en_us" % (code, k))
            elif snap and k in snap and snap[k] != en[k]:
                warnings.append("translations/%s.lang: %s - English changed since it was translated"
                                % (code, k))
            elif snap and k not in snap:
                warnings.append("translations/%s.lang: %s not in en_us.source.lang (run snapshot)"
                                % (code, k))
    for w in warnings:
        print("warning: " + w)
    for e in errors:
        print("ERROR: " + e)
    complete = sum(1 for c in counts.values() if c[0] == c[1])
    print("check: %d/%d lang files complete (%d keys each), %d errors, %d warnings"
          % (complete, len(ALL_OUTPUTS), len(en), len(errors), len(warnings)))
    return 1 if errors else 0


def fmt_ph(ph):
    return "[" + " ".join("%%%d$%s" % p for p in ph) + "]"


def cmd_todo(s):
    mojang = load_mojang(s)
    matcher = Matcher(s, mojang)
    en_entries, _ = read_lang(s["lang_dir_path"] / "en_us.lang")
    tdir = SCRIPT_DIR / "translations"
    snap = lang_dict(tdir / "en_us.source.lang")
    todo_dir = SCRIPT_DIR / "todo"
    total = 0
    for code in LANGUAGES:
        ours = lang_dict(tdir / (code + ".lang"))
        lines = []
        for key, text, _ in en_entries:
            mk, how = matcher.match(key, text)
            if mk is not None and matcher.translation(mk, code)[0] is not None:
                continue
            stale = key in ours and snap.get(key) not in (None, text)
            if key not in ours or stale:
                lines.append("%s=%s" % (key, text))
        path = todo_dir / (code + ".lang")
        if lines:
            write_text(path, "\n".join(lines) + "\n")
        elif path.exists():
            path.unlink()
        total += len(lines)
        print("todo %s: %d keys" % (code, len(lines)))
    print("todo: %d keys in %s (English; translate into translations/<code>.lang)"
          % (total, todo_dir.relative_to(ROOT)))


def cmd_report(s):
    mojang = load_mojang(s)
    matcher = Matcher(s, mojang)
    en_entries, _ = read_lang(s["lang_dir_path"] / "en_us.lang")
    newest = mojang[0][0]
    tally = {}
    renamed = []
    for key, text, _ in en_entries:
        mk, how = matcher.match(key, text)
        tally[how] = tally.get(how, 0) + 1
        if mk is None:
            if how not in ("ours", "forced ours"):
                print("ours   %-62s %s   [%s]" % (key, text, how))
            continue
        en_new = mojang[0][1]["en_us"].get(mk)
        es = matcher.translation(mk, "es_mx")[0]
        print("mojang %-62s %s  ->  %s  (%s, %s)" % (key, text, mk, how, es))
        if en_new is not None and norm(en_new) != norm(text):
            renamed.append((key, text, en_new))
        if en_new is None:
            renamed.append((key, text, "(not in %s)" % newest))
    print()
    print("by how matched: " + ", ".join("%s %d" % kv for kv in sorted(tally.items())))
    if renamed:
        print("\nEnglish differs in %s (translations use %s's text):" % (newest, newest))
        for key, old, new in renamed:
            print("  %s: %r -> %r" % (key, old, new))


def cmd_verify(s, codes):
    """Read-only check of translations/<code>.lang files (safe to run while others translate)."""
    en_entries, _ = read_lang(s["lang_dir_path"] / "en_us.lang")
    en = {k: v for k, v, _ in en_entries}
    mojang = load_mojang(s)
    matcher = Matcher(s, mojang)
    bad = 0
    for code in codes or LANGUAGES:
        path = SCRIPT_DIR / "translations" / (code + ".lang")
        if not path.exists():
            print("%s: no translations file" % code)
            bad += 1
            continue
        entries, _ = read_lang(path)
        problems, seen = [], set()
        for k, v, n in entries:
            where = "%s.lang:%d %s" % (code, n, k)
            if k in seen:
                problems.append(where + ": duplicate key")
            seen.add(k)
            if k not in en:
                problems.append(where + ": not in en_us")
                continue
            if not v.strip():
                problems.append(where + ": empty")
            ph, probs = placeholders(v)
            problems += [where + ": " + p for p in probs]
            if ph != placeholders(en[k])[0]:
                problems.append(where + ": placeholders %s, en_us has %s"
                                % (fmt_ph(ph), fmt_ph(placeholders(en[k])[0])))
            if sorted(FORMAT_RX.findall(v.lower())) != sorted(FORMAT_RX.findall(en[k].lower())):
                problems.append(where + ": section-sign codes differ")
            if v.count("\\n") != en[k].count("\\n"):
                problems.append(where + ": %d \\n, en_us has %d" % (v.count("\\n"), en[k].count("\\n")))
            problems += [where + ": names %r" % w for w in s["banned_words"] if w.casefold() in v.casefold()]
        todo = [k for k, t, _ in en_entries if k not in seen and matcher.match(k, t)[0] is None]
        for p in problems:
            print("  " + p)
        print("%s: %d keys, %d problems, %d of our keys untranslated%s"
              % (code, len(seen), len(problems), len(todo), (": " + ", ".join(todo[:5])) if todo else ""))
        bad += 1 if problems or todo else 0
    return 1 if bad else 0


def cmd_snapshot(s):
    en_entries, _ = read_lang(s["lang_dir_path"] / "en_us.lang")
    mojang = load_mojang(s)
    matcher = Matcher(s, mojang)
    lines = ["# The English each translations/<code>.lang entry was made from. Written by"
             " build_langs.py snapshot."]
    for key, text, _ in en_entries:
        if matcher.match(key, text)[0] is None:
            lines.append("%s=%s" % (key, text))
    write_text(SCRIPT_DIR / "translations" / "en_us.source.lang", "\n".join(lines) + "\n")
    print("snapshot: %d keys" % (len(lines) - 1))


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[1],
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["fetch", "config", "build", "check", "todo", "report",
                                        "snapshot", "verify"])
    ap.add_argument("codes", nargs="*", help="verify: languages to verify (default: all)")
    args = ap.parse_args()
    s = load_settings()
    if args.command == "verify":
        sys.exit(cmd_verify(s, args.codes))
    rc = {
        "fetch": cmd_fetch, "config": cmd_config, "build": cmd_build, "check": cmd_check,
        "todo": cmd_todo, "report": cmd_report, "snapshot": cmd_snapshot,
    }[args.command](s)
    sys.exit(rc or 0)


if __name__ == "__main__":
    main()
