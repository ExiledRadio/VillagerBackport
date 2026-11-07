"""Draws an icon for every mod in the backport family, in Village & Pillage's frame.

Reuses the frame and the bell from make_icons.py next to this file, and writes one SVG and a 512x512
PNG per mod into ../family-icons, rendered with Microsoft Edge in headless mode. Run with Python on
Windows, from anywhere.
"""
import math
import os
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(os.path.dirname(HERE), 'family-icons')
EDGE = r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'

source = open(os.path.join(HERE, 'make_icons.py'), encoding='utf-8').read()
exec(source[:source.index('OPTIONS = {')])  # FRAME, BELL, BELL_FRAME
OUT = os.path.join(os.path.dirname(HERE), 'family-icons')  # the exec above sets its own OUT


def cube(top, left, right, edge=None, cx=200, cy=110, w=84, h=42, depth=104):
    """An isometric block: the top face's corners, then the two side faces."""
    t = (cx, cy)
    r = (cx + w, cy + h)
    b = (cx, cy + 2 * h)
    l = (cx - w, cy + h)
    pts = lambda *ps: ' '.join('%g,%g' % p for p in ps)
    down = lambda p: (p[0], p[1] + depth)
    out = '<polygon points="%s" fill="%s"/>' % (pts(l, b, down(b), down(l)), left)
    out += '<polygon points="%s" fill="%s"/>' % (pts(b, r, down(r), down(b)), right)
    out += '<polygon points="%s" fill="%s"/>' % (pts(t, r, b, l), top)
    if edge:
        out += '<polygon points="%s" fill="none" stroke="%s" stroke-width="4" stroke-linejoin="round"/>' % (
            pts(t, r, down(r), down(b), down(l), l), edge)
        out += '<polyline points="%s" fill="none" stroke="%s" stroke-width="4" stroke-linejoin="round"/>' % (
            pts(l, b, r), edge)
        out += '<line x1="%g" y1="%g" x2="%g" y2="%g" stroke="%s" stroke-width="4"/>' % (b + down(b) + (edge,))
    return out


# Update Aquatic: a trident, with bubbles.
TRIDENT = '''
  <rect x="192" y="150" width="16" height="182" rx="6" fill="#2f8f86"/>
  <rect x="192" y="150" width="6" height="182" rx="3" fill="#56c3b6"/>
  <rect x="188" y="262" width="24" height="10" rx="3" fill="#1f6a63"/>
  <rect x="188" y="284" width="24" height="10" rx="3" fill="#1f6a63"/>
  <path d="M146 92 L146 144 Q146 172 200 172 Q254 172 254 144 L254 92" stroke="#2f8f86" stroke-width="15" fill="none" stroke-linejoin="round"/>
  <path d="M141 92 L141 144" stroke="#56c3b6" stroke-width="5" fill="none"/>
  <rect x="193" y="84" width="14" height="80" rx="4" fill="#2f8f86"/>
  <rect x="193" y="84" width="5" height="80" rx="2" fill="#56c3b6"/>
  <path d="M200 56 L212 88 L188 88 Z" fill="#9ff0e2"/>
  <path d="M146 64 L157 96 L135 96 Z" fill="#9ff0e2"/>
  <path d="M254 64 L265 96 L243 96 Z" fill="#9ff0e2"/>
  <g fill="none" stroke="#bff3ff" stroke-width="3" opacity="0.85">
    <circle cx="262" cy="230" r="10"/><circle cx="282" cy="200" r="6"/><circle cx="140" cy="250" r="8"/>
    <circle cx="124" cy="222" r="5"/><circle cx="276" cy="262" r="5"/>
  </g>'''

