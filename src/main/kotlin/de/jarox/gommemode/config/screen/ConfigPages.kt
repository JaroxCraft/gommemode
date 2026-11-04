package de.jarox.gommemode.config.screen

import de.jarox.gommemode.config.GommemodeConfig
import de.jarox.gommemode.config.screen.rows.ColorRow
import de.jarox.gommemode.config.screen.rows.EnumRow
import de.jarox.gommemode.config.screen.rows.KeyRow
import de.jarox.gommemode.config.screen.rows.OptionRow
import de.jarox.gommemode.config.screen.rows.SliderRow
import de.jarox.gommemode.config.screen.rows.TextRow
import de.jarox.gommemode.config.screen.rows.ToggleRow
import de.jarox.gommemode.util.RegistryIds
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.HumanoidArm
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty1

/** A page of the screen, listed in the sidebar. */
internal class Page(
    name: String,
    val groups: List<Group>,
) {
    val title: Component = Component.translatable("$KEY_PREFIX.category.$name")
    val rows: List<OptionRow> get() = groups.flatMap { it.rows }
}

/** A titled block of options on a page. */
internal class Group(
    name: String,
    val rows: List<OptionRow>,
) {
    val title: Component = Component.translatable("$KEY_PREFIX.group.$name")
}

/**
 * Builds all pages with their options, editing [state].
 *
 * A new option needs a row here, next to its field in [GommemodeConfig] and its translations.
 */
internal fun buildPages(state: EditState): List<Page> {
    val rows = RowFactory(state)
    val itemIds = RegistryIds.itemIds()
    return listOf(
        Page(
            "general",
            listOf(
                Group(
                    "toggle",
                    listOfNotNull(
                        rows.toggle(GommemodeConfig::enabled),
                        rows.toggleKey(),
                        rows.slider(GommemodeConfig::toggleCooldownMs, GommemodeConfig.COOLDOWN_MS, 50, MILLIS),
                        rows.enum(GommemodeConfig::messageDisplay),
                    ),
                ),
                Group(
                    "songEnd",
                    listOf(
                        rows.enum(GommemodeConfig::songEndBehavior),
                        rows.slider(GommemodeConfig::loopIntervalMs, GommemodeConfig.LOOP_INTERVAL_MS, 100, MILLIS),
                    ),
                ),
            ),
        ),
        Page(
            "sound",
            listOf(
                Group(
                    "song",
                    listOf(
                        rows.toggle(GommemodeConfig::soundEnabled),
                        rows.slider(GommemodeConfig::volumePercent, GommemodeConfig.VOLUME_PERCENT, 1, PERCENT),
                        rows.slider(GommemodeConfig::pitchPercent, GommemodeConfig.PITCH_PERCENT, 1, PERCENT),
                        rows.enum(GommemodeConfig::soundSource) { Component.translatable("soundCategory." + it.getName()) },
                        rows.toggle(GommemodeConfig::positionalSound),
                    ),
                ),
                Group("music", listOf(rows.toggle(GommemodeConfig::stopMusic))),
            ),
        ),
        Page(
            "spawn",
            listOf(
                Group(
                    "position",
                    listOf(
                        rows.enum(GommemodeConfig::spawnLocation),
                        rows.slider(GommemodeConfig::spawnDistance, GommemodeConfig.SPAWN_DISTANCE, 0.5, BLOCKS),
                        rows.toggle(GommemodeConfig::spawnHitsFluids),
                    ),
                ),
            ),
        ),
        Page(
            "particles",
            listOf(
                Group(
                    "sphere",
                    listOf(
                        rows.toggle(GommemodeConfig::particlesEnabled),
                        rows.idField(GommemodeConfig::particleType, ::validateParticle, RegistryIds.simpleParticleIds()),
                        rows.slider(GommemodeConfig::particleRadius, GommemodeConfig.PARTICLE_RADIUS, 0.1, BLOCKS),
                        rows.slider(GommemodeConfig::particleStepSize, GommemodeConfig.PARTICLE_STEP, 0.05, null),
                    ),
                ),
            ),
        ),
        Page(
            "entity",
            listOf(
                Group(
                    "items",
                    listOf(
                        rows.idField(GommemodeConfig::mainHandItem, ::validateItem, itemIds, NOTHING),
                        rows.idField(GommemodeConfig::offHandItem, ::validateItem, itemIds, NOTHING),
                        rows.enum(GommemodeConfig::mainArm, HumanoidArm::caption),
                        rows.enum(GommemodeConfig::mainArmPose),
                        rows.enum(GommemodeConfig::offArmPose),
                    ),
                ),
                Group(
                    "look",
                    listOf(
                        rows.toggle(GommemodeConfig::lookAtPlayer),
                        rows.enum(GommemodeConfig::lookTarget),
                    ),
                ),
                Group(
                    "appearance",
                    listOf(
                        rows.slider(GommemodeConfig::scale, GommemodeConfig.SCALE, 0.0625, TIMES),
                        rows.slider(GommemodeConfig::shadowRadius, GommemodeConfig.SHADOW_RADIUS, 0.1, BLOCKS),
                        rows.toggle(GommemodeConfig::glowing),
                        rows.color(GommemodeConfig::glowColor),
                    ),
                ),
                Group(
                    "nameTag",
                    listOf(
                        rows.toggle(GommemodeConfig::showNameTag),
                        rows.text(GommemodeConfig::nameTagText, 140, Component.translatable("entity.gommemode.gomme")),
                    ),
                ),
            ),
        ),
    )
}

