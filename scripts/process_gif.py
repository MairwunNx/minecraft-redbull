"""Draw media/process.gif: the whole way to a can, one window of the game after another.

Standard library only: ./gradlew :neoforge:generateData, then python3 -I scripts/process_gif.py [--client-jar <jar>]
A cow gives bull bile, a brewing stand turns it into crude taurine, a furnace into taurine, and a crafting table
makes an empty can and fills it. Recipes come from the generated data and windows from a client jar, as in
crafting_gif.py.
"""

import argparse
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from crafting_gif import (  # noqa: E402
    GUI, RECIPES, ROOT, ZOOM, Vanilla, crafting, gif, own, scale, stamp, width, window, write,
)
from social_preview import Font, client_jar  # noqa: E402

OUT = ROOT / "media/process.gif"
TITLE = (0x40, 0x40, 0x40)
SPRITES = "assets/minecraft/textures/gui/sprites/container"


def colour(name):
    """A bottle's tint, from core's Colors, so the GIF matches every platform."""
    source = (ROOT / "core/src/main/kotlin/ru/mairwunnx/redbull/core/Ingredients.kt").read_text()
    value = int(re.search(rf"const val {name} = 0x([0-9A-Fa-f]{{6}})", source).group(1), 16)
    return (value >> 16, (value >> 8) & 255, value & 255)


def bottle(vanilla, tint):
    base = vanilla.png("assets/minecraft/textures/item/potion.png")
    overlay = vanilla.png("assets/minecraft/textures/item/potion_overlay.png")
    tinted = [[(p[0] * tint[0] // 255, p[1] * tint[1] // 255, p[2] * tint[2] // 255, p[3]) for p in row] for row in overlay]
    return scale([[t if t[3] else b for b, t in zip(brow, trow)] for brow, trow in zip(base, tinted)], GUI)


def sprite(vanilla, name, keep_width=1.0, keep_height=1.0, from_bottom=False):
    """A progress sprite cut as the game cuts it: from the left, the top, or for flames from the bottom."""
    image = vanilla.png(f"{SPRITES}/{name}.png")
    rows = len(image)
    keep_rows = round(rows * keep_height)
    columns = round(len(image[0]) * keep_width)
    cut = [row[:columns] + [(0, 0, 0, 0)] * (len(row) - columns) for row in image]
    if from_bottom:
        cut = [[(0, 0, 0, 0)] * len(row) for row in cut[:rows - keep_rows]] + cut[rows - keep_rows:]
    else:
        cut = cut[:keep_rows] + [[(0, 0, 0, 0)] * len(row) for row in cut[keep_rows:]]
    return scale(cut, GUI)


def place(canvas, image, x, y):
    stamp(canvas, image, x * GUI, y * GUI)


def titled(base, font, title, x=8):
    canvas = [list(row) for row in base]
    write(canvas, font, title, x * GUI, 6 * GUI, TITLE, False)
    return canvas


def cow_step(vanilla, font, table):
    """A blank window with two slots: a cow becomes bull bile."""
    canvas = [list(row) for row in table]
    panel = canvas[10 * GUI][10 * GUI]
    for y in range(14 * GUI, 78 * GUI):
        for x in range(7 * GUI, 169 * GUI):
            canvas[y][x] = panel
    slot = [row[29 * GUI:47 * GUI] for row in table[16 * GUI:34 * GUI]]
    arrow = [row[89 * GUI:113 * GUI] for row in table[34 * GUI:51 * GUI]]
    stamp(canvas, slot, 47 * GUI, 34 * GUI)
    stamp(canvas, arrow, 76 * GUI, 34 * GUI)
    stamp(canvas, slot, 113 * GUI, 34 * GUI)
    place(canvas, scale(vanilla.png("assets/minecraft/textures/item/cow_spawn_egg.png"), GUI), 48, 35)
    place(canvas, bottle(vanilla, colour("BILE")), 114, 35)
    note = "5%, +2% per Looting level"
    write(canvas, font, note, (176 * GUI - width(font, note)) // 2, 60 * GUI, TITLE, False)
    return titled(canvas, font, "1. Kill cows for bull bile")


def brewing(vanilla, font, done):
    canvas = titled(window(vanilla, "brewing_stand"), font, "2. Brew bile into water bottles")
    place(canvas, scale(vanilla.png("assets/minecraft/textures/item/blaze_powder.png"), GUI), 17, 17)
    place(canvas, sprite(vanilla, "brewing_stand/fuel_length", keep_width=0.75), 60, 44)
    contents = bottle(vanilla, colour("CRUDE_TAURINE")) if done else vanilla.water_bottle()
    for x, y in ((56, 51), (79, 58), (102, 51)):
        place(canvas, contents, x, y)
    if not done:
        place(canvas, bottle(vanilla, colour("BILE")), 79, 17)
        place(canvas, sprite(vanilla, "brewing_stand/brew_progress", keep_height=0.6), 97, 16)
        place(canvas, sprite(vanilla, "brewing_stand/bubbles", keep_height=0.7, from_bottom=True), 63, 14)
    return canvas


def smelting(vanilla, font, done):
    canvas = titled(window(vanilla, "furnace"), font, "3. Smelt it into taurine")
    place(canvas, scale(vanilla.png("assets/minecraft/textures/item/coal.png"), GUI), 56, 53)
    place(canvas, sprite(vanilla, "furnace/lit_progress", keep_height=0.8, from_bottom=True), 56, 36)
    if done:
        place(canvas, own("redbull:taurine"), 116, 35)
    else:
        place(canvas, bottle(vanilla, colour("CRUDE_TAURINE")), 56, 17)
        place(canvas, sprite(vanilla, "furnace/burn_progress", keep_width=0.55), 79, 34)
    return canvas


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--client-jar")
    args = parser.parse_args()
    if not RECIPES.is_dir():
        sys.exit("No generated recipes; run ./gradlew :neoforge:generateData first")
    vanilla = Vanilla(client_jar(args.client_jar))
    font = Font(vanilla.png("assets/minecraft/textures/font/ascii.png"))
    table = window(vanilla, "crafting_table")

    def recipe(name):
        return json.loads((RECIPES / f"{name}.json").read_text())

    steps = [  # the window, and how long it shows in hundredths of a second
        (cow_step(vanilla, font, table), 220),
        (brewing(vanilla, font, done=False), 150),
        (brewing(vanilla, font, done=True), 150),
        (smelting(vanilla, font, done=False), 150),
        (smelting(vanilla, font, done=True), 150),
        (crafting(vanilla, font, table, recipe("empty_can"), "4. Make an empty can"), 200),
        (crafting(vanilla, font, table, recipe("original"), "5. Fill a can"), 300),
    ]
    gif([scale(canvas, ZOOM) for canvas, _ in steps], [delay for _, delay in steps], OUT)
    print(f"{OUT}: {len(steps)} steps, {OUT.stat().st_size // 1024} KiB")


if __name__ == "__main__":
    main()
