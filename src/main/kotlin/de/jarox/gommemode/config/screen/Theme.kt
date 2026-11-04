package de.jarox.gommemode.config.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

/** Colors (ARGB) and small drawing helpers for the flat, dark look of the config screen. */
internal object Theme {
    const val PANEL: Int = 0x90000000.toInt()
    const val PANEL_DARK: Int = 0xB0000000.toInt()
    const val HOVER: Int = 0x20FFFFFF
    const val SELECTED: Int = 0x30FFFFFF
    const val INPUT: Int = 0x60000000
    const val DROPDOWN: Int = 0xF0101010.toInt()
    const val DIVIDER: Int = 0x40FFFFFF

    const val TEXT: Int = 0xFFFFFFFF.toInt()
    const val TEXT_SOFT: Int = 0xFFD0D0D0.toInt()
    const val TEXT_DIM: Int = 0xFF9A9A9A.toInt()
    const val TEXT_DISABLED: Int = 0xFF606060.toInt()
    const val ACCENT: Int = 0xFF8AD7FF.toInt()
    const val MODIFIED: Int = 0xFFFFE080.toInt()
    const val ERROR: Int = 0xFFFF6060.toInt()

    /** Plays the vanilla button click. */
    fun playClick() {
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
    }

    /** Draws a flat button with centered [text]; dimmed when not [active], lighter when [hovered]. */
    fun button(
        g: GuiGraphicsExtractor,
        font: Font,
        text: Component,
        rect: Rect,
        hovered: Boolean,
        active: Boolean = true,
    ) {
        g.fill(rect.x, rect.y, rect.right, rect.bottom, if (hovered && active) 0xC0303030.toInt() else PANEL_DARK)
        val color = if (active) TEXT else TEXT_DISABLED
        g.centeredText(font, text, rect.x + rect.width / 2, rect.y + (rect.height - font.lineHeight) / 2 + 1, color)
    }

    /**
     * Draws a pixel [icon] (one string per line, `X` = filled) centered in [rect].
     * Used instead of font glyphs, which come from the fallback font and are not centered reliably.
     */
    fun pattern(
        g: GuiGraphicsExtractor,
        icon: List<String>,
        rect: Rect,
        color: Int,
    ) {
        val left = rect.x + (rect.width - icon.first().length) / 2
        val top = rect.y + (rect.height - icon.size) / 2
        icon.forEachIndexed { y, line ->
            line.forEachIndexed { x, pixel ->
                if (pixel == 'X') g.fill(left + x, top + y, left + x + 1, top + y + 1, color)
            }
        }
    }

    /** Draws a square checkbox, filled with the accent color when [checked]. */
    fun checkbox(
        g: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        checked: Boolean,
        hovered: Boolean,
    ) {
        val size = 9
        g.outline(x, y, size, size, if (hovered) ACCENT else TEXT)
        if (checked) g.fill(x + 2, y + 2, x + size - 2, y + size - 2, ACCENT)
    }
}

/** An axis-aligned rectangle in GUI coordinates. */
internal data class Rect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    /** Whether the point ([px], [py]) lies inside this rectangle. */
    fun contains(
        px: Double,
        py: Double,
    ): Boolean = px >= x && px < right && py >= y && py < bottom
}
