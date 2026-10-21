---
name: update-minecraft
description: Moves this project to a new Minecraft version, like 26.3 to 26.4. In a plugin or mod it updates the Paper, Spigot, Fabric, or NeoForge API, the descriptor, the stores' game versions, Java, and the code the new API breaks, then checks the result on a real server. On Moonlands it checks that every plugin and the datapack have a release for the version, runs the whole set on a local Paper, backs up the world, and switches the server. Use when a new Minecraft version is out and the project should move to it.
argument-hint: "<game version, e.g. 26.4>"
---

# Move to a new Minecraft version

Move this project to Minecraft `$ARGUMENTS`, or to the newest release if no version was given. The same file sits in every project of the family; this project's own facts live in its CLAUDE.md, and the places to ask for versions in `.claude/skills/update-deps/sources.md`.

## 1. Which project, which version

- **A plugin or mod:** a `build.gradle.kts` that builds against Paper, Spigot, Fabric, or NeoForge, in one module or in several (`paper/`, `fabric/`, `neoforge/` next to a shared `core/`). Follow “Plugins and mods”.
- **Moonlands:** a `compose.yaml` with `PAPER_BUILD`. Follow “Moonlands”.
- **Anything else** (minecraft-mcp, minecraft-exporter, minecraft-grafana, Allay's backend) has no game version. Say so and stop.

The target is `latest.release` in Mojang's manifest. A project that builds only for Fabric targets the newest version in Fabric Meta's game list instead, snapshots, pre-releases, and release candidates included, and moves from one snapshot to the next. Everything else, Moonlands too, moves only to releases. A patch (26.3.1) counts as a new version too. If the project already targets it, say so and stop, with no commit.

A platform is ready for the version when:

- **Paper** has a build for it on the `BETA` or `STABLE` channel. Only `ALPHA` builds: not ready.
- **Spigot** has `https://hub.spigotmc.org/versions/<version>.json`.
- **Fabric** has a Fabric API build that lists the version on Modrinth. The loader is the newest stable one.
- **NeoForge** has any `<version>` release (`26.4.0.x` for 26.4), a beta too.

When something isn't ready, say what's missing and stop. Nothing changes; the next run asks again.

## Plugins and mods

### 2. Is everything ready?

Check every platform the project builds for, as above, and the Java the game needs (`javaVersion.majorVersion` in the version's JSON). Then the plugins or mods it needs on a server, like PlaceholderAPI, on Modrinth for the new version. One project builds for one game version, so every target must be ready before any moves. A project with a module per platform ships one version to all of them: it moves only when every platform it builds for is ready, Fabric Language Kotlin included when a Fabric module is written in Kotlin.

### 3. Read what changed

Read the platform's announcement for the version (papermc.io/news, the Spigot forum, the Fabric blog, the NeoForge blog) and the game's changelog on minecraft.net. Note removed and renamed API, new deprecations, and game changes that touch what this project does.

### 4. Change every place the version lives

CLAUDE.md's “Versions” section lists them. Then search for the old version (`git grep -n "26\.3"`) to catch the rest:

- the API dependency: `paper-api` with the newest beta or stable build of the new version, `spigot-api`, Fabric's `minecraft`, loader, and Fabric API, NeoForge. With a module per platform, each module's `build.gradle.kts` keeps its own
- the descriptor: `api-version` in `paper-plugin.yml` or `plugin.yml`, `depends` in `fabric.mod.json`, `neoforge.mods.toml`
- the stores: `gameVersions` for Modrinth and `platformVersions` for Hangar, in every module that publishes
- a resource pack's `pack.mcmeta` (`min_format` and `max_format`, from `resource_major` in the game's `version.json`), where the project ships one
- the Java toolchain and CI's `setup-java`, when the game needs a newer Java
- tests that spell out version strings
- texts: README.md, README.RU.md, MODRINTH.md, and CLAUDE.md where they name the version

The project targets one game version. Replace the old one; the earlier release stays on the stores for servers that haven't moved yet.

### 5. Make it build and keep it true

Build, fix what the new API broke, and clear new deprecation warnings that point at removals. Then look at what the project copies from the game: items, blocks, mobs, enchantments, formulas, tables, version-specific behavior. Read the new game jar for them instead of guessing (“Reading the game” below).

Dependencies that move with the game, like Fabric API or a PlaceholderAPI built for the new version, move now too. The rest wait for `/update-deps`.

### 6. Check

Run the whole check in CLAUDE.md's “Checking a change” on the **new** version, including the run on a real server. With several platforms, that's every one: `paper_smoke.py` for Paper, `fabric_smoke.py` for Fabric, the game tests and a dev server for NeoForge, as CLAUDE.md lists. A plugin that loads but logs an error doesn't pass. If something can only be checked in a game client, list it in the report.

### 7. Changelog, commit, report

Add a “## X.Y.Z” section to CHANGELOG.md with the next minor version, leading with the game version (“Runs on Paper 26.4”) and naming anything that changed for server owners. On a beta Paper build, say so. Commit and push in the project's style: “Move to Paper 26.4”. Release when the owner asks or when the autopilot runs you. Moonlands moves on its own, once every plugin it runs has a release for the version.

Report: the version moved from and to, the API changes that needed code, the game changes that reached the project, the check's result, and what still needs a game client.

## Moonlands

A world opened by a newer game can't go back to the older one, so nothing changes on the server until every step before the switch has passed.

### 2. Is everything ready?

- **Paper:** the newest `BETA` or `STABLE` build for the version.
- **Java:** what the game needs, and an `itzg/minecraft-server` tag with that Java (`-java25`).
- **Each line of `minecraft/extras/plugins.txt`:** a Modrinth version for the new game version and the `paper` loader. That includes the datapack, with the `datapack` loader.
- **Each jar in `minecraft/extras/jars.txt`,** the owner's plugins: a GitHub release built for the new version (a release with jars for several platforms counts by its Paper jar) (its changelog says “Runs on Paper 26.4”, or its jar's `paper-plugin.yml` names the `api-version`). Each plugin's own autopilot moves and releases it.

