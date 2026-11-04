package de.jarox.gommemode.config.screen.components

import de.jarox.gommemode.config.screen.KEY_PREFIX
import de.jarox.gommemode.config.screen.ScreenLayout
import de.jarox.gommemode.config.screen.rows.RowSummary
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component

/** The buttons at the bottom of the screen. Whether each one can be pressed follows the state of the rows. */
internal class ButtonBar(
    private val summary: RowSummary,
) {
    enum class Button {
        RESET_ALL,
        CANCEL,
        APPLY,
        DONE,
    }

    /** A tooltip text and its color. */
    class Tip(
        val text: Component,
        val color: Int,
    )

    fun rectOf(
        layout: ScreenLayout,
        button: Button,
    ): Rect =
        when (button) {
            Button.RESET_ALL -> layout.resetAll
            Button.CANCEL -> layout.cancel
            Button.APPLY -> layout.apply
            Button.DONE -> layout.done
        }

    fun buttonAt(
        layout: ScreenLayout,
        x: Double,
        y: Double,
    ): Button? = Button.entries.firstOrNull { rectOf(layout, it).contains(x, y) }

    /** Whether [button] can be pressed: Reset All needs something to reset, Apply something to apply, and both need valid values. */
    fun isActive(button: Button): Boolean =
        when (button) {
            Button.RESET_ALL -> summary.nonDefaultCount > 0
            Button.CANCEL -> true
            Button.APPLY -> summary.isDirty && !summary.hasErrors
            Button.DONE -> !summary.hasErrors
        }

    /** What [button] explains on hover right now, if anything. */
    fun tooltip(button: Button): Tip? =
        when {
            button == Button.RESET_ALL -> Tip(RESET_ALL_TOOLTIP, Theme.TEXT_SOFT)
            button != Button.CANCEL && summary.hasErrors -> Tip(INVALID, Theme.ERROR)
            else -> null
        }

    fun extract(
        g: GuiGraphicsExtractor,
        font: Font,
        layout: ScreenLayout,
        mouseX: Int,
        mouseY: Int,
    ) {
        for (button in Button.entries) {
            val rect = rectOf(layout, button)
            Theme.button(g, font, labelOf(button), rect, rect.contains(mouseX.toDouble(), mouseY.toDouble()), isActive(button))
        }
    }

    private fun labelOf(button: Button): Component =
        when (button) {
            Button.RESET_ALL -> RESET_ALL
            Button.CANCEL -> CommonComponents.GUI_CANCEL
            Button.APPLY -> summary.modifiedCount.let { if (it > 0) APPLY.copy().append(" ($it)") else APPLY }
            Button.DONE -> CommonComponents.GUI_DONE
        }

    companion object {
        private val RESET_ALL: Component = Component.translatable("$KEY_PREFIX.resetAll")
        private val RESET_ALL_TOOLTIP: Component = Component.translatable("$KEY_PREFIX.resetAll.tooltip")
        private val APPLY: Component = Component.translatable("$KEY_PREFIX.apply")
        private val INVALID: Component = Component.translatable("$KEY_PREFIX.invalid")

        /** The width of the Reset All button, which depends on its translated label. */
        fun resetAllWidth(font: Font): Int = font.width(RESET_ALL) + 20
    }
}
