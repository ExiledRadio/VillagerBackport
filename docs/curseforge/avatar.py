"""Generates the CurseForge avatar for 1.14 Villager Backport.

Writes project-avatar.png in the project root, plus docs/curseforge/icon.png and
icon-400.png. Run it from anywhere: python docs/curseforge/avatar.py

The frame is sampled from RLCraftVillagerTomes/project-avatar.png so the set matches:
corner radius 101/512, background (14,13,22) at the edges lifting to (42,26,40) in the
middle, sparkles (206,190,246), and the same cut emerald. The bed's red and cream are
taken from the Death Overhaul heart and the Enchant Recipes book for the same reason.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT, S = 512, 4
W = OUT * S

BG_EDGE, BG_CORE = (14, 13, 22), (42, 26, 40)
SPARK = (206, 190, 246)
GLOW = (47, 164, 92)
GEM_HI, GEM_MID, GEM_LO = (126, 230, 168), (61, 194, 128), (36, 142, 91)
SHEET_HI, SHEET_LO = (186, 62, 82), (132, 38, 56)
BLANKET_HI, BLANKET_LO = (212, 84, 102), (154, 48, 68)
FOLD_HI, FOLD_LO = (232, 112, 128), (176, 64, 84)
CREAM_HI, CREAM_LO = (236, 220, 186), (198, 180, 146)
WOOD_HI, WOOD_LO = (130, 94, 64), (76, 52, 34)

SPARKLE_FIELD = [
    (86, 122, 26, 235), (424, 116, 22, 228), (58, 252, 15, 200), (456, 250, 14, 198),
    (150, 62, 13, 195), (356, 58, 12, 190), (46, 372, 12, 180), (466, 356, 11, 175),
    (112, 436, 17, 205), (394, 440, 15, 200), (256, 448, 13, 185), (44, 158, 9, 165),
    (462, 168, 8, 160), (196, 468, 8, 155), (322, 470, 9, 158), (72, 462, 7, 148),
    (438, 452, 8, 152), (150, 196, 8, 150), (368, 190, 7, 148),
]


def p(v):
    return int(round(v * S))


def bx(x0, y0, x1, y1):
    return [p(x0), p(y0), p(x1), p(y1)]


def blank():
    return Image.new("L", (W, W), 0)


def rect(b, r=0):
    m = blank()
    d = ImageDraw.Draw(m)
    if r:
        d.rounded_rectangle(b, radius=p(r), fill=255)
    else:
        d.rectangle(b, fill=255)
    return m


def vgrad(b, top, bottom):
    x0, y0, x1, y1 = b
    h = max(1, y1 - y0)
    col = np.zeros((h, 1, 3))
    for i in range(3):
        col[:, 0, i] = np.linspace(top[i], bottom[i], h)
    return Image.fromarray(col.astype(np.uint8)).resize((max(1, x1 - x0), h), Image.BILINEAR)


def fill(layer, mask, b, top, bottom):
    layer.paste(vgrad(b, top, bottom), (b[0], b[1]), mask.crop(b))


def glow(layer, mask, colour, blur, strength):
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
    return Image.fromarray(img.astype(np.uint8)).resize((W, W), Image.BICUBIC).convert("RGBA")


def sparkle_pts(cx, cy, r):
    pts = []
    for i in range(240):
        t = 2 * math.pi * i / 240
        c, s = math.cos(t), math.sin(t)
        pts.append((p(cx) + r * S * math.copysign(abs(c) ** 3, c),
                    p(cy) + r * S * math.copysign(abs(s) ** 3, s)))
    return pts


def sparkles(layer, occupied):
    """Draw the field, skipping any star that would fall under the subject.

    `occupied` is the subject's own silhouette, widened - a box would rule out the
    corners beside the gem and under the bed, which is where the big ones belong.
    """
    taken = occupied.filter(ImageFilter.MaxFilter(31)).point(lambda v: 255 if v > 8 else 0)
    px = taken.load()
    shape = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(shape)
    for x, y, r, a in SPARKLE_FIELD:
        probes = [(x, y), (x - r, y), (x + r, y), (x, y - r), (x, y + r)]
        if any(px[min(W - 1, max(0, p(qx))), min(W - 1, max(0, p(qy)))] for qx, qy in probes):
            continue
        d.polygon(sparkle_pts(x, y, r), fill=SPARK + (a,))
        d.polygon(sparkle_pts(x, y, r * 0.3), fill=(255, 255, 255, min(255, a + 40)))
    soft = shape.filter(ImageFilter.GaussianBlur(p(4)))
    soft.putalpha(soft.split()[3].point(lambda v: int(v * 0.8)))
    layer.alpha_composite(soft)
    layer.alpha_composite(shape)


def emerald(layer, cx, cy, size):
    hw, hh = size / 2, size / 2 * 1.15
    ty, my, by = cy - hh, cy - hh * 0.3, cy + hh
    q = lambda x, y: (p(x), p(y))
    body = [q(cx - hw * 0.6, ty), q(cx + hw * 0.6, ty), q(cx + hw, my), q(cx, by), q(cx - hw, my)]
    m = blank()
    ImageDraw.Draw(m).polygon(body, fill=255)
    glow(layer, m, GLOW, 15, 0.9)
    g = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    d.polygon(body, fill=GEM_MID + (255,))
    d.polygon([q(cx - hw * 0.6, ty), q(cx + hw * 0.6, ty), q(cx + hw * 0.3, my),
               q(cx - hw * 0.3, my)], fill=GEM_HI + (255,))
    d.polygon([q(cx + hw * 0.6, ty), q(cx + hw, my), q(cx + hw * 0.3, my)], fill=GEM_LO + (255,))
    d.polygon([q(cx - hw * 0.3, my), q(cx + hw * 0.3, my), q(cx, by)], fill=GEM_LO + (255,))
    layer.alpha_composite(g)


def bed(layer):
    left, right = 118, 394
    sheet_top, sheet_bot = 286, 338
    frame_top, frame_bot = 338, 360
    leg_bot = 392
    pillow_l, pillow_r = left + 16, left + 106

    glow(layer, rect(bx(left, sheet_top, right, frame_bot), 12), (168, 58, 78), 28, 0.55)

    for lx in (left + 6, right - 40):
        b = bx(lx, frame_bot - 6, lx + 34, leg_bot)
        fill(layer, rect(b, 7), b, WOOD_HI, WOOD_LO)

    b = bx(left - 8, frame_top, right + 8, frame_bot)
    fill(layer, rect(b, 9), b, WOOD_HI, WOOD_LO)

    b = bx(left, sheet_top, right, sheet_bot + 6)
    fill(layer, rect(b, 15), b, SHEET_HI, SHEET_LO)

    b = bx(pillow_r + 4, sheet_top - 8, right, sheet_bot)
    fill(layer, rect(b, 13), b, BLANKET_HI, BLANKET_LO)
    b = bx(pillow_r + 4, sheet_top - 8, pillow_r + 22, sheet_bot)
    fill(layer, rect(b, 8), b, FOLD_HI, FOLD_LO)

    b = bx(pillow_l, sheet_top - 26, pillow_r, sheet_top + 22)
    fill(layer, rect(b, 14), b, CREAM_HI, CREAM_LO)


def build():
    subject = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    bed(subject)
    emerald(subject, 256, 178, 100)

    art = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    sparkles(art, subject.split()[3])
    art.alpha_composite(subject)

    canvas = background()
    canvas.alpha_composite(art)
    mask = blank()
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, W - 1], radius=p(101), fill=255)
    canvas.putalpha(mask)
    return canvas.resize((OUT, OUT), Image.LANCZOS)


if __name__ == "__main__":
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    icon = build()
    icon.save(os.path.join(root, "project-avatar.png"))
    icon.save(os.path.join(root, "docs", "curseforge", "icon.png"))
    icon.resize((400, 400), Image.LANCZOS).save(
        os.path.join(root, "docs", "curseforge", "icon-400.png"))
    print("written")