# Buzzy Bees: a boxy bee, side on.
BEE = '''
  <rect x="176" y="106" width="44" height="62" rx="16" fill="#e8f6ff" opacity="0.85"/>
  <rect x="222" y="114" width="40" height="56" rx="16" fill="#d3ecff" opacity="0.8"/>
  <path d="M152 162 L138 128" stroke="#3a2614" stroke-width="5" stroke-linecap="round"/>
  <path d="M168 160 L162 124" stroke="#3a2614" stroke-width="5" stroke-linecap="round"/>
  <rect x="126" y="158" width="150" height="112" rx="20" fill="#f6c445"/>
  <rect x="200" y="158" width="76" height="112" rx="20" fill="#e3a92c"/>
  <rect x="190" y="158" width="20" height="112" fill="#4a2f17"/>
  <rect x="236" y="158" width="20" height="112" fill="#4a2f17"/>
  <rect x="142" y="190" width="22" height="26" rx="3" fill="#1b1410"/>
  <rect x="146" y="193" width="8" height="8" fill="#fff"/>
  <path d="M276 206 L300 214 L276 222 Z" fill="#cfd5db"/>
  <rect x="160" y="268" width="10" height="20" rx="3" fill="#2a1c10"/>
  <rect x="204" y="268" width="10" height="20" rx="3" fill="#2a1c10"/>
  <rect x="248" y="268" width="10" height="20" rx="3" fill="#2a1c10"/>'''

# Nether Update: a netherite ingot, with embers.
INGOT = '''<g transform="translate(206 216) scale(1.3) translate(-209 -218)">
  <polygon points="118,196 244,160 300,192 174,230" fill="#776a71"/>
  <polygon points="118,196 174,230 174,276 118,240" fill="#4d4248"/>
  <polygon points="174,230 300,192 300,238 174,276" fill="#3a3136"/>
  <polyline points="134,200 244,170 282,192" fill="none" stroke="#a698a1" stroke-width="5" stroke-linejoin="round"/>
  <polyline points="182,248 286,216" fill="none" stroke="#4a3f45" stroke-width="4"/>
  </g>
  <g fill="#ff8a3d"><circle cx="150" cy="140" r="5"/><circle cx="268" cy="128" r="4"/><circle cx="296" cy="282" r="5"/>
  <circle cx="124" cy="292" r="4"/><circle cx="214" cy="118" r="3"/></g>'''


def amethyst():
    """Caves & Cliffs: an amethyst cluster fanned out of a stone base."""
    out = '<ellipse cx="200" cy="304" rx="96" ry="18" fill="#4b4f55"/>'
    for angle, length in [(-38, 0.62), (38, 0.6), (-17, 0.86), (19, 0.8), (0, 1.0)]:
        out += ('<g transform="translate(200 300) rotate(%g) scale(%g)">'
                '<path d="M0 0 L-26 -70 L0 -196 Z" fill="#c9a1ff"/>'
                '<path d="M0 0 L26 -70 L0 -196 Z" fill="#8a55d6"/>'
                '<path d="M-8 -60 L0 -170" stroke="#efe0ff" stroke-width="5" stroke-linecap="round"/>'
                '</g>') % (angle, length)
    return out


# The Wild Update: a frog.
FROG = '''
  <ellipse cx="200" cy="238" rx="98" ry="62" fill="#5f9e45"/>
  <ellipse cx="200" cy="254" rx="60" ry="34" fill="#a6d07a"/>
  <rect x="116" y="270" width="40" height="22" rx="10" fill="#4e8838"/>
  <rect x="244" y="270" width="40" height="22" rx="10" fill="#4e8838"/>
  <circle cx="152" cy="176" r="32" fill="#5f9e45"/>
  <circle cx="248" cy="176" r="32" fill="#5f9e45"/>
  <rect x="136" y="160" width="30" height="30" rx="6" fill="#1b1d12"/>
  <rect x="234" y="160" width="30" height="30" rx="6" fill="#1b1d12"/>
  <rect x="140" y="164" width="9" height="9" fill="#fff"/>
  <rect x="238" y="164" width="9" height="9" fill="#fff"/>
  <path d="M156 226 Q200 246 244 226" stroke="#2f5a21" stroke-width="5" fill="none" stroke-linecap="round"/>'''


def blossom(cx, cy, r):
    out = ''
    for i in range(5):
        a = math.radians(i * 72 - 90)
        out += '<circle cx="%.1f" cy="%.1f" r="%.1f" fill="#ffb3d1"/>' % (cx + math.cos(a) * r, cy + math.sin(a) * r, r * 0.95)
    out += '<circle cx="%g" cy="%g" r="%.1f" fill="#ffe28a"/>' % (cx, cy, r * 0.55)
    return out


