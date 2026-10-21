"""Draw every can from one 13×32 template, crumpled too, and the empty and winged cans from the same one;
then give every texture in the folder an item model.

Standard library only: python3 -I scripts/textures.py
A new flavour is one line in CANS. Taurine's texture comes from Mekanism and isn't drawn here.
"""

import json
import struct
import sys
import zlib
from pathlib import Path

# Colour roles of the template:
#   A lid, C body edge, D body, E silver, F silver edge, S sugarfree stripe (silver otherwise),
#   T wordmark on the body, V wordmark on silver, U wordmark on the silver edge, R bulls, Y sun
TEMPLATE = [
    "AAAAAAAAAAAAA",
    ".CDDDDDDDEEF.",
    "CDEEEDDDDEEEF",
    "CDDDDDDDDEEEF",
    "CDSSSSSDDEEEF",
    "CDDDDDDDEEEEF",
    "CDDDDDDDEEEEF",
    "CDDDDDDDEEEEF",
    "CDDDDDDDEEEEF",
    "CDDDDDDDEEEEF",
    "CTTDDDDTVEVEU",
    "CTTDTTDTEEVEU",
    "CTTDTTDTVEVEU",
    "CTDTDTDTVVVEU",
    "CDDDDDEEEEEEF",
    "CDDDDDEEEEEEF",
    "CDDDDYYYEEEEF",
    "FEEEYYYYYDDDC",
    "FERERRYRRDRDC",
    "FEERRRYRRRDDC",
    "FEERERYRDRDDC",
    "FEEEEDDDDDDDC",
    "FEEEEDDDDDDDC",
    "FEEEEDDDDDDDC",
    "FEEEDDDDDDDDC",
    "FEEEDDDDDDDDC",
    "FEEDDDDDDDDDC",
    "CDDEEEEEEEEEF",
    "CDDEEDDDDDEEF",
    "FEEDDEEEEEDDC",
    ".FEDDDDDDDDC.",
    "AAAAAAAAAAAAA",
]

BASE = {
    "A": "#cec7d5",
    "E": "#e1dee4",
    "F": "#a29fa3",
    "T": "#fd0046",
    "V": "#fd0046",
    "U": "#b00032",
    "R": "#fd0046",
    "Y": "#fbcd00",
}
SUGARFREE = "#49b4ee"
WHITE_TEXT = {"T": "#ffffff"}

# body: one colour or a top-to-bottom gradient; edge: darker body (derived when left out);
# extra: overrides of any role
CANS = {
    "original": {"body": "#213ba2", "edge": "#13225d"},
    "sugarfree": {"body": "#2f8fd8", "sugarfree": True},
    "zero": {"body": "#8ccbef", "edge": "#4f8fbf", "sugarfree": True},
    "red": {"body": "#e63b2e", "extra": WHITE_TEXT},
    "red_sugarfree": {"body": "#e63b2e", "extra": WHITE_TEXT, "sugarfree": True},
    "ruby": {"body": "#a3173c", "extra": WHITE_TEXT},
    "peach": {"body": "#ec5a72", "extra": WHITE_TEXT},
    "pink_sugarfree": {"body": "#ee6fb0", "extra": WHITE_TEXT, "sugarfree": True},
    "lilac_sugarfree": {"body": "#8b67cf", "extra": WHITE_TEXT, "sugarfree": True},
    "blue": {"body": "#2d7fe0"},
    "sea_blue": {"body": "#1f9ad0"},
    "ice": {"body": "#45c9c8"},
    "yellow": {"body": "#f6d10a"},
    # Where a real can draws its big bull in colour, E and F paint the silver diagonal that colour
    "tropical": {"body": "#f7d117", "extra": {"E": "#2a3fa8", "F": "#17256b", "T": "#2a3fa8"}},
    "glacier": {"body": "#b8def3", "edge": "#6fa3c4", "extra": {"E": "#1f3b9a", "F": "#142660", "T": "#1f3b9a"}},
    "purple": {"body": "#6a2c8e", "extra": WHITE_TEXT},
    "summer": {"body": "#f2e48a", "edge": "#b9a650", "extra": {"T": "#7a5a2e"}},
    "amber": {"body": "#f39320", "extra": WHITE_TEXT},
    "amber_sugarfree": {"body": "#f39320", "extra": WHITE_TEXT, "sugarfree": True},
    "green": {"body": "#5bbf3c"},
    "coconut": {"body": "#f4f4f2", "edge": "#b8bcc4", "extra": {"E": "#c9ccd3", "F": "#8f939b"}},
    "coconut_sugarfree": {
        "body": "#f4f4f2", "edge": "#b8bcc4", "extra": {"E": "#c9ccd3", "F": "#8f939b"}, "sugarfree": True,
    },
    "winter_apple": {"body": ("#f6c51b", "#e2482c")},
    "winter_pistachio": {"body": "#b3cc3a", "extra": WHITE_TEXT},
    "spring": {
        "body": "#f6eff1", "edge": "#c9b3ba", "extra": {"E": "#dccfd4", "F": "#a8959c", "T": "#e0365c"},
    },
    "cherry": {
        "body": "#f6f4f4", "edge": "#b9b5b8",
        "extra": {"E": "#d6334f", "F": "#9e1f37", "T": "#7c7c80", "V": "#ffffff", "U": "#f0d6db"},
    },
}


def rgb(colour):
    return tuple(int(colour[i:i + 2], 16) for i in (1, 3, 5))


