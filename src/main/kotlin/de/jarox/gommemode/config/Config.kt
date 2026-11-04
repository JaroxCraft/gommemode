package de.jarox.gommemode.config

import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.GommemodeManager
import de.jarox.gommemode.entity.GommeEntity
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.model.HumanoidModel
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.Items
import java.util.Locale
import java.util.Optional

/**
 * Configuration screen factory for the Gommemode mod.
 *
 * Builds a ClothConfig screen exposing every option of [GommemodeConfig]. Saving writes the
 * config to disk and applies changes to a running Gommemode where possible.
 */
object Config {
    private const val KEY_PREFIX = "config.${Gommemode.MOD_ID}"

    /**
     * Builds and returns the configuration screen.
     *
     * @param parentScreen The screen to return to when pressing Escape
     * @return The constructed configuration screen
     */
    fun buildScreen(parentScreen: Screen?): Screen {
        Gommemode.LOGGER.debug("Building Gommemode config screen")
        val builder =
            ConfigBuilder
                .create()
                .setParentScreen(parentScreen)
                .setTitle(Component.translatable("$KEY_PREFIX.title"))
                .setSavingRunnable {
                    ConfigManager.config.sanitize()
                    ConfigManager.save()
                    GommemodeManager.onConfigChanged()
                }

        val config = ConfigManager.config
        val defaults = GommemodeConfig()
        val entries = builder.entryBuilder()

        addGeneralOptions(builder.category("general"), entries, config, defaults)
        addSoundOptions(builder.category("sound"), entries, config, defaults)
        addSpawnOptions(builder.category("spawn"), entries, config, defaults)
        addParticleOptions(builder.category("particles"), entries, config, defaults)
        addEntityOptions(builder.category("entity"), entries, config, defaults)

        return builder.build()
    }

    /** Adds the enable switch, toggle key, cooldown, song-end behavior and message options. */
    private fun addGeneralOptions(
        category: ConfigCategory,
        entries: ConfigEntryBuilder,
        config: GommemodeConfig,
        defaults: GommemodeConfig,
    ) {
        category.addEntry(
            entries
                .startBooleanToggle(label("enabled"), config.enabled)
                .setDefaultValue(defaults.enabled)
                .setTooltip(tooltip("enabled"))
                .setSaveConsumer { config.enabled = it }
                .build(),
        )

        Gommemode.toggleKeyMapping?.let { keyMapping -> category.addEntry(keyEntry(entries, keyMapping)) }

        category.addEntry(
            entries
                .startIntField(label("toggleCooldownMs"), config.toggleCooldownMs)
                .setDefaultValue(defaults.toggleCooldownMs)
                .setMin(GommemodeConfig.MIN_COOLDOWN_MS)
                .setMax(GommemodeConfig.MAX_COOLDOWN_MS)
                .setTooltip(tooltip("toggleCooldownMs"))
                .setSaveConsumer { config.toggleCooldownMs = it }
                .build(),
        )
        category.addEntry(
            entries
                .startEnumSelector(label("songEndBehavior"), SongEndBehavior::class.java, config.songEndBehavior)
                .setDefaultValue(defaults.songEndBehavior)
                .setEnumNameProvider(::enumName)
                .setTooltip(tooltip("songEndBehavior"))
                .setSaveConsumer { config.songEndBehavior = it }
                .build(),
        )
        category.addEntry(
            entries
                .startEnumSelector(label("messageDisplay"), MessageDisplay::class.java, config.messageDisplay)
                .setDefaultValue(defaults.messageDisplay)
                .setEnumNameProvider(::enumName)
                .setTooltip(tooltip("messageDisplay"))
                .setSaveConsumer { config.messageDisplay = it }
                .build(),
        )
    }

    /** Builds the toggle key field; saving it rebinds the vanilla key mapping and saves the options. */
    private fun keyEntry(
        entries: ConfigEntryBuilder,
        keyMapping: KeyMapping,
    ) = entries
        .startKeyCodeField(label("toggleKey"), keyMapping.boundKey())
        .setDefaultValue(keyMapping.defaultKey)
        // Vanilla key mappings have no modifier support, so Ctrl/Shift/Alt combinations would be dropped silently.
        .setAllowModifiers(false)
        .setTooltip(tooltip("toggleKey"))
        .setKeySaveConsumer { key ->
            if (key != keyMapping.boundKey()) {
                val minecraft = Minecraft.getInstance()
                keyMapping.setKey(key)
                KeyMapping.resetMapping()
                minecraft.options.save()
            }
        }.build()

