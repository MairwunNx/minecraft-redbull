# Red Bull: notes for AI agents

A Paper 26.3 plugin, and NeoForge and Fabric 26.3 mods, that bring 26 real Red Bull cans into survival: bull bile from cows brews into crude taurine, a furnace evaporates it into taurine, a crafting table fills a can; cans with wings let the player glide; caffeine builds up, and above 100 the heart gives out. The owner, a Red Bull fan, dedicated it to the drink; the name stays “Red Bull”. README.md is the shop window and, without a wiki yet, the manual; README.RU.md is its Russian copy; MODRINTH.md is the store page. Moonlands, the owner's server (MairwunNx/minecraft), is where it runs.

## Talking to the owner

- Reply in Russian, address the owner as «вы». Quotes «ёлочки», nested „лапки“, an em dash with spaces « — ».
- Code, comments, commit messages, README.md, MODRINTH.md, and `en_us.json` are in English: curly quotes, an em dash without spaces or an en dash with spaces, never a hyphen as a dash.
- README.RU.md and `ru_ru.json` follow Russian typography; `de_de.json` uses „…“, an en dash with spaces, and „du“. Change both READMEs and MODRINTH.md together.
- Can names stay English in every language, as on the real cans; flavours, slogans, and everything else are translated.

## How it fits together

Four modules: `core` holds what every platform shares, `paper` is the Paper plugin, `neoforge` and `fabric` are the mods.

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
| `paper/…/Wings.kt` | Crumples spent winged cans; looks only at players it was told hold one |
| `paper/…/Heart.kt` | Jitters and crash every two seconds: hunger, heartbeats, warnings |
| `paper/…/Loot.kt` | Bull bile from cows, cans in chests by biome |
| `paper/…/Pack.kt` | Sends the resource pack on join |
| `paper/…/Collected.kt` | The cans each player has ever held, in their persistent data, recorded on pickup, craft, drink, inventory close, join, and `RedBullPlugin.handed` |
| `paper/…/Menu.kt` | The `/redbull` chest: a player's collection with question marks, or every item to take for admins |
| `paper/…/RedBullCommand.kt` | `/redbull` for everyone; `/redbull give` and `/redbull caffeine` for `redbull.admin` |
| `paper/src/pack/` | `pack.mcmeta` and the pack icon; the `pack` Gradle task zips them with core's assets |
| `neoforge/…/RedBullMod.kt` | The `@Mod` entry: registers everything, hands recipes to the recipe book on join, logs “Red Bull is on” |
| `neoforge/…/Items.kt` | Every item as a real item under its asset's name (`redbull:original`, `redbull:crumpled_original`, …), with components, and the `redbull:redbull` creative tab |
| `neoforge/…/Caffeines.kt` | Caffeine in a data attachment, saved, synced to the player's own client, dropped on death |
| `neoforge/…/Drinks.kt`, `Heart.kt` | The same as Paper's, on NeoForge's events; `Later.kt` runs the crash and the overdose ticks later |
| `neoforge/…/Wings.kt` | The `redbull:wings` effect a can with wings gives instead of a winged can, and Slow Falling when it ends mid-glide |
| `neoforge/…/Loot.kt` | Two global loot modifiers, `redbull:bull_bile` and `redbull:cans`, switched on in `data/redbull/loot_modifiers/` |
| `neoforge/…/Collected.kt`, `Menu.kt`, `RedBullCommand.kt` | The collection in an attachment kept through death, the `/redbull` chest, and the command; admin is operator level 2 |
| `neoforge/…/Settings.kt` | `config/redbull.toml`: Paper's `config.yml` without the pack |
| `neoforge/…/client/CaffeineHud.kt` | The caffeine meter: ten small cans above the hunger bar |
| `neoforge/src/data/` | Writes every recipe and the `crumpled_cans` tag from `Cans.kt` into `build/generated/data` on each build |
| `neoforge/src/main/resources/` | `neoforge.mods.toml`, the overdose damage type and its tags, the loot modifiers, and the tinted bottles' item definitions |
| `neoforge/src/gametest/` | A dev-only mod with game tests: drinking, the overdose, milk, the bottles, wings, landing, bile, and the creative tab |
| `fabric/…/RedBullItems.kt` | Registers every item under its asset's id (`redbull:original`, `redbull:crumpled_original`, `redbull:empty_can`), with its components as defaults, and the creative tab |
| `fabric/…/ItemTypes.kt` | `CanItem` adds caffeine and wings to a drink, `BottleItem` the bottles' moo and bull mode, `WingedCanItem` crumples a spent winged can from `inventoryTick` |
| `fabric/…/Drinks.kt` | Caffeine on a drink, the crash, the overdose, wings, milk, and the bottles; a tick queue for what comes later |
| `fabric/…/Caffeines.kt` | Caffeine in a data attachment: saved with the player, synced to them alone, dropped on death |
| `fabric/…/Heart.kt` | Jitters and crash every two seconds, as on Paper, and the level written back for the HUD |
| `fabric/…/Loot.kt` | Bull bile and chest cans as loot pools in the game's JSON, built from core and read through `LootPool.CODEC` |
| `fabric/…/mixin/MilkMixin.java` | Milk takes caffeine away; Fabric has no event for a finished drink |
| `fabric/src/client/…/RedBullClient.kt` | The caffeine meter beside the hotbar |
| `fabric/src/generator/…/Recipes.kt` | Writes every recipe and the `crumpled_cans` tag as data from core; the `generateData` task runs it before each build |
| `fabric/src/main/resources/` | `fabric.mod.json`, the bottles' item definitions (the game's potion model, tinted), and the `overdose` damage type with its tags |
| `fabric/src/gametest/` | Game tests on a real server, and a client test that screenshots the HUD |

