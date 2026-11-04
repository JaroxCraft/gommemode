package de.jarox.gommemode.config.screen.components

import de.jarox.gommemode.config.screen.KEY_PREFIX
import de.jarox.gommemode.config.screen.ScreenLayout
import de.jarox.gommemode.config.screen.rows.OptionRow
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Util
import kotlin.math.max

/** One tooltip line in [color]; a `null` [text] draws a thin divider. */
internal class TipLine(
    val text: FormattedCharSequence?,
    val color: Int,
)

/**
 * The tooltips of the config screen. They are drawn in the style of the screen's panels next to the element
 * they explain, never at the mouse, so they do not cover that element or the control under the mouse.
 * Row tooltips show after a short hover; right after one, the next row shows its tooltip at once.
 */
internal class Tooltips(
    private val font: Font,
) {
    private var hoveredRow: OptionRow? = null
    private var hoverStart = 0L
    private var hoverInstant = false
    private var lastShownAt = 0L

    /**
     * Tracks that [row] is under the mouse (or nothing, for `null`) and returns whether its tooltip is due.
     * [force] shows it at once, e.g. for an invalid value. Call once per frame.
     */
    fun rowTooltipDue(
        row: OptionRow?,
        force: Boolean,
    ): Boolean {
        val now = Util.getMillis()
        if (row != hoveredRow) {
            hoveredRow = row
            hoverStart = now
            hoverInstant = now - lastShownAt < GRACE_MS
        }
        val due = row != null && (force || hoverInstant || now - hoverStart >= DELAY_MS)
        if (due) lastShownAt = now
        return due
    }

    /** [text] wrapped to the tooltip width, in [color]. */
    fun lines(
        text: Component,
        color: Int,
    ): List<TipLine> = font.split(text, MAX_WIDTH).map { TipLine(it, color) }

    /**
     * The tooltip of a row: the full label if it is cut, the description, then below a divider the default value
     * and any error.
     */
    fun rowLines(entry: PlacedRow): List<TipLine> {
        val row = entry.row
        val result = mutableListOf<TipLine>()
        if (entry.isLabelCut(font)) result += lines(row.label, Theme.TEXT)
        result += lines(row.description, Theme.TEXT_SOFT)
        result += TipLine(null, 0)
        // Showing the default lets players judge a change without resetting it first.
        val default = row.defaultText().copy().withColor(Theme.ACCENT)
        result += lines(Component.translatable("$KEY_PREFIX.default", default), Theme.TEXT_DIM)
        row.error()?.let { result += lines(it, Theme.ERROR) }
        return result
    }

    /**
     * Draws [lines] next to [anchor]: below or above it as [preferBelow] says, or on the other side when it does
     * not fit between [ScreenLayout.floatingTop] and [ScreenLayout.floatingBottom]. Its left edge is [x], or it is
     * centered on the anchor when [centered]; it is kept on screen.
     *
     * @return whether the tooltip went below the anchor.
     */
    fun extract(
        g: GuiGraphicsExtractor,
        layout: ScreenLayout,
        lines: List<TipLine>,
        anchor: Rect,
        x: Int = anchor.x,
        centered: Boolean = false,
        preferBelow: Boolean = false,
    ): Boolean {
        val w = lines.maxOf { line -> line.text?.let(font::width) ?: 0 } + 2 * PADDING
        val h = lines.sumOf { if (it.text == null) DIVIDER_HEIGHT else LINE_HEIGHT } - (LINE_HEIGHT - font.lineHeight) + 2 * PADDING
        val fitsBelow = anchor.bottom + GAP + h <= layout.floatingBottom
        val fitsAbove = anchor.y - GAP - h >= layout.floatingTop
        val below = if (preferBelow) fitsBelow || !fitsAbove else !fitsAbove && fitsBelow
        val edge = ScreenLayout.MARGIN
        val left = (if (centered) anchor.x + (anchor.width - w) / 2 else x).coerceAtMost(layout.width - edge - w).coerceAtLeast(edge)
        val top = if (below) anchor.bottom + GAP else max(layout.floatingTop, anchor.y - GAP - h)

        // Above everything else, including the id suggestions.
        g.nextStratum()
        g.fill(left, top, left + w, top + h, Theme.DROPDOWN)
        g.outline(left, top, w, h, Theme.TEXT_DIM)
        var y = top + PADDING
        for (line in lines) {
            val text = line.text
            if (text == null) {
                g.fill(left + PADDING, y + DIVIDER_HEIGHT / 2 - 1, left + w - PADDING, y + DIVIDER_HEIGHT / 2, Theme.DIVIDER)
                y += DIVIDER_HEIGHT
            } else {
                g.text(font, text, left + PADDING, y, line.color)
                y += LINE_HEIGHT
            }
        }
        return below
    }

    private companion object {
        const val MAX_WIDTH = 220
        const val PADDING = 5
        const val LINE_HEIGHT = 10
        const val DIVIDER_HEIGHT = 7

        /** The space between a tooltip and its anchor. */
        const val GAP = 3
        const val DELAY_MS = 500L
        const val GRACE_MS = 300L
    }
}
