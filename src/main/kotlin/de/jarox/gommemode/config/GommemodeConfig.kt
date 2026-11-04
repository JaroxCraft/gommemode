package de.jarox.gommemode.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
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
    var loopIntervalMs: Int = 1000
    var messageDisplay: MessageDisplay = MessageDisplay.NONE

    // Sound
    var soundEnabled: Boolean = true
    var volumePercent: Int = 100
    var pitchPercent: Int = 100
    var soundSource: SoundSource = SoundSource.MASTER
    var positionalSound: Boolean = true
    var stopMusic: Boolean = true

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
    var offArmPose: HumanoidModel.ArmPose = HumanoidModel.ArmPose.ITEM
    var lookAtPlayer: Boolean = true
    var lookTarget: LookTarget = LookTarget.EYES
    var scale: Double = 1.0
    var shadowRadius: Double = 0.5
    var showNameTag: Boolean = false
    var nameTagText: String = ""
    var glowing: Boolean = false

    @field:JsonAdapter(HexColorAdapter::class)
    var glowColor: Int = 0xFFFFFF

    /** Clamps every value into its supported range and replaces invalid `null`s from Gson. */
    @Suppress("SENSELESS_COMPARISON") // Gson can bypass Kotlin null-safety for missing/invalid enum values
    fun sanitize(): GommemodeConfig {
        val defaults = GommemodeConfig()
        toggleCooldownMs = toggleCooldownMs.coerceIn(MIN_COOLDOWN_MS, MAX_COOLDOWN_MS)
        if (songEndBehavior == null) songEndBehavior = defaults.songEndBehavior
        loopIntervalMs = loopIntervalMs.coerceIn(MIN_LOOP_INTERVAL_MS, MAX_LOOP_INTERVAL_MS)
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
        if (offArmPose == null) offArmPose = defaults.offArmPose
        if (lookTarget == null) lookTarget = defaults.lookTarget
        scale = scale.finiteOr(defaults.scale).coerceIn(MIN_SCALE, MAX_SCALE)
        shadowRadius = shadowRadius.finiteOr(defaults.shadowRadius).coerceIn(MIN_SHADOW_RADIUS, MAX_SHADOW_RADIUS)
        if (nameTagText == null) nameTagText = defaults.nameTagText
        glowColor = glowColor and 0xFFFFFF
        return this
    }

    /** This value, or [fallback] if it is NaN or infinite. */
    private fun Double.finiteOr(fallback: Double): Double = if (isFinite()) this else fallback

    companion object {
        const val MIN_COOLDOWN_MS = 0
        const val MAX_COOLDOWN_MS = 10_000

        // Lower bound keeps a song that cannot play from restarting every tick.
        const val MIN_LOOP_INTERVAL_MS = 100
        const val MAX_LOOP_INTERVAL_MS = 60_000
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
 * Stores an RGB color as a readable `#RRGGBB` string in the config file.
 *
 * Plain numbers are still read, so files written before this format keep working.
 * An unparsable value falls back to white instead of discarding the whole config.
 */
internal class HexColorAdapter : TypeAdapter<Int>() {
    /** Writes the color as `#RRGGBB`. */
    override fun write(
        out: JsonWriter,
        value: Int,
    ) {
        out.value("#%06X".format(value and 0xFFFFFF))
    }

    /** Reads `#RRGGBB`, `RRGGBB` or a plain number; anything else becomes white. */
    override fun read(reader: JsonReader): Int {
        val token = reader.peek()
        if (token == JsonToken.NULL) {
            reader.nextNull()
            return WHITE
        }
        if (token != JsonToken.NUMBER && token != JsonToken.STRING) {
            reader.skipValue()
            Gommemode.LOGGER.warn("Invalid color of type {} in config, using white", token)
            return WHITE
        }

        // Read numbers as text too: nextInt() throws for values like 1.5 or 4294967295.
        val text = reader.nextString().trim()
        val color = if (token == JsonToken.NUMBER) text.toIntOrNull() else text.removePrefix("#").toIntOrNull(16)
        return color ?: run {
            Gommemode.LOGGER.warn("Invalid color '{}' in config, using white", text)
            WHITE
        }
    }

    private companion object {
        const val WHITE = 0xFFFFFF
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
     * so a failed write (e.g. a full disk) leaves the previous file intact. Each call uses its
     * own temporary file, so two game instances sharing the config directory cannot mix their writes.
     */
    fun save() {
        var tempPath: Path? = null
        try {
            Files.createDirectories(path.parent)
            tempPath = Files.createTempFile(path.parent, "${path.fileName}.", ".tmp")
            Files.newBufferedWriter(tempPath).use { writer -> gson.toJson(config, writer) }
            try {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            Gommemode.LOGGER.error("Failed to write config file {}", path, e)
            tempPath?.let { runCatching { Files.deleteIfExists(it) } }
        }
    }
}
