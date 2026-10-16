"""Parses 1.14.4's *VillagePools.java into plain data.

Only the expression shapes those five files use are understood; anything else raises, so a gap shows
up as an error rather than a silently missing pool.
"""
import re

TOKEN = re.compile(r'\s*(?:(?P<str>"[^"]*")|(?P<num>-?\d+(?:\.\d+)?[FfDdL]?)|(?P<id>[A-Za-z_$][\w$]*)|(?P<op>[().,<>;=\[\]{}]))')


def tokenize(src):
    pos = 0
    out = []
    while True:
        m = TOKEN.match(src, pos)
        if not m or m.end() == pos:
            rest = src[pos:].strip()
            if rest:
                raise ValueError("cannot tokenise at: " + rest[:80])
            return out
        pos = m.end()
        for kind in ("str", "num", "id", "op"):
            if m.group(kind) is not None:
                out.append((kind, m.group(kind)))
                break


class Parser:
    def __init__(self, tokens, env):
        self.t = tokens
        self.i = 0
        self.env = env

    def peek(self, k=0):
        return self.t[self.i + k] if self.i + k < len(self.t) else (None, None)

    def take(self, value=None):
        tok = self.t[self.i]
        if value is not None and tok[1] != value:
            raise ValueError("expected %s got %s near %s" % (value, tok, self.t[self.i:self.i + 8]))
        self.i += 1
        return tok

    def args(self):
        self.take("(")
        out = []
        if self.peek()[1] == ")":
            self.take(")")
            return out
        while True:
            out.append(self.expr())
            if self.peek()[1] == ",":
                self.take(",")
                continue
            self.take(")")
            return out

    def expr(self):
        kind, val = self.peek()
        if kind == "str":
            self.take()
            return val[1:-1]
        if kind == "num":
            self.take()
            v = val.rstrip("FfDdL")
            return float(v) if "." in v else int(v)
        if val == "new":
            self.take()
            name = self.take()[1]
            while self.peek()[1] == ".":
                self.take(".")
                name += "." + self.take()[1]
            if self.peek()[1] == "<":
                depth = 0
                while True:
                    v = self.take()[1]
                    if v == "<":
                        depth += 1
                    elif v == ">":
                        depth -= 1
                        if depth == 0:
                            break
            node = {"$new": name.split(".")[-1], "args": self.args()}
            return self.chain(node)
        if kind == "id":
            name = self.take()[1]
            while self.peek()[1] == ".":
                self.take(".")
                part = self.take()[1]
                if self.peek()[1] == "(":
                    return self.chain({"$call": part, "on": name, "args": self.args()})
                name += "." + part
            if self.peek()[1] == "(":
                return self.chain({"$call": name, "on": None, "args": self.args()})
            if name in self.env:
                return self.env[name]
            return {"$ref": name}
        raise ValueError("unexpected %s" % (self.peek(),))

    def chain(self, node):
        while self.peek()[1] == ".":
            self.take(".")
            part = self.take()[1]
            node = {"$call": part, "on": node, "args": self.args()}
        return node


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def block_of(ref):
    """Blocks.OAK_LOG -> minecraft:oak_log"""
    if isinstance(ref, dict) and "$ref" in ref and ref["$ref"].startswith("Blocks."):
        return "minecraft:" + ref["$ref"][len("Blocks."):].lower()
    raise ValueError("not a block: %r" % (ref,))


def state_of(node):
    """Blocks.X.getDefaultState().with(Prop.P, value)... -> {name, props}"""
    if node["$call"] == "getDefaultState":
        return {"name": block_of({"$ref": node["on"]} if isinstance(node["on"], str) else node["on"]), "props": {}}
    if node["$call"] == "with":
        base = state_of(node["on"])
        prop = node["args"][0]["$ref"].split(".")[-1].lower()
        val = node["args"][1]
        if isinstance(val, dict) and val.get("$call") == "valueOf":
            val = val["args"][0]
            if isinstance(val, dict):
                val = val["$ref"]
        if isinstance(val, dict):
            val = val["$ref"].split(".")[-1]
        base["props"][prop] = str(val).lower()
        return base
    raise ValueError("not a state: %r" % (node,))


