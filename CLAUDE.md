# Red Bull: notes for AI agents

A Paper 26.3 plugin, and NeoForge and Fabric 26.3 mods, that bring 26 real Red Bull cans into survival: bull bile from cows brews into crude taurine, a furnace evaporates it into taurine, a crafting table fills a can; cans with wings let the player glide; caffeine builds up, and above 100 the heart gives out. The owner, a Red Bull fan, dedicated it to the drink; the name stays “Red Bull”. README.md is the shop window and, without a wiki yet, the manual; README.RU.md is its Russian copy; MODRINTH.md is the store page. Moonlands, the owner's server (MairwunNx/minecraft), is where it runs.

## Talking to the owner

- Reply in Russian, address the owner as «вы». Quotes «ёлочки», nested „лапки“, an em dash with spaces « — ».
- Code, comments, commit messages, README.md, MODRINTH.md, and `en_us.json` are in English: curly quotes, an en dash with spaces as the dash (never an em dash glued to the words, never a hyphen).
- README.RU.md and `ru_ru.json` follow Russian typography; `de_de.json` uses „…“, an en dash with spaces, and „du“. Change both READMEs and MODRINTH.md together.
- Can names stay English in every language, as on the real cans; flavours, slogans, and everything else are translated.

## How it fits together

Five modules: `core` holds what every platform shares, `paper` is the Paper plugin, `modcore` is what both mods share (the game without a loader), and `neoforge` and `fabric` are the mods, thin adapters around it.