/** Short constructors for the row types, all editing the same state. */
private class RowFactory(
    private val state: EditState,
) {
    fun toggle(property: KMutableProperty1<GommemodeConfig, Boolean>) = ToggleRow(property, state)

    /** The toggle key row, if the key mapping is registered. */
    fun toggleKey() = state.keyMapping?.let { KeyRow(state, it) }

    fun <E : Enum<E>> enum(
        property: KMutableProperty1<GommemodeConfig, E>,
        names: (E) -> Component = ::enumName,
    ) = EnumRow(property, state, names)

    fun slider(
        property: KMutableProperty1<GommemodeConfig, Int>,
        range: IntRange,
        step: Int,
        unit: Component?,
    ) = SliderRow(property, state, range.first.toDouble()..range.last.toDouble(), step.toDouble(), { it.roundToInt() }, unit)

    fun slider(
        property: KMutableProperty1<GommemodeConfig, Double>,
        range: ClosedFloatingPointRange<Double>,
        step: Double,
        unit: Component?,
    ) = SliderRow(property, state, range, step, { it }, unit)

    fun text(
        property: KMutableProperty1<GommemodeConfig, String>,
        width: Int,
        hint: Component,
    ) = TextRow(property, state, width, hint = hint)

    /** A registry id field: trimmed, checked with [validate] and suggesting [ids] while typing. */
    fun idField(
        property: KMutableProperty1<GommemodeConfig, String>,
        validate: (String) -> Component?,
        ids: List<String>,
        hint: Component? = null,
    ) = TextRow(property, state, ID_FIELD_WIDTH, validate, ids, trim = true, hint = hint)

    fun color(property: KMutableProperty1<GommemodeConfig, Int>) = ColorRow(property, state)

    private companion object {
        const val ID_FIELD_WIDTH = 160
    }
}

private val MILLIS: Component = Component.literal("ms")
private val PERCENT: Component = Component.literal("%")
private val BLOCKS: Component = Component.translatable("$KEY_PREFIX.unit.blocks")
private val TIMES: Component = Component.literal("×")

/** Placeholder of an empty item field. */
private val NOTHING: Component = Component.translatable("$KEY_PREFIX.hint.nothing")

/**
 * The translated name of an enum value, keyed by its enum class and constant name.
 * Uses the declaring class, because a constant with its own body (like one of the vanilla arm poses) is an anonymous subclass.
 */
private fun enumName(value: Enum<*>): Component {
    val enumClass = value.declaringJavaClass.simpleName.lowercase(Locale.ROOT)
    return Component.translatable("$KEY_PREFIX.enum.$enumClass.${value.name.lowercase(Locale.ROOT)}")
}

/** Accepts an empty field or a known item id; rejects air and unknown ids. */
private fun validateItem(id: String): Component? =
    when {
        id.isBlank() -> null
        RegistryIds.isAir(id) -> Component.translatable("$KEY_PREFIX.error.airItem")
        RegistryIds.item(id) != null -> null
        else -> Component.translatable("$KEY_PREFIX.error.unknownItem", id.trim())
    }

/** Accepts only known particle ids that need no extra parameters. */
private fun validateParticle(id: String): Component? =
    when (RegistryIds.particleType(id)) {
        null -> Component.translatable("$KEY_PREFIX.error.unknownParticle", id.trim())
        !is SimpleParticleType -> Component.translatable("$KEY_PREFIX.error.complexParticle", id.trim())
        else -> null
    }