## Rules that keep it working

- Every item is `minecraft:potion` underneath. The game drinks it, lists its effects in the tooltip, applies them, and no vanilla crafting, brewing, or trade takes a potion without a potion id. Non-drinkable items unset `consumable`.
- A potion names itself after its contents and ignores `item_name`, so names go in `custom_name`, not italic.
- Every text players read is a translation key with the English text as its fallback (`Texts`). A new key goes into all five language files; `AssetsTest` fails otherwise.
- A new can is a line in `Cans.kt`, a line in `scripts/textures.py`, a name, flavour, and slogan in every language, and a row in both READMEs and MODRINTH.md. `CansTest` keeps recipes unique and every can obtainable; `AssetsTest` checks its texture and model.
- The winged can glides because the game lets any item with `glider` and `equippable` for a slot glide from that slot, the main hand included (`LivingEntity.canGlide` walks every slot). The game wears it by a point per second of flight and stops a point before it breaks; that's the flight time.
- The jar carries the same pack zip the release attaches, and `Pack` hashes the copy in the jar. The release URL comes from the version, so dev builds send no pack unless `resource_pack.url` is set.
- Nothing loops over every online player but `Heart`, which reads two numbers each: the collection and the winged cans run on events, so hundreds of players cost little.
- `HEALING` (16 s) caps anything that restores health, and `STRENGTH_II` (3 min) caps Strength II; `CansTest` holds every can to them.
- A drink leaves a crumpled can of its flavour (`crumpled_<id>`, drawn by `scripts/textures.py`); a winged can keeps its flavour to crumple into.
- Overdose kills with `setHealth(0)`, which dies without a totem check; the death message is set in `PlayerDeathEvent`. On Fabric it's the `redbull:overdose` damage type: `bypasses_invulnerability` skips the totem, and its `message_id` names the death `death.attack.redbull.overdose`.
- Caffeine lives in the player's persistent data and wears off by play time (`PLAY_ONE_MINUTE` counts ticks), so it waits while they're offline. On Fabric it's the `redbull:caffeine` attachment, on the same clock.
- On Fabric every item is its own item, not a potion. Cans and bottles carry `potion_contents` for their effects and tooltip; a can's `use_remainder` is its crumpled can, and a drink with wings returns the winged can instead, which leaves no crumpled one. The game applies the effects and food; `CanItem` and `BottleItem` add the rest.
- Fabric recipes are data. Nothing writes them by hand: change `Cans.kt` or `Recipes.kt` in `fabric/src/generator`. A water bottle in a recipe is Fabric's `fabric:components` ingredient; brewing is the game's own `minecraft:brewing` recipe type since 26.3.

## NeoForge

`neoforge` is a mod built with ModDevGradle, in Kotlin with the plain `javafml` loader: the jar carries core's classes and assets and nests `kotlin-stdlib` through jar-in-jar, so players need no Kotlin mod. Where it differs from Paper:

