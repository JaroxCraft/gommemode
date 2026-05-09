# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Project

Gommemode is a client-side Fabric mod for Minecraft 26.1.2, written in Kotlin. When activated, it spawns a GommeHD entity that looks at the player and plays the Gomme hymn. Activated via keybind (default: G) or automatically when the song stops playing.

## Build & Run

```bash
./gradlew build          # produces jar in build/libs/
./gradlew runClient      # launch Minecraft with the mod loaded
```

There are no tests. The only way to verify correctness is to run the client.

## Key Constraints

**Minecraft 26.1.2 is fully unobfuscated** — Mojang ships real class/method names. Consequences:
- Plugin ID is `net.fabricmc.fabric-loom` (not `fabric-loom` or `remap` variants)
- No `mappings()` declaration in `build.gradle.kts` at all
- No `modImplementation` / `modApi` — use plain `implementation()` for all deps
- `fabric-language-kotlin` is declared as a mod dependency (not bundled); it must be present at runtime
- Java 25 is required (`sourceCompatibility`, `jvmTarget`, Gradle JVM)
- Gradle 9.4.1+ required (Loom 1.16 requires Gradle 9.x)

## MC 26.1.2 API Names (Mojang Mapping)

Key renames from older Yarn-mapped code that are easy to get wrong:

| Old (Yarn / pre-26.1)                             | Current (Mojang / 26.1)                                                                            |
|---------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `ResourceLocation`                                | `Identifier` (`net.minecraft.resources.Identifier`)                                                |
| `KeyBindingHelper`                                | `KeyMappingHelper` (`net.fabricmc.fabric.api.client.keymapping.v1`)                                |
| `KeyBinding` category as `String`                 | `KeyMapping.Category` — create with `KeyMapping.Category.register(Identifier)`                     |
| `LivingEntityRenderer<T, M>`                      | `LivingEntityRenderer<T, S : LivingEntityRenderState, M : EntityModel<? super S>>` — 3 type params |
| `PlayerModel<T>` (generic)                        | `PlayerModel` (no type param, extends `HumanoidModel<AvatarRenderState>`)                          |
| `getTextureLocation(entity: T)`                   | `getTextureL ocation(state: S): Identifier` — takes render state                                   |
| `EntityType.Builder.build()`                      | `.build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier))`                                   |
| `SoundEvent.location` (field)                     | `SoundEvent.location()` (method)                                                                   |
| `SoundInstance` at `client.sounds`                | `net.minecraft.client.resources.sounds.SoundInstance`                                              |
| `PlayerModel` at `client.model`                   | `net.minecraft.client.model.player.PlayerModel`                                                    |
| `LivingEntityFeatureRendererRegistrationCallback` | `LivingEntityRenderLayerRegistrationCallback`                                                      |
| `EntityModelLayerRegistry`                        | `ModelLayerRegistry`                                                                               |

`createRenderState()` is abstract in `EntityRenderer` and **must** be overridden in custom renderers.

## Architecture

All logic flows through two classes:

**`Gommemode.kt`** — mod entry point (implements both `ModInitializer` and `ClientModInitializer`). The companion object holds static references to the entity type and sound event (initialized at class load). `onInitialize` registers the entity type and attributes; `onInitializeClient` registers the renderer, key binding, and tick event listener.

**`GommemodeManager.kt`** — singleton object that owns activation state. `toggleActive()` has a 1-second cooldown. On activate: casts a ray (`player.pick`) to find spawn position, creates `SimpleSoundInstance` with positional audio, spawns the `GommeEntity`, and calls `spawnSphere`. On deactivate: stops the sound and removes the entity. The tick event in `Gommemode` also calls `toggleActive` when the song finishes playing while still active.

**`GommeEntity.kt`** — minimal `LivingEntity` subclass. Always holds a diamond sword (`getItemBySlot`). `baseTick` makes it rotate to face the local player each tick via `lookAt`.

**`GommeEntityRenderer.kt`** — uses `PlayerModel` + `AvatarRenderState` to render the entity with a custom skin texture (`textures/entity/gommehd.png`). Overrides `createRenderState()` to return a plain `AvatarRenderState()` — player-specific fields (cape, skin type) are left at defaults.

## Agent Behavior

**Never push changes to the remote repository unless explicitly asked to do so.** Always ask for confirmation before running `git push`, `git push --force`, or any other command that mutates the remote history. This applies even when the user previously gave permission in an earlier conversation.

## Commit Style

**ALWAYS use conventional commits** for every commit you create. This includes commits you author yourself and commits you rewrite during merge/rebase operations.

Format: `<type>(<scope>): <description>`

Common types for this project:
- `feat:` — new feature or behavior
- `fix:` — bug fix
- `ci:` — CI/CD changes
- `docs:` — documentation updates
- `chore(deps):` — dependency updates (e.g., Gradle, Fabric, Kotlin)
- `refactor:` — code restructuring without behavior change
- `build:` — build system or tooling changes

**Never** leave a non-conventional commit message on `master`, even if it comes from an external PR or bot. Amend it during merge/rebase if necessary.

## Version Management

All dependency versions live in `gradle/libs.versions.toml`. The artifact version is composed as `mod_version+minecraft_version` in `build.gradle.kts`. A GitHub Actions workflow creates a release on every `v*` tag, validating that the tag matches `mod_version+minecraft_version` from `gradle.properties`.
