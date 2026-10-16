"""Minimal NBT reader/writer. Tags are plain Python values wrapped where the type matters.

Compound -> dict (ordered), List -> TagList(type, items), scalars -> Tag(type, value) on read so a
round trip keeps exact types. Helpers below build tags for writing.
"""
import gzip
import io
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class Tag:
    __slots__ = ("type", "value")

    def __init__(self, t, v):
        self.type = t
        self.value = v

    def __repr__(self):
        return "Tag(%d,%r)" % (self.type, self.value)


class TagList(list):
    def __init__(self, t, items=()):
        super().__init__(items)
        self.type = t


def _read(f, t):
    if t == BYTE:
        return Tag(t, struct.unpack(">b", f.read(1))[0])
    if t == SHORT:
        return Tag(t, struct.unpack(">h", f.read(2))[0])
    if t == INT:
        return Tag(t, struct.unpack(">i", f.read(4))[0])
    if t == LONG:
        return Tag(t, struct.unpack(">q", f.read(8))[0])
    if t == FLOAT:
        return Tag(t, struct.unpack(">f", f.read(4))[0])
    if t == DOUBLE:
        return Tag(t, struct.unpack(">d", f.read(8))[0])
    if t == BYTE_ARRAY:
        n = struct.unpack(">i", f.read(4))[0]
        return Tag(t, f.read(n))
    if t == STRING:
        n = struct.unpack(">H", f.read(2))[0]
        return f.read(n).decode("utf-8")
    if t == LIST:
        et = f.read(1)[0]
        n = struct.unpack(">i", f.read(4))[0]
        items = []
        for _ in range(n):
            items.append(_read(f, et))
        return TagList(et, items)
    if t == COMPOUND:
        d = {}
        while True:
            ct = f.read(1)[0]
            if ct == END:
                return d
            n = struct.unpack(">H", f.read(2))[0]
            name = f.read(n).decode("utf-8")
            val = _read(f, ct)
            d[name] = val
    if t == INT_ARRAY:
        n = struct.unpack(">i", f.read(4))[0]
        return Tag(t, list(struct.unpack(">%di" % n, f.read(4 * n))))
    if t == LONG_ARRAY:
        n = struct.unpack(">i", f.read(4))[0]
        return Tag(t, list(struct.unpack(">%dq" % n, f.read(8 * n))))
    raise ValueError("bad tag %d" % t)


def type_of(v):
    if isinstance(v, Tag):
        return v.type
    if isinstance(v, str):
        return STRING
    if isinstance(v, TagList):
        return LIST
    if isinstance(v, dict):
        return COMPOUND
    raise TypeError(repr(v))


def _write(f, v):
    t = type_of(v)
    if t == STRING:
        b = v.encode("utf-8")
        f.write(struct.pack(">H", len(b)))
        f.write(b)
    elif t == LIST:
        et = v.type if v else (v.type or END)
        f.write(bytes([et]))
        f.write(struct.pack(">i", len(v)))
        for item in v:
            _write(f, item)
    elif t == COMPOUND:
        for k, item in v.items():
            f.write(bytes([type_of(item)]))
            b = k.encode("utf-8")
            f.write(struct.pack(">H", len(b)))
            f.write(b)
            _write(f, item)
        f.write(bytes([END]))
    else:
        val = v.value
        fmt = {BYTE: ">b", SHORT: ">h", INT: ">i", LONG: ">q", FLOAT: ">f", DOUBLE: ">d"}.get(t)
        if fmt:
            f.write(struct.pack(fmt, val))
        elif t == BYTE_ARRAY:
            f.write(struct.pack(">i", len(val)))
            f.write(val)
        elif t == INT_ARRAY:
            f.write(struct.pack(">i", len(val)))
            f.write(struct.pack(">%di" % len(val), *val))
        elif t == LONG_ARRAY:
            f.write(struct.pack(">i", len(val)))
            f.write(struct.pack(">%dq" % len(val), *val))


def load(path):
    with open(path, "rb") as fh:
        data = fh.read()
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    f = io.BytesIO(data)
    t = f.read(1)[0]
    n = struct.unpack(">H", f.read(2))[0]
    f.read(n)
    return _read(f, t)


def save(path, root):
    f = io.BytesIO()
    f.write(bytes([COMPOUND]))
    f.write(struct.pack(">H", 0))
    _write(f, root)
    with open(path, "wb") as fh:
        fh.write(gzip.compress(f.getvalue(), mtime=0))


def i(v):
    return Tag(INT, v)


def b(v):
    return Tag(BYTE, v)


def s(v):
    return Tag(SHORT, v)


def d(v):
    return Tag(DOUBLE, v)


def plain(v):
    """Tags to plain Python, for inspection."""
    if isinstance(v, Tag):
        return v.value
    if isinstance(v, TagList):
        return [plain(x) for x in v]
    if isinstance(v, dict):
        return {k: plain(x) for k, x in v.items()}
    return v
