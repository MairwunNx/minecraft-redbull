# Red Bull: notes for AI agents

A Paper 26.3 plugin that brings 26 real Red Bull cans into survival: bull bile from cows brews into crude taurine, a furnace evaporates it into taurine, a crafting table fills a can; cans with wings let the player glide; caffeine builds up, and above 100 the heart gives out. The owner, a Red Bull fan, dedicated it to the drink; the name stays “Red Bull”. README.md is the shop window and, without a wiki yet, the manual; README.RU.md is its Russian copy; MODRINTH.md is the store page. Moonlands, the owner's server (MairwunNx/minecraft), is where it runs.

## Talking to the owner

- Reply in Russian, address the owner as «вы». Quotes «ёлочки», nested „лапки“, an em dash with spaces « — ».
- Code, comments, commit messages, README.md, MODRINTH.md, and `en_us.json` are in English: curly quotes, an em dash without spaces or an en dash with spaces, never a hyphen as a dash.
- README.RU.md and `ru_ru.json` follow Russian typography; `de_de.json` uses „…“, an en dash with spaces, and „du“. Change both READMEs and MODRINTH.md together.
- Can names stay English in every language, as on the real cans; flavours, slogans, and everything else are translated.

## How it fits together

Two modules: `core` holds what every platform shares, `paper` is the Paper plugin.

| File | What it does |
|---|---|
| `core/…/Cans.kt` | Every can: buffs, wings, recipe (flavour and dye), and the biomes and chance of its chests. A can without a recipe is chest-only |
| `core/…/Caffeine.kt` | `Rules`, the caffeine level that wears off with play time, the stages, a drink, milk, and the crash |
| `core/…/Ingredients.kt` | Bull bile's drop chance, the bottles' effects, bull mode, and how long a winged can lasts |
| `core/src/main/resources/assets/redbull/` | The assets every platform uses: textures, item models and definitions, and the five languages |
| `scripts/textures.py` | Draws every can from one 13×32 template (a new flavour is one line in `CANS`), the empty and winged cans, and an item model for every texture. Standard library only: `python3 -I scripts/textures.py` |
| `paper/…/Items.kt` | Builds each item: a potion underneath, with its model, name, lore, and effects; the `redbull:item` mark says which one |
| `paper/…/Recipes.kt` | The crafting recipes, the brewing mix, and the furnace recipe; removed again on disable |
| `paper/…/Drinks.kt` | Caffeine on a drink, the crash, the overdose, wings, milk, the bottles, and the death message |
| `paper/…/Wings.kt` | Turns spent winged cans back into empty ones |
| `paper/…/Heart.kt` | Jitters and crash every two seconds: hunger, heartbeats, warnings |
| `paper/…/Loot.kt` | Bull bile from cows, cans in chests by biome |
| `paper/…/Pack.kt` | Sends the resource pack on join |
| `paper/…/RedBullCommand.kt` | `/redbull give` and `/redbull caffeine`, for `redbull.admin` |
| `paper/src/pack/` | `pack.mcmeta` and the pack icon; the `pack` Gradle task zips them with core's assets |

## Rules that keep it working

- Every item is `minecraft:potion` underneath. The game drinks it, lists its effects in the tooltip, applies them, and no vanilla crafting, brewing, or trade takes a potion without a potion id. Non-drinkable items unset `consumable`.
- A potion names itself after its contents and ignores `item_name`, so names go in `custom_name`, not italic.
- Every text players read is a translation key with the English text as its fallback (`Texts`). A new key goes into all five language files; `AssetsTest` fails otherwise.
- A new can is a line in `Cans.kt`, a line in `scripts/textures.py`, a name, flavour, and slogan in every language, and a row in both READMEs and MODRINTH.md. `CansTest` keeps recipes unique and every can obtainable; `AssetsTest` checks its texture and model.
- The winged can glides because the game lets any item with `glider` and `equippable` for a slot glide from that slot, the main hand included (`LivingEntity.canGlide` walks every slot). The game wears it by a point per second of flight and stops a point before it breaks; that's the flight time.
- The jar carries the same pack zip the release attaches, and `Pack` hashes the copy in the jar. The release URL comes from the version, so dev builds send no pack unless `resource_pack.url` is set.
- Overdose kills with `setHealth(0)`, which dies without a totem check; the death message is set in `PlayerDeathEvent`.
- Caffeine lives in the player's persistent data and wears off by play time (`PLAY_ONE_MINUTE` counts ticks), so it waits while they're offline.