    /** The key this mapping is currently bound to. */
    private fun KeyMapping.boundKey() = KeyMappingHelper.getBoundKeyOf(this)

    /** Adds the song, volume, pitch, sound category and positional sound options. */
    private fun addSoundOptions(
        category: ConfigCategory,
        entries: ConfigEntryBuilder,
        config: GommemodeConfig,
        defaults: GommemodeConfig,
    ) {
        category.addEntry(
            entries
                .startBooleanToggle(label("soundEnabled"), config.soundEnabled)
                .setDefaultValue(defaults.soundEnabled)
                .setTooltip(tooltip("soundEnabled"))
                .setSaveConsumer { config.soundEnabled = it }
                .build(),
        )
        category.addEntry(
            entries
                .startIntSlider(
                    label("volumePercent"),
                    config.volumePercent,
                    GommemodeConfig.MIN_VOLUME_PERCENT,
                    GommemodeConfig.MAX_VOLUME_PERCENT,
                ).setDefaultValue(defaults.volumePercent)
                .setTextGetter(::percent)
                .setTooltip(tooltip("volumePercent"))
                .setSaveConsumer { config.volumePercent = it }
                .build(),
        )
        category.addEntry(
            entries
                .startIntSlider(
                    label("pitchPercent"),
                    config.pitchPercent,
                    GommemodeConfig.MIN_PITCH_PERCENT,
                    GommemodeConfig.MAX_PITCH_PERCENT,
                ).setDefaultValue(defaults.pitchPercent)
                .setTextGetter(::percent)
                .setTooltip(tooltip("pitchPercent"))
                .setSaveConsumer { config.pitchPercent = it }
                .build(),
        )
        category.addEntry(
            entries
                .startEnumSelector(label("soundSource"), SoundSource::class.java, config.soundSource)
                .setDefaultValue(defaults.soundSource)
                .setEnumNameProvider { Component.translatable("soundCategory." + (it as SoundSource).getName()) }
                .setTooltip(tooltip("soundSource"))
                .setSaveConsumer { config.soundSource = it }
                .build(),
        )
        category.addEntry(
            entries
                .startBooleanToggle(label("positionalSound"), config.positionalSound)
                .setDefaultValue(defaults.positionalSound)
                .setTooltip(tooltip("positionalSound"))
                .setSaveConsumer { config.positionalSound = it }
                .build(),
        )
        category.addEntry(
            entries
                .startBooleanToggle(label("stopMusic"), config.stopMusic)
                .setDefaultValue(defaults.stopMusic)
                .setTooltip(tooltip("stopMusic"))
                .setSaveConsumer { config.stopMusic = it }
                .build(),
        )
    }

    /** Adds the spawn location, spawn distance and fluid targeting options. */
    private fun addSpawnOptions(
        category: ConfigCategory,
        entries: ConfigEntryBuilder,
        config: GommemodeConfig,
        defaults: GommemodeConfig,
    ) {
        category.addEntry(
            entries
                .startEnumSelector(label("spawnLocation"), SpawnLocation::class.java, config.spawnLocation)
                .setDefaultValue(defaults.spawnLocation)
                .setEnumNameProvider(::enumName)
                .setTooltip(tooltip("spawnLocation"))
                .setSaveConsumer { config.spawnLocation = it }
                .build(),
        )
        category.addEntry(
            entries
                .startDoubleField(label("spawnDistance"), config.spawnDistance)
                .setDefaultValue(defaults.spawnDistance)
                .setMin(GommemodeConfig.MIN_SPAWN_DISTANCE)
                .setMax(GommemodeConfig.MAX_SPAWN_DISTANCE)
                .setTooltip(tooltip("spawnDistance"))
                .setSaveConsumer { config.spawnDistance = it }
                .build(),
        )
        category.addEntry(
            entries
                .startBooleanToggle(label("spawnHitsFluids"), config.spawnHitsFluids)
                .setDefaultValue(defaults.spawnHitsFluids)
                .setTooltip(tooltip("spawnHitsFluids"))
                .setSaveConsumer { config.spawnHitsFluids = it }
                .build(),
        )
    }

