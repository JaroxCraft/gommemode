package de.jarox.gommemode

import de.jarox.gommemode.config.ConfigManager
import de.jarox.gommemode.config.GommemodeConfig
import de.jarox.gommemode.config.MessageDisplay
import de.jarox.gommemode.config.SongEndBehavior
import de.jarox.gommemode.config.SpawnLocation
import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.util.spawnSphere
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/**
 * Manages the activation state of the Gommemode, including sound playback,
 * entity spawning, and particle effects.
 *
 * This singleton coordinates the lifecycle of the Gomme entity and its
 * associated sound effects. All behavior is driven by [ConfigManager.config].
 */
object GommemodeManager {
    private val minecraft: Minecraft = Minecraft.getInstance()
    private var activeSound: SoundInstance? = null
    private var gommeEntity: GommeEntity? = null
    private var spawnLocation: Vec3? = null
    private var soundEnabledWhenActive: Boolean = false
    private var playingSettings: SoundSettings? = null
    private var lastToggleTime: Long = 0L
    private var songStartTime: Long = 0L

    // Client-only entity: negative so it can never collide with a server-assigned ID.
    private const val GOMME_ENTITY_ID: Int = -1_000_000

    // Minimum time between two starts of a looping song.
    private const val MIN_LOOP_INTERVAL_MS: Long = 1000L

    var isActive: Boolean = false
        private set

    /**
     * Resets all state when joining or leaving a world.
     * Ensures no stale references or cooldown times carry over to a new world.
     */
    fun reset() {
        Gommemode.LOGGER.debug("Resetting Gommemode state")
        stopSongAndRemoveEntity()
        isActive = false
        lastToggleTime = 0L
    }

    /**
     * Checks whether the background song is currently playing.
     *
     * @return true if the sound is active, false otherwise
     */
    fun isPlaying(): Boolean {
        val sound = activeSound
        return sound != null && minecraft.soundManager.isActive(sound)
    }

    /**
     * Runs once per client tick: keeps the game music quiet while the song plays and
     * handles the configured song-end behavior.
     */
    fun tick(player: LocalPlayer) {
        if (ConfigManager.config.pauseMusic && isPlaying()) {
            // Stops the current track and pushes the next one back, so no track starts during the song.
            // The music returns on its own after the usual delay once the song is over.
            minecraft.musicManager.stopPlaying()
        }

        if (!isActive || activeSound == null || isPlaying()) return

        // The song has finished while Gommemode is active.
        when (ConfigManager.config.songEndBehavior) {
            SongEndBehavior.DEACTIVATE -> {
                deactivate(player)
                lastToggleTime = System.currentTimeMillis()
            }

            SongEndBehavior.LOOP -> {
                // If the sound cannot play at all (e.g. no audio device), it "ends" right away.
                // Wait between restarts instead of starting it again every tick.
                if (System.currentTimeMillis() >= songStartTime + MIN_LOOP_INTERVAL_MS) {
                    spawnLocation?.let(::playSong)
                }
            }

            SongEndBehavior.KEEP_ENTITY -> {
                stopSong()
            }
        }
    }

    /**
     * Toggles the mod activation state with a cooldown to prevent rapid toggling.
     *
     * @param player The local player initiating the toggle
     * @param level The client world
     */
    fun toggleActive(
        player: LocalPlayer,
        level: ClientLevel,
    ) {
        val config = ConfigManager.config
        if (!config.enabled) {
            Gommemode.LOGGER.debug("Gommemode is disabled in the config, ignoring toggle")
            return
        }
        if (System.currentTimeMillis() < lastToggleTime + config.toggleCooldownMs) {
            Gommemode.LOGGER.debug("Toggle on cooldown, ignoring")
            return
        }

        if (isActive) {
            deactivate(player)
        } else {
            activate(player, level)
        }
        lastToggleTime = System.currentTimeMillis()
    }

    /**
     * Applies config changes that affect an already running Gommemode.
     * Called after the config screen has been saved.
     */
    fun onConfigChanged() {
        val config = ConfigManager.config
        if (!config.enabled) {
            if (isActive) {
                Gommemode.LOGGER.info("Gommemode disabled in config, deactivating")
                isActive = false
                stopSongAndRemoveEntity()
            }
            return
        }
        if (isActive) {
            when {
                !config.soundEnabled -> stopSong()
                !soundEnabledWhenActive -> spawnLocation?.let(::playSong)
                activeSound != null && playingSettings != SoundSettings.of(config) -> {
                    // A playing sound instance cannot be changed, so start it again with the new settings.
                    stopSong()
                    spawnLocation?.let(::playSong)
                }
            }
            soundEnabledWhenActive = config.soundEnabled
        }
    }

