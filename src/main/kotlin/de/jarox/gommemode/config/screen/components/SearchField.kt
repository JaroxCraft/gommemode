package de.jarox.gommemode.config.screen.components

import de.jarox.gommemode.config.screen.KEY_PREFIX
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component

/** The search field on top of the screen; reports the trimmed query to [onQuery] on every edit. */
internal class SearchField(
    font: Font,
    onQuery: (String) -> Unit,
) {
    val box: EditBox =
        EditBox(font, 0, 0, 100, 9, PLACEHOLDER).apply {
            setBordered(false)
            setMaxLength(64)
            setHint(PLACEHOLDER.copy().withColor(Theme.TEXT_DIM))
            setResponder { text -> onQuery(text.trim()) }
        }

    /** Draws the field into [rect], with an accent outline while it has focus. */
    fun extract(
        g: GuiGraphicsExtractor,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.PANEL_DARK)
        if (box.isFocused) g.outline(rect.x, rect.y, rect.width, rect.height, Theme.ACCENT)
        box.setSize(rect.width - 12, 9)
        box.setX(rect.x + 6)
        box.setY(rect.y + (rect.height - 8) / 2)
        box.extractRenderState(g, mouseX, mouseY, delta)
    }

    private companion object {
        val PLACEHOLDER: Component = Component.translatable("$KEY_PREFIX.search")
    }
}
