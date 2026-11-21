"""PNG textures 1.12.2 can read: keyed transparency rewritten as an alpha channel.

Newer vanilla jars store some textures as grayscale or RGB with a tRNS chunk (one color that means "transparent").
1.12.2's texture loader drops that key, so those pixels draw opaque black. keyed_to_rgba() rewrites such a file as
8-bit RGBA with the key's pixels at alpha 0; every other PNG is returned unchanged.

    python -I tools/png_rgba.py <file.png>...   rewrites the given files in place, printing those it changed
"""
import struct
import sys
import zlib

SIG = b"\x89PNG\r\n\x1a\n"


def _chunks(data):
    i = 8
    while i < len(data):
        n = struct.unpack(">I", data[i:i + 4])[0]
        yield data[i + 4:i + 8], data[i + 8:i + 8 + n]
        i += 12 + n


def _paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def _unfilter(raw, width, height, bpp):
    stride = width * bpp
    out = bytearray()
    prev = bytearray(stride)
    i = 0
    for _ in range(height):
        kind = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if kind == 1:
                line[x] = (line[x] + a) & 255
            elif kind == 2:
                line[x] = (line[x] + b) & 255
            elif kind == 3:
                line[x] = (line[x] + ((a + b) >> 1)) & 255
            elif kind == 4:
                line[x] = (line[x] + _paeth(a, b, c)) & 255
        out += line
        prev = line
    return bytes(out)


def _chunk(kind, body):
    return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)


def keyed_to_rgba(data):
    """@return the PNG as 8-bit RGBA if it is 8-bit grayscale/RGB with a tRNS key, else the bytes unchanged"""
    if data[:8] != SIG:
        return data
    chunks = list(_chunks(data))
    ihdr = chunks[0][1]
    width, height, depth, kind, _, _, interlace = struct.unpack(">IIBBBBB", ihdr)
    trns = [b for k, b in chunks if k == b"tRNS"]
    if not trns or kind not in (0, 2) or depth != 8 or interlace != 0:
        return data
    bpp = 1 if kind == 0 else 3
    pixels = _unfilter(zlib.decompress(b"".join(b for k, b in chunks if k == b"IDAT")), width, height, bpp)
    if kind == 0:
        key = (struct.unpack(">H", trns[0][:2])[0] & 255,)
    else:
        r, g, b = struct.unpack(">HHH", trns[0][:6])
        key = (r & 255, g & 255, b & 255)
    rows = bytearray()
    for y in range(height):
        rows.append(0)
        for x in range(width):
            px = tuple(pixels[(y * width + x) * bpp:(y * width + x + 1) * bpp])
            rgb = px * 3 if kind == 0 else px
            rows += bytes(rgb) + (b"\x00" if px == key else b"\xff")
    out = SIG + _chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    out += _chunk(b"IDAT", zlib.compress(bytes(rows), 9)) + _chunk(b"IEND", b"")
    return out


if __name__ == "__main__":
    for path in sys.argv[1:]:
        with open(path, "rb") as f:
            before = f.read()
        after = keyed_to_rgba(before)
        if after != before:
            with open(path, "wb") as f:
                f.write(after)
            print("rewrote", path)