    /** Turns Gommemode on: plays the song, spawns the entity and shows the feedback message. */
    private fun activate(
        player: LocalPlayer,
        level: ClientLevel,
    ) {
        if (isActive) return
        Gommemode.LOGGER.info("Activating Gommemode")
        isActive = true
        soundEnabledWhenActive = ConfigManager.config.soundEnabled
        startSongAndSpawnEntity(player, level)
        sendFeedback(player, "gommemode.started")
    }

    /** Turns Gommemode off: stops the song, removes the entity and shows the feedback message. */
    private fun deactivate(player: LocalPlayer) {
        if (!isActive) return
        Gommemode.LOGGER.info("Deactivating Gommemode")
        isActive = false
        stopSongAndRemoveEntity()
        sendFeedback(player, "gommemode.stopped")
    }

    /** Shows the toggle message where the config says (nowhere, action bar or chat). */
    private fun sendFeedback(
        player: LocalPlayer,
        stateKey: String,
    ) {
        val message = Component.translatable("gommemode.change").append(Component.translatable(stateKey))
        when (ConfigManager.config.messageDisplay) {
            MessageDisplay.NONE -> {}
            MessageDisplay.ACTION_BAR -> player.sendOverlayMessage(message)
            MessageDisplay.CHAT -> player.sendSystemMessage(message)
        }
    }

    /** Picks the spawn location, then starts the song, spawns the particles and adds the entity. */
    private fun startSongAndSpawnEntity(
        player: LocalPlayer,
        level: ClientLevel,
    ) {
        val config = ConfigManager.config
        val location =
            when (config.spawnLocation) {
                SpawnLocation.CROSSHAIR -> player.pick(config.spawnDistance, 0f, config.spawnHitsFluids).location
                SpawnLocation.PLAYER -> player.position()
            }
        spawnLocation = location

        Gommemode.LOGGER.info("Starting song and spawning entity at ({}, {}, {})", location.x, location.y, location.z)

        playSong(location)

        if (config.particlesEnabled) {
            spawnSphere(
                level = level,
                center = location,
                radius = config.particleRadius,
                particleType = resolveParticle(config.particleType),
                stepSize = config.particleStepSize,
            )
        }

        val newEntity = GommeEntity(Gommemode.GOMME_ENTITY_TYPE, level)
        newEntity.id = GOMME_ENTITY_ID
        newEntity.setPos(location.x, location.y, location.z)

        level.addEntity(newEntity)
        gommeEntity = newEntity
        Gommemode.LOGGER.debug("Entity spawned with ID {}", newEntity.id)
    }

    /** Starts the song at [location] with the current sound settings, unless sound is disabled. */
    private fun playSong(location: Vec3) {
        val config = ConfigManager.config
        if (!config.soundEnabled) {
            activeSound = null
            return
        }

        val positional = config.positionalSound
        // canStartSilent: keep the song "running" even at zero volume, so song-end detection
        // still works when the volume or the sound category is muted.
        val sound =
            object : SimpleSoundInstance(
                Gommemode.GOMMEMODE_SONG_IDENTIFIER,
                config.soundSource,
                config.volumePercent / 100f,
                config.pitchPercent / 100f,
                RandomSource.create(),
                false,
                0,
                if (positional) SoundInstance.Attenuation.LINEAR else SoundInstance.Attenuation.NONE,
                if (positional) location.x else 0.0,
                if (positional) location.y else 0.0,
                if (positional) location.z else 0.0,
                !positional,
            ) {
                override fun canStartSilent(): Boolean = true
            }
        activeSound = sound
        playingSettings = SoundSettings.of(config)
        songStartTime = System.currentTimeMillis()
        minecraft.soundManager.play(sound)
    }

    /** Stops the song and removes the entity from the world. */
    private fun stopSongAndRemoveEntity() {
        Gommemode.LOGGER.debug("Stopping song and removing entity")
        stopSong()

        gommeEntity?.let { entity ->
            entity.remove(Entity.RemovalReason.DISCARDED)
            gommeEntity = null
        }
        spawnLocation = null
    }

    /** Stops the song, if one is playing. */
    private fun stopSong() {
        activeSound?.let(minecraft.soundManager::stop)
        activeSound = null
        playingSettings = null
    }

    /**
     * Resolves a particle id to a spawnable particle, falling back to fireworks for unknown
     * ids or particle types that need extra parameters.
     */
    fun resolveParticle(id: String): ParticleOptions =
        Identifier
            .tryParse(id.trim())
            ?.let { BuiltInRegistries.PARTICLE_TYPE.getOptional(it).orElse(null) } as? SimpleParticleType
            ?: ParticleTypes.FIREWORK
}

/** The config values a song instance was started with; used to detect changes while it plays. */
private data class SoundSettings(
    val source: SoundSource,
    val volumePercent: Int,
    val pitchPercent: Int,
    val positional: Boolean,
) {
    companion object {
        /** Captures the sound settings of [config]. */
        fun of(config: GommemodeConfig) =
            SoundSettings(config.soundSource, config.volumePercent, config.pitchPercent, config.positionalSound)
    }
}
