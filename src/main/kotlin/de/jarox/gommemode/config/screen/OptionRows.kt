package de.jarox.gommemode.config.screen

import com.mojang.blaze3d.platform.InputConstants
import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.config.ConfigManager
import de.jarox.gommemode.config.GommemodeConfig
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.reflect.KMutableProperty1

internal const val KEY_PREFIX = "config.${Gommemode.MOD_ID}"

/**
 * The values the config screen works with: [pending] is edited by the screen, [saved] is what was
 * last applied, and [defaults] are the built-in defaults. The toggle key lives in the vanilla key
 * mapping, so it is tracked here separately.
 */
internal class EditState(
    val keyMapping: KeyMapping?,
) {
    val defaults = GommemodeConfig()
    val pending: GommemodeConfig = ConfigManager.copyOf(ConfigManager.config)
    var saved: GommemodeConfig = ConfigManager.copyOf(ConfigManager.config)
    var pendingKey: InputConstants.Key = keyMapping?.boundKey() ?: InputConstants.UNKNOWN
    var savedKey: InputConstants.Key = pendingKey
}

/** The key the mapping is bound to right now (which may differ from its default). */
internal fun KeyMapping.boundKey(): InputConstants.Key =
    net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
        .getBoundKeyOf(this)

/**
 * One option line of the screen: the label on the left, the control on the right.
 * The screen does the layout; a row only draws its control into the rectangle it is given and
 * reacts to input inside it.
 */
internal abstract class OptionRow(
    val name: String,
) {
    val label: Component = Component.translatable("$KEY_PREFIX.option.$name")
    val description: Component = Component.translatable("$KEY_PREFIX.option.$name.tooltip")

    /** Width of the control on the right side of the row. */
    abstract val controlWidth: Int

    /** Whether the edited value differs from the applied one. */
    abstract val isModified: Boolean

    /** Whether the edited value equals the default. */
    abstract val isDefault: Boolean

    /** Goes back to the applied value. */
    abstract fun undo()

    /** Goes back to the default value. */
    abstract fun reset()

    /** Why the current input cannot be applied, or `null` if it is valid. */
    open fun error(): Component? = null

    /** The text field of this row, if it has one; the screen routes keyboard input to it while focused. */
    open val editBox: EditBox? get() = null

    /** Updates the control (e.g. the text field) after the value was changed from outside. */
    open fun syncFromState() {}

    /** The part of the control (in [rect]) that is the text field; clicking it focuses [editBox]. */
    open fun editBoxArea(rect: Rect): Rect? = null

    /** Called when [editBox] loses keyboard focus. */
    open fun onFocusLost() {}

    /** Draws the control into [rect]. */
    abstract fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    )

    /** Handles a click inside the control; returns whether it was used. */
    open fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
        doubleClick: Boolean,
    ): Boolean = false

    /** Handles dragging that started on the control. */
    open fun dragControl(
        mouseX: Double,
        rect: Rect,
    ) {}

    /** Handles Shift + mouse wheel over the control; returns whether it was used. */
    open fun scrollControl(amount: Double): Boolean = false

    /** The default value as the player sees it, shown in the tooltip. */
    abstract fun defaultText(): Component

    /** Changes the value with the arrow keys; [direction] is -1 or 1. Returns whether something changed. */
    open fun adjust(direction: Int): Boolean = false

    /**
     * Enter or Space on the selected row. Returns whether the row handled it; rows with a text field
     * return `false`, and the screen focuses the field instead.
     */
    open fun activate(): Boolean = false

    /** Whether the label or the description contains [query], ignoring case. */
    fun matches(query: String): Boolean =
        label.string.contains(query, ignoreCase = true) || description.string.contains(query, ignoreCase = true)
}

/** A row that edits one property of [GommemodeConfig]. */
internal abstract class PropertyRow<T>(
    private val property: KMutableProperty1<GommemodeConfig, T>,
    protected val state: EditState,
) : OptionRow(property.name) {
    var value: T
        get() = property.get(state.pending)
        set(newValue) = property.set(state.pending, newValue)

    protected val defaultValue: T get() = property.get(state.defaults)

    override val isModified: Boolean get() = value != property.get(state.saved)
    override val isDefault: Boolean get() = value == defaultValue

    override fun undo() {
        value = property.get(state.saved)
        syncFromState()
    }

    override fun reset() {
        value = property.get(state.defaults)
        syncFromState()
    }
}