- Items are real items, so no resource pack: the jar carries core's assets, plus `es_mx` and `pt_pt` copies of the Spanish and Portuguese texts. A can is drunk through its `consumable` and `potion_contents` components and leaves its crumpled can through `use_remainder`. Bull bile and crude taurine draw the game's potion model, tinted by their `potion_contents` colour.
- Recipes are data. Since 26.x brewing is a recipe type too (`minecraft:brewing`), so bile into a water bottle is one more JSON file. The water bottle in a can's recipe is a `neoforge:components` ingredient. `Recipes.kt` in `src/data` writes them all from `Cans.kt`; don't write recipe JSON by hand.
- Wings are the `redbull:wings` effect, not a winged can, for as many seconds as the can has wings. The effect adds NeoForge's `gliding_flight` attribute, which `LivingEntity.canGlide` checks on the server and on the client, so the player glides as with an elytra and no item wears. Wings that end mid-glide leave 5 seconds of Slow Falling, a tick later: the game is still walking the effects when one ends.
- Overdose deals the `redbull:overdose` damage type, tagged `bypasses_invulnerability` and the like, so no totem saves the player and the game writes `death.attack.redbull.overdose` itself.
- Chest cans go to any table under `chests/`, by the biome at the loot's origin. `/loot` takes the command's position as the origin, so test a biome with `execute positioned`.
- The dev run's version is `0.0.0-dev`, since FML rejects `dev`.

## Fabric

The Fabric mod has neither `config.yml` nor `/redbull` yet: the overdose, wings, and loot can't be switched off, and there is no collection screen or admin command. `/give` and `/item` hand out any item by its id.

