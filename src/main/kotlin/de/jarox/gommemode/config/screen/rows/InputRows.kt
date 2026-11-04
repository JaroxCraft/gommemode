package de.jarox.gommemode.config.screen.rows

import de.jarox.gommemode.config.GommemodeConfig
import de.jarox.gommemode.config.screen.EditState
import de.jarox.gommemode.config.screen.KEY_PREFIX
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.reflect.KMutableProperty1

/**
 * A number option shown as a slider over [range] in steps of [step], next to a text field for
 * typing an exact value (which does not have to lie on a step). [unit] is shown after the field.
 * Works on doubles internally; [fromDouble] converts back to the property type.
 */
internal class SliderRow<T : Number>(
    property: KMutableProperty1<GommemodeConfig, T>,
    state: EditState,
    private val range: ClosedFloatingPointRange<Double>,
    private val step: Double,
    private val fromDouble: (Double) -> T,
    private val unit: Component? = null,
) : PropertyRow<T>(property, state) {
    private val min = range.start
    private val max = range.endInclusive
    private var invalid = false

    private val unitWidth = unit?.let { Minecraft.getInstance().font.width(it) + 4 } ?: 0

    override val controlWidth = TRACK_WIDTH + 8 + FIELD_WIDTH + unitWidth

    override val editBox: EditBox =
        textField(FIELD_WIDTH, 12, plain(value.toDouble())) { text ->
            val parsed =
                text
                    .trim()
                    .replace(',', '.')
                    .toDoubleOrNull()
                    ?.takeIf { it.isFinite() && it in range }
            invalid = parsed == null
            if (parsed != null) value = fromDouble(parsed)
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
        val fraction = ((value.toDouble() - min) / (max - min)).coerceIn(0.0, 1.0)
        val trackY = rect.y + rect.height / 2
        val knobX = rect.x + (fraction * (TRACK_WIDTH - KNOB_WIDTH)).roundToInt()
        val hovered = trackRect(rect).contains(mouseX.toDouble(), mouseY.toDouble())

        g.fill(rect.x, trackY - 1, rect.x + TRACK_WIDTH, trackY + 1, Theme.TEXT_DIM)
        g.fill(rect.x, trackY - 1, knobX, trackY + 1, Theme.ACCENT)
        g.fill(knobX, trackY - 5, knobX + KNOB_WIDTH, trackY + 5, if (hovered) Theme.ACCENT else Theme.TEXT)

        extractTextField(g, editBox, editBoxArea(rect), invalid, mouseX, mouseY, delta)
        unit?.let { g.text(font, it, rect.right - font.width(it), rect.y + (rect.height - 8) / 2, Theme.TEXT_DIM) }
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
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
        val fraction = ((mouseX - rect.x - KNOB_WIDTH / 2) / (TRACK_WIDTH - KNOB_WIDTH)).coerceIn(0.0, 1.0)
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
        val snapped = (min + steps * step).coerceIn(range)
        // Rounding removes floating point noise such as 0.30000000000000004.
        value = fromDouble(BigDecimal(snapped).setScale(4, RoundingMode.HALF_UP).toDouble())
        syncFromState()
    }

    /** The part of the control that reacts to the mouse: the track plus a small margin. */
    private fun trackRect(rect: Rect) = Rect(rect.x - 3, rect.y, TRACK_WIDTH + 6, rect.height)

    private companion object {
        const val TRACK_WIDTH = 90
        const val KNOB_WIDTH = 4
        const val FIELD_WIDTH = 48

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
        textField(controlWidth, 256, value) { text ->
            value = if (trim) text.trim() else text
            updateInlineSuggestion()
        }.apply { hint?.let { setHint(it.copy().withColor(Theme.TEXT_DIM)) } }

    override fun error(): Component? = validate?.invoke(value)

    // An empty default (e.g. no off hand item) is shown with the field placeholder, such as "Nothing".
    override fun defaultText(): Component = if (defaultValue.isEmpty() && hint != null) hint else Component.literal(defaultValue)

    override fun editBoxArea(rect: Rect): Rect = rect

    override fun syncFromState() {
        if (editBox.value != value) editBox.value = value
    }

    /** Suggested ids that match the typed text; ids whose name starts with it come first. */
    fun matchingSuggestions(): List<String> {
        if (suggestions.isEmpty()) return emptyList()
        val query = editBox.value.trim().lowercase()
        val (starting, others) =
            suggestions
                .filter { it.contains(query) }
                .partition { it.startsWith(query) || it.substringAfter(':').startsWith(query) }
        return (starting + others).take(MAX_SUGGESTIONS)
    }

    /** Shows the rest of the first suggestion in gray after the cursor; Tab accepts it. */
    private fun updateInlineSuggestion() {
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
    ): Boolean = true

    private companion object {
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
        textField(FIELD, 7, hex(value)) { text ->
            val parsed =
                text
                    .trim()
                    .removePrefix("#")
                    .takeIf { it.length == 6 }
                    ?.toIntOrNull(16)
            invalid = parsed == null
            if (parsed != null) value = parsed
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
    ): Boolean = true

    override fun defaultText(): Component = Component.literal(hex(defaultValue))

    private fun hex(color: Int) = "#%06X".format(color and 0xFFFFFF)

    private companion object {
        const val SWATCH = 12
        const val FIELD = 56
    }
}

/** A borderless text field for a row, showing [initial] and reporting every edit to [onChange]. */
private fun OptionRow.textField(
    width: Int,
    maxLength: Int,
    initial: String,
    onChange: (String) -> Unit,
): EditBox =
    EditBox(Minecraft.getInstance().font, 0, 0, width, 9, label).apply {
        setBordered(false)
        setMaxLength(maxLength)
        setValue(initial)
        setResponder(onChange)
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