/** An on/off option shown as a checkbox. */
internal class ToggleRow(
    property: KMutableProperty1<GommemodeConfig, Boolean>,
    state: EditState,
) : PropertyRow<Boolean>(property, state) {
    override val controlWidth = 9

    override fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
        Theme.checkbox(g, rect.right - 9, rect.y + (rect.height - 9) / 2, value, hovered)
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
        doubleClick: Boolean,
    ): Boolean = activate()

    override fun defaultText(): Component = if (defaultValue) CommonComponents.OPTION_ON else CommonComponents.OPTION_OFF

    override fun adjust(direction: Int): Boolean = activate()

    override fun activate(): Boolean {
        value = !value
        Theme.playClick()
        return true
    }
}

/** An enum option; left click selects the next value, right click the previous one. */
internal class EnumRow<E : Enum<E>>(
    property: KMutableProperty1<GommemodeConfig, E>,
    state: EditState,
    private val names: (E) -> Component,
) : PropertyRow<E>(property, state) {
    private val values: Array<E> = property.get(state.defaults).declaringJavaClass.enumConstants

    override val controlWidth: Int =
        values.maxOf {
            Minecraft
                .getInstance()
                .font
                .width(names(it))
        }

    override fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val text = names(value)
        val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
        g.text(font, text, rect.right - font.width(text), rect.y + (rect.height - 8) / 2, if (hovered) Theme.ACCENT else Theme.TEXT)
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
        doubleClick: Boolean,
    ): Boolean = adjust(if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) -1 else 1)

    override fun defaultText(): Component = names(defaultValue)

    override fun adjust(direction: Int): Boolean {
        value = values[(value.ordinal + direction + values.size) % values.size]
        Theme.playClick()
        return true
    }

    override fun activate(): Boolean = adjust(1)
}

/**
 * A number option shown as a slider from [min] to [max] in steps of [step], next to a text field for
 * typing an exact value (which does not have to lie on a step). [unit] is shown after the field.
 * Works on doubles internally; [fromDouble] converts back to the property type.
 */
internal class SliderRow<T : Number>(
    property: KMutableProperty1<GommemodeConfig, T>,
    state: EditState,
    private val min: Double,
    private val max: Double,
    private val step: Double,
    private val fromDouble: (Double) -> T,
    private val unit: Component? = null,
) : PropertyRow<T>(property, state) {
    private var invalid = false

    private val unitWidth = unit?.let { Minecraft.getInstance().font.width(it) + 4 } ?: 0

    override val controlWidth = TRACK_WIDTH + 8 + FIELD_WIDTH + unitWidth

    override val editBox: EditBox =
        EditBox(Minecraft.getInstance().font, 0, 0, FIELD_WIDTH, 9, label).apply {
            setBordered(false)
            setMaxLength(12)
            setValue(plain(this@SliderRow.value.toDouble()))
            setResponder { text ->
                val parsed =
                    text
                        .trim()
                        .replace(',', '.')
                        .toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it in min..max }
                invalid = parsed == null
                if (parsed != null) this@SliderRow.value = fromDouble(parsed)
            }
        }

    override fun error(): Component? = if (invalid) Component.translatable("$KEY_PREFIX.error.outOfRange", plain(min), plain(max)) else null

    override fun syncFromState() {
        editBox.value = plain(value.toDouble())
        invalid = false
    }

    /** Shows the stored value again, e.g. `1` instead of a typed `1.000`, unless the input is invalid. */
    override fun onFocusLost() {
        if (!invalid) syncFromState()
    }

    override fun editBoxArea(rect: Rect): Rect = Rect(rect.x + TRACK_WIDTH + 8, rect.y, FIELD_WIDTH, rect.height)

    override fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val current = value.toDouble()
        val fraction = ((current - min) / (max - min)).coerceIn(0.0, 1.0)
        val trackY = rect.y + rect.height / 2
        val knobX = rect.x + (fraction * (TRACK_WIDTH - 4)).roundToInt()
        val hovered = trackRect(rect).contains(mouseX.toDouble(), mouseY.toDouble())

        g.fill(rect.x, trackY - 1, rect.x + TRACK_WIDTH, trackY + 1, Theme.TEXT_DIM)
        g.fill(rect.x, trackY - 1, knobX, trackY + 1, Theme.ACCENT)
        g.fill(knobX, trackY - 5, knobX + 4, trackY + 5, if (hovered) Theme.ACCENT else Theme.TEXT)

        extractTextField(g, editBox, editBoxArea(rect), invalid, mouseX, mouseY, delta)
        unit?.let { g.text(font, it, rect.right - font.width(it), rect.y + (rect.height - 8) / 2, Theme.TEXT_DIM) }
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
        doubleClick: Boolean,
    ): Boolean {
        if (editBoxArea(rect).contains(event.x(), event.y())) return true
        if (!trackRect(rect).contains(event.x(), event.y())) return false
        dragControl(event.x(), rect)
        return true
    }

    override fun dragControl(
        mouseX: Double,
        rect: Rect,
    ) {
        val fraction = ((mouseX - rect.x - 2) / (TRACK_WIDTH - 4)).coerceIn(0.0, 1.0)
        setSnapped(min + fraction * (max - min))
    }

    override fun scrollControl(amount: Double): Boolean = adjust(amount.sign.toInt())

    override fun defaultText(): Component {
        val number = Component.literal(plain(defaultValue.toDouble()))
        return if (unit == null) number else number.append(" ").append(unit)
    }

    override fun adjust(direction: Int): Boolean {
        setSnapped(value.toDouble() + direction * step)
        return true
    }

    /** Sets the value nearest to [raw] that lies on a step, within the range. */
    private fun setSnapped(raw: Double) {
        val steps = ((raw - min) / step).roundToInt()
        val snapped = (min + steps * step).coerceIn(min, max)
        // Rounding removes floating point noise such as 0.30000000000000004.
        value = fromDouble(BigDecimal(snapped).setScale(4, RoundingMode.HALF_UP).toDouble())
        syncFromState()
    }

    /** The part of the control that reacts to the mouse: the track plus a small margin. */
    private fun trackRect(rect: Rect) = Rect(rect.x - 3, rect.y, TRACK_WIDTH + 6, rect.height)

    companion object {
        private const val TRACK_WIDTH = 90
        private const val FIELD_WIDTH = 48

        /** Formats [value] without trailing zeros, e.g. 1.5 or 0.0625. */
        fun plain(value: Double): String =
            BigDecimal(value)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
    }
}

