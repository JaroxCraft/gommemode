package de.jarox.gommemode.config.screen.components

import de.jarox.gommemode.config.screen.rows.TextRow
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * The list of matching ids below a focused id field. Remembers which entry is highlighted and whether the
 * player picked it with Up/Down, so Enter only replaces a valid typed id with a suggestion that was chosen.
 */
internal class SuggestionList {
    private var highlighted = 0
    private var navigated = false

    /** The list as it is open below [row]'s field: each suggested id with its position. */
    class Open(
        val row: TextRow,
        val entries: List<Pair<String, Rect>>,
    ) {
        /** The id at ([x], [y]), if the mouse is on the list. */
        fun idAt(
            x: Double,
            y: Double,
        ): String? = entries.firstOrNull { it.second.contains(x, y) }?.first
    }

    /**
     * The list for the id field of [target], or `null` if nothing to suggest. It opens below the field,
     * or above it when it would reach past [maxBottom]. The typed id itself is not suggested.
     */
    fun open(
        target: PlacedRow?,
        maxBottom: Int,
    ): Open? {
        val row = target?.row as? TextRow ?: return null
        val ids = row.matchingSuggestions().filter { it != row.editBox.value.trim() }
        if (ids.isEmpty()) return null
        val control = target.control
        val listHeight = ids.size * ENTRY_HEIGHT
        val below = control.bottom - 3
        val top = if (below + listHeight <= maxBottom) below else control.y + 3 - listHeight
        return Open(row, ids.mapIndexed { index, id -> id to Rect(control.x, top + index * ENTRY_HEIGHT, control.width, ENTRY_HEIGHT) })
    }

    /** Forgets the highlight, e.g. after typing or when another field gets focus. */
    fun reset() {
        highlighted = 0
        navigated = false
    }

    /** Moves the highlight by [step] entries, wrapping around. */
    fun move(
        open: Open,
        step: Int,
    ) {
        highlighted = Math.floorMod(highlightedIn(open) + step, open.entries.size)
        navigated = true
    }

    /** Puts the highlighted id into the field (Tab). */
    fun acceptHighlighted(open: Open) {
        open.row.accept(open.entries[highlightedIn(open)].first)
    }

    /** Enter: keeps a valid typed id; a suggestion only replaces it when picked with Up/Down or when the id is invalid. */
    fun acceptOnEnter(open: Open) {
        if (navigated || open.row.error() != null) acceptHighlighted(open)
    }

    /** Puts the clicked id into the field; returns whether the click was on the list. */
    fun click(
        open: Open,
        x: Double,
        y: Double,
    ): Boolean {
        val id = open.idAt(x, y) ?: return false
        open.row.accept(id)
        // The clicked id is the choice now; a later Enter must not swap it for the highlighted suggestion.
        reset()
        return true
    }

    fun extract(
        g: GuiGraphicsExtractor,
        font: Font,
        open: Open,
        mouseX: Int,
        mouseY: Int,
    ) {
        // Draw above the option rows, whose text would otherwise cover the list.
        g.nextStratum()
        val first = open.entries.first().second
        val last = open.entries.last().second
        g.fill(first.x, first.y, first.right, last.bottom, Theme.DROPDOWN)
        g.outline(first.x, first.y, first.width, last.bottom - first.y, Theme.TEXT_DIM)
        val current = highlightedIn(open)
        open.entries.forEachIndexed { index, (id, rect) ->
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            if (hovered || index == current) g.fill(rect.x + 1, rect.y, rect.right - 1, rect.bottom, Theme.SELECTED)
            g.text(font, font.plainSubstrByWidth(id, rect.width - 8), rect.x + 4, rect.y + 2, if (hovered) Theme.ACCENT else Theme.TEXT)
        }
    }

    /** The highlighted index, kept inside the list, which gets shorter while typing. */
    private fun highlightedIn(open: Open): Int = highlighted.coerceIn(open.entries.indices)

    private companion object {
        const val ENTRY_HEIGHT = 12
    }
}