# Trails & Tales: a cherry blossom branch, with falling petals.
CHERRY = ('''
  <path d="M104 316 C150 270 170 230 214 196 C246 172 268 140 296 100" stroke="#5b3424" stroke-width="16" fill="none" stroke-linecap="round"/>
  <path d="M190 214 C176 182 178 156 192 132" stroke="#5b3424" stroke-width="10" fill="none" stroke-linecap="round"/>'''
          + blossom(192, 128, 16) + blossom(298, 98, 17) + blossom(240, 168, 15) + blossom(150, 262, 15) + blossom(262, 214, 13)
          + '''
  <g fill="#ff9cc3"><ellipse cx="120" cy="200" rx="7" ry="4" transform="rotate(-30 120 200)"/>
  <ellipse cx="300" cy="280" rx="7" ry="4" transform="rotate(25 300 280)"/>
  <ellipse cx="226" cy="300" rx="6" ry="3.5" transform="rotate(-15 226 300)"/></g>''')

# Tricky Trials: a copper trial key.
KEY = '''
  <circle cx="200" cy="132" r="44" fill="none" stroke="#c9773d" stroke-width="22"/>
  <path d="M168 104 A44 44 0 0 1 228 104" stroke="#f0a96c" stroke-width="7" fill="none" stroke-linecap="round"/>
  <rect x="189" y="170" width="22" height="146" rx="6" fill="#c9773d"/>
  <rect x="189" y="170" width="8" height="146" rx="4" fill="#f0a96c"/>
  <rect x="209" y="264" width="38" height="16" rx="4" fill="#a85e2c"/>
  <rect x="209" y="294" width="28" height="16" rx="4" fill="#a85e2c"/>
  <circle cx="200" cy="132" r="12" fill="#3fbf9f"/>'''

# Backport Core: a dark block with glowing seams, the frame's own lavender.
CORE = cube('#3a3150', '#2a2339', '#1f1a2b', edge='#b7a6f2') + '''
  <circle cx="200" cy="152" r="16" fill="#e9e1ff"/>
  <circle cx="200" cy="152" r="28" fill="#b7a6f2" opacity="0.35"/>'''

# Modern MC: a grass block, drawn texel by texel from Minecraft's own 16x16 grass_block_side and
# grass_block_top (1.18.2, in grass/), so every pixel row runs parallel to the block's edges as in game.
# Top tinted with plains grass (#91BD59); side faces shaded as vanilla lights them (0.8 and 0.6).
def textured_cube(cx=200, cy=110, w=84, h=42, depth=104):
    from PIL import Image
    side = Image.open(os.path.join(HERE, 'grass', 'grass_block_side.png')).convert('RGB')
    top = Image.open(os.path.join(HERE, 'grass', 'grass_block_top.png')).convert('RGB')
    tint = (0x91, 0xBD, 0x59)
    n = 16

    def col(rgb, k):
        return '#%02x%02x%02x' % tuple(max(0, min(255, int(round(c * k)))) for c in rgb)

    def face(origin, du, dv, texel):
        # origin: corner of texel (0,0); du/dv: one texel step along u and v.
        out = []
        for v in range(n):
            for u in range(n):
                p0 = (origin[0] + u * du[0] + v * dv[0], origin[1] + u * du[1] + v * dv[1])
                q = [p0, (p0[0] + du[0], p0[1] + du[1]),
                     (p0[0] + du[0] + dv[0], p0[1] + du[1] + dv[1]), (p0[0] + dv[0], p0[1] + dv[1])]
                c = texel(u, v)
                out.append('<polygon points="%s" fill="%s" stroke="%s" stroke-width="0.6" stroke-linejoin="round"/>'
                           % (' '.join('%.2f,%.2f' % pt for pt in q), c, c))
        return ''.join(out)

    l = (cx - w, cy + h)      # left corner of the top diamond
    b = (cx, cy + 2 * h)      # front corner
    t = (cx, cy)              # back corner
    down = depth / float(n)
    # Left face: u runs from the left corner to the front corner, v straight down.
    left = face(l, (w / float(n), h / float(n)), (0, down),
                lambda u, v: col(side.getpixel((u, v)), 0.8))
    # Right face: u runs from the front corner to the right corner, v straight down.
    right = face(b, (w / float(n), -h / float(n)), (0, down),
                 lambda u, v: col(side.getpixel((u, v)), 0.6))
    # Top face: u from the left corner toward the back, v from the left corner toward the front.
    top_face = face(l, (w / float(n), -h / float(n)), (w / float(n), h / float(n)),
                    lambda u, v: col(tuple(a * t_ / 255.0 for a, t_ in zip(top.getpixel((u, v)), tint)), 1.0))
    return left + right + top_face


