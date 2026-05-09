# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Project

Gommemode is a client-side Fabric mod written in Kotlin. When activated, it spawns a GommeHD entity that looks at the
player and plays the Gomme hymn. Activated via keybind (default: G) or automatically when the song stops playing.

## Build & Run

```bash
./gradlew build          # produces jar in build/libs/
./gradlew runClient      # launch Minecraft with the mod loaded
```

On Windows (PowerShell), omit the `./` prefix.

There are no tests. The only way to verify correctness is to run the client.

## Agent Behavior

**Never push changes to the remote repository unless explicitly asked to do so.** Always ask for confirmation before
running `git push`, `git push --force`, or any other command that mutates the remote history. This applies even when the
user previously gave permission in an earlier conversation.

**If something is unclear, always ask the user** using the question tool before proceeding. Do not guess or assume
intent — clarify requirements, design choices, or ambiguous instructions.

**When generating a plan, present it to the user before implementing.** Outline the steps you intend to take and wait
for confirmation before writing code or making changes.

**When writing Fabric mod code, consult the official documentation.** Use the WebFetch tool to search
[Fabric Docs](https://docs.fabricmc.net/develop/) for tutorials and conceptual guides, or the
[Fabric Javadocs](https://maven.fabricmc.net/docs/fabric-api-<fabric-api-version>/index.html) for API references.
Replace `<fabric-api-version>` with the actual Fabric API version from `gradle/libs.versions.toml`. Prefer official
sources over guesses — Fabric APIs change across versions, and this project targets specific Minecraft versions
defined in `gradle/libs.versions.toml`.

## Commit Style

**ALWAYS use conventional commits** for every commit you create. This includes commits you author yourself and commits
you rewrite during merge/rebase operations.

**Never** leave a non-conventional commit message on `master` or any other branch, even if it comes from an external PR
or bot. Amend it during merge/rebase if necessary.

**Split changes into multiple logical commits.** Do not bundle unrelated changes into a single commit. Each commit
should represent one coherent change:

## Version Management

All dependency versions live in `gradle/libs.versions.toml`. The artifact version is composed as
`mod_version+minecraft_version` in `build.gradle.kts`. A GitHub Actions workflow creates a release on every `v*` tag,
validating that the tag matches `mod_version+minecraft_version` from `gradle.properties`.

## Workflows

### Build (`build.yml`)

Runs on every push to `master` and on all pull requests targeting `master`. Sets up Java 25, caches Gradle, and runs
`./gradlew build`. Ensures the project compiles before merging.

### Release (`release.yml`)

Triggered by pushing a `v*` tag (e.g. `v1.0.0+26.1.2`). Steps:

1. Validates that the tag version matches `mod_version` in `gradle.properties` and `minecraft` in
   `gradle/libs.versions.toml`
2. Checks version format: mod version must be semver (optionally with prerelease suffix like `-beta.1`), Minecraft
   version must be `X.Y.Z`
3. Builds the mod with `./gradlew build`
4. Publishes to Modrinth via `./gradlew modrinth` (requires `MODRINTH_TOKEN`)
5. Syncs the release body to Modrinth via `./gradlew modrinthSyncBody`
6. Creates a GitHub Release with auto-generated notes and attaches the built jar

Prerelease detection: if the mod version contains a `-` (e.g. `1.0.0-beta.1`), the GitHub release is marked as
prerelease.

### Renovate Config Validator (`renovate-config-validator.yml`)

Runs when `renovate.json` is changed. Validates the Renovate configuration using the official validator action to catch
syntax errors before they break dependency updates.

## Renovate

Renovate manages dependency updates automatically. Configuration is in `renovate.json`:

- **Base config**: `config:recommended` with the dependency dashboard disabled
- **Ignored**: `com.mojang:minecraft` is ignored from standard updates (handled via custom manager)
- **Minecraft tracking**: A custom manager reads the `minecraft` version from `gradle/libs.versions.toml` and tracks
  releases via Mojang's version manifest. Minecraft update PRs are created as draft PRs with labels
  `minecraft-update` and `manual-action-required` — they require manual review before merging
- **Custom registries**:
    - `net.fabricmc.*` packages → `maven.fabricmc.net`
    - `com.terraformersmc:modmenu` → `maven.terraformersmc.com`
    - `me.shedaniel.cloth:cloth-config-fabric` → `maven.shedaniel.me`
- **Auto-merge policy**: Fabric API updates are labeled `dependencies` and can be merged directly. Minecraft updates
  always require manual intervention.

## Keeping This File Updated

**This file must be kept current with the codebase.** After making significant changes to the project, update the
relevant sections:
