package de.jarox.gommemode.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import de.jarox.gommemode.Gommemode
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.model.HumanoidModel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.HumanoidArm
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** What happens when the Gomme hymn finishes while Gommemode is active. */
enum class SongEndBehavior {
    /** Turn Gommemode off and remove the entity. */
    DEACTIVATE,

    /** Start the hymn again from the beginning. */
    LOOP,

    /** Keep the entity around in silence until toggled off. */
    KEEP_ENTITY,
}

/** Where (if anywhere) the activation/deactivation feedback message is shown. */
enum class MessageDisplay {
    NONE,
    ACTION_BAR,
    CHAT,
}

/** Where the entity spawns when Gommemode is activated. */
enum class SpawnLocation {
    /** Where the crosshair points, up to the configured spawn distance. */
    CROSSHAIR,

    /** Directly at the player's feet. */
    PLAYER,
}

/** Which part of the player the entity looks at. */
enum class LookTarget {
    FEET,
    EYES,
}

/**
 * All user-configurable settings of the mod.
 *
 * Persisted as JSON in `config/gommemode.json`. Every property has a default, so missing or
 * unknown keys in the file are tolerated. Values are clamped into their valid ranges by [sanitize].
 */
class GommemodeConfig {
    // General
    var enabled: Boolean = true
    var toggleCooldownMs: Int = 1000
    var songEndBehavior: SongEndBehavior = SongEndBehavior.DEACTIVATE
    var messageDisplay: MessageDisplay = MessageDisplay.NONE

    // Sound
    var soundEnabled: Boolean = true
    var volumePercent: Int = 100
    var pitchPercent: Int = 100
    var soundSource: SoundSource = SoundSource.MASTER
    var positionalSound: Boolean = true

    // Spawning
    var spawnLocation: SpawnLocation = SpawnLocation.CROSSHAIR
    var spawnDistance: Double = 5.0
    var spawnHitsFluids: Boolean = false

    // Particles
    var particlesEnabled: Boolean = true
    var particleType: String = "minecraft:firework"
    var particleRadius: Double = 2.0
    var particleStepSize: Double = 0.2

    // Entity
    var mainHandItem: String = "minecraft:diamond_sword"
    var offHandItem: String = ""
    var mainArm: HumanoidArm = HumanoidArm.RIGHT
    var mainArmPose: HumanoidModel.ArmPose = HumanoidModel.ArmPose.ITEM
    var lookAtPlayer: Boolean = true
    var lookTarget: LookTarget = LookTarget.FEET
    var scale: Double = 1.0
    var shadowRadius: Double = 0.5
    var showNameTag: Boolean = false
    var nameTagText: String = ""
    var glowing: Boolean = false
    var glowColor: Int = 0xFFFFFF