def darker(colour, factor=0.58):
    return tuple(round(channel * factor) for channel in colour)


def mix(top, bottom, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(top, bottom))


def paint(can):
    roles = {role: rgb(colour) for role, colour in BASE.items()}
    roles.update({role: rgb(colour) for role, colour in can.get("extra", {}).items()})
    roles["S"] = rgb(SUGARFREE) if can.get("sugarfree") else roles["E"]
    body = can["body"]
    top, bottom = (rgb(body[0]), rgb(body[1])) if isinstance(body, tuple) else (rgb(body), rgb(body))
    size, left = 32, (32 - len(TEMPLATE[0])) // 2
    pixels = [[(0, 0, 0, 0)] * size for _ in range(size)]
    for y, row in enumerate(TEMPLATE):
        roles["D"] = mix(top, bottom, y / (len(TEMPLATE) - 1))
        roles["C"] = rgb(can["edge"]) if "edge" in can else darker(roles["D"])
        for x, role in enumerate(row):
            if role != ".":
                pixels[y][left + x] = roles[role] + (255,)
    return pixels


def png(pixels):
    raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    header = struct.pack(">IIBBBBB", len(pixels[0]), len(pixels), 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


EMPTY = {
    "body": "#b7bcc5", "edge": "#6f7681",
    "extra": {"T": "#b7bcc5", "V": "#e1dee4", "U": "#a29fa3", "R": "#e1dee4", "Y": "#e1dee4"},
}

LEFT_WING = [
    ".......OO",
    ".....OOWO",
    "...OOWWWO",
    "..OWWWWWO",
    ".OWWWWWWO",
    "OWWWWWWLO",
    "OOOWWWWLO",
    ".OWWWWLLO",
    "OOOWWWLO.",
    ".OWWWLLO.",
    "..OOWLO..",
    "....OO...",
]


def winged_can():
    pixels = paint(EMPTY)
    colours = {"O": rgb("#8f99a4"), "W": rgb("#ffffff"), "L": rgb("#d3dae1")}
    for y, row in enumerate(LEFT_WING):
        for x, role in enumerate(row):
            if role != ".":
                pixels[6 + y][x] = colours[role] + (255,)
                pixels[6 + y][31 - x] = colours[role] + (255,)
    return pixels


def scale(colour, factor):
    return tuple(max(0, min(255, round(channel * factor))) for channel in colour[:3]) + (255,)


# A can squeezed in a hand after the last sip: how far each row's left and right walls are pushed in. The dents
# are uneven on purpose, and the top half leans right, as a can squeezed off-centre does.
DENTS = {5: (1, 0), 6: (2, 0), 7: (2, 1), 8: (3, 1), 9: (3, 1), 10: (2, 2), 11: (2, 2), 12: (3, 2), 13: (3, 3),
         14: (4, 2), 15: (3, 2), 16: (2, 3), 17: (2, 3), 18: (1, 3), 19: (1, 2), 20: (2, 1), 21: (2, 1), 22: (1, 0), 23: (1, 0)}
LEAN_UNTIL = 12


def crumple(can):
    whole = paint(can)
    left_edge = (32 - len(TEMPLATE[0])) // 2
    rows = [whole[y][left_edge:left_edge + len(TEMPLATE[0])] for y in range(32)]
    pixels = [[(0, 0, 0, 0)] * 32 for _ in range(32)]
    for y, row in enumerate(rows):
        left, right = DENTS.get(y, (0, 0))
        width = len(row) - left - right
        line = [row[round(i * (len(row) - 1) / (width - 1))] for i in range(width)]
        if y in DENTS:
            for i, pixel in enumerate(line):
                if not pixel[3] or i in (0, width - 1):
                    continue
                fold = (i + y) % 6
                if fold == 0:
                    line[i] = scale(pixel, 0.72)  # a crease running down to the right
                elif fold == 1:
                    line[i] = scale(pixel, 1.15)  # the light on its edge
                if 6 <= y <= 11 and 1 <= i <= 3:
                    line[i] = scale(line[i], 0.8)  # the dent where the thumb pressed
            line[0], line[-1] = scale(line[0], 0.75), scale(line[-1], 0.75)
        x0 = left_edge + left + (1 if y <= LEAN_UNTIL else 0)
        for i, pixel in enumerate(line):
            if pixel[3]:
                pixels[y][x0 + i] = pixel
    return pixels


ITEMS = {
    "empty_can": lambda: paint(EMPTY),
    "winged_can": winged_can,
}

ASSETS = Path(__file__).resolve().parent.parent / "core/src/main/resources/assets/redbull"
OUT = ASSETS / "textures/item"


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def models():
    """A flat item model and an item definition for every texture, the way the game draws its own items."""
    names = sorted(texture.stem for texture in OUT.glob("*.png"))
    for name in names:
        write_json(ASSETS / f"models/item/{name}.json",
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"redbull:item/{name}"}})
        write_json(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"redbull:item/{name}"}})
    return names


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, can in CANS.items():
        (OUT / f"{name}.png").write_bytes(png(paint(can)))
        (OUT / f"crumpled_{name}.png").write_bytes(png(crumple(can)))
    for name, draw in ITEMS.items():
        (OUT / f"{name}.png").write_bytes(png(draw()))
    print(f"{2 * len(CANS) + len(ITEMS)} textures, {len(models())} models in {ASSETS}")


if __name__ == "__main__":
    main()
