package de.jarox.gommemode

import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.util.spawnSphere
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity

/**
 * Manages the activation state of the Gommemode, including sound playback,
 * entity spawning, and particle effects.
 *
 * This singleton coordinates the lifecycle of the Gomme entity and its
 * associated sound effects.
 */
object GommemodeManager {
    private val minecraft: Minecraft = Minecraft.getInstance()
    private var activeSound: SoundInstance? = null
    private var gommeEntity: GommeEntity? = null
    private var lastToggleTick: Long = 0L

    private const val COOLDOWN_TICKS: Long = 20L // 1 second at 20 ticks/second
    private const val SPAWN_RADIUS: Double = 2.0
    private const val PARTICLE_DENSITY: Double = 0.2
    private const val LOOK_DISTANCE: Double = 5.0

    var isActive: Boolean = false
        private set

    /**
     * Resets all state when disconnecting from a world.
     * Ensures no stale references or cooldown times carry over to a new world.
     */
    fun reset() {
        stopSongAndRemoveEntity()
        isActive = false
        lastToggleTick = 0L
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
     * Toggles the mod activation state with a cooldown to prevent rapid toggling.
     *
     * @param player The local player initiating the toggle
     * @param level The client world
     */
    fun toggleActive(
        player: LocalPlayer,
        level: ClientLevel,
    ) {
        if (level.gameTime < lastToggleTick + COOLDOWN_TICKS) return

        if (isActive) {
            deactivate()
        } else {
            activate(player, level)
        }
        lastToggleTick = level.gameTime
    }

    private fun activate(
        player: LocalPlayer,
        level: ClientLevel,
    ) {
        if (isActive) return
        isActive = true
        startSongAndSpawnEntity(player, level)
    }

    private fun deactivate() {
        if (!isActive) return
        isActive = false
        stopSongAndRemoveEntity()
    }

    private fun startSongAndSpawnEntity(
        player: LocalPlayer,
        level: ClientLevel,
    ) {
        val hitResult = player.pick(LOOK_DISTANCE, 0f, false)
        val spawnLocation = hitResult.location

        activeSound =
            SimpleSoundInstance(
                Gommemode.GOMMEMODE_SOUND_EVENT,
                SoundSource.MASTER,
                1f,
                1f,
                RandomSource.create(),
                spawnLocation.x,
                spawnLocation.y,
                spawnLocation.z,
            )

        activeSound?.let { sound ->
            minecraft.soundManager.play(sound)
        }

        spawnSphere(
            level = level,
            center = spawnLocation,
            radius = SPAWN_RADIUS,
            particleType = ParticleTypes.FIREWORK,
            stepSize = PARTICLE_DENSITY,
        )

        val newEntity = GommeEntity(Gommemode.GOMME_ENTITY_TYPE, level)
        newEntity.setPos(spawnLocation.x, spawnLocation.y, spawnLocation.z)

        level.addEntity(newEntity)
        gommeEntity = newEntity
    }

    private fun stopSongAndRemoveEntity() {
        activeSound?.let { sound ->
            minecraft.soundManager.stop(sound)
            activeSound = null
        }

        gommeEntity?.let { entity ->
            entity.remove(Entity.RemovalReason.DISCARDED)
            gommeEntity = null
        }
    }
}
