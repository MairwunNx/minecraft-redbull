"""Draw media/crafting.gif: a crafting table cycling through every Red Bull recipe, 1.5 seconds each.

Standard library only: ./gradlew :neoforge:generateData, then python3 -I scripts/crafting_gif.py [--client-jar <jar>]
The recipes come from the data the build generates from core, so the GIF can't drift from the game. The window, the
vanilla items, and the font come from a client jar in Gradle's caches, as in social_preview.py; Mojang's files aren't
kept in this repository.
"""

import argparse
import json
import struct
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from social_preview import Font, client_jar, read_png  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "core/src/main/resources/assets/redbull"
RECIPES = ROOT / "neoforge/build/generated/data/data/redbull/recipe"
OUT = ROOT / "media/crafting.gif"
GUI = 2  # the game's interface scale: a slot is 36 pixels, our 32-pixel cans fit it as they are
ZOOM = 2  # and the whole picture twice again, as wide as media/cans.png
FRAME_MS = 1500
WATER = (0x38, 0x5D, 0xC6)


class Vanilla:
    """Items as the game draws them in a slot: a flat sprite, or a block as a little cube."""

    def __init__(self, jar):
        self.zip = zipfile.ZipFile(jar)

    def png(self, path):
        return read_png(self.zip.read(path))

    def json(self, path):
        return json.loads(self.zip.read(path))

    def texture(self, ref):
        namespace, path = ref.split(":") if ":" in ref else ("minecraft", ref)
        return self.png(f"assets/{namespace}/textures/{path}.png")

    def item(self, item_id):
        name = item_id.split(":")[1]
        definition = self.json(f"assets/minecraft/items/{name}.json")["model"]
        model_ref = definition.get("model") or definition.get("fallback", {}).get("model")
        textures, parent = {}, model_ref
        while parent:
            namespace, path = parent.split(":") if ":" in parent else ("minecraft", parent)
            try:
                model = self.json(f"assets/{namespace}/models/{path}.json")
            except KeyError:
                break
            textures = {**model.get("textures", {}), **textures}
            if "layer0" in textures:
                return scale(self.texture(textures["layer0"]), GUI)
            parent = model.get("parent")
            if parent and parent.split("/")[-1] in ("cube_all", "cube_column", "cube"):
                top = textures.get("all") or textures.get("end") or textures.get("up")
                side = textures.get("all") or textures.get("side") or textures.get("north")
                return cube(self.texture(top), self.texture(side))
        sys.exit(f"Can't draw {item_id}")

    def water_bottle(self):
        bottle = self.png("assets/minecraft/textures/item/potion.png")
        overlay = self.png("assets/minecraft/textures/item/potion_overlay.png")
        tinted = [[(p[0] * WATER[0] // 255, p[1] * WATER[1] // 255, p[2] * WATER[2] // 255, p[3]) for p in row] for row in overlay]
        return scale(over(bottle, tinted), GUI)


def scale(image, factor):
    return [[pixel for pixel in row for _ in range(factor)] for row in image for _ in range(factor)]


def over(base, top):
    return [[t if t[3] else b for b, t in zip(brow, trow)] for brow, trow in zip(base, top)]


def shade(pixel, factor):
    return tuple(round(c * factor) for c in pixel[:3]) + (pixel[3],)


def cube(top, side):
    """A block as a slot shows it: three faces of a cube seen from above, the sides a little darker."""
    size = 16 * GUI
    image = [[(0, 0, 0, 0)] * size for _ in range(size)]
    s = size / 32
    faces = [  # origin, edge a, edge b in a 32-pixel box, the texture, the shade
        ((16, 1), (14, 7), (-14, 7), top, 1.0),
        ((2, 8), (14, 7), (0, 16), side, 0.8),
        ((16, 15), (14, -7), (0, 16), side, 0.6),
    ]
    for y in range(size):
        for x in range(size):
            px, py = (x + 0.5) / s, (y + 0.5) / s
            for (ox, oy), (ax, ay), (bx, by), texture, light in faces:
                det = ax * by - ay * bx
                u = ((px - ox) * by - (py - oy) * bx) / det
                v = (ax * (py - oy) - ay * (px - ox)) / det
                if 0 <= u < 1 and 0 <= v < 1:
                    image[y][x] = shade(texture[int(v * 16)][int(u * 16)], light)
                    break
    return image


def own(item_id):
    """Our item as a slot shows it: the 32-pixel cans as they are, 16-pixel textures (taurine) at the game's scale."""
    image = read_png((ASSETS / f"textures/item/{item_id.split(':')[1]}.png").read_bytes())
    return image if len(image) == 16 * GUI else scale(image, GUI)


def stamp(canvas, image, left, top):
    for y, row in enumerate(image):
        for x, pixel in enumerate(row):
            if pixel[3] and 0 <= top + y < len(canvas) and 0 <= left + x < len(canvas[0]):
                canvas[top + y][left + x] = pixel[:3] + (255,)


def write(canvas, font, string, left, top, colour, shadow):
    def draw(x, y, c):
        for char in string:
            glyph = font.glyph(char)
            tinted = [[c + (255,) if p[3] else (0, 0, 0, 0) for p in row] for row in glyph]
            stamp(canvas, scale(tinted, GUI), x, y)
            x += (len(glyph[0]) + 1) * GUI
    if shadow:
        draw(left + GUI, top + GUI, tuple(c // 4 for c in colour))
    draw(left, top, colour)


def width(font, string):
    return sum((len(font.glyph(c)[0]) + 1) * GUI for c in string)


def gif(frames, delay_cs, path):
    """An animated GIF, looping, each frame with its own palette of up to 256 colours."""
    height, wide = len(frames[0]), len(frames[0][0])
    out = bytearray(b"GIF89a" + struct.pack("<HHBBB", wide, height, 0, 0, 0))
    out += b"\x21\xff\x0bNETSCAPE2.0\x03\x01\x00\x00\x00"
    for frame in frames:
        colours = sorted({p[:3] for row in frame for p in row})
        assert len(colours) <= 256, f"{len(colours)} colours in a frame"
        index = {c: i for i, c in enumerate(colours)}
        bits = max(1, (len(colours) - 1).bit_length())
        table = b"".join(bytes(c) for c in colours) + b"\x00" * (3 * ((1 << bits) - len(colours)))
        out += b"\x21\xf9\x04\x04" + struct.pack("<H", delay_cs) + b"\x00\x00"
        out += b"\x2c" + struct.pack("<HHHHB", 0, 0, wide, height, 0x80 | (bits - 1)) + table
        code_size = max(2, bits)
        data = lzw([index[p[:3]] for row in frame for p in row], code_size)
        out += bytes([code_size])
        for i in range(0, len(data), 255):
            chunk = data[i:i + 255]
            out += bytes([len(chunk)]) + chunk
        out += b"\x00"
    out += b"\x3b"
    path.write_bytes(bytes(out))


def lzw(pixels, code_size):
    clear, end = 1 << code_size, (1 << code_size) + 1
    out, buffer, nbits = bytearray(), 0, 0

    def emit(code, width):
        nonlocal buffer, nbits
        buffer |= code << nbits
        nbits += width
        while nbits >= 8:
            out.append(buffer & 255)
            buffer >>= 8
            nbits -= 8

    table = {(i,): i for i in range(clear)}
    width, next_code = code_size + 1, end + 1
    emit(clear, width)
    current = (pixels[0],)
    for pixel in pixels[1:]:
        candidate = current + (pixel,)
        if candidate in table:
            current = candidate
            continue
        emit(table[current], width)
        if next_code < 4096:
            table[candidate] = next_code
            next_code += 1
            if next_code > (1 << width) and width < 12:
                width += 1
        else:
            emit(clear, width)
            table = {(i,): i for i in range(clear)}
            width, next_code = code_size + 1, end + 1
        current = (pixel,)
    emit(table[current], width)
    emit(end, width)
    if nbits:
        out.append(buffer & 255)
    return bytes(out)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--client-jar")
    args = parser.parse_args()
    if not RECIPES.is_dir():
        sys.exit("No generated recipes; run ./gradlew :neoforge:generateData first")
    vanilla = Vanilla(client_jar(args.client_jar))
    font = Font(vanilla.png("assets/minecraft/textures/font/ascii.png"))
    names = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))

    # The crafting table's window down to the grid, then its bottom edge.
    sheet = vanilla.png("assets/minecraft/textures/gui/container/crafting_table.png")
    window = [row[:176] for row in sheet[:80]] + [row[:176] for row in sheet[159:166]]
    window = scale(window, GUI)

    order = ["empty_can"] + [line.split('"')[1] for line in (ROOT / "core/src/main/kotlin/ru/mairwunnx/redbull/core/Cans.kt")
                             .read_text().splitlines() if line.strip().startswith(('Can("', '"')) and ", " in line]
    recipes = []
    for name in order:
        file = RECIPES / f"{name}.json"
        if file.exists():
            recipes.append(json.loads(file.read_text()))

    def picture(ingredient):
        if isinstance(ingredient, dict):
            return vanilla.water_bottle()
        return own(ingredient) if ingredient.startswith("redbull:") else vanilla.item(ingredient)

    frames = []
    for recipe in recipes:
        canvas = [list(row) for row in window]
        for y, row in enumerate(recipe["pattern"]):
            for x, symbol in enumerate(row):
                if symbol != " ":
                    stamp(canvas, picture(recipe["key"][symbol]), (30 + x * 18) * GUI, (17 + y * 18) * GUI)
        result = recipe["result"]["id"]
        stamp(canvas, own(result), 124 * GUI, 35 * GUI)
        count = recipe["result"].get("count", 1)
        if count > 1:
            label = str(count)
            write(canvas, font, label, (124 + 17) * GUI - width(font, label), (35 + 9) * GUI, (255, 255, 255), True)
        title = names[f"item.redbull.{result.split(':')[1]}"]
        write(canvas, font, title, 29 * GUI, 6 * GUI, (0x40, 0x40, 0x40), False)
        frames.append(scale(canvas, ZOOM))

    gif(frames, FRAME_MS // 10, OUT)
    print(f"{OUT}: {len(frames)} recipes, {len(frames[0][0])}×{len(frames[0])}, {OUT.stat().st_size // 1024} KiB")


if __name__ == "__main__":
    main()