GRASS = textured_cube()


def flat_cube(cx=200, cy=110, w=84, h=42, depth=104):
    """The family's flat style: the old colors, with the grass edge stepped texel by texel exactly where
    Minecraft's grass_block_side_overlay is, so each step runs parallel to the block's edges."""
    from PIL import Image
    mask = Image.open(os.path.join(HERE, 'grass', 'grass_block_side_overlay.png')).convert('RGBA')
    n = 16
    out = cube('#6bb04a', '#8a5a3a', '#6e4630', cx=cx, cy=cy, w=w, h=h, depth=depth)
    l, b = (cx - w, cy + h), (cx, cy + 2 * h)
    down = depth / float(n)

    def strip(origin, du, color):
        # Union of green texels per column: one parallelogram per column, from the top edge down to its
        # last green texel, so the edge steps along the face's own grid.
        parts = []
        for u in range(n):
            last = max([v for v in range(n) if mask.getpixel((u, v))[3] > 0] or [-1])
            if last < 0:
                continue
            x0, y0 = origin[0] + u * du[0], origin[1] + u * du[1]
            q = [(x0, y0), (x0 + du[0], y0 + du[1]), (x0 + du[0], y0 + du[1] + (last + 1) * down),
                 (x0, y0 + (last + 1) * down)]
            parts.append('<polygon points="%s" fill="%s" stroke="%s" stroke-width="0.6"/>'
                         % (' '.join('%.2f,%.2f' % pt for pt in q), color, color))
        return ''.join(parts)

    out += strip(l, (w / float(n), h / float(n)), '#5a9a3c')
    out += strip(b, (w / float(n), -h / float(n)), '#4c8732')
    return out


GRASS_FLAT = flat_cube()

ICONS = [
    ('backport-core', 'Backport Core', '#b7a6f2', CORE),
    ('update-aquatic-backport', 'Update Aquatic Backport', '#3fd0ff', TRIDENT),
    ('village-and-pillage-backport', 'Village & Pillage Backport', '#ffcf5a', BELL_FRAME),
    ('buzzy-bees-backport', 'Buzzy Bees Backport', '#ffc23a', BEE),
    ('nether-update-backport', 'Nether Update Backport', '#e0533a', INGOT),
    ('caves-and-cliffs-backport', 'Caves & Cliffs Backport', '#a86cff', amethyst()),
    ('wild-update-backport', 'Wild Update Backport', '#7fd36b', FROG),
    ('trails-and-tales-backport', 'Trails & Tales Backport', '#ff8fc0', CHERRY),
    ('tricky-trials-backport', 'Tricky Trials Backport', '#ffa64d', KEY),
    ('modern-mc', 'Modern MC', '#7fd36b', GRASS_FLAT),  # chosen 2026-11-07; GRASS is the textured option
]

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    profile = os.path.join(os.environ.get('TEMP', OUT), 'vbp-edge-profile')
    import sys
    only = set(sys.argv[1:])  # optional: the slugs to redraw
    for slug, title, halo, emblem in ICONS:
        if only and slug not in only:
            continue
        svg = os.path.join(OUT, slug + '.svg')
        with open(svg, 'w', encoding='utf-8', newline='\n') as f:
            f.write(FRAME.replace('{halo}', halo).replace('{emblem}', emblem))
        subprocess.run([EDGE, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--user-data-dir=' + profile,
                        '--default-background-color=00000000', '--force-device-scale-factor=1.28',
                        '--window-size=400,400', '--screenshot=' + os.path.join(OUT, slug + '.png'),
                        'file:///' + svg.replace('\\', '/')], check=True, capture_output=True)
        print('drew', title)