/**
 * A free text option. With [suggestions], the screen shows matching ids below the field while it is focused,
 * and [trim] removes surrounding spaces before the value is stored. [hint] is shown while the field is empty.
 */
internal class TextRow(
    property: KMutableProperty1<GommemodeConfig, String>,
    state: EditState,
    override val controlWidth: Int,
    private val validate: ((String) -> Component?)? = null,
    val suggestions: List<String> = emptyList(),
    private val trim: Boolean = false,
    private val hint: Component? = null,
) : PropertyRow<String>(property, state) {
    override val editBox: EditBox =
        EditBox(Minecraft.getInstance().font, 0, 0, controlWidth, 9, label).apply {
            setBordered(false)
            setMaxLength(256)
            hint?.let { setHint(it.copy().withColor(Theme.TEXT_DIM)) }
            setValue(this@TextRow.value)
            setResponder { text ->
                this@TextRow.value = if (trim) text.trim() else text
                updateInlineSuggestion()
            }
        }

    override fun error(): Component? = validate?.invoke(value)

    // An empty default (e.g. no off hand item) is shown with the field placeholder, such as "Nothing".
    override fun defaultText(): Component = if (defaultValue.isEmpty() && hint != null) hint else Component.literal(defaultValue)

    override fun editBoxArea(rect: Rect): Rect = rect

    override fun syncFromState() {
        if (editBox.value != value) editBox.value = value
    }

    /** Suggested ids that match the typed text; ids whose name starts with it come first. */
    fun matchingSuggestions(): List<String> {
        val query = editBox.value.trim().lowercase()
        if (suggestions.isEmpty()) return emptyList()
        val matches = suggestions.filter { it.contains(query) }
        val (starting, others) = matches.partition { it.startsWith(query) || it.substringAfter(':').startsWith(query) }
        return (starting + others).take(MAX_SUGGESTIONS)
    }

    /** Shows the rest of the first suggestion in gray after the cursor; Tab accepts it. */
    fun updateInlineSuggestion() {
        val typed = editBox.value
        val first = matchingSuggestions().firstOrNull()
        editBox.setSuggestion(if (typed.isNotEmpty() && first != null && first.startsWith(typed)) first.removePrefix(typed) else null)
    }

    /** Replaces the text with [suggestion]. */
    fun accept(suggestion: String) {
        editBox.value = suggestion
        editBox.moveCursorToEnd(false)
    }

    override fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        extractTextField(g, editBox, rect, error() != null, mouseX, mouseY, delta)
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
        doubleClick: Boolean,
    ): Boolean = true

    companion object {
        const val MAX_SUGGESTIONS = 8
    }
}

