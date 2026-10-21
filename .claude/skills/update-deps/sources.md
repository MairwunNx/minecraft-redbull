# Where fresh versions come from

The same file sits in every project of MairwunNx's Minecraft family; change all copies together. Ask the source, never your memory: versions move every week. Take the newest **stable** version. Skip anything marked alpha, beta, rc, pre, M1, snapshot, nightly, or dev, unless the project already uses that channel (Spigot's API is published only as `-R0.1-SNAPSHOT`, and that is its release). Two platforms are exceptions: a Paper build on the `BETA` channel counts (an `ALPHA` one never does), and so does any NeoForge release, beta included.

The game moves to Mojang's releases (`type` `release`). A project that builds only for Fabric moves sooner: to the newest version in Fabric Meta's game list, a snapshot, pre-release, or release candidate too, once Fabric API has a build for it. A project that also builds for Paper, Spigot, or NeoForge, and Moonlands, wait for the release.

Compare versions as numbers, part by part: 26.10 is newer than 26.9, and build 205 newer than 99. A list sorted as text, or a `<latest>` tag, can name an older version.

## Game and platforms

| What | Where to ask | Notes |
|---|---|---|
| Minecraft releases | `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` | `latest.release`; each version's own JSON has `javaVersion.majorVersion`, the Java it needs |
| Paper server and its builds | `https://fill.papermc.io/v3/projects/paper` (versions), `…/versions/<game>/builds` (every build, with `channel` and the jar's sha256) | A build's `channel` is `ALPHA`, `BETA`, or `STABLE`. Take the newest `BETA` or `STABLE` build; a version with only `ALPHA` builds isn't ready |
| Paper API (`io.papermc.paper:paper-api`) | `https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml` | Versions read `<game>.build.<n>-<channel>`, like `26.3.build.190-stable` or `26.4.build.3-beta`. The file's `<latest>` lies; pick the highest `<n>` for your game version from `<versions>` whose channel is `beta` or `stable`, or take `<n>` from Fill |
| Spigot API (`org.spigotmc:spigot-api`) | `https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/maven-metadata.xml` | One version per game version: `<game>-R0.1-SNAPSHOT` |
| Spigot builds | `https://hub.spigotmc.org/versions/` (a list of `<name>.json` files), `https://hub.spigotmc.org/versions/<game>.json` | Spigot supports a game version once its `<game>.json` exists. The list also holds build numbers (`4663.json`); pick the game versions from it and compare them as numbers. `javaVersions` lists the class file versions it runs: 69 is Java 25, 70 is Java 26 |
| Velocity | `https://fill.papermc.io/v3/projects/velocity` | Same API as Paper's |
| Fabric game versions, loader | `https://meta.fabricmc.net/v2/versions/game`, `https://meta.fabricmc.net/v2/versions/loader` | Both flag `stable`; in the game list, `stable` marks releases, and snapshots read `false`. Take the newest loader with `stable: true`; it works with any game version |
| Fabric API | `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml` | Versions read `<api>+<game>`, like `0.163.0+26.4`. Take the highest `<api>` whose suffix is your game version; `<latest>` may belong to a newer game. Builds for a version's snapshots carry its suffix too (`+26.4` before 26.4 is out), so check on Modrinth (`fabric-api`, `game_versions=["<game>"]`) that the build lists the exact version you move to, like `26.4-snapshot-4` or `26.4` |
| Other mods and plugins | `https://api.modrinth.com/v2/project/<slug>/version?game_versions=["<game>"]&loaders=["fabric"]` | Use `loaders=["paper"]` for Paper plugins. Newest first; `version_type` is `release`, `beta`, or `alpha` |
| Fabric Loom | `https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml` | |
| Fabric Language Kotlin | `https://api.modrinth.com/v2/project/fabric-language-kotlin/version?game_versions=["<game>"]&loaders=["fabric"]` | Versions read `<flk>+kotlin.<kotlin>`, like `1.14.2+kotlin.2.4.21`. Take the one built for the project's Kotlin; when Kotlin moves, it moves with it, and a Kotlin without a matching build stays behind |
| NeoForge | `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge` | Versions follow the game: `26.3.0.92` is for 26.3, `26.3.1.x` for 26.3.1. `…/api/maven/latest/version/releases/net/neoforged/neoforge?filter=26.3` gives the newest for one game version. Any release counts, `-beta` ones too |
| NeoForge ModDevGradle | `https://maven.neoforged.net/api/maven/latest/version/releases/net/neoforged/moddev-gradle` | |
| Hangar | `https://hangar.papermc.io/api/v1/projects/<owner>/<slug>/versions?limit=5` | Plugins that aren't on Modrinth |

## Build tools and libraries

| What | Where to ask | Notes |
|---|---|---|
| Gradle | `https://services.gradle.org/versions/current` | Update with `./gradlew wrapper --gradle-version <v>`, then run it once more so the wrapper jar and scripts update too |
| Gradle plugins | `https://plugins.gradle.org/m2/<id with / for .>/<id>.gradle.plugin/maven-metadata.xml` | Like `com/gradleup/shadow/com.gradleup.shadow.gradle.plugin` |
| Maven Central libraries (Kotlin, kotlinx, OkHttp…) | `https://repo1.maven.org/maven2/<group with / for .>/<artifact>/maven-metadata.xml` | `<release>` may be a beta: pick the newest stable from `<versions>`. Kotlin's Gradle plugin, stdlib, and serialization plugin share one version |
| Other Maven repos | the same `maven-metadata.xml` path under the repo's URL in `build.gradle.kts`, like `https://repo.extendedclip.com/releases/me/clip/placeholderapi/` | |
| Java | `https://api.adoptium.net/v3/info/available_releases` | Stay on the newest LTS the game needs unless the project says otherwise |
| npm packages | `npm view <package> version`, `npm outdated` | `npm install <package>@latest` for a major; keep `package-lock.json` in step |
| Go | `https://go.dev/dl/?mode=json` | The first entry is the newest stable release. `toolchain go<version>` in `go.mod` takes it, and so does `golang:<version>-alpine` in a `Dockerfile`; the `go` line stays at the oldest release the code needs |
| Go modules | `go list -m -u all` lists what's behind | `go get -u ./... && go mod tidy` updates them; a new major is a new module path (`/v2`) and changes the imports |
| Grafana plugins | `https://grafana.com/api/plugins/<id>` | `version` is the newest; Grafana's own version comes from its Docker image |
| Bun packages and Bun | `bun outdated`, `bun update --latest`; Bun itself: `gh api repos/oven-sh/bun/releases/latest -q .tag_name` | Bun's Docker image is `oven/bun:<version>-alpine` |
| Docker Hub images | `https://hub.docker.com/v2/repositories/<namespace>/<name>/tags?page_size=100&ordering=last_updated` | `library/` for official images like `nginx` and `mongo`. Keep the tag's shape (`-alpine`, `-java25`) |
| GHCR images | `gh api /orgs/<org>/packages/container/<name>/versions`, or the project's GitHub releases | |
| GitHub Actions | `gh api repos/<owner>/<action>/releases/latest -q .tag_name` | Pin the major tag (`actions/checkout@v7`) as the workflows already do |
