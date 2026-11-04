package de.jarox.gommemode.config

import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.GommemodeManager
import de.jarox.gommemode.util.RegistryIds
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.HumanoidArm
import java.util.Locale
import java.util.Optional
import java.util.function.Supplier
import kotlin.reflect.KMutableProperty1

/** A config option; its property name is also the option name in the translation keys. */
private typealias Option<T> = KMutableProperty1<GommemodeConfig, T>

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

        val entries = builder.entryBuilder()
        val config = ConfigManager.config
        val defaults = GommemodeConfig()

        /** The entries for the category called [name]. */
        fun category(name: String) =
            CategoryEntries(
                builder.getOrCreateCategory(Component.translatable("$KEY_PREFIX.category.$name")),
                entries,
                config,
                defaults,
            )

        category("general").addGeneralOptions()
        category("sound").addSoundOptions()
        category("spawn").addSpawnOptions()
        category("particles").addParticleOptions()
        category("entity").addEntityOptions()

        return builder.build()
    }

    /** Adds the enable switch, toggle key, cooldown, song-end behavior, loop interval and message options. */
    private fun CategoryEntries.addGeneralOptions() {
        toggle(GommemodeConfig::enabled)
        Gommemode.toggleKeyMapping?.let { add(keyEntry(entries, it)) }
        intField(GommemodeConfig::toggleCooldownMs, GommemodeConfig.MIN_COOLDOWN_MS, GommemodeConfig.MAX_COOLDOWN_MS)
        enumSelector(GommemodeConfig::songEndBehavior)
        intField(GommemodeConfig::loopIntervalMs, GommemodeConfig.MIN_LOOP_INTERVAL_MS, GommemodeConfig.MAX_LOOP_INTERVAL_MS)
        enumSelector(GommemodeConfig::messageDisplay)
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

    /** Adds the song, volume, pitch, sound category, positional sound and game music options. */
    private fun CategoryEntries.addSoundOptions() {
        toggle(GommemodeConfig::soundEnabled)
        percentSlider(GommemodeConfig::volumePercent, GommemodeConfig.MIN_VOLUME_PERCENT, GommemodeConfig.MAX_VOLUME_PERCENT)
        percentSlider(GommemodeConfig::pitchPercent, GommemodeConfig.MIN_PITCH_PERCENT, GommemodeConfig.MAX_PITCH_PERCENT)
        enumSelector(GommemodeConfig::soundSource) { Component.translatable("soundCategory." + (it as SoundSource).getName()) }
        toggle(GommemodeConfig::positionalSound)
        toggle(GommemodeConfig::stopMusic)
    }

    /** Adds the spawn location, spawn distance and fluid targeting options. */
    private fun CategoryEntries.addSpawnOptions() {
        enumSelector(GommemodeConfig::spawnLocation)
        doubleField(GommemodeConfig::spawnDistance, GommemodeConfig.MIN_SPAWN_DISTANCE, GommemodeConfig.MAX_SPAWN_DISTANCE)
        toggle(GommemodeConfig::spawnHitsFluids)
    }

    /** Adds the particle switch, type, sphere radius and spacing options. */
    private fun CategoryEntries.addParticleOptions() {
        toggle(GommemodeConfig::particlesEnabled)
        idDropdown(GommemodeConfig::particleType, RegistryIds.simpleParticleIds(), ::validateParticle)
        doubleField(GommemodeConfig::particleRadius, GommemodeConfig.MIN_PARTICLE_RADIUS, GommemodeConfig.MAX_PARTICLE_RADIUS)
        doubleField(GommemodeConfig::particleStepSize, GommemodeConfig.MIN_PARTICLE_STEP, GommemodeConfig.MAX_PARTICLE_STEP)
    }

    /** Adds the held items, arms, poses, look, scale, shadow, name tag and glow options. */
    private fun CategoryEntries.addEntityOptions() {
        val itemIds = RegistryIds.itemIds()
        idDropdown(GommemodeConfig::mainHandItem, itemIds, ::validateItem)
        idDropdown(GommemodeConfig::offHandItem, itemIds, ::validateItem)
        enumSelector(GommemodeConfig::mainArm) { (it as HumanoidArm).caption() }
        enumSelector(GommemodeConfig::mainArmPose, ::armPoseName)
        enumSelector(GommemodeConfig::offArmPose, ::armPoseName)
        toggle(GommemodeConfig::lookAtPlayer)
        enumSelector(GommemodeConfig::lookTarget)
        doubleField(GommemodeConfig::scale, GommemodeConfig.MIN_SCALE, GommemodeConfig.MAX_SCALE)
        doubleField(GommemodeConfig::shadowRadius, GommemodeConfig.MIN_SHADOW_RADIUS, GommemodeConfig.MAX_SHADOW_RADIUS)
        toggle(GommemodeConfig::showNameTag)
        add(GommemodeConfig::nameTagText) { label, value -> entries.startStrField(label, value) }
        toggle(GommemodeConfig::glowing)
        add(GommemodeConfig::glowColor) { label, value -> entries.startColorField(label, value) }
    }

    /**
     * Adds config options to one category of the screen. Each entry gets the label, tooltip and
     * default value of its option and writes the edited value back to [config] on save.
     */
    private class CategoryEntries(
        private val category: ConfigCategory,
        val entries: ConfigEntryBuilder,
        private val config: GommemodeConfig,
        private val defaults: GommemodeConfig,
    ) {
        /** Adds an entry that is not tied to a config option. */
        fun add(entry: AbstractConfigListEntry<*>) {
            category.addEntry(entry)
        }

        /** Adds an entry for [option]; [start] creates the field from the label and the current value. */
        fun <T : Any, B : AbstractFieldBuilder<T, *, B>> add(
            option: Option<T>,
            start: (Component, T) -> B,
        ) {
            add(
                start(label(option.name), option.get(config))
                    .setDefaultValue(Supplier { option.get(defaults) })
                    .setTooltip(tooltip(option.name))
                    .setSaveConsumer { option.set(config, it) }
                    .build(),
            )
        }

        /** Adds an on/off switch. */
        fun toggle(option: Option<Boolean>) = add(option) { label, value -> entries.startBooleanToggle(label, value) }

        /** Adds a whole-number field limited to [min]..[max]. */
        fun intField(
            option: Option<Int>,
            min: Int,
            max: Int,
        ) = add(option) { label, value -> entries.startIntField(label, value).setMin(min).setMax(max) }

        /** Adds a slider from [min] to [max] that shows its value as a percentage. */
        fun percentSlider(
            option: Option<Int>,
            min: Int,
            max: Int,
        ) = add(option) { label, value -> entries.startIntSlider(label, value, min, max).setTextGetter(::percent) }

        /** Adds a decimal field limited to [min]..[max]. */
        fun doubleField(
            option: Option<Double>,
            min: Double,
            max: Double,
        ) = add(option) { label, value -> entries.startDoubleField(label, value).setMin(min).setMax(max) }

        /** Adds a selector for an enum option; [names] gives the shown name of each value. */
        fun <E : Enum<E>> enumSelector(
            option: Option<E>,
            names: (Enum<*>) -> Component = ::enumName,
        ) = add(option) { label, value ->
            entries.startEnumSelector(label, value.declaringJavaClass, value).setEnumNameProvider(names)
        }

        /**
         * Adds a text field with a dropdown of suggested registry ids. Any text can still be typed;
         * [validate] decides whether it is accepted. The saved value is trimmed.
         */
        fun idDropdown(
            option: Option<String>,
            suggestions: List<String>,
            validate: (String) -> Optional<Component>,
        ) = add(
            entries
                .startStringDropdownMenu(label(option.name), option.get(config))
                .setSelections(suggestions)
                .setSuggestionMode(true)
                .setDefaultValue(option.get(defaults))
                .setTooltip(tooltip(option.name))
                .setErrorSupplier(validate)
                .setSaveConsumer { option.set(config, it.trim()) }
                .build(),
        )
    }

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

    /** The translated name of an arm pose. */
    private fun armPoseName(pose: Enum<*>): Component =
        Component.translatable("$KEY_PREFIX.enum.armpose.${pose.name.lowercase(Locale.ROOT)}")

    /** Accepts an empty field or a known item id; rejects air and unknown ids. */
    private fun validateItem(id: String): Optional<Component> =
        when {
            id.isBlank() -> Optional.empty()
            RegistryIds.isAir(id) -> Optional.of(Component.translatable("$KEY_PREFIX.error.airItem"))
            RegistryIds.item(id) != null -> Optional.empty()
            else -> Optional.of(Component.translatable("$KEY_PREFIX.error.unknownItem", id.trim()))
        }

    /** Accepts only known particle ids that need no extra parameters. */
    private fun validateParticle(id: String): Optional<Component> =
        when (RegistryIds.particleType(id)) {
            null -> Optional.of(Component.translatable("$KEY_PREFIX.error.unknownParticle", id.trim()))
            !is SimpleParticleType -> Optional.of(Component.translatable("$KEY_PREFIX.error.complexParticle", id.trim()))
            else -> Optional.empty()
        }
}