    /** Clamps every value into its supported range and replaces invalid `null`s from Gson. */
    @Suppress("SENSELESS_COMPARISON") // Gson can bypass Kotlin null-safety for missing/invalid enum values
    fun sanitize(): GommemodeConfig {
        val defaults = GommemodeConfig()
        toggleCooldownMs = toggleCooldownMs.coerceIn(MIN_COOLDOWN_MS, MAX_COOLDOWN_MS)
        if (songEndBehavior == null) songEndBehavior = defaults.songEndBehavior
        if (messageDisplay == null) messageDisplay = defaults.messageDisplay

        volumePercent = volumePercent.coerceIn(MIN_VOLUME_PERCENT, MAX_VOLUME_PERCENT)
        pitchPercent = pitchPercent.coerceIn(MIN_PITCH_PERCENT, MAX_PITCH_PERCENT)
        if (soundSource == null) soundSource = defaults.soundSource

        if (spawnLocation == null) spawnLocation = defaults.spawnLocation
        spawnDistance = spawnDistance.finiteOr(defaults.spawnDistance).coerceIn(MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE)

        if (particleType == null) particleType = defaults.particleType
        particleRadius = particleRadius.finiteOr(defaults.particleRadius).coerceIn(MIN_PARTICLE_RADIUS, MAX_PARTICLE_RADIUS)
        particleStepSize = particleStepSize.finiteOr(defaults.particleStepSize).coerceIn(MIN_PARTICLE_STEP, MAX_PARTICLE_STEP)

        if (mainHandItem == null) mainHandItem = defaults.mainHandItem
        if (offHandItem == null) offHandItem = defaults.offHandItem
        if (mainArm == null) mainArm = defaults.mainArm
        if (mainArmPose == null) mainArmPose = defaults.mainArmPose
        if (lookTarget == null) lookTarget = defaults.lookTarget
        scale = scale.finiteOr(defaults.scale).coerceIn(MIN_SCALE, MAX_SCALE)
        shadowRadius = shadowRadius.finiteOr(defaults.shadowRadius).coerceIn(MIN_SHADOW_RADIUS, MAX_SHADOW_RADIUS)
        if (nameTagText == null) nameTagText = defaults.nameTagText
        glowColor = glowColor and 0xFFFFFF
        return this
    }

    private fun Double.finiteOr(fallback: Double): Double = if (isFinite()) this else fallback

    companion object {
        const val MIN_COOLDOWN_MS = 0
        const val MAX_COOLDOWN_MS = 10_000
        const val MIN_VOLUME_PERCENT = 0
        const val MAX_VOLUME_PERCENT = 100
        const val MIN_PITCH_PERCENT = 50
        const val MAX_PITCH_PERCENT = 200
        const val MIN_SPAWN_DISTANCE = 1.0
        const val MAX_SPAWN_DISTANCE = 64.0
        const val MIN_PARTICLE_RADIUS = 0.1
        const val MAX_PARTICLE_RADIUS = 16.0
        const val MIN_PARTICLE_STEP = 0.05
        const val MAX_PARTICLE_STEP = 1.0

        // Bounds of the vanilla `minecraft:scale` attribute.
        const val MIN_SCALE = 0.0625
        const val MAX_SCALE = 16.0
        const val MIN_SHADOW_RADIUS = 0.0
        const val MAX_SHADOW_RADIUS = 4.0
    }
}

/**
 * Holds the active [GommemodeConfig] and handles loading/saving it from/to disk.
 */
object ConfigManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val path: Path = FabricLoader.getInstance().configDir.resolve("${Gommemode.MOD_ID}.json")

    /** The currently active configuration. */
    var config: GommemodeConfig = GommemodeConfig()
        private set

    /** Loads the configuration from disk, writing defaults if no file exists. */
    fun load() {
        var readFailed = false
        config =
            try {
                if (Files.exists(path)) {
                    Files.newBufferedReader(path).use { reader ->
                        gson.fromJson(reader, GommemodeConfig::class.java)
                    } ?: GommemodeConfig()
                } else {
                    GommemodeConfig()
                }
            } catch (e: IOException) {
                Gommemode.LOGGER.error("Failed to read config file {}, using defaults", path, e)
                readFailed = true
                GommemodeConfig()
            } catch (e: JsonParseException) {
                Gommemode.LOGGER.error("Config file {} is malformed, using defaults", path, e)
                readFailed = true
                GommemodeConfig()
            }.sanitize()
        if (!readFailed) save()
    }

    /**
     * Writes the current configuration to disk.
     *
     * The config is written to a temporary file first and then moved over the real one,
     * so a failed write (e.g. a full disk) leaves the previous file intact.
     */
    fun save() {
        val tempPath = path.resolveSibling("${path.fileName}.tmp")
        try {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(tempPath).use { writer -> gson.toJson(config, writer) }
            try {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            Gommemode.LOGGER.error("Failed to write config file {}", path, e)
            runCatching { Files.deleteIfExists(tempPath) }
        }
    }
}
