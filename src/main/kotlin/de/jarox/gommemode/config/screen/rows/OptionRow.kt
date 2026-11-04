package de.jarox.gommemode.config.screen.rows

import de.jarox.gommemode.config.GommemodeConfig
import de.jarox.gommemode.config.screen.EditState
import de.jarox.gommemode.config.screen.KEY_PREFIX
import de.jarox.gommemode.config.screen.theme.Rect
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.reflect.KMutableProperty1

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

    /** Whether the applied value equals the default. */
    protected abstract val isSavedDefault: Boolean

    /** Whether the undo button is shown: only when undo would not do the same as reset. */
    val showsUndo: Boolean get() = isModified && !isSavedDefault

    /** The label as listed, marked with "*" while the row has a change that is not applied yet. */
    val listedLabel: String get() = if (isModified) label.string + " *" else label.string

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
    private val savedValue: T get() = property.get(state.saved)

    override val isModified: Boolean get() = value != savedValue
    override val isDefault: Boolean get() = value == defaultValue
    override val isSavedDefault: Boolean get() = savedValue == defaultValue

    override fun undo() {
        value = savedValue
        syncFromState()
    }

    override fun reset() {
        value = defaultValue
        syncFromState()
    }
}
