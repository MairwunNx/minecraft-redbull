---
name: update-deps
description: Updates every dependency of this project to its newest stable version, while the Minecraft version it targets stays the same. Covers Gradle, Kotlin and other libraries, the Paper, Spigot, Fabric, or NeoForge API within the same game version, npm and Bun packages, Go modules, Grafana plugins, Docker images, and GitHub Actions. Use when the owner asks to update dependencies, libraries, or tools.
argument-hint: "[only these, e.g. gradle kotlin]"
---

# Update dependencies

Bring every pinned version in this project up to the newest stable one, prove the project still works, and commit. The same file sits in every project of the family; this project's own facts live in its CLAUDE.md.

If the owner named only some dependencies (`$ARGUMENTS`), update just those.

## The one limit: the game version

The game version stays where it is; `/update-minecraft` moves it. For a project on Minecraft 26.3, that means:

- **Allowed:** newer builds and patches of 26.3: Paper API `26.3.build.181-stable` to `26.3.build.190-stable` (or a newer `-beta` build), a 26.3.1 patch, NeoForge `26.3.0.x`, a newer Fabric loader or Fabric API for 26.3.
- **Not allowed:** 26.4, or anything that needs it. A library whose newest version wants another game version stays on the newest one that fits.

A server (Moonlands) moves Paper builds here too, but a new patch of the game (26.3.1) changes the world, so it goes through `/update-minecraft`.

## Steps

1. **Read CLAUDE.md.** Its “Versions” section lists where versions are pinned and what must stay as it is (a minimum Bun, a database major); its “Checking a change” section says how to prove the project works. Then run `git status`: leave other people's uncommitted changes alone and keep them out of your commits.
2. **List every pinned version.** Use “Versions” and look for more: `gradle/wrapper/gradle-wrapper.properties`, the `plugins` and `dependencies` blocks of every `*.gradle.kts`, each platform module's own (`paper/`, `fabric/`, `neoforge/`) included, `gradle/libs.versions.toml`, `gradle.properties`, `jvmToolchain`, `package.json`, `go.mod` (its `go`, `toolchain`, and `require` lines), `Dockerfile` `FROM` lines, `image:` in compose files, `uses:` in `.github/workflows/`, and plugin lists like `plugins.txt` or `jars.txt`.
3. **Find each newest version** where `sources.md` (next to this file) says to ask. Note old and new side by side. If nothing is behind, say so and stop: no commit, no changelog, no release.
4. **Update in groups:** the build tool (Gradle, Bun), the language (Kotlin, TypeScript, Go), the platform API, libraries, images, Actions. Build and run the tests after each group, so a failure points at its cause.
5. **Majors:** read the release notes or migration guide and fix the code. If a major breaks too much to fix well, keep the old major and say so in the report. Some majors need care beyond code. MongoDB moves only along the upgrade path its release notes give, one step per deploy (8.0 to the next version they allow, not straight to the newest); on Moonlands `scripts/deploy.py` dumps the database before the image changes and raises the feature compatibility version once the new one runs, so the next step can follow on the next run.
6. **Check** as CLAUDE.md's “Checking a change” says, all of it, including the run on a real server where the project has one.
7. **Changelog.** When the update reaches what the project ships (the jar, the image, the npm package, the server), add a “## X.Y.Z” section to CHANGELOG.md with the next patch version, or add a line to the topmost section if it isn't released yet (`gh release view vX.Y.Z` fails). Updates that only touch CI don't need one. Moonlands has no changelog: its commit messages say what changed.
8. **Commit and push,** one commit per group, in the project's commit style: “Update Gradle to 9.8.1 and Kotlin to 2.4.30”. Release when the owner asks or when the autopilot runs you (its workflow says so), and ship to Moonlands as `~/c/personal/minecraft/CLAUDE.md` says.

## Running unattended

The autopilot (“Autopilot” in `~/c/personal/minecraft/CLAUDE.md`) runs this every week in GitHub Actions, with nobody to answer. Where a step says to ask the owner, take the choice that keeps the project as it works today, and name it in the report. A failure you can't fix stays out of the commit: keep the old version and go on with the rest.

## Report

A short table: each dependency, old version, new version. Then what stayed behind and why, and how the check went.
