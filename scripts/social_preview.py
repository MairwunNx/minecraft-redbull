"""Draw media/social-preview.png, the 1280×640 picture GitHub shows for the repository, and the README's header.

Standard library only: python3 -I scripts/social_preview.py [--client-jar <jar>]
The text uses the game's own font, read from a client jar; Gradle's caches hold one after any mod build, and
--client-jar names another. Mojang's font isn't kept in this repository.
"""

import argparse
import glob
import os
import struct
import sys
import zipfile
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from textures import png  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "core/src/main/resources/assets/redbull/textures/item"
OUT = ROOT / "media/social-preview.png"
FONT = "assets/minecraft/textures/font/ascii.png"
WIDTH, HEIGHT = 1280, 640

LINES = [
    # text, scale, colour, x, y
    ("Red Bull", 13, (255, 255, 255), 64, 120),
    ("26 real cans to brew, find, and drink", 4, (0xFB, 0xCD, 0x00), 70, 290),
    ("Wiiings, caffeine, and a heart to mind", 4, (255, 255, 255), 70, 350),
    ("Paper  Fabric  NeoForge  26.3", 4, (0xFD, 0x00, 0x46), 70, 470),
]
CANS = [("spring", 880, 60, 9), ("original", 990, 150, 12), ("cherry", 1150, 80, 9)]  # texture, x, y, scale
BAND = [(1000, 0), (1180, 0), (900, HEIGHT), (720, HEIGHT)]  # the can's silver diagonal
BAND_EDGE = [(1180, 0), (1200, 0), (920, HEIGHT), (900, HEIGHT)]


def read_png(data):
    """RGBA rows of an 8-bit PNG of any colour type, or a palette one of any depth: what the game's files use."""
    pos, chunks = 8, {}
    idat = b""
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IDAT":
            idat += body
        else:
            chunks[kind] = body
    width, height, depth, colour = struct.unpack(">IIBB", chunks[b"IHDR"][:10])
    assert colour == 3 or depth == 8, "16-bit PNGs aren't used here"
    size = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colour]
    raw = zlib.decompress(idat)
    stride = (width * depth + 7) // 8 if colour == 3 else width * size
    rows, previous = [], bytearray(stride)
    for y in range(height):
        kind, line = raw[y * (stride + 1)], bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for i in range(stride):
            left = line[i - size] if i >= size else 0
            up, corner = previous[i], previous[i - size] if i >= size else 0
            if kind == 1:
                line[i] = (line[i] + left) & 255
            elif kind == 2:
                line[i] = (line[i] + up) & 255
            elif kind == 3:
                line[i] = (line[i] + (left + up) // 2) & 255
            elif kind == 4:
                p = left + up - corner
                pa, pb, pc = abs(p - left), abs(p - up), abs(p - corner)
                line[i] = (line[i] + (left if pa <= pb and pa <= pc else up if pb <= pc else corner)) & 255
        rows.append(line)
        previous = line
    if colour == 6:
        return [[tuple(row[x * 4:x * 4 + 4]) for x in range(width)] for row in rows]
    if colour == 2:
        return [[tuple(row[x * 3:x * 3 + 3]) + (255,) for x in range(width)] for row in rows]
    if colour == 4:
        return [[(row[x * 2],) * 3 + (row[x * 2 + 1],) for x in range(width)] for row in rows]
    if colour == 0:
        return [[(row[x],) * 3 + (255,) for x in range(width)] for row in rows]
    palette = chunks[b"PLTE"]
    alpha = chunks.get(b"tRNS", b"")
    def entry(i):
        return tuple(palette[i * 3:i * 3 + 3]) + ((alpha[i] if i < len(alpha) else 255),)
    def index(row, x):
        bit = x * depth
        return (row[bit // 8] >> (8 - depth - bit % 8)) & ((1 << depth) - 1)
    return [[entry(index(row, x)) for x in range(width)] for row in rows]


def client_jar(given):
    if given:
        return given
    patterns = ["~/.gradle/caches/neoformruntime/artifacts/minecraft_*_client.jar", "~/.gradle/caches/fabric-loom/**/*client*.jar"]
    for pattern in patterns:
        for jar in sorted(glob.glob(os.path.expanduser(pattern), recursive=True), reverse=True):
            try:
                with zipfile.ZipFile(jar) as archive:
                    if FONT in archive.namelist():
                        return jar
            except zipfile.BadZipFile:
                continue
    sys.exit("No Minecraft client jar with the font in Gradle's caches; build a mod once or pass --client-jar")


class Font:
    def __init__(self, sheet):
        self.sheet = sheet

    def glyph(self, char):
        code = ord(char)
        x0, y0 = (code % 16) * 8, (code // 16) * 8
        cells = [row[x0:x0 + 8] for row in self.sheet[y0:y0 + 8]]
        if char == " ":
            return [row[:4] for row in cells]
        used = [x for x in range(8) if any(row[x][3] for row in cells)]
        return [row[:max(used) + 1 if used else 4] for row in cells]


def blend(canvas, x, y, colour):
    if 0 <= x < WIDTH and 0 <= y < HEIGHT and colour[3]:
        canvas[y][x] = colour[:3] + (255,)


def stamp(canvas, image, left, top, scale):
    for y, row in enumerate(image):
        for x, pixel in enumerate(row):
            if pixel[3]:
                for dy in range(scale):
                    for dx in range(scale):
                        blend(canvas, left + x * scale + dx, top + y * scale + dy, pixel)


def text(canvas, font, string, scale, colour, left, top):
    """The game's way: each glyph, a shadow a quarter as bright one pixel down and right, one pixel between glyphs."""
    shadow = tuple(c // 4 for c in colour)
    for shade, offset in ((shadow, 1), (colour, 0)):
        x = left
        for char in string:
            glyph = font.glyph(char)
            tinted = [[shade + (255,) if pixel[3] else (0, 0, 0, 0) for pixel in row] for row in glyph]
            stamp(canvas, tinted, x + offset * scale, top + offset * scale, scale)
            x += (len(glyph[0]) + 1) * scale


def inside(polygon, x, y):
    result, j = False, len(polygon) - 1
    for i, (xi, yi) in enumerate(polygon):
        xj, yj = polygon[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            result = not result
        j = i
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--client-jar")
    args = parser.parse_args()
    with zipfile.ZipFile(client_jar(args.client_jar)) as archive:
        font = Font(read_png(archive.read(FONT)))

    top_left, bottom_right = (0x21, 0x3B, 0xA2), (0x13, 0x22, 0x5D)
    canvas = []
    for y in range(HEIGHT):
        row = []
        for x in range(WIDTH):
            t = x / WIDTH
            colour = tuple(round(a + (b - a) * t) for a, b in zip(top_left, bottom_right)) + (255,)
            if inside(BAND_EDGE, x + 0.5, y + 0.5):
                colour = (0xA2, 0x9F, 0xA3, 255)
            elif inside(BAND, x + 0.5, y + 0.5):
                colour = (0xE1, 0xDE, 0xE4, 255)
            row.append(colour)
        canvas.append(row)

    for name, x, y, scale in CANS:
        can = read_png((TEXTURES / f"{name}.png").read_bytes())
        stamp(canvas, [row[9:22] for row in can], x, y, scale)
    for string, scale, colour, x, y in LINES:
        text(canvas, font, string, scale, colour, x, y)

    OUT.write_bytes(png(canvas))
    print(f"{OUT} ({WIDTH}×{HEIGHT})")


if __name__ == "__main__":
    main()
