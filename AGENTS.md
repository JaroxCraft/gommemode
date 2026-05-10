# AGENTS.md

Compact guidance for AI agents working in this repository.

## Project

Gommemode is a client-side Fabric mod written in Kotlin. It spawns a GommeHD entity that looks at the player and plays
the Gomme hymn, activated via keybind (default: `G`) or automatically when the song stops.

## Build, Lint & Run

```bash
./gradlew build          # produces jar in build/libs/
./gradlew runClient      # launch Minecraft with the mod loaded
./gradlew ktlintCheck    # lint Kotlin sources
./gradlew ktlintFormat   # auto-format Kotlin sources
```

On Windows (PowerShell), omit the `./` prefix.

- There are no tests. Verification is only via `./gradlew runClient`. Tell the user what to do in game and report the
  outcome back
- Java and Kotlin target **25** (see `build.gradle.kts` and CI workflows).
- `.editorconfig` enforces LF line endings, UTF-8, and 4-space indents for Kotlin.

## Version Management

- `gradle.properties` → `mod_version` and `maven_group`.
- `gradle/libs.versions.toml` → all dependency versions.
- Artifact version is composed as `mod_version+minecraft_version` in `build.gradle.kts`.

## CI Workflows

| Workflow                  | Trigger                                      | Key step                                                                                                                                                                                                  |
|---------------------------|----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Build                     | push / PR to `master`                        | `./gradlew build`                                                                                                                                                                                         |
| Lint                      | push / PR to `master`                        | `./gradlew ktlintCheck`                                                                                                                                                                                   |
| Release                   | `v*` tag push                                | validates tag matches `mod_version` (`gradle.properties`) + `minecraft` (`gradle/libs.versions.toml`), then `./gradlew build`, `./gradlew modrinth`, `./gradlew modrinthSyncBody`, creates GitHub Release |
| Renovate Config Validator | changes to `renovate.json`                   | validates config                                                                                                                                                                                          |
| OpenCode                  | issue / PR comment with `/oc` or `/opencode` | runs OpenCode agent                                                                                                                                                                                       |

Release tag format: `v<mod_version>+<minecraft_version>` (e.g. `v0.1.0+26.1.2`). If `mod_version` contains `-`, the
GitHub release is marked prerelease.

## Renovate

Configuration in `renovate.json`:

- Base: `config:recommended` with dependency dashboard disabled.
- `com.mojang:minecraft` is ignored from standard updates; tracked via a custom manager reading
  `gradle/libs.versions.toml`.
- Minecraft update PRs are draft with labels `minecraft-update` and `manual-action-required`.
- Custom registries:
    - `net.fabricmc.*` → `maven.fabricmc.net`
    - `com.terraformersmc:modmenu` → `maven.terraformersmc.com`
    - `me.shedaniel.cloth:cloth-config-fabric` → `maven.shedaniel.me`
- Fabric API updates are labeled `dependencies`; Minecraft updates always require manual intervention.

## Mod Architecture

- Entrypoint class: `de.jarox.gommemode.Gommemode` registered in `src/main/resources/fabric.mod.json` under both
  `client` and `main`.
- ModMenu config entrypoint: `de.jarox.gommemode.modmenu.ModMenuApiImpl`.
- Uses Cloth Config API + ModMenu for in-game configuration.
- Main source lives under `src/main/kotlin/de/jarox/gommemode/`.

## Agent Behavior

- **Never push changes to the remote repository unless explicitly asked to do so.** Always ask for confirmation before
  running `git push`, `git push --force`, or any other command that mutates remote history.
- **When writing Fabric mod code, consult official documentation.**
  Use [Fabric Docs](https://docs.fabricmc.net/develop/) or
  the [Fabric Javadocs](https://maven.fabricmc.net/docs/fabric-api-<fabric-api-version>/index.html), substituting the
  actual `fabric-api` version from `gradle/libs.versions.toml`.

## Commit Style

- **Always use conventional commits** for every commit you create or rewrite during merge/rebase.
- **Never** leave a non-conventional commit message on `master` or any other branch. Amend if necessary.
- **Split changes into multiple logical commits.** One coherent change per commit.