## Adding NeoForge or Fabric

The build is ready for them. Add a module next to `paper` (`include("neoforge")` in `settings.gradle.kts`), depend on `project(":core")`, and take the assets from core's resources. Give it Minotaur's `modrinth` block with its loader: `./gradlew modrinth` in the release runs every module's. Add its jar to “Publish the GitHub release” in `release.yml`, its version keys to “Versions”, and a smoke check to “Checking a change”.

## Building and testing

- `./gradlew build` runs the tests and builds `paper/build/libs/redbull-<version>.jar`, shaded and relocated under `ru.mairwunnx.redbull.libs`, and `paper/build/pack/redbull-pack.zip`. JDK 25; Gradle provisions it through the foojay resolver.
- There is no MockBukkit for Paper 26.3; tests cover `core`. The rest runs on a real Paper.

## Checking a change

1. `./gradlew build`: the tests and the jar.
2. A real Paper with the jar. On start the plugin builds every item through the game's codecs, so a component the game rejects fails here:
   ```sh
   python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin paper/build/libs/redbull-dev.jar --expect "Red Bull dev is on: 26 cans, 23 recipes"
   ```
3. Drinking, flying, and dying need a game client: after a release reaches Moonlands, the owner tries them.

## Autopilot

`.github/workflows/autopilot.yml` runs Claude every Monday at 03:00 UTC: `/update-deps`, then `/update-minecraft`, a fix for whatever they break, a push to `master`, and the “Release” workflow with the stores “Stores” allows when what the project ships changed. The release's `deploy` job then starts Moonlands' `ship-plugin.yml` with the `FAMILY_ACTIONS_TOKEN` secret. The run needs the `CLAUDE_CODE_OAUTH_TOKEN` secret. “Autopilot” in `~/c/personal/minecraft/CLAUDE.md` explains the family's runs. A session with the owner releases only when the owner asks.

## Versions

Where the versions live, for `/update-deps` and `/update-minecraft` (both in `.claude/skills/`, the same files in every project of the family):

- The game: `io.papermc.paper:paper-api` (twice in `paper/build.gradle.kts`), `gameVersions` (Modrinth) and `platformVersions` (Hangar) there, `api-version` in `paper/src/main/resources/paper-plugin.yml`, and `min_format`/`max_format` in `paper/src/pack/pack.mcmeta` (the game's `resource_major` in its `version.json`).
- Java: `jvmToolchain` in both modules, the foojay resolver in `settings.gradle.kts`, and CI's `setup-java`.
- Gradle in `gradle/wrapper/gradle-wrapper.properties`; Kotlin and the Gradle plugins in the root `build.gradle.kts`; libraries in each module's `dependencies`.

## Releasing and Moonlands

1. Keep a `## X.Y.Z` section in CHANGELOG.md, written for server owners. After a change that reaches what the project ships, add it in the same push: under the topmost section while that version isn't released (`gh release view vX.Y.Z` fails), or under a new section with the next version.
2. Run the “Release” workflow: `gh workflow run release.yml -R MairwunNx/minecraft-redbull -f version=X.Y.Z`, with the store switches “Stores” allows. It builds with `-PreleaseVersion`, tags `vX.Y.Z`, and attaches `redbull-X.Y.Z.jar`, `redbull.jar`, and `redbull-pack-X.Y.Z.zip`. Nothing in the repo holds the version; local and CI builds are `dev`.
3. Its `deploy` job ships the jar to Moonlands through `ship-plugin.yml`, which points the Red Bull line in `minecraft/extras/jars.txt` at it.

## Stores

Not yet: the owner tests each version on Moonlands first and says when a store gets it. Until then every release, the autopilot's too, runs with `modrinth` and `hangar` off. When the owner says so, create the Modrinth project `redbull` through the API and the Hangar project `MairwunNx/RedBull` (the first Hangar version goes through the website), as Moonlands' CLAUDE.md describes, add their badges to the READMEs, and change this section to say which switches to turn on. The tokens are the `MODRINTH_TOKEN` and `HANGAR_TOKEN` secrets.

## Commits

Short one-line imperative English messages, one logical change per commit, each one building: “Let the winged can glide from the main hand”.
