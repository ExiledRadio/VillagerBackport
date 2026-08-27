"""Project avatar for 1.14 Villager Backport, matching the other mods' icons.

Palette and geometry sampled from RLCraftVillagerTomes/project-avatar.png:
  corner radius 101/512, edge background (14,13,22), centre lift toward (42,26,40),
  sparkles (206,190,246), gem highlights (126,230,168)/(61,194,128).
"""
import math
import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT, S = 512, 4
W = OUT * S

BG_EDGE, BG_CORE = (14, 13, 22), (42, 26, 40)
SPARK = (206, 190, 246)
GLOW = (47, 164, 92)

SKIN_HI, SKIN_LO = (224, 172, 134), (188, 134, 101)
NOSE_HI, NOSE_LO = (236, 190, 156), (200, 150, 116)
JAW_HI, JAW_LO = (152, 100, 72), (116, 72, 52)
BROW_HI, BROW_LO = (72, 52, 26), (44, 31, 14)
EYE_HI, EYE_LO = (250, 250, 250), (214, 214, 218)
IRIS_HI, IRIS_LO = (26, 176, 46), (0, 128, 14)
ROBE_HI, ROBE_LO = (120, 84, 64), (62, 43, 34)
COLLAR = (132, 100, 82)
GEM_HI, GEM_MID, GEM_LO = (126, 230, 168), (61, 194, 128), (36, 142, 91)


def p(v):
    return int(round(v * S))


def bx(x0, y0, x1, y1):
    return [p(x0), p(y0), p(x1), p(y1)]


def blank():
    return Image.new("L", (W, W), 0)


def rect(b, r=0):
    m = blank()
    if r:
        ImageDraw.Draw(m).rounded_rectangle(b, radius=p(r), fill=255)
    else:
        ImageDraw.Draw(m).rectangle(b, fill=255)
    return m


def top_rounded(x0, y0, x1, y1, r):
    """Rounded at the top, square at the bottom."""
    return ImageChops.lighter(rect(bx(x0, y0, x1, y1), r),
                              rect(bx(x0, y0 + r, x1, y1)))


def bottom_rounded(x0, y0, x1, y1, r):
    return ImageChops.lighter(rect(bx(x0, y0, x1, y1), r),
                              rect(bx(x0, y0, x1, y1 - r)))


def vgrad(b, top, bottom):
    x0, y0, x1, y1 = b
    h = max(1, y1 - y0)
    col = np.zeros((h, 1, 3), dtype=np.float64)
    for i in range(3):
        col[:, 0, i] = np.linspace(top[i], bottom[i], h)
    return Image.fromarray(col.astype(np.uint8)).resize((max(1, x1 - x0), h), Image.BILINEAR)


def fill(layer, mask, b, top, bottom):
    layer.paste(vgrad(b, top, bottom), (b[0], b[1]), mask.crop(b))


def soft_glow(layer, mask, colour, blur, strength):
    g = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    g.paste(colour + (255,), (0, 0), mask)
    g = g.filter(ImageFilter.GaussianBlur(p(blur)))
    g.putalpha(g.split()[3].point(lambda v: int(v * strength)))
    layer.alpha_composite(g)


def background():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n]
    d = np.clip(np.sqrt((xx - n / 2) ** 2 + (yy - n / 2) ** 2) / (n * 0.62), 0, 1)
    t = 1 - (d * d * (3 - 2 * d))
    img = np.zeros((n, n, 3))
    for i in range(3):
        img[:, :, i] = BG_EDGE[i] + (BG_CORE[i] - BG_EDGE[i]) * t
    return Image.fromarray(img.astype(np.uint8)).resize((W, W), Image.BICUBIC)


def sparkle_pts(cx, cy, r, power=3.0):
    out = []
    for i in range(240):
        t = 2 * math.pi * i / 240
        c, s = math.cos(t), math.sin(t)
        out.append((p(cx) + r * S * math.copysign(abs(c) ** power, c),
                    p(cy) + r * S * math.copysign(abs(s) ** power, s)))
    return out


def sparkles(layer, specs):
    shape = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(shape)
    for x, y, r, a in specs:
        d.polygon(sparkle_pts(x, y, r), fill=SPARK + (a,))
        d.polygon(sparkle_pts(x, y, r * 0.3), fill=(255, 255, 255, min(255, a + 40)))
    glow = shape.filter(ImageFilter.GaussianBlur(p(4)))
    glow.putalpha(glow.split()[3].point(lambda v: int(v * 0.8)))
    layer.alpha_composite(glow)
    layer.alpha_composite(shape)


def emerald(layer, cx, cy, size, glow=True):
    hw, hh = size / 2, size / 2 * 1.15
    ty, my, by = cy - hh, cy - hh * 0.3, cy + hh
    q = lambda x, y: (p(x), p(y))
    body = [q(cx - hw * 0.6, ty), q(cx + hw * 0.6, ty), q(cx + hw, my),
            q(cx, by), q(cx - hw, my)]
    if glow:
        m = blank()
        ImageDraw.Draw(m).polygon(body, fill=255)
        soft_glow(layer, m, GLOW, 14, 0.9)
    g = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    d.polygon(body, fill=GEM_MID + (255,))
    d.polygon([q(cx - hw * 0.6, ty), q(cx + hw * 0.6, ty),
               q(cx + hw * 0.3, my), q(cx - hw * 0.3, my)], fill=GEM_HI + (255,))
    d.polygon([q(cx + hw * 0.6, ty), q(cx + hw, my), q(cx + hw * 0.3, my)], fill=GEM_LO + (255,))
    d.polygon([q(cx - hw * 0.3, my), q(cx + hw * 0.3, my), q(cx, by)], fill=GEM_LO + (255,))
    layer.alpha_composite(g)