def rule_test(node):
    if isinstance(node, dict) and node.get("$ref", "").endswith("AlwaysTrueRuleTest.INSTANCE"):
        return {"kind": "always"}
    cls = node["$new"]
    a = node["args"]
    if cls == "RandomBlockMatchRuleTest":
        return {"kind": "random_block", "block": block_of(a[0]), "probability": a[1]}
    if cls == "BlockMatchRuleTest":
        return {"kind": "block", "block": block_of(a[0])}
    if cls == "BlockStateMatchRuleTest":
        return {"kind": "state", "state": state_of(a[0])}
    if cls == "TagMatchRuleTest":
        return {"kind": "tag", "tag": a[0]["$ref"].split(".")[-1].lower()}
    raise ValueError(cls)


def processors(node):
    # ImmutableList.of(new RuleStructureProcessor(ImmutableList.of(entries...)))
    if isinstance(node, list):
        return node
    out = []
    for proc in node["args"]:
        if proc["$new"] != "RuleStructureProcessor":
            raise ValueError(proc["$new"])
        rules = []
        for entry in proc["args"][0]["args"]:
            a = entry["args"]
            rules.append({"input": rule_test(a[0]), "location": rule_test(a[1]), "output": state_of(a[2])})
        out.append(rules)
    return out


def projection(node):
    return node["$ref"].split(".")[-1].lower()


def element(node):
    if isinstance(node, dict) and node.get("$ref", "").endswith("EmptyJigsawPiece.INSTANCE"):
        return {"type": "empty"}
    cls = node["$new"]
    a = node["args"]
    if cls == "SingleJigsawPiece":
        el = {"type": "single", "location": a[0], "processors": processors(a[1]) if len(a) > 1 else []}
        if len(a) > 2:
            el["projection"] = projection(a[2])
        return el
    if cls == "FeatureJigsawPiece":
        feat = a[0]["args"][0]["$ref"].split(".")[-1].lower()
        el = {"type": "feature", "feature": feat}
        if len(a) > 1:
            el["projection"] = projection(a[1])
        return el
    if cls == "ListJigsawPiece":
        el = {"type": "list", "elements": [element(x) for x in a[0]["args"]]}
        if len(a) > 1:
            el["projection"] = projection(a[1])
        return el
    raise ValueError(cls)


def parse_file(path, pools):
    src = strip_comments(open(path, encoding="utf-8").read())
    body = src[src.index("static {") + len("static {"):src.rindex("}")]
    body = body[:body.rindex("}")]
    env = {}
    for stmt in split_statements(body):
        toks = tokenize(stmt)
        if not toks:
            continue
        if toks[0][1] == "ImmutableList" and toks[1][1] == "<":
            # ImmutableList<StructureProcessor> name = expr
            j = [t[1] for t in toks].index("=")
            name = toks[j - 1][1]
            p = Parser(toks[j + 1:], env)
            env[name] = processors(p.expr())
            continue
        p = Parser(toks, env)
        node = p.expr()
        if node.get("$call") != "register":
            raise ValueError("unexpected statement " + stmt[:80])
        pat = node["args"][0]
        a = pat["args"]
        name = a[0]["args"][0]
        fallback = a[1]["args"][0]
        proj = projection(a[3])
        elems = []
        for pair in a[2]["args"]:
            el = element(pair["args"][0])
            el.setdefault("projection", proj)
            elems.append({"element": el, "weight": pair["args"][1]})
        pools[name] = {"fallback": fallback, "projection": proj, "elements": elems}


def split_statements(body):
    out = []
    depth = 0
    cur = []
    in_str = False
    for ch in body:
        cur.append(ch)
        if ch == '"':
            in_str = not in_str
        elif not in_str:
            if ch in "({":
                depth += 1
            elif ch in ")}":
                depth -= 1
            elif ch == ";" and depth == 0:
                out.append("".join(cur[:-1]).strip())
                cur = []
    if "".join(cur).strip():
        out.append("".join(cur).strip())
    return out