A new loader joins the same way: a module next to `paper` that takes core's classes and assets, Minotaur's `modrinth` block with its loader (`./gradlew modrinth` in the release runs every module's), its jar in “Publish the GitHub release” in `release.yml` and in `build.yml`'s artifacts, its version keys in “Versions”, and a check in “Checking a change”.

## Building and testing

- `./gradlew build` runs the tests and builds `paper/build/libs/redbull-<version>.jar`, shaded and relocated under `ru.mairwunnx.redbull.libs`, `paper/build/pack/redbull-pack.zip`, and `neoforge/build/libs/redbull-neoforge-<version>.jar`. JDK 25; Gradle provisions it through the foojay resolver.
- It also builds `fabric/build/libs/redbull-fabric-<version>.jar`, with core's classes and assets inside, and runs the Fabric game tests on a real server (`./gradlew :fabric:runGameTest`): drinking, wings, milk, the overdose through a totem, bull mode, every crafting recipe, brewing, both furnaces, bile from cows, and chest loot by biome.
- There is no MockBukkit for Paper 26.3; tests cover `core`. The rest runs on a real Paper. On NeoForge and Fabric, game tests drive a real server with a fake survival player.

## Checking a change

1. `./gradlew build`: the tests and the jar.
2. A real Paper with the jar. On start the plugin builds every item through the game's codecs, so a component the game rejects fails here:
   ```sh
   python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin paper/build/libs/redbull-dev.jar --expect "Red Bull dev is on: 26 cans, 23 recipes"
   ```
3. NeoForge's game tests: drinking, caffeine, the overdose and its death message, milk, bull mode, gliding on the Wings effect and only with it, Slow Falling when it ends mid-glide, no winged can, bile from a cow, and the creative tab, on a real server. It ends with “All 7 required tests passed”:
   ```sh
   ./gradlew :neoforge:runGameTestServer
   ```
   `./gradlew :neoforge:runServer` starts a dev server in `neoforge/run/`; it logs “Red Bull is on: 26 cans, 56 items, 27 recipes” once every recipe parsed.
4. A real Fabric server with the mod and its dependencies from Modrinth:
   ```sh
   python3 -I .claude/scripts/fabric_smoke.py --game 26.3 --mod fabric/build/libs/redbull-fabric-dev.jar --modrinth fabric-api --modrinth fabric-language-kotlin --expect "Red Bull dev is on: 26 cans, 23 recipes"
   ```
5. The Fabric HUD on a real client: `./gradlew :fabric:runClientGameTest` opens a game window, fills the hotbar, and saves `fabric/build/run/clientGameTest/screenshots/0000_redbull-hud.png`.
6. Gliding everywhere, and NeoForge's meter and tooltips, need a player: `./gradlew :neoforge:runClient` for NeoForge; for Paper, the owner tries them after a release reaches Moonlands.

## Autopilot

`.github/workflows/autopilot.yml` runs Claude every Monday at 03:00 UTC: `/update-deps`, then `/update-minecraft`, a fix for whatever they break, a push to `master`, and the “Release” workflow with the stores “Stores” allows when what the project ships changed. The release's `deploy` job then starts Moonlands' `ship-plugin.yml` with the `FAMILY_ACTIONS_TOKEN` secret. The run needs the `CLAUDE_CODE_OAUTH_TOKEN` secret. “Autopilot” in `~/c/personal/minecraft/CLAUDE.md` explains the family's runs. A session with the owner releases only when the owner asks.

## Versions

Where the versions live, for `/update-deps` and `/update-minecraft` (both in `.claude/skills/`, the same files in every project of the family):

- The game: `io.papermc.paper:paper-api` (twice in `paper/build.gradle.kts`), `gameVersions` (Modrinth) and `platformVersions` (Hangar) there, `api-version` in `paper/src/main/resources/paper-plugin.yml`, and `min_format`/`max_format` in `paper/src/pack/pack.mcmeta` (the game's `resource_major` in its `version.json`).
- NeoForge: `minecraft` and `neoforgeVersion` at the top of `neoforge/build.gradle.kts` (they also fill `neoforge.mods.toml` and Modrinth's `gameVersions`); ModDevGradle (`net.neoforged.moddev`) in the root `build.gradle.kts`. The versions list: `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge`.
- Fabric: `minecraft`, `loader`, `fabricApi`, and `fabricKotlin` at the top of `fabric/build.gradle.kts` (the game version also goes into `fabric.mod.json` and Modrinth's `gameVersions` from there); Loom in the root `build.gradle.kts`. `fabric_smoke.py` takes the newest stable loader.
- Java: `jvmToolchain` in every module, the foojay resolver in `settings.gradle.kts`, and CI's `setup-java`.
- Gradle in `gradle/wrapper/gradle-wrapper.properties`; Kotlin and the Gradle plugins in the root `build.gradle.kts`; libraries in each module's `dependencies`.

## Releasing and Moonlands

1. Keep a `## X.Y.Z` section in CHANGELOG.md, written for server owners. After a change that reaches what the project ships, add it in the same push: under the topmost section while that version isn't released (`gh release view vX.Y.Z` fails), or under a new section with the next version.
2. Run the “Release” workflow: `gh workflow run release.yml -R MairwunNx/minecraft-redbull -f version=X.Y.Z -f modrinth=true -f hangar=true`. It builds with `-PreleaseVersion`, tags `vX.Y.Z`, and attaches `redbull-X.Y.Z.jar`, `redbull.jar`, `redbull-pack-X.Y.Z.zip`, and the Fabric mod `redbull-fabric-X.Y.Z.jar`. Nothing in the repo holds the version; local and CI builds are `dev`.
3. Its `deploy` job ships the jar to Moonlands through `ship-plugin.yml`, which points the Red Bull line in `minecraft/extras/jars.txt` at it.

## Stores

Both are on since 1.1.0 (2026-10-21), at the owner's word: run every release, the autopilot's too, with `-f modrinth=true -f hangar=true`, then check the version on each store.

- **Modrinth:** `minecraft-redbull` (id `X5nYFSkK`; `redbull` and `red-bull` belong to other, hidden projects), MIT. `./gradlew modrinth` uploads the Paper jar as `X.Y.Z`, the NeoForge jar as `X.Y.Z+neoforge`, and the Fabric jar (which requires Fabric API and Fabric Language Kotlin), and syncs MODRINTH.md as the description. Submitted for review with 1.1.0.
- **Hangar:** `MairwunNx/RedBull`, MIT, created 2026-10-21 through `POST /api/internal/projects/create`. `./gradlew publishPluginPublicationToHangar` uploads each version; it took the first one, 1.1.0, too, which older Hangar projects of the family had to upload on the website. The page came from MODRINTH.md; Hangar doesn't sync it, so edit it and the icon on the site.

The tokens are the `MODRINTH_TOKEN` and `HANGAR_TOKEN` secrets, also in `~/.secrets/cloud.env`. Never put them in the repo.

## Commits

Short one-line imperative English messages, one logical change per commit, each one building: “Let the winged can glide from the main hand”.
