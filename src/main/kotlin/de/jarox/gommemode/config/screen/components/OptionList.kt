package de.jarox.gommemode.config.screen.components

import de.jarox.gommemode.config.screen.KEY_PREFIX
import de.jarox.gommemode.config.screen.Page
import de.jarox.gommemode.config.screen.rows.OptionRow
import de.jarox.gommemode.config.screen.theme.Icons
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.roundToInt

/** An entry of the option list: a group title or an option row. */
private sealed interface ListEntry {
    val height: Int
}

private class HeaderEntry(
    val title: Component,
) : ListEntry {
    // Group titles are taller than rows, so the space above a title is larger than the space below it.
    override val height = OptionList.HEADER_HEIGHT
}

private class RowEntry(
    val row: OptionRow,
) : ListEntry {
    override val height = OptionList.ITEM_HEIGHT
}

/** A list entry at its position on screen; entries scrolled out of view are placed too. */
internal sealed class Placed(
    val rect: Rect,
)

internal class PlacedHeader(
    val title: Component,
    rect: Rect,
) : Placed(rect)

internal class PlacedRow(
    val row: OptionRow,
    rect: Rect,
) : Placed(rect) {
    /** Where the control of the row is drawn. */
    val control = Rect(rect.right - 8 - row.controlWidth, rect.y, row.controlWidth, rect.height)
    val resetIcon = Rect(control.x - ICON_SIZE - 6, rect.y + (rect.height - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE)
    val undoIcon = Rect(resetIcon.x - ICON_SIZE - 2, resetIcon.y, ICON_SIZE, ICON_SIZE)

    /** Where the "!" of an invalid row is drawn: just left of the control. */
    val errorMark = Rect(control.x - 10, rect.y, 6, rect.height)

    val labelX = rect.x + LABEL_X

    /** Whether the reset icon shows, and so takes clicks. */
    val hasReset: Boolean get() = !row.isDefault

    /** The space for the label, left of the control (and of the undo/reset icons while they show on [hovered]). */
    fun labelSpace(hovered: Boolean): Int {
        val right =
            when {
                hovered && row.showsUndo -> undoIcon.x
                hovered && hasReset -> resetIcon.x
                row.error() != null -> errorMark.x
                else -> control.x
            }
        return right - 6 - labelX
    }

    /** Whether the label does not fit next to the hovered row's icons, so it is cut with "…". */
    fun isLabelCut(font: Font): Boolean = font.width(row.listedLabel) > labelSpace(hovered = true)

    /** Where [icon] is drawn. */
    fun rectOf(icon: RowIcon): Rect =
        when (icon) {
            RowIcon.RESET -> resetIcon
            RowIcon.UNDO -> undoIcon
        }

    /** Whether ([x], [y]) is on an icon that is showing: reset, undo, or `null` for neither. */
    fun iconAt(
        x: Double,
        y: Double,
    ): RowIcon? =
        when {
            hasReset && resetIcon.contains(x, y) -> RowIcon.RESET
            row.showsUndo && undoIcon.contains(x, y) -> RowIcon.UNDO
            else -> null
        }

    private companion object {
        const val ICON_SIZE = 16
        const val LABEL_X = 8
    }
}

/** The two buttons a hovered row shows next to its control. */
internal enum class RowIcon(
    val pixels: List<String>,
    val tooltip: Component,
) {
    RESET(Icons.RESET, Component.translatable("$KEY_PREFIX.reset")),
    UNDO(Icons.UNDO, Component.translatable("$KEY_PREFIX.undo")),
}

/**
 * The scrollable list on the right: the groups of the selected page, or all matching options grouped by
 * page while a search runs. The screen sets [area] whenever its size changes.
 */
internal class OptionList(
    private val pages: List<Page>,
) {
    var area = Rect(0, 0, 0, 0)

    /** The page shown when no search runs. Reopening the screen shows the page that was open last time. */
    var selectedPage = lastPage.coerceIn(pages.indices)
        set(value) {
            field = value
            lastPage = value
            scroll = 0.0
        }

    var query = ""
        set(value) {
            field = value
            scroll = 0.0
        }

    private var scroll = 0.0

    /** Whether the scrollbar thumb is being dragged. */
    var draggingScrollbar = false
        private set

    private fun entries(): List<ListEntry> =
        if (query.isEmpty()) {
            pages[selectedPage].groups.flatMap { group -> listOf(HeaderEntry(group.title)) + group.rows.map(::RowEntry) }
        } else {
            pages.flatMap { page ->
                val matches = page.rows.filter { it.matches(query) }
                if (matches.isEmpty()) emptyList() else listOf(HeaderEntry(page.title)) + matches.map(::RowEntry)
            }
        }

    /** How many options of [page] match the search. */
    fun matchCount(page: Page): Int = page.rows.count { it.matches(query) }

    /** The rows currently listed, in order. */
    fun visibleRows(): List<OptionRow> = entries().mapNotNull { (it as? RowEntry)?.row }

    /** The entries with their current positions. */
    private fun placed(): List<Placed> {
        var y = area.y - scroll.roundToInt()
        return entries().map { entry ->
            val rect = Rect(area.x, y, area.width - SCROLLBAR_SPACE, entry.height)
            y += entry.height
            when (entry) {
                is HeaderEntry -> PlacedHeader(entry.title, rect)
                is RowEntry -> PlacedRow(entry.row, rect)
            }
        }
    }

    /** The rows with their current positions. */
    fun placedRows(): List<PlacedRow> = placed().filterIsInstance<PlacedRow>()

    /** The row at the given position, if it is in the visible part of the list. */
    fun rowAt(
        x: Double,
        y: Double,
    ): PlacedRow? = if (area.contains(x, y)) placedRows().firstOrNull { it.rect.contains(x, y) } else null

    /** Whether [placed] is at least partly inside the visible part of the list. */
    fun isOnScreen(placed: Placed): Boolean = placed.rect.bottom > area.y && placed.rect.y < area.bottom

    // Scrolling

    private fun maxScroll(entries: List<ListEntry>): Double = max(0, entries.sumOf { it.height } - area.height).toDouble()

    /** Keeps the scroll position valid, e.g. after the list got shorter or the window larger. */
    fun clampScroll() {
        scroll = scroll.coerceIn(0.0, maxScroll(entries()))
    }

    /** Scrolls by [rows] rows; positive values scroll up, like the mouse wheel. */
    fun scrollBy(rows: Double) {
        scroll -= rows * ITEM_HEIGHT
        clampScroll()
    }

    /** Scrolls just enough to show [row] completely. */
    fun scrollIntoView(row: OptionRow) {
        val rect = placedRows().firstOrNull { it.row === row }?.rect ?: return
        if (rect.y < area.y) scroll -= area.y - rect.y
        if (rect.bottom > area.bottom) scroll += rect.bottom - area.bottom
        clampScroll()
    }

    /** The thumb of the scrollbar, or `null` if everything fits. */
    private fun scrollThumb(): Rect? {
        val entries = entries()
        val maxScroll = maxScroll(entries)
        if (maxScroll <= 0) return null
        val thumbHeight = max(20, area.height * area.height / entries.sumOf { it.height })
        val thumbY = area.y + ((scroll / maxScroll) * (area.height - thumbHeight)).roundToInt()
        return Rect(area.right - 3, thumbY, 3, thumbHeight)
    }

    /** The area that reacts to scrollbar clicks: a bit wider than the thin bar itself. */
    private val scrollbar: Rect get() = Rect(area.right - 6, area.y, 6, area.height)

    /** Whether ([x], [y]) is on the scrollbar, which only shows when the list does not fit. */
    fun isOnScrollbar(
        x: Double,
        y: Double,
    ): Boolean = scrollThumb() != null && scrollbar.contains(x, y)

    /** Starts dragging the scrollbar thumb at the mouse height [y]. */
    fun startScrollbarDrag(y: Double) {
        draggingScrollbar = true
        dragScrollbar(y)
    }

    /** Scrolls so that the thumb center follows the mouse at [y]. */
    fun dragScrollbar(y: Double) {
        val thumb = scrollThumb() ?: return
        val fraction = (y - area.y - thumb.height / 2.0) / (area.height - thumb.height)
        scroll = fraction * maxScroll(entries())
        clampScroll()
    }

    fun stopScrollbarDrag() {
        draggingScrollbar = false
    }

    // Drawing

    /** Draws the list, highlighting the hovered row and framing [selectedRow]. */
    fun extract(
        g: GuiGraphicsExtractor,
        font: Font,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        selectedRow: OptionRow?,
    ) {
        val placed = placed()
        val mx = mouseX.toDouble()
        val my = mouseY.toDouble()
        g.enableScissor(area.x, area.y, area.right, area.bottom)
        if (placed.isEmpty()) extractNoResults(g, font)
        for (entry in placed) {
            if (!isOnScreen(entry)) continue
            when (entry) {
                is PlacedHeader -> {
                    g.centeredText(font, entry.title, entry.rect.x + entry.rect.width / 2, entry.rect.bottom - 12, Theme.TEXT)
                }

                is PlacedRow -> {
                    val hovered = area.contains(mx, my) && entry.rect.contains(mx, my)
                    extractRow(g, font, entry, hovered, mouseX, mouseY, delta)
                    // The row selected with the arrow keys gets a white frame, unlike the lighter background on hover.
                    if (entry.row === selectedRow) g.outline(entry.rect.x, entry.rect.y, entry.rect.width, entry.rect.height, Theme.TEXT)
                }
            }
        }
        g.disableScissor()

        scrollThumb()?.let { thumb ->
            val thumbHovered = draggingScrollbar || scrollbar.contains(mx, my)
            g.fill(thumb.x, area.y, thumb.right, area.bottom, Theme.PANEL)
            g.fill(thumb.x, thumb.y, thumb.right, thumb.bottom, if (thumbHovered) Theme.TEXT else Theme.TEXT_DIM)
        }
    }

    private fun extractNoResults(
        g: GuiGraphicsExtractor,
        font: Font,
    ) {
        val centerX = area.x + area.width / 2
        g.centeredText(font, Component.translatable("$KEY_PREFIX.noResults", query), centerX, area.y + 20, Theme.TEXT)
        g.centeredText(font, Component.translatable("$KEY_PREFIX.noResults.hint"), centerX, area.y + 32, Theme.TEXT_DIM)
    }

    private fun extractRow(
        g: GuiGraphicsExtractor,
        font: Font,
        entry: PlacedRow,
        hovered: Boolean,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val row = entry.row
        val rect = entry.rect
        g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.PANEL)
        if (hovered) g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.HOVER)
        // Values that differ from the default get a colored bar on the left.
        if (!row.isDefault) g.fill(rect.x, rect.y, rect.x + 3, rect.bottom, Theme.ACCENT)

        val labelColor =
            when {
                row.error() != null -> Theme.ERROR
                row.isModified -> Theme.MODIFIED
                else -> Theme.TEXT
            }
        g.text(font, fittedLabel(font, entry, hovered), entry.labelX, rect.y + (rect.height - 8) / 2, labelColor)
        row.extractControl(g, font, entry.control, mouseX, mouseY, delta)

        // Invalid values get a "!" next to the control, so the state does not rely on red alone.
        if (row.error() != null && !hovered) Theme.pattern(g, Icons.ERROR_MARK, entry.errorMark, Theme.ERROR)
        if (hovered) {
            if (entry.hasReset) extractIcon(g, RowIcon.RESET, entry.rectOf(RowIcon.RESET), mouseX, mouseY)
            if (row.showsUndo) extractIcon(g, RowIcon.UNDO, entry.rectOf(RowIcon.UNDO), mouseX, mouseY)
        }
    }

    /** The label, cut with "…" if it would run into the control (e.g. long translations in small windows). */
    private fun fittedLabel(
        font: Font,
        entry: PlacedRow,
        hovered: Boolean,
    ): String {
        val label = entry.row.listedLabel
        val space = entry.labelSpace(hovered)
        if (font.width(label) <= space) return label
        return font.plainSubstrByWidth(label, max(0, space - font.width(ELLIPSIS))).trimEnd() + ELLIPSIS
    }

    /**
     * Draws a small button with a pixel icon centered in it.
     * Pixel patterns instead of font glyphs, because the arrow glyphs come from the fallback font
     * and are not centered reliably.
     */
    private fun extractIcon(
        g: GuiGraphicsExtractor,
        icon: RowIcon,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
    ) {
        val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
        g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.PANEL_DARK)
        if (hovered) g.outline(rect.x, rect.y, rect.width, rect.height, Theme.ACCENT)
        Theme.pattern(g, icon.pixels, rect, if (hovered) Theme.ACCENT else Theme.TEXT)
    }

    companion object {
        const val ITEM_HEIGHT = 20
        const val HEADER_HEIGHT = 26
        const val SCROLLBAR_SPACE = 5
        const val ELLIPSIS = "…"

        /** The page shown when the screen opens; remembered while the game runs. */
        private var lastPage = 0
    }
}