def villager(layer, left, top, w, robe=True):
    """Front view, proportioned from the villager texture's own 8x10 head face:
    forehead is the top half, the unibrow one tenth, the eyes one tenth under it,
    and the nose a quarter of the width, darker than the face, hanging past the chin."""
    h = w * 1.22
    right, bottom = left + w, top + h
    cx = left + w / 2
    r = 0.09 * w

    head = rect(bx(left, top, right, bottom), r)
    soft_glow(layer, head, GLOW, 34, 0.8)

    # robe, behind the head
    if not robe:
        rw = None
    rw = w * 1.56
    rl, rr = cx - rw / 2, cx + rw / 2
    rtop, rbot = bottom - 0.015 * h, bottom + 0.33 * h
    rb = bx(rl, rtop, rr, rbot)
    if robe:
        fill(layer, top_rounded(rl, rtop, rr, rbot, 0.16 * w), rb, ROBE_HI, ROBE_LO)
        cb = bx(rl + 0.05 * w, rtop, rr - 0.05 * w, rtop + 0.06 * h)
        fill(layer, top_rounded(rl + 0.05 * w, rtop, rr - 0.05 * w, rtop + 0.06 * h, 0.04 * w),
             cb, COLLAR, ROBE_HI)
        # crossed arms, the one bit of shape a villager's robe has from the front
        ab = bx(rl + 0.28 * w, rtop + 0.14 * h, rr - 0.28 * w, rtop + 0.235 * h)
        fill(layer, rect(ab, 0.035 * w), ab, (96, 66, 50), (58, 40, 31))

    # head
    hb = bx(left, top, right, bottom)
    fill(layer, head, hb, SKIN_HI, SKIN_LO)

    # jaw shading down the outer eighth, as the texture has
    for sx in (left, right - 0.14 * w):
        b = bx(sx, top + 0.54 * h, sx + 0.14 * w, bottom)
        fill(layer, ImageChops.darker(rect(b, r * 0.5), head), b, JAW_HI, JAW_LO)

    # unibrow
    brow = bx(left + 0.125 * w, top + 0.435 * h, right - 0.125 * w, top + 0.545 * h)
    fill(layer, rect(brow, 0.02 * w), brow, BROW_HI, BROW_LO)

    # eyes, one tenth of the head tall, white outside and green inside
    ey0, ey1 = top + 0.55 * h, top + 0.70 * h
    for ox, iris_right in ((left + 0.125 * w, True), (left + 0.625 * w, False)):
        eb = bx(ox, ey0, ox + 0.25 * w, ey1)
        fill(layer, rect(eb, 0.018 * w), eb, EYE_HI, EYE_LO)
        ix = ox + 0.125 * w if iris_right else ox
        ib = bx(ix, ey0, ix + 0.125 * w, ey1)
        fill(layer, rect(ib, 0.018 * w), ib, IRIS_HI, IRIS_LO)

    # nose: a quarter of the width, from the brow to past the chin
    nl, nr = cx - 0.115 * w, cx + 0.115 * w
    ntop, nbot = top + 0.435 * h, bottom - 0.01 * h
    nose = bottom_rounded(nl, ntop, nr, nbot, 0.05 * w)
    soft_glow(layer, nose, (8, 5, 3), 6, 0.7)
    fill(layer, nose, bx(nl, ntop, nr, nbot), NOSE_HI, NOSE_LO)


def build(gem):
    canvas = background().convert("RGBA")
    art = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    sparkles(art, [
        (92, 112, 25, 235), (402, 96, 20, 225), (444, 220, 14, 205),
        (62, 240, 12, 195), (120, 408, 16, 210), (414, 392, 18, 220),
        (256, 52, 11, 190), (466, 330, 9, 165), (48, 336, 8, 160),
        (334, 458, 10, 175), (172, 456, 8, 155),
    ])
    if gem == "head":
        villager(art, left=160, top=170, w=192, robe=False)
        emerald(art, 256, 96, 86)
    elif gem:
        villager(art, left=166, top=162, w=180)
        emerald(art, 256, 90, 84)
    else:
        villager(art, left=152, top=136, w=208)
    canvas.alpha_composite(art)

    mask = blank()
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, W - 1], radius=p(101), fill=255)
    canvas.putalpha(mask)
    return canvas.resize((OUT, OUT), Image.LANCZOS)


icon = build(True)
icon.save("E:/MincraftModding/Workspaces/RLCraftVillagerTrading/project-avatar.png")
icon.save("E:/MincraftModding/Workspaces/RLCraftVillagerTrading/docs/curseforge/icon.png")
icon.resize((400, 400), Image.LANCZOS).save(
    "E:/MincraftModding/Workspaces/RLCraftVillagerTrading/docs/curseforge/icon-400.png")
print("written")
