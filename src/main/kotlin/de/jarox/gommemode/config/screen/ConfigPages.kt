package de.jarox.gommemode.config.screen

import de.jarox.gommemode.config.GommemodeConfig
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
                        state.keyMapping?.let { KeyRow(state, it) },
                        rows.intSlider(
                            GommemodeConfig::toggleCooldownMs,
                            GommemodeConfig.MIN_COOLDOWN_MS,
                            GommemodeConfig.MAX_COOLDOWN_MS,
                            50,
                            MILLIS,
                        ),
                        rows.enum(GommemodeConfig::messageDisplay),
                    ),
                ),
                Group(
                    "songEnd",
                    listOf(
                        rows.enum(GommemodeConfig::songEndBehavior),
                        rows.intSlider(
                            GommemodeConfig::loopIntervalMs,
                            GommemodeConfig.MIN_LOOP_INTERVAL_MS,
                            GommemodeConfig.MAX_LOOP_INTERVAL_MS,
                            100,
                            MILLIS,
                        ),
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
                        rows.intSlider(
                            GommemodeConfig::volumePercent,
                            GommemodeConfig.MIN_VOLUME_PERCENT,
                            GommemodeConfig.MAX_VOLUME_PERCENT,
                            1,
                            PERCENT,
                        ),
                        rows.intSlider(
                            GommemodeConfig::pitchPercent,
                            GommemodeConfig.MIN_PITCH_PERCENT,
                            GommemodeConfig.MAX_PITCH_PERCENT,
                            1,
                            PERCENT,
                        ),
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
                        rows.doubleSlider(
                            GommemodeConfig::spawnDistance,
                            GommemodeConfig.MIN_SPAWN_DISTANCE,
                            GommemodeConfig.MAX_SPAWN_DISTANCE,
                            0.5,
                            BLOCKS,
                        ),
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
                        TextRow(
                            GommemodeConfig::particleType,
                            state,
                            ID_FIELD_WIDTH,
                            ::validateParticle,
                            RegistryIds.simpleParticleIds(),
                            trim = true,
                        ),
                        rows.doubleSlider(
                            GommemodeConfig::particleRadius,
                            GommemodeConfig.MIN_PARTICLE_RADIUS,
                            GommemodeConfig.MAX_PARTICLE_RADIUS,
                            0.1,
                            BLOCKS,
                        ),
                        rows.doubleSlider(
                            GommemodeConfig::particleStepSize,
                            GommemodeConfig.MIN_PARTICLE_STEP,
                            GommemodeConfig.MAX_PARTICLE_STEP,
                            0.05,
                            null,
                        ),
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
                        TextRow(GommemodeConfig::mainHandItem, state, ID_FIELD_WIDTH, ::validateItem, itemIds, trim = true, hint = NOTHING),
                        TextRow(GommemodeConfig::offHandItem, state, ID_FIELD_WIDTH, ::validateItem, itemIds, trim = true, hint = NOTHING),
                        rows.enum(GommemodeConfig::mainArm, HumanoidArm::caption),
                        rows.enum(GommemodeConfig::mainArmPose, ::armPoseName),
                        rows.enum(GommemodeConfig::offArmPose, ::armPoseName),
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
                        rows.doubleSlider(GommemodeConfig::scale, GommemodeConfig.MIN_SCALE, GommemodeConfig.MAX_SCALE, 0.0625, TIMES),
                        rows.doubleSlider(
                            GommemodeConfig::shadowRadius,
                            GommemodeConfig.MIN_SHADOW_RADIUS,
                            GommemodeConfig.MAX_SHADOW_RADIUS,
                            0.1,
                            BLOCKS,
                        ),
                        rows.toggle(GommemodeConfig::glowing),
                        ColorRow(GommemodeConfig::glowColor, state),
                    ),
                ),
                Group(
                    "nameTag",
                    listOf(
                        rows.toggle(GommemodeConfig::showNameTag),
                        TextRow(GommemodeConfig::nameTagText, state, 140, hint = Component.translatable("entity.gommemode.gomme")),
                    ),
                ),
            ),
        ),
    )
}

private const val ID_FIELD_WIDTH = 160

/** Short constructors for the common row types. */
private class RowFactory(
    private val state: EditState,
) {
    fun toggle(property: KMutableProperty1<GommemodeConfig, Boolean>) = ToggleRow(property, state)

    fun <E : Enum<E>> enum(
        property: KMutableProperty1<GommemodeConfig, E>,
        names: (E) -> Component = ::enumName,
    ) = EnumRow(property, state, names)

    fun intSlider(
        property: KMutableProperty1<GommemodeConfig, Int>,
        min: Int,
        max: Int,
        step: Int,
        unit: Component?,
    ) = SliderRow(property, state, min.toDouble(), max.toDouble(), step.toDouble(), { it.roundToInt() }, unit)

    fun doubleSlider(
        property: KMutableProperty1<GommemodeConfig, Double>,
        min: Double,
        max: Double,
        step: Double,
        unit: Component?,
    ) = SliderRow(property, state, min, max, step, { it }, unit)
}

private val MILLIS: Component = Component.literal("ms")
private val PERCENT: Component = Component.literal("%")
private val BLOCKS: Component = Component.translatable("$KEY_PREFIX.unit.blocks")
private val TIMES: Component = Component.literal("×")

/** Placeholder of an empty item field. */
private val NOTHING: Component = Component.translatable("$KEY_PREFIX.hint.nothing")

/** The translated name of an enum value, keyed by its class and constant name. */
private fun enumName(value: Enum<*>): Component =
    Component.translatable("$KEY_PREFIX.enum.${value.javaClass.simpleName.lowercase(Locale.ROOT)}.${value.name.lowercase(Locale.ROOT)}")

/** The translated name of an arm pose. */
private fun armPoseName(pose: Enum<*>): Component = Component.translatable("$KEY_PREFIX.enum.armpose.${pose.name.lowercase(Locale.ROOT)}")

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
