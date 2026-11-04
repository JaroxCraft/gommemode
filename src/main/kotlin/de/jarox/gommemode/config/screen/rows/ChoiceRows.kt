package de.jarox.gommemode.config.screen.rows

import com.mojang.blaze3d.platform.InputConstants
import de.jarox.gommemode.config.GommemodeConfig
import de.jarox.gommemode.config.screen.EditState
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import kotlin.reflect.KMutableProperty1

/** An on/off option shown as a checkbox. */
internal class ToggleRow(
    property: KMutableProperty1<GommemodeConfig, Boolean>,
    state: EditState,
) : PropertyRow<Boolean>(property, state) {
    override val controlWidth = Theme.CHECKBOX_SIZE

    override fun extractControl(
        g: GuiGraphicsExtractor,
        font: Font,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
        Theme.checkbox(g, rect.right - Theme.CHECKBOX_SIZE, rect.y + (rect.height - Theme.CHECKBOX_SIZE) / 2, value, hovered)
    }

    override fun clickControl(
        event: MouseButtonEvent,
        rect: Rect,
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

    override val controlWidth: Int = values.maxOf { Minecraft.getInstance().font.width(names(it)) }

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
    ): Boolean = adjust(if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) -1 else 1)

    override fun defaultText(): Component = names(defaultValue)

    override fun adjust(direction: Int): Boolean {
        value = values[(value.ordinal + direction + values.size) % values.size]
        Theme.playClick()
        return true
    }

    override fun activate(): Boolean = adjust(1)
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
    override val isSavedDefault: Boolean get() = state.savedKey == keyMapping.defaultKey

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
    ): Boolean = activate()
}
