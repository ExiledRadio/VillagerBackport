"""Writes three Village & Pillage icon options as SVG, in the current icon's frame.

The frame (dark rounded square, warm center glow, lavender-edged sparkles) is shared, so the family of
backport icons can reuse it with a different emblem each.
"""
import os
import sys

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))

FRAME = '''<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
<defs>
  <radialGradient id="bg" cx="50%" cy="54%" r="62%">
    <stop offset="0" stop-color="#3b2433"/>
    <stop offset="0.42" stop-color="#21161f"/>
    <stop offset="1" stop-color="#110f18"/>
  </radialGradient>
  <radialGradient id="halo" cx="50%" cy="50%" r="50%">
    <stop offset="0" stop-color="{halo}" stop-opacity="0.38"/>
    <stop offset="1" stop-color="{halo}" stop-opacity="0"/>
  </radialGradient>
  <filter id="sparkglow" x="-100%" y="-100%" width="300%" height="300%">
    <feGaussianBlur in="SourceAlpha" stdDeviation="2.2" result="b"/>
    <feFlood flood-color="#b7a6f2"/>
    <feComposite in2="b" operator="in" result="g"/>
    <feMerge><feMergeNode in="g"/><feMergeNode in="g"/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
  <filter id="soft" x="-20%" y="-20%" width="140%" height="140%">
    <feDropShadow dx="0" dy="6" stdDeviation="6" flood-color="#000" flood-opacity="0.45"/>
  </filter>
  <symbol id="spark" viewBox="-10 -10 20 20" overflow="visible">
    <path d="M0 -10 C0.9 -2.2 2.2 -0.9 10 0 C2.2 0.9 0.9 2.2 0 10 C-0.9 2.2 -2.2 0.9 -10 0 C-2.2 -0.9 -0.9 -2.2 0 -10 Z" fill="#f4f0ff"/>
  </symbol>
</defs>
<rect width="400" height="400" rx="78" fill="url(#bg)"/>
<g filter="url(#sparkglow)">
  <use href="#spark" x="48" y="78" width="36" height="36"/>
  <use href="#spark" x="312" y="72" width="36" height="36"/>
  <use href="#spark" x="32" y="184" width="24" height="24"/>
  <use href="#spark" x="346" y="186" width="20" height="20"/>
  <use href="#spark" x="28" y="282" width="18" height="18"/>
  <use href="#spark" x="352" y="268" width="18" height="18"/>
  <use href="#spark" x="74" y="326" width="28" height="28"/>
  <use href="#spark" x="296" y="330" width="26" height="26"/>
</g>
<circle cx="200" cy="205" r="150" fill="url(#halo)"/>
<g filter="url(#soft)">
{emblem}
</g>
</svg>
'''

# A 1.14-style bell: gold body, its lip, the clapper, hung from a dark oak beam.
BELL = '''
  <g id="bell">
    <rect x="191" y="132" width="18" height="18" rx="3" fill="#7a5a2c"/>
    <path d="M200 146 C168 146 160 176 158 214 C157 232 154 246 150 254 L250 254 C246 246 243 232 242 214 C240 176 232 146 200 146 Z" fill="#f2c14e"/>
    <path d="M200 146 C232 146 240 176 242 214 C243 232 246 246 250 254 L208 254 C212 222 214 176 200 146 Z" fill="#d9962b"/>
    <path d="M178 172 C172 190 170 210 170 230" stroke="#ffe7a3" stroke-width="7" stroke-linecap="round" fill="none"/>
    <rect x="142" y="250" width="116" height="20" rx="6" fill="#e8b23f"/>
    <rect x="200" y="250" width="58" height="20" rx="6" fill="#c9862a"/>
    <rect x="142" y="250" width="116" height="5" rx="2.5" fill="#ffd977"/>
    <circle cx="200" cy="281" r="10" fill="#b7782a"/>
  </g>'''

BELL_FRAME = '''
  <rect x="100" y="112" width="200" height="22" rx="5" fill="#5b3a22"/>
  <rect x="100" y="112" width="200" height="6" rx="3" fill="#77502f"/>
  <rect x="104" y="112" width="26" height="196" rx="5" fill="#6f747a"/>
  <rect x="104" y="112" width="9" height="196" rx="4" fill="#8f959b"/>
  <rect x="270" y="112" width="26" height="196" rx="5" fill="#5e6369"/>
  <rect x="270" y="112" width="9" height="196" rx="4" fill="#7f858b"/>
  <rect x="92" y="300" width="216" height="16" rx="5" fill="#4b4f55"/>''' + BELL