Make a table: each plugin, its version now, the version for the new game, or “none yet”. Any “none yet” blocks the move: report the table and stop, and the next run asks again. Only the owner can agree to drop a plugin.

### 3. Change the repository

- `compose.yaml`: `VERSION`, `PAPER_BUILD`, and the `itzg/minecraft-server` tag when Java changes.
- `minecraft/extras/plugins.txt` and `minecraft/extras/jars.txt`: the versions from step 2. When an owner's plugin added settings or message keys, follow “Shipping a plugin or backend update” in CLAUDE.md.
- Files in `minecraft/config/` whose format a plugin's new version changed.
- Texts that name the version: README.md, `site/index.html` (“Java Edition 26.3”, twice), and CLAUDE.md (“The server runs 26.3” under “Studying the game”). Run `git grep -n "<old version>"` to catch the rest.
- When the site copies something the new version changed (sky colors in `day.json`, textures, fonts), read the new jar as “Reading the game” says.

Don't push yet.

### 4. Run the whole set locally

`python3 -I .claude/scripts/moonlands_smoke.py` starts a local Paper as the changed files pin it, with every plugin, and stops it. Every plugin must enable without errors; one that logs an error blocks the move just as a missing release does. A new error that only a fresh folder causes (a plugin asking to accept a EULA) goes into `IGNORE` in that script.

### 5. Switch the server

1. Back up first: `gh workflow run backups.yml -f action=now`, then `gh run watch`, then `-f action=list` to see the snapshot. Note its ID for the report.
2. Push in one commit, “Move to Paper 26.4”. Komodo deploys the push by itself: the server stops with its world saved and starts on the new version a few minutes later.

### 6. Check the live server

Once it switched:

- The server's log (`ssh minecraft docker logs --since 15m minecraft`) shows `This server is running Paper version <version>-<build>`, every plugin enabling, and no `ERROR` line or exception.
- WorldAPI answers with the new `minecraftVersion`: `curl -s https://minecraft.mairwunnx.com/worldapi/api/v1/server`. The site shows live data.
- The relay still lets players in: `nc -z -w 5 ru.minecraft.mairwunnx.com 25565`.

If the server won't come up or loses data, stop and report it. Rolling back means restoring the snapshot from step 5 with `backups.yml -f action=restore`, which needs the owner's go-ahead.

### 7. Report

The version moved from and to, each plugin's old and new version, the snapshot ID taken before the switch, whether the server switched or waits for players to leave, the check's result, and what players should know (the new client version they need).

## Reading the game

Since 26.1 Mojang ships the game unobfuscated: its jars carry the real class, method, and field names. The version's JSON in Mojang's manifest links the jars under `downloads.server.url` and `downloads.client.url`; on the owner's machine they also sit in `~/.gradle/caches/neoformruntime/artifacts/`. `javap -c -p -cp <jar> <class>` shows a class with its constants. Much of the game is data in the jar: `data/minecraft/` (timelines, dimension types, biomes, loot tables, recipes) and `assets/minecraft/`. The server jar keeps the real one nested under `META-INF/versions/`.

## Running unattended

The autopilot runs this every week in GitHub Actions, with nobody to answer. Where a step says to ask the owner, take the choice that keeps things as they work today, and name it in the report.
