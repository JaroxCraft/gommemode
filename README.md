![Gommemode Banner](https://raw.githubusercontent.com/JaroxCraft/gommemode/master/.github/assets/banner.jpg)

[![fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/compact/supported/fabric_vector.svg)](https://modrinth.com/mod/gommemode)
[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/mod/gommemode)
[![github](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/github_vector.svg)](https://github.com/JaroxCraft/gommemode/releases/latest)
[![discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/compact/social/discord-singular_vector.svg)](https://discordapp.com/users/780518380733988874)

## Overview

Gommemode is a fun client-side Fabric mod for Minecraft that brings the legendary GommeHD into your world. Spawn a fully rendered GommeHD entity that follows and looks at you, while the iconic Gomme hymn (produced by [Lukas, der Rapper](https://www.instagram.com/luzudemkas/)) plays in the background. Whether you're feeling nostalgic or just want some company while mining, Gommemode delivers the ultimate GommeHD experience.

## Features

- **GommeHD Entity**: Spawns a custom-rendered GommeHD entity with a unique skin texture that rotates to face you every tick
- **Gomme Hymn**: Plays the iconic Gomme hymn as positional audio when the entity spawns
- **Firework Effect**: A burst of firework particles surrounds the spawn point for a dramatic entrance
- **Toggle Keybind**: Press `G` (configurable) to toggle Gommemode on and off
- **Auto-Reactivation**: The mod automatically reactivates when the song finishes playing while still active
- **Smart Placement**: Raycasts 5 blocks ahead of the player to determine the optimal spawn position
- **Config Integration**: Cloth Config API + ModMenu support for in-game configuration
- **Custom Rendering**: Uses Minecraft's player model system with a custom GommeHD skin (`textures/entity/gommehd.png`)

## How to Use

1. **Install the mod** along with its required dependencies (see below)
2. **Launch Minecraft** with the Fabric loader
3. **Press `G`** (default keybind) to toggle Gommemode on
4. The GommeHD entity will spawn 5 blocks in front of you with a firework particle effect
5. The Gomme hymn will start playing automatically
6. **Press `G` again** to deactivate and remove the entity
7. If the song ends while Gommemode is still active, it will automatically toggle off

### Placement Mechanics

The entity spawns at the location your crosshair is pointing at, up to 5 blocks away. Make sure you're looking at a valid position before toggling.

## Dependencies

### Required

- [Fabric API](https://modrinth.com/mod/fabric-api) — Core Fabric hooks and utilities
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) — Kotlin runtime for Fabric mods

### Optional

- [ModMenu](https://modrinth.com/mod/modmenu) — Enables in-game mod configuration screen
- [Cloth Config API](https://modrinth.com/mod/cloth-config) — Configuration framework used by Gommemode

## FAQ

**Q: Is this mod server-side compatible?**
A: Gommemode is a client-side only mod. It does not require server installation, but you need Fabric API and Fabric Language Kotlin installed on your client.

**Q: Can I change the keybind?**
A: Yes! Open Controls in the Minecraft settings and look for "Gommemode" under the keybinding categories.

**Q: Why isn't the sound playing?**
A: Make sure your Master volume is not muted. The sound plays at full volume by default.

**Q: The entity isn't spawning!**
A: Ensure you're looking at a valid position within 5 blocks. The entity needs a valid spawn location to appear.

**Q: Can I use this on multiplayer servers?**
A: Yes, since it's client-side only. Other players won't see the GommeHD entity or hear the sound unless they also have the mod installed.

## Links

- [GitHub Repository](https://github.com/JaroxCraft/gommemode)
- [Modrinth Page](https://modrinth.com/mod/gommemode)
- [Discord](https://discordapp.com/users/780518380733988874)
- [Gomme Hymn on YouTube](https://www.youtube.com/watch?v=2h1BJCxtNes)

<br>