def crossbow(transform):
    """A loaded crossbow pointing up, centered on the origin, placed by `transform`."""
    return '''
  <g transform="{t}">
    <path d="M-86 -32 Q0 -86 86 -32" stroke="#4a2e18" stroke-width="15" stroke-linecap="round" fill="none"/>
    <path d="M-86 -32 Q0 -86 86 -32" stroke="#7a4d2b" stroke-width="9" stroke-linecap="round" fill="none"/>
    <path d="M-84 -31 L0 -2 L84 -31" stroke="#e9e4da" stroke-width="3" fill="none"/>
    <rect x="-9" y="-72" width="18" height="168" rx="6" fill="#8a5a34"/>
    <rect x="-9" y="-72" width="7" height="168" rx="3" fill="#a8744a"/>
    <rect x="-13" y="58" width="26" height="40" rx="7" fill="#6b4426"/>
    <rect x="-4" y="-104" width="8" height="106" rx="3" fill="#b9b3a7"/>
    <path d="M0 -122 L13 -98 L-13 -98 Z" fill="#cfd5db"/>
    <path d="M0 -122 L13 -98 L0 -98 Z" fill="#9aa2aa"/>
    <rect x="-6" y="30" width="12" height="18" rx="3" fill="#3d3f44"/>
  </g>'''.format(t=transform)


# An ominous-banner-like cloth: pale with a dark band, a teal diamond and gray trim. Drawn fresh,
# not traced from the game's pattern.
BANNER = '''
  <rect x="128" y="74" width="144" height="12" rx="5" fill="#5b3a22"/>
  <path d="M142 86 L258 86 L258 300 L200 278 L142 300 Z" fill="#ece9e1"/>
  <path d="M200 86 L258 86 L258 300 L200 278 Z" fill="#d6d2c8"/>
  <rect x="142" y="102" width="116" height="16" fill="#8d9296"/>
  <rect x="142" y="170" width="116" height="20" fill="#2b2b31"/>
  <path d="M200 132 L232 180 L200 228 L168 180 Z" fill="#3aa9a5"/>
  <path d="M200 132 L232 180 L200 228 Z" fill="#2c8c89"/>
  <rect x="142" y="242" width="116" height="12" fill="#8d9296"/>
  <rect x="176" y="176" width="14" height="8" fill="#111"/>
  <rect x="210" y="176" width="14" height="8" fill="#111"/>'''

# A Minecraft-shaped crossbow standing upright behind the bell, drawn back with one arrow nocked: the
# limbs sweep back from an iron-banded front, the string runs from their tips down to the latch below
# the bell, and the arrow's shaft passes up behind the bell to its head above the limbs.
LOADED_CROSSBOW_BACK = '''
  <path d="M96 156 Q200 74 304 156" stroke="#3f2715" stroke-width="18" stroke-linecap="round" fill="none"/>
  <path d="M96 156 Q200 74 304 156" stroke="#6e4527" stroke-width="10" stroke-linecap="round" fill="none"/>
  <path d="M104 148 Q200 84 296 148" stroke="#8d5f37" stroke-width="3" stroke-linecap="round" fill="none"/>
  <circle cx="96" cy="156" r="7" fill="#9aa2aa"/>
  <circle cx="304" cy="156" r="7" fill="#9aa2aa"/>
  <path d="M98 158 L200 300 L302 158" stroke="#ece6da" stroke-width="3.5" stroke-linejoin="round" fill="none"/>
  <rect x="188" y="104" width="24" height="236" rx="6" fill="#7b4f2d"/>
  <rect x="188" y="104" width="9" height="236" rx="4" fill="#9b6a40"/>
  <rect x="184" y="106" width="32" height="16" rx="4" fill="#8f979f"/>
  <rect x="184" y="106" width="32" height="5" rx="2.5" fill="#b9c0c7"/>
  <rect x="196.5" y="66" width="7" height="236" rx="3" fill="#cdb78c"/>
  <path d="M200 38 L214 70 L186 70 Z" fill="#d2d8de"/>
  <path d="M200 38 L214 70 L200 70 Z" fill="#9aa2aa"/>'''

LOADED_CROSSBOW_FRONT = '''
  <path d="M196 280 L184 268 L184 294 L196 300 Z" fill="#f1efe9"/>
  <path d="M204 280 L216 268 L216 294 L204 300 Z" fill="#c9c4bb"/>
  <rect x="192" y="294" width="16" height="12" rx="3" fill="#4a4d53"/>
  <rect x="182" y="304" width="36" height="42" rx="8" fill="#5a3a20"/>
  <rect x="182" y="304" width="12" height="42" rx="6" fill="#74502e"/>
  <path d="M218 312 L230 318 L226 330 L218 326 Z" fill="#7d858d"/>'''

OPTIONS = {
    'option-a-bell': ('#ffcf5a', BELL_FRAME),
    'option-b-banner-crossbow': ('#46c2bd', BANNER + crossbow('translate(208 214) rotate(-38) scale(0.92)')),
    'option-c-bell-crossbow': ('#ffcf5a', LOADED_CROSSBOW_BACK
                               + '<g transform="translate(200 206) scale(0.78) translate(-200 -206)">' + BELL + '</g>'
                               + LOADED_CROSSBOW_FRONT),
}

os.makedirs(OUT, exist_ok=True)
for name, (halo, emblem) in OPTIONS.items():
    with open(os.path.join(OUT, name + '.svg'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(FRAME.replace('{halo}', halo).replace('{emblem}', emblem))
    print('wrote', name)