/** An RGB color, shown as a color swatch and a `#RRGGBB` text field. */
internal class ColorRow(
    property: KMutableProperty1<GommemodeConfig, Int>,
    state: EditState,
) : PropertyRow<Int>(property, state) {
    private var invalid = false

    override val controlWidth = SWATCH + 4 + FIELD

    override val editBox: EditBox =
        EditBox(Minecraft.getInstance().font, 0, 0, FIELD, 9, label).apply {
            setBordered(false)
            setMaxLength(7)
            setValue(hex(this@ColorRow.value))
            setResponder { text ->
                val parsed =
                    text
                        .trim()
                        .removePrefix("#")
                        .takeIf { it.length == 6 }
                        ?.toIntOrNull(16)
                invalid = parsed == null
                if (parsed != null) this@ColorRow.value = parsed
            }
        }

    override fun error(): Component? = if (invalid) Component.translatable("$KEY_PREFIX.error.invalidColor") else null

    override fun editBoxArea(rect: Rect): Rect = Rect(rect.x + SWATCH + 4, rect.y, FIELD, rect.height)

    override fun syncFromState() {
        editBox.value = hex(value)
        invalid = false
    }

    override fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val swatchY = rect.y + (rect.height - SWATCH) / 2
        g.fill(rect.x, swatchY, rect.x + SWATCH, swatchY + SWATCH, 0xFF000000.toInt() or value)
        g.outline(rect.x, swatchY, SWATCH, SWATCH, Theme.TEXT_DIM)
        extractTextField(g, editBox, editBoxArea(rect), invalid, mouseX, mouseY, delta)
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
        doubleClick: Boolean,
    ): Boolean = true

    override fun defaultText(): Component = Component.literal(hex(defaultValue))

    private fun hex(color: Int) = "#%06X".format(color and 0xFFFFFF)

    companion object {
        private const val SWATCH = 12
        private const val FIELD = 56
    }
}

/** The toggle key. Clicking the control waits for the next key press; Escape cancels. */
internal class KeyRow(
    private val state: EditState,
    private val keyMapping: KeyMapping,
) : OptionRow("toggleKey") {
    /** Whether the row waits for a key press. */
    var listening = false

    override val controlWidth = 80
    override val isModified: Boolean get() = state.pendingKey != state.savedKey
    override val isDefault: Boolean get() = state.pendingKey == keyMapping.defaultKey

    override fun defaultText(): Component = keyMapping.defaultKey.displayName

    override fun activate(): Boolean {
        listening = true
        Theme.playClick()
        return true
    }

    override fun undo() {
        state.pendingKey = state.savedKey
    }

    override fun reset() {
        state.pendingKey = keyMapping.defaultKey
    }

    /** Uses [key] as the new toggle key and stops waiting. */
    fun assign(key: InputConstants.Key) {
        state.pendingKey = key
        listening = false
    }

    override fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val box = Rect(rect.x, rect.y + 3, rect.width, rect.height - 6)
        val hovered = box.contains(mouseX.toDouble(), mouseY.toDouble())
        g.fill(box.x, box.y, box.right, box.bottom, Theme.INPUT)
        if (listening || hovered) g.outline(box.x, box.y, box.width, box.height, Theme.ACCENT)
        val keyName = state.pendingKey.displayName
        val text = if (listening) Component.literal("> ").append(keyName).append(" <") else keyName
        g.centeredText(font, text, box.x + box.width / 2, box.y + (box.height - 8) / 2, if (listening) Theme.MODIFIED else Theme.TEXT)
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
        doubleClick: Boolean,
    ): Boolean {
        listening = true
        Theme.playClick()
        return true
    }
}

/** Draws a flat text field: dark background, accent outline while focused, red outline and text on [error]. */
private fun extractTextField(
    g: GuiGraphicsExtractor,
    box: EditBox,
    rect: Rect,
    error: Boolean,
    mouseX: Int,
    mouseY: Int,
    delta: Float,
) {
    val field = Rect(rect.x, rect.y + 3, rect.width, rect.height - 6)
    g.fill(field.x, field.y, field.right, field.bottom, Theme.INPUT)
    when {
        error -> g.outline(field.x, field.y, field.width, field.height, Theme.ERROR)
        box.isFocused -> g.outline(field.x, field.y, field.width, field.height, Theme.ACCENT)
    }
    box.setTextColor(if (error) Theme.ERROR else Theme.TEXT)
    box.setSize(field.width - 8, 9)
    box.setX(field.x + 4)
    box.setY(field.y + (field.height - 8) / 2)
    box.extractRenderState(g, mouseX, mouseY, delta)
}