    /** Adds the particle switch, type, sphere radius and spacing options. */
    private fun addParticleOptions(
        category: ConfigCategory,
        entries: ConfigEntryBuilder,
        config: GommemodeConfig,
        defaults: GommemodeConfig,
    ) {
        category.addEntry(
            entries
                .startBooleanToggle(label("particlesEnabled"), config.particlesEnabled)
                .setDefaultValue(defaults.particlesEnabled)
                .setTooltip(tooltip("particlesEnabled"))
                .setSaveConsumer { config.particlesEnabled = it }
                .build(),
        )
        category.addEntry(
            idDropdown(entries, "particleType", config.particleType, defaults.particleType, particleIds(), ::validateParticle) {
                config.particleType = it
            },
        )
        category.addEntry(
            entries
                .startDoubleField(label("particleRadius"), config.particleRadius)
                .setDefaultValue(defaults.particleRadius)
                .setMin(GommemodeConfig.MIN_PARTICLE_RADIUS)
                .setMax(GommemodeConfig.MAX_PARTICLE_RADIUS)
                .setTooltip(tooltip("particleRadius"))
                .setSaveConsumer { config.particleRadius = it }
                .build(),
        )
        category.addEntry(
            entries
                .startDoubleField(label("particleStepSize"), config.particleStepSize)
                .setDefaultValue(defaults.particleStepSize)
                .setMin(GommemodeConfig.MIN_PARTICLE_STEP)
                .setMax(GommemodeConfig.MAX_PARTICLE_STEP)
                .setTooltip(tooltip("particleStepSize"))
                .setSaveConsumer { config.particleStepSize = it }
                .build(),
        )
    }

    /** Adds the held items, arm, pose, look, scale, shadow, name tag and glow options. */
    private fun addEntityOptions(
        category: ConfigCategory,
        entries: ConfigEntryBuilder,
        config: GommemodeConfig,
        defaults: GommemodeConfig,
    ) {
        category.addEntry(
            idDropdown(entries, "mainHandItem", config.mainHandItem, defaults.mainHandItem, itemIds(), ::validateItem) {
                config.mainHandItem = it
            },
        )
        category.addEntry(
            idDropdown(entries, "offHandItem", config.offHandItem, defaults.offHandItem, itemIds(), ::validateItem) {
                config.offHandItem = it
            },
        )
        category.addEntry(
            entries
                .startEnumSelector(label("mainArm"), HumanoidArm::class.java, config.mainArm)
                .setDefaultValue(defaults.mainArm)
                .setEnumNameProvider { (it as HumanoidArm).caption() }
                .setTooltip(tooltip("mainArm"))
                .setSaveConsumer { config.mainArm = it }
                .build(),
        )
        category.addEntry(
            entries
                .startEnumSelector(label("mainArmPose"), HumanoidModel.ArmPose::class.java, config.mainArmPose)
                .setDefaultValue(defaults.mainArmPose)
                .setEnumNameProvider { Component.translatable("$KEY_PREFIX.enum.armpose.${it.name.lowercase(Locale.ROOT)}") }
                .setTooltip(tooltip("mainArmPose"))
                .setSaveConsumer { config.mainArmPose = it }
                .build(),
        )
        category.addEntry(
            entries
                .startEnumSelector(label("offArmPose"), HumanoidModel.ArmPose::class.java, config.offArmPose)
                .setDefaultValue(defaults.offArmPose)
                .setEnumNameProvider { Component.translatable("$KEY_PREFIX.enum.armpose.${it.name.lowercase(Locale.ROOT)}") }
                .setTooltip(tooltip("offArmPose"))
                .setSaveConsumer { config.offArmPose = it }
                .build(),
        )
        category.addEntry(
            entries
                .startBooleanToggle(label("lookAtPlayer"), config.lookAtPlayer)
                .setDefaultValue(defaults.lookAtPlayer)
                .setTooltip(tooltip("lookAtPlayer"))
                .setSaveConsumer { config.lookAtPlayer = it }
                .build(),
        )
        category.addEntry(
            entries
                .startEnumSelector(label("lookTarget"), LookTarget::class.java, config.lookTarget)
                .setDefaultValue(defaults.lookTarget)
                .setEnumNameProvider(::enumName)
                .setTooltip(tooltip("lookTarget"))
                .setSaveConsumer { config.lookTarget = it }
                .build(),
        )
        category.addEntry(
            entries
                .startDoubleField(label("scale"), config.scale)
                .setDefaultValue(defaults.scale)
                .setMin(GommemodeConfig.MIN_SCALE)
                .setMax(GommemodeConfig.MAX_SCALE)
                .setTooltip(tooltip("scale"))
                .setSaveConsumer { config.scale = it }
                .build(),
        )
        category.addEntry(
            entries
                .startDoubleField(label("shadowRadius"), config.shadowRadius)
                .setDefaultValue(defaults.shadowRadius)
                .setMin(GommemodeConfig.MIN_SHADOW_RADIUS)
                .setMax(GommemodeConfig.MAX_SHADOW_RADIUS)
                .setTooltip(tooltip("shadowRadius"))
                .setSaveConsumer { config.shadowRadius = it }
                .build(),
        )
        category.addEntry(
            entries
                .startBooleanToggle(label("showNameTag"), config.showNameTag)
                .setDefaultValue(defaults.showNameTag)
                .setTooltip(tooltip("showNameTag"))
                .setSaveConsumer { config.showNameTag = it }
                .build(),
        )
        category.addEntry(
            entries
                .startStrField(label("nameTagText"), config.nameTagText)
                .setDefaultValue(defaults.nameTagText)
                .setTooltip(tooltip("nameTagText"))
                .setSaveConsumer { config.nameTagText = it }
                .build(),
        )
        category.addEntry(
            entries
                .startBooleanToggle(label("glowing"), config.glowing)
                .setDefaultValue(defaults.glowing)
                .setTooltip(tooltip("glowing"))
                .setSaveConsumer { config.glowing = it }
                .build(),
        )
        category.addEntry(
            entries
                .startColorField(label("glowColor"), config.glowColor)
                .setDefaultValue(defaults.glowColor)
                .setTooltip(tooltip("glowColor"))
                .setSaveConsumer { config.glowColor = it }
                .build(),
        )
    }