| File | What it does |
|---|---|
| `core/…/Cans.kt` | Every can: buffs, wings, recipe (flavour and dye), and the biomes and chance of its chests. A can without a recipe is chest-only |
| `core/…/Caffeine.kt` | `Rules`, the caffeine level that wears off with play time, the stages, a drink, milk, and the crash |
| `core/…/RedBullExpansion.kt` | Red Bull for Allay (`chat.allay:allay-expansion`, `compileOnly`): the `x_redbull_recipes` tool (the ingredients' way, the grid, each can's recipe or chests, from `Cans.kt`, and the items' names and flavours in the language Allay answers in, from the language files) and a few prompt lines; `RedBullInfo.version` is set by each platform on start. Allay finds it through `META-INF/services/chat.allay.expansion.AllayExpansion` in core's resources, on every platform; without Allay nothing loads it |
| `core/…/Ingredients.kt` | Bull bile's drop chance, the bottles' effects, bull mode, how long a winged can lasts, and the colours of the bottles and both effects (`Colors`) |
| `core/src/main/resources/assets/redbull/` | The assets every platform uses: textures, item models and definitions (the mods' bottles tint the game's potion model), and the five languages |
| `scripts/textures.py` | Draws every can from one 13×32 template (a new flavour is one line in `CANS`), the empty and winged cans, and an item model for every texture. Standard library only: `python3 -I scripts/textures.py` |
| `scripts/crafting_gif.py` | Draws `media/crafting.gif`, every crafting recipe at a crafting table, 1.5 seconds each, from the recipes NeoForge's build generates and a client jar's window, items, and font. Standard library only: `./gradlew :neoforge:generateData`, then `python3 -I scripts/crafting_gif.py`; run it again when a recipe changes |
| `scripts/process_gif.py` | Draws `media/process.gif`, the whole way to a can window by window: a cow's bile, the brewing stand, the furnace, the empty can, the filled can. Same sources and command as `crafting_gif.py` |
| `scripts/modrinth_mods.py` | Uploads the Fabric and NeoForge jars to Modrinth through its v3 API, each marked to run on the client and the server; the release runs it after Paper's Minotaur upload |
| `scripts/social_preview.py` | Draws `media/social-preview.png`, GitHub's 1280×640 preview and the READMEs' header, with the game's font read from a client jar in Gradle's caches. Standard library only: `python3 -I scripts/social_preview.py` |
| `paper/…/Items.kt` | Builds each item: a potion underneath (bull bile an ominous bottle), with its model, name, lore, and effects; the `redbull:item` mark says which one |
| `paper/…/Upgrades.kt` | Swaps items of older versions for today's where a player meets them: 1.4.1's potion bile on join, in an opened container, and on pickup |
| `paper/…/Recipes.kt` | The crafting recipes, the brewing mix, and the furnace recipe; removed again on disable |
| `paper/…/Drinks.kt` | Caffeine on a drink, the crash, the overdose, wings, milk, the bottles, and the death message |
| `paper/…/Wings.kt` | Crumples spent winged cans; looks only at players it was told hold one |
| `paper/…/Heart.kt` | Jitters and crash every two seconds: hunger, heartbeats, warnings |
| `paper/…/Loot.kt` | Bull bile from cows, cans in chests by biome |
| `paper/…/Wanderers.kt` | A wandering trader's cans and cans among fishing junk, by core's `Trades` and `Fishing` |
| `paper/…/Awards.kt`, `RedBullBootstrap.kt` | The six advancements: the bootstrapper registers the data pack inside the jar (`datapack/`, core's `data/` plus `paper/src/datapack/pack.mcmeta`), and `Awards` grants them |
| `paper/…/Pack.kt` | Sends the resource pack on join |
| `paper/…/Collected.kt` | The cans each player has ever held, in their persistent data, recorded on pickup, craft, drink, inventory close, join, and `RedBullPlugin.handed` |
| `paper/…/Menu.kt` | The `/redbull` chest: a player's collection with question marks, or every item to take for admins |
| `paper/…/RedBullCommand.kt` | `/redbull` for everyone; `/redbull give` and `/redbull caffeine` for `redbull.admin` |
| `paper/src/check/` | `redbull-check.jar`, a plugin for test servers only (Java, so it needs no Kotlin of its own): on start it walks the way to a can in blocks, bile through a hopper into a brewing stand, the brew, a furnace, and the crafting grid, and logs “Red Bull check passed”. `/redbullcheck <player> <x> <y> <z>` opens a block's own window for a test client |
| `paper/src/pack/` | `pack.mcmeta` and the pack icon; the `pack` Gradle task zips them with core's assets |
| `modcore/…/Mod.kt`, `Loader.kt` | `Mod` is what each loader's entry calls: `init` first, then commands, the tick, joins, quits, deaths, start and stop. `Loader` is what a loader hands in: item and effect registration, the Wings effect's glide (`glider`), the caffeine and collection attachments (`Store`), and `Settings` |
| `modcore/…/RedBullItems.kt` | Every item as a real item under its asset's name (`redbull:original`, `redbull:crumpled_original`, `redbull:empty_can`), with its components as defaults, and the `redbull:redbull` tab's title, icon, and items. `CanItem` adds caffeine, wings, and the collection to a drink and the collection to a craft; `BottleItem` the bottles' moo and bull mode |
| `modcore/…/Caffeines.kt`, `Heart.kt` | Caffeine in the loader's attachment, saved with the player and dropped on death; the `redbull:caffeine` effect that shows it, and the bed it refuses. Jitters and crash every two seconds, as on Paper, and the Caffeine effect put back in line |
| `modcore/…/Drinks.kt`, `Later.kt` | Caffeine on a drink, the crash, the overdose, wings, milk, and the bottles; `Later` runs what comes ticks later, at the end of a server tick |
| `modcore/…/Wings.kt` | The `redbull:wings` effect: its feathers behind a glide, the first glide's advancement, and `redbull.wings.gone` and Slow Falling when it runs out or is taken mid-glide. How a player glides on it is each loader's |
| `modcore/…/Collected.kt`, `Awards.kt`, `Menu.kt`, `RedBullCommand.kt`, `Trader.kt`, `Junk.kt` | The collection, core's advancements, the `/redbull` chest, the command (admin is operator level 2), a wandering trader's cans, and the cans among fishing junk |
| `modcore/…/mixin/` | What neither loader has an event for: `GlideMixin` hands every tick of a glide to `Wings`, `WanderingTraderMixin` adds the trader's cans (`redbull.mixins.json`) |
| `modcore/src/data/` | Writes every recipe and the `crumpled_cans` tag from `Cans.kt`, with each loader's water-bottle ingredient; each mod's `generateData` runs it into its own `build/generated/data` before every build |
| `modcore/src/main/resources/` | The `overdose` damage type and its tags, and modcore's mixins |
| `modcore/src/gametest/` | The game tests both mods run, with real survival players (`Players`): drinking, the Caffeine effect, milk, the bed, the overdose, bull mode, wings, landing, the elytra, feathers, bile, chests, the trader, fishing, every recipe, the collection, the advancements, the menu, the commands, and the creative tab. They name `GameTest` from their own package: Fabric's typealias to Fabric API's annotation, or `src/gametestPlain`'s for NeoForge |
| `neoforge/…/RedBullMod.kt` | The `@Mod` entry: NeoForge's events to `Mod`, and `OnNeoForge`, the `Loader` (deferred registers, data attachments, and the `gliding_flight` attribute on Wings) |
| `neoforge/…/mixin/` | `GliderWearMixin` spares an elytra worn with the Wings effect, and `LootPoolAccessor` reaches a pool's entries (`redbull.neoforge.mixins.json`) |
| `neoforge/…/Loot.kt` | Two global loot modifiers, `redbull:bull_bile` and `redbull:cans`, switched on in `data/redbull/loot_modifiers/`, and modcore's cans added to the fishing junk's pool as it loads |
| `neoforge/…/Settings.kt` | `config/redbull.toml`: Paper's `config.yml` without the pack |
| `neoforge/src/main/resources/` | `neoforge.mods.toml` and the loot modifiers |
| `neoforge/src/gametest/` | The `redbull_gametest` dev mod: `RedBullTests` registers modcore's tests as Fabric API would, in the game's empty structure |
| `fabric/…/RedBullMod.kt` | The entry: Fabric API's events to `Mod` (the Wings effect's glide through `EntityElytraEvents.CUSTOM`, which also sends the glide's vibration it skips), and `OnFabric`, the `Loader` (registries at once, Fabric API's attachments) |
| `fabric/…/Loot.kt` | Bull bile and chest cans as loot pools in the game's JSON, built from core and read through `LootPool.CODEC`, times the settings' multipliers; cans go to any table under `chests/`, a mod's or a data pack's too, as on NeoForge; modcore's cans join the fishing junk's own pool |
| `fabric/…/Settings.kt` | `config/redbull.toml`, NeoForge's switches in the same file, read on start and `/reload` without a config library |
| `fabric/…/mixin/` | Milk takes caffeine away once it took every effect (`MilkMixin` wraps `ItemStack.finishUsingItem`), so the Caffeine effect is back at once; a picked-up can (`PickupMixin`) and a closed container (`CloseContainerMixin`) feed the collection. Fabric has events for none of them; NeoForge's events do the same |
| `fabric/src/main/resources/` | `fabric.mod.json` and the Fabric mixins |
| `fabric/src/gametest/` | Fabric's `GameTest` for modcore's tests, `FabricGameTests` for its own settings reader, and a client test that takes the README's screenshots and one of the advancements |

## Rules that keep it working

- Every item is `minecraft:potion` underneath. The game drinks it, lists its effects in the tooltip, applies them, and no vanilla crafting, brewing, or trade takes a potion without a potion id. Non-drinkable items unset `consumable`.
- Bull bile alone is `minecraft:ominous_bottle` underneath, drawn with the potion model and drunk as a potion. A brewing stand's ingredient slot, the vanilla client's too, takes an item by its type through the brewing recipes' `brewing_reagents` set, so a potion there would let every potion in and send shift-clicked water bottles to the ingredient slot. The ominous bottle has no recipe, tag, or trade, and without its amplifier it does nothing. The brewing mix's choices are `RecipeChoice.predicateChoice` with our item as the example: the example's type is what the slot takes (`PotionMix.createPredicateChoice` uses stone). `Upgrades` swaps 1.4.1's potion bile.
- A potion names itself after its contents and ignores `item_name`, so names go in `custom_name`, not italic.
- Every text players read is a translation key with the English text as its fallback (`Texts`). A new key goes into all five language files; `AssetsTest` fails otherwise.
- A new can is a line in `Cans.kt`, a line in `scripts/textures.py`, a name, flavour, and slogan in every language, and a row in both READMEs and MODRINTH.md. `CansTest` keeps recipes unique and every can obtainable; `AssetsTest` checks its texture and model.
- The winged can glides because the game lets any item with `glider` and `equippable` for a slot glide from that slot, the main hand included (`LivingEntity.canGlide` walks every slot). The game wears it by a point per second of flight and stops a point before it breaks; that's the flight time.
- The jar carries the same pack zip the release attaches, and `Pack` hashes the copy in the jar. The release URL comes from the version, so dev builds send no pack unless `resource_pack.url` is set.
- Nothing loops over every online player but `Heart`, which reads two numbers each: the collection and the winged cans run on events, so hundreds of players cost little.
- Sugar or sugarfree: `Can.sweetener` fills a recipe's bottom corners (sugar, or honeycomb), `crashes()` lets only a can with sugar bring the crash, and every can with sugar sinks the player into `sugarPit()` when its buffs end, announced in gold on every platform.
- `HEALING` (16 s) caps anything that restores health, and `STRENGTH_II` (3 min) caps Strength II; `CansTest` holds every can to them.
- A drink leaves a crumpled can of its flavour (`crumpled_<id>`, drawn by `scripts/textures.py`); a winged can keeps its flavour to crumple into.
- Overdose kills with `setHealth(0)`, which dies without a totem check; the death message is set in `PlayerDeathEvent`. On both mods it's the `redbull:overdose` damage type: `bypasses_invulnerability` skips the totem, and its `message_id` names the death `death.attack.redbull.overdose`, or `death.attack.redbull.overdose.player` when something hurt the player shortly before. Both keys carry the same text, as Paper names every overdose alike; `AssetsTest` wants the twin.
- Caffeine lives in the player's persistent data and wears off by play time (`PLAY_ONE_MINUTE` counts ticks), so it waits while they're offline. On Fabric it's the `redbull:caffeine` attachment, on the same clock.
- On both mods the `redbull:caffeine` effect shows caffeine, with no HUD of our own: level I calm, II in the jitters, III in the crash (`Caffeine.effectLevel`), for the seconds until it wears off (`secondsLeft`), without particles. `Caffeines.set` and the heart's beat replace it with `forceAddEffect`, since the game's merge never lowers a level; milk or `/effect clear` take it off until then. Nothing reads the effect: the mechanics read the attachment.
- Any caffeine keeps a player out of bed (`Caffeine.sleepless`) with `redbull.sleepless` in the action bar, in yellow. NeoForge's `CanPlayerSleepEvent` and Fabric's `EntitySleepEvents.ALLOW_SLEEPING` both come after the game's own checks, so the spawn point is set first, as when it's too early to sleep.
- On Fabric every item is its own item, not a potion. Cans and bottles carry `potion_contents` for their effects and tooltip; a can's `use_remainder` is its crumpled can, wings or not. The game applies the effects and food; `CanItem` and `BottleItem` add the rest.
- Fabric has no winged can: a can with wings gives the `redbull:wings` effect for `Can.wings` seconds. The game asks `LivingEntity.canGlide` on the server and the client, and Fabric API's `EntityElytraEvents.CUSTOM` answers inside it, after the ground, vehicle, and Levitation checks; the same event skips the wear `updateFallFlying` deals to a glider, and with it the `elytra_glide` game event, so `Wings` sends that itself. Its icon and name come from core (`textures/mob_effect/wings.png`, `effect.redbull.wings`).
- Fabric recipes are data. Nothing writes them by hand: change `Cans.kt` or `Recipes.kt` in `modcore/src/data`. A water bottle in a recipe is Fabric's `fabric:components` ingredient; brewing is the game's own `minecraft:brewing` recipe type since 26.3.

## modcore

What both mods share: the game without a loader, compiled against NeoForm's Minecraft through ModDevGradle (`neoFormVersion`, no NeoForge), whose names are the game's own since 26.1. It never imports a loader's API: what differs goes through `Loader`, never into modcore as an `if`. `fabric` puts its classes in the jar and its resources in the mod's own (`resources.srcDir`); `neoforge` adds it to the `redbull` mod's source sets, in dev runs as in the jar. A new event goes into `Mod`, fed by both loaders' entries or by a mixin in modcore, so both mods see the same moment; a loader's own event comes first where it has one.

## NeoForge

`neoforge` is a mod built with ModDevGradle, in Kotlin with the plain `javafml` loader: the jar carries modcore's and core's classes and assets and nests `kotlin-stdlib` through jar-in-jar, so players need no Kotlin mod. Where it differs from Paper:

- Items are real items, so no resource pack: the jar carries core's assets, plus `es_mx` and `pt_pt` copies of the Spanish and Portuguese texts. A can is drunk through its `consumable` and `potion_contents` components and leaves its crumpled can through `use_remainder`. Bull bile and crude taurine draw the game's potion model, tinted by their `potion_contents` colour.
- Recipes are data. Since 26.x brewing is a recipe type too (`minecraft:brewing`), so bile into a water bottle is one more JSON file. The water bottle in a can's recipe is a `neoforge:components` ingredient. modcore's `Recipes.kt` in `src/data` writes them all from `Cans.kt`; don't write recipe JSON by hand.
- Wings are the `redbull:wings` effect, not a winged can, for as many seconds as the can has wings. The effect adds NeoForge's `gliding_flight` attribute, which `LivingEntity.canGlide` checks on the server and on the client, so the player glides as with an elytra. An elytra worn with them still wears in `LivingEntity.updateFallFlying`, so `GliderWearMixin` skips its wear while Wings last, as Fabric's elytra event does. Wings that end mid-glide leave `SOFT_LANDING_SECONDS` of Slow Falling, a tick later: the game is still walking the effects when one ends.
- Overdose deals the `redbull:overdose` damage type, tagged `bypasses_invulnerability` and the like, so no totem saves the player and the game writes `death.attack.redbull.overdose` itself.
- Chest cans go to any table under `chests/`, by the biome at the loot's origin. `/loot` takes the command's position as the origin, so test a biome with `execute positioned`.
- The dev run's version is `0.0.0-dev`, since FML rejects `dev`.

Both mods add the same things to the world the same way, since neither loader has an event for them in 26.3:

- **The wandering trader.** Trades are data since 26.x, and a wandering trader reads three fixed trade sets, so `WanderingTraderMixin` adds `Trades.pick`'s cans after them: `Trades.price` emeralds for one can, `Trades.MAX_USES` uses, 1 experience.
- **Fishing.** The junk table rolls inside `gameplay/fishing`, where no loot modifier sees it, so its own pool takes an empty can (`Fishing.EMPTY_WEIGHT`) and an inline table of the 26 crumpled cans (`Fishing.CRUMPLED_WEIGHT`), beside the game's 110: Fabric through `LootTableEvents.MODIFY`, NeoForge through `LootTableLoadEvent` and `LootPoolAccessor`.
- **Feathers.** The game calls `updateFallFlying` only for an entity in flight; `GlideMixin` hands each server tick of it to `Wings.glide`, which sends three feather bits behind a player on Wings every other tick, to everyone within 32 blocks, as Paper does.
- **Advancements** come from core's data, loaded from the jar like the rest of its resources; `Awards` awards their `done` criterion, with the root alongside: the collection's on every change and on join, the edge after a drink that didn't kill, the sugar pit when it comes, and the first glide on Wings.

## Fabric

The Fabric mod behaves and looks as NeoForge's, since both run modcore: the same items, creative tab, effects, colours, messages, sounds, menu, commands, and texts (`es_mx` and `pt_pt` copies included); only the hooks differ. `config/redbull.toml` takes NeoForge's keys, comments, and defaults (`overdose`, `wings`, `[loot]` `bile` and `chests`); `Settings` writes it with the defaults when it's missing and reads only that much TOML, so the mod needs no config library. It reads the file on start and on `/reload`, while NeoForge reads it again whenever it's saved. `/redbull`, the collection, and `/redbull give` and `/redbull caffeine` behave as on NeoForge; `/give` and `/item` also hand out any item by its id.

A new loader joins the same way: a module next to `paper` that takes core's classes and assets (a mod loader takes modcore too and implements its `Loader`), Minotaur's `modrinth` block with its loader (`./gradlew modrinth` in the release runs every module's), its jar in “Publish the GitHub release” in `release.yml` and in `build.yml`'s artifacts, its version keys in “Versions”, and a check in “Checking a change”.

## Building and testing

- `./gradlew build` runs the tests and builds `paper/build/libs/redbull-paper-<version>.jar`, shaded and relocated under `ru.mairwunnx.redbull.libs`, `paper/build/pack/redbull-pack.zip`, and `neoforge/build/libs/redbull-neoforge-<version>.jar`. JDK 25; Gradle provisions it through the foojay resolver.
- It also builds `fabric/build/libs/redbull-fabric-<version>.jar`, with modcore's and core's classes and assets inside, and runs the game tests on a real Fabric server (`./gradlew :fabric:runGameTest`), modcore's shared ones and Fabric's settings: drinking, the Caffeine effect after one, two, and three cans and its return after milk, the bed, the Wings effect (the launch, gliding with it and not without, no wear, the glide's vibrations, Slow Falling and `redbull.wings.gone` when it ends mid-flight, silence when it's taken on the ground), milk, the overdose through a totem and its death message in a fight, the settings, bull mode, the collection, the `/redbull` chest for players and admins, the commands, the creative tab, every crafting recipe, brewing, both furnaces, bile from cows, chest loot by biome in the game's chests and another mod's, the wandering trader's cans, cans among fishing junk and in a real catch, feathers only while gliding on Wings, and each advancement when it's earned and not before. It ends with “All 34 required tests passed”.
- There is no MockBukkit for Paper 26.3; tests cover `core`. The rest runs on a real Paper. On NeoForge and Fabric, game tests drive a real server with a fake survival player.

## Checking a change

1. `./gradlew build`: the tests and the jar.
2. A real Paper with the jar. On start the plugin builds every item through the game's codecs, so a component the game rejects fails here:
   ```sh
   python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin paper/build/libs/redbull-paper-dev.jar --plugin paper/build/libs/redbull-check.jar \
       --command "tick query" --command "tick query" --command "tick query" --command "tick query" \
       --expect "Red Bull dev is on: 26 cans, 23 recipes" --expect "Red Bull check passed"
   ```
   `redbull-check.jar` walks the way to a can in blocks at `tick rate 400` (a sprint skips block entities); the commands only give it the seconds. Clicks need a client: a mineflayer bot on 26.1 through ViaVersion and ViaBackwards clicks, shift-clicks, and swaps bile into the stand once `/redbullcheck` opened it (the snapshot of ViaBackwards garbles the bot's moves and block clicks, so drop its move packets).
3. modcore's game tests on NeoForge, the same as on Fabric but its settings reader, on a real server. It ends with “All 33 required tests passed”:
   ```sh
   ./gradlew :neoforge:runGameTestServer
   ```
   `./gradlew :neoforge:runServer` starts a dev server in `neoforge/run/`; it logs “Red Bull 0.0.0-dev is on: 26 cans, 23 recipes, 56 items” once every recipe parsed.
4. A real Fabric server with the mod and its dependencies from Modrinth, and a real NeoForge server with the mod alone; each fails on any error about the mod (`neoforge_smoke.py` is Allay's, made like `fabric_smoke.py`):
   ```sh
   python3 -I .claude/scripts/fabric_smoke.py --game 26.3 --mod fabric/build/libs/redbull-fabric-dev.jar --modrinth fabric-api --modrinth fabric-language-kotlin --expect "Red Bull dev is on: 26 cans, 23 recipes"
   python3 -I .claude/scripts/neoforge_smoke.py --game 26.3 --mod neoforge/build/libs/redbull-neoforge-dev.jar --expect "Red Bull 0.0.0-dev is on: 26 cans, 23 recipes"
   ```
   With Allay's jar beside it (`--plugin` on Paper, `--mod` on the mods; the release's from `allay-ai/minecraft`), the log must also say “Expansion Red Bull dev is on: 1 tool, 0 items, a prompt” (`0.0.0-dev` on NeoForge). A question needs Allay's backend: the tool's answers are `RedBullExpansionTest`'s.
5. The README's screenshots on a real Fabric client: `./gradlew :fabric:runClientGameTest` opens a 1280×720 window in English and saves `world.png` (the hotbar at noon), `effects.png` (Caffeine II and Wings beside the inventory), `collection.png` (`/redbull` for a player without operator rights), and `advancements.png` (Red Bull's tab) in `fabric/build/run/clientGameTest/screenshots/`. Copy them to `media/screenshots/` when the look changes.
6. Gliding everywhere, and NeoForge's effect icons and tooltips, need a player: `./gradlew :neoforge:runClient` for NeoForge; for Paper, the owner tries them after a release reaches Moonlands.

## Autopilot

`.github/workflows/autopilot.yml` runs Claude every Monday at 03:00 UTC: `/update-deps`, then `/update-minecraft`, a fix for whatever they break, a push to `master`, and the “Release” workflow with the stores “Stores” allows when what the project ships changed. The release's `deploy` job then starts Moonlands' `ship-plugin.yml` with the `FAMILY_ACTIONS_TOKEN` secret. The run needs the `CLAUDE_CODE_OAUTH_TOKEN` secret. “Autopilot” in `~/c/personal/minecraft/CLAUDE.md` explains the family's runs. A session with the owner releases only when the owner asks.

## Versions

Where the versions live, for `/update-deps` and `/update-minecraft` (both in `.claude/skills/`, the same files in every project of the family):

- The game: `io.papermc.paper:paper-api` (twice in `paper/build.gradle.kts`), `gameVersions` (Modrinth) and `platformVersions` (Hangar) there, `api-version` in `paper/src/main/resources/paper-plugin.yml`, and `min_format`/`max_format` in `paper/src/pack/pack.mcmeta` (the game's `resource_major` in its `version.json`).
- NeoForge: `minecraft` and `neoforgeVersion` at the top of `neoforge/build.gradle.kts` (they also fill `neoforge.mods.toml` and Modrinth's `gameVersions`); ModDevGradle (`net.neoforged.moddev`) in the root `build.gradle.kts`. The versions list: `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge`.
- Fabric: `minecraft`, `loader`, `fabricApi`, and `fabricKotlin` at the top of `fabric/build.gradle.kts` (the game version also goes into `fabric.mod.json` and Modrinth's `gameVersions` from there); Loom in the root `build.gradle.kts`. `fabric_smoke.py` takes the newest stable loader.
- modcore: `neoFormVersion` in `modcore/build.gradle.kts` (NeoForm's `26.3-N`, the newest for the game) and Mixin's annotations (`net.fabricmc:sponge-mixin`, compile only, from Fabric's Maven in the root `build.gradle.kts`).
- Allay's expansion API: `chat.allay:allay-expansion` in `core/build.gradle.kts`, compile only. It builds against 1.3.0 for `Asker.language`, but `requiresApi` stays 1.0.0 (Allay 6.12), and `RedBullExpansion.language` calls the newer method only where `AllayExpansions.apiVersion()` has it; an older Allay refuses an expansion that requires a newer API.
- Java: `jvmToolchain` in every module, the foojay resolver in `settings.gradle.kts`, and CI's `setup-java`.
- Gradle in `gradle/wrapper/gradle-wrapper.properties`; Kotlin and the Gradle plugins in the root `build.gradle.kts`; libraries in each module's `dependencies`.

## Platforms

How the family ships a project to more than one platform; this one follows it since 1.2.0.

- **Modules.** One Gradle module per platform, `paper/`, `fabric/`, `neoforge/`, next to a `core/` module with what they share: the rules as plain Kotlin without game imports, data, and assets. The mods share a `modcore/` module too, the game without a loader, compiled against NeoForm's Minecraft, so `fabric/` and `neoforge/` stay thin adapters around it (since 1.4.0, as in Allay). Every module builds from one version, and one release ships them all.
- **Jars.** `<name>-paper-X.Y.Z.jar`, `<name>-fabric-X.Y.Z.jar`, and `<name>-neoforge-X.Y.Z.jar`, each attached again without the version, so `releases/latest/download/<name>-<platform>.jar` always fetches the newest. A project that builds only for Paper keeps its jar’s present name until it gains a second platform, so links and Moonlands’ `jars.txt` keep working.
- **Modrinth.** One project for every platform. Each platform is its own version, numbered `X.Y.Z+paper`, `X.Y.Z+fabric`, or `X.Y.Z+neoforge`, with its own loaders and dependencies; a mod’s version says it runs on the client and the server.
- **Hangar.** Paper only, numbered `X.Y.Z`.
- **Moonlands** runs the Paper jar only.

## Releasing and Moonlands

1. Keep a `## X.Y.Z` section in CHANGELOG.md, written for server owners. After a change that reaches what the project ships, add it in the same push: under the topmost section while that version isn't released (`gh release view vX.Y.Z` fails), or under a new section with the next version.
2. Run the “Release” workflow: `gh workflow run release.yml -R MairwunNx/minecraft-redbull -f version=X.Y.Z -f modrinth=true -f hangar=true`. It builds with `-PreleaseVersion`, tags `vX.Y.Z`, and attaches every platform’s jar as “Platforms” names them, `redbull.jar` (the Paper jar’s name up to 1.1.1, kept so old links work), and `redbull-pack-X.Y.Z.zip`. Nothing in the repo holds the version; local and CI builds are `dev`.
3. Its `deploy` job ships the jar to Moonlands through `ship-plugin.yml`, which points the Red Bull line in `minecraft/extras/jars.txt` at it.

## Stores

Both are on since 1.1.0 (2026-10-21), at the owner's word: run every release, the autopilot's too, with `-f modrinth=true -f hangar=true`, then check the version on each store.

- **Modrinth:** `minecraft-redbull` (id `X5nYFSkK`; `redbull` and `red-bull` belong to other, hidden projects), MIT. `./gradlew :paper:modrinth` uploads the Paper jar as `X.Y.Z+paper` (`X.Y.Z` up to 1.1.1) and syncs MODRINTH.md as the description. `scripts/modrinth_mods.py` uploads the mods as `X.Y.Z+fabric` (with Fabric API and Fabric Language Kotlin required) and `X.Y.Z+neoforge` through Modrinth's v3 API, marked `client_and_server`: Minotaur can't set a version's `environment`, and Modrinth refuses a mod version without it while the project is in review. It skips versions already there. Submitted for review with 1.1.0.
- **Hangar:** `MairwunNx/RedBull`, MIT, created 2026-10-21 through `POST /api/internal/projects/create`. `./gradlew publishPluginPublicationToHangar` uploads each version; it took the first one, 1.1.0, too, which older Hangar projects of the family had to upload on the website. The release syncs the project page from MODRINTH.md (`syncAllPluginPublicationPagesToHangar`); the icon is edited on the site.

The tokens are the `MODRINTH_TOKEN` and `HANGAR_TOKEN` secrets, also in `~/.secrets/cloud.env`. Never put them in the repo.

## Commits

Short one-line imperative English messages, one logical change per commit, each one building: “Let the winged can glide from the main hand”.