    /**
     * Builds a text field with a dropdown of suggested registry ids. Any text can still be typed;
     * [validate] decides whether it is accepted. The saved value is trimmed.
     */
    private fun idDropdown(
        entries: ConfigEntryBuilder,
        option: String,
        value: String,
        default: String,
        suggestions: List<String>,
        validate: (String) -> Optional<Component>,
        save: (String) -> Unit,
    ) = entries
        .startStringDropdownMenu(label(option), value)
        .setSelections(suggestions)
        .setSuggestionMode(true)
        .setDefaultValue(default)
        .setTooltip(tooltip(option))
        .setErrorSupplier(validate)
        .setSaveConsumer { save(it.trim()) }
        .build()

    /** All item ids except air, sorted, as dropdown suggestions. */
    private fun itemIds(): List<String> =
        BuiltInRegistries.ITEM
            .keySet()
            .filter { it != BuiltInRegistries.ITEM.getKey(Items.AIR) }
            .map(Identifier::toString)
            .sorted()

    /** All particle ids that can be spawned without extra parameters, sorted. */
    private fun particleIds(): List<String> =
        BuiltInRegistries.PARTICLE_TYPE
            .keySet()
            .filter { BuiltInRegistries.PARTICLE_TYPE.getValue(it) is SimpleParticleType }
            .map(Identifier::toString)
            .sorted()

    /** Gets or creates the category with the translated name for [name]. */
    private fun ConfigBuilder.category(name: String): ConfigCategory =
        getOrCreateCategory(Component.translatable("$KEY_PREFIX.category.$name"))

    /** The translated label of [option]. */
    private fun label(option: String): Component = Component.translatable("$KEY_PREFIX.option.$option")

    /** The translated tooltip of [option]. */
    private fun tooltip(option: String): Component = Component.translatable("$KEY_PREFIX.option.$option.tooltip")

    /** Formats a slider value as a percentage. */
    private fun percent(value: Int): Component = Component.literal("$value %")

    /** The translated name of an enum value, keyed by its class and constant name. */
    private fun enumName(value: Enum<*>): Component =
        Component.translatable(
            "$KEY_PREFIX.enum.${value.javaClass.simpleName.lowercase(Locale.ROOT)}.${value.name.lowercase(Locale.ROOT)}",
        )

    /** Accepts an empty field or a known item id; rejects air and unknown ids. */
    private fun validateItem(id: String): Optional<Component> =
        when {
            id.isBlank() -> Optional.empty()
            Identifier.tryParse(id.trim()) == BuiltInRegistries.ITEM.getKey(Items.AIR) ->
                Optional.of(Component.translatable("$KEY_PREFIX.error.airItem"))
            GommeEntity.findItem(id) != null -> Optional.empty()
            else -> Optional.of(Component.translatable("$KEY_PREFIX.error.unknownItem", id.trim()))
        }

    /** Accepts only known particle ids that need no extra parameters. */
    private fun validateParticle(id: String): Optional<Component> {
        val particle =
            Identifier
                .tryParse(id.trim())
                ?.let { BuiltInRegistries.PARTICLE_TYPE.getOptional(it).orElse(null) }
        return when (particle) {
            null -> Optional.of(Component.translatable("$KEY_PREFIX.error.unknownParticle", id.trim()))
            !is SimpleParticleType -> Optional.of(Component.translatable("$KEY_PREFIX.error.complexParticle", id.trim()))
            else -> Optional.empty()
        }
    }
}
