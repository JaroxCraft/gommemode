package de.jarox.gommemode.config.screen

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.GommemodeManager
import de.jarox.gommemode.config.ConfigManager
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Util
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Gommemode config screen: a search field on top, the pages in a sidebar on the left and the
 * options of the selected page (or the search results of all pages) on the right.
 *
 * Changes are made to a working copy and only take effect with "Apply" or "Done".
 */
class GommemodeConfigScreen(
    private val parent: Screen?,
) : Screen(Component.translatable("$KEY_PREFIX.title")) {
    private val state = EditState(Gommemode.toggleKeyMapping)
    private val pages = buildPages(state)
    private val allRows = pages.flatMap { it.rows }

    // Reopening the screen shows the page that was open last time.
    private var selectedPage = lastPage.coerceIn(pages.indices)
        set(value) {
            field = value
            lastPage = value
        }
    private var query = ""
    private var scroll = 0.0

    private val searchBox =
        EditBox(minecraft.font, 0, 0, 100, 9, Component.translatable("$KEY_PREFIX.search")).apply {
            setBordered(false)
            setMaxLength(64)
            setHint(Component.translatable("$KEY_PREFIX.search").withColor(Theme.TEXT_DIM))
            setResponder { text ->
                query = text.trim()
                scroll = 0.0
            }
        }

    /** The text field that receives keyboard input, if any. */
    private var focusedBox: EditBox? = null

    /** The slider that is being dragged, if any. */
    private var draggedRow: OptionRow? = null

    /** The highlighted entry of the id suggestion list. */
    private var suggestionIndex = 0

    /** Whether the player moved the suggestion highlight with Up/Down since the last edit. */
    private var suggestionNavigated = false

    /** Set when a key press was consumed; the character event that follows it is then ignored. */
    private var suppressNextChar = false

    private var hoveredRow: OptionRow? = null
    private var hoverStart = 0L
    private var hoverInstant = false
    private var lastTooltipAt = 0L

    /** Whether the scrollbar thumb is being dragged. */
    private var draggingScrollbar = false

    /** The row selected with the arrow keys (or last clicked); Enter, Space and the arrow keys act on it. */
    private var selectedRow: OptionRow? = null

    /** Whether the last input was the keyboard; the selected row then shows its tooltip. Moving the mouse ends it. */
    private var keyboardMode = false

    /** Whether [init] ran before; the search field only gets the initial focus on the first opening. */
    private var opened = false

    /**
     * Runs whenever this screen is shown, including after returning from a confirmation dialog.
     *
     * Switching screens turns off Minecraft's text input, and only focusing a field turns it on again,
     * so the focus is applied here rather than in the constructor. Like vanilla's world selection, the
     * search field is focused when the screen first opens.
     */
    override fun init() {
        super.init()
        val box = focusedBox ?: searchBox.takeUnless { opened }
        opened = true
        focusedBox?.isFocused = false
        focusedBox = null
        focus(box)
    }

    private val modVersion: String =
        FabricLoader
            .getInstance()
            .getModContainer(Gommemode.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("")

    private val hasErrors: Boolean get() = allRows.any { it.error() != null }
    private val isDirty: Boolean get() = allRows.any { it.isModified }

    // Reset All is disabled when there is nothing to reset.
    private val allDefault: Boolean get() = allRows.all { it.isDefault }

    // Layout. Everything is computed from the current screen size, so resizing needs no extra work.

    private val searchRect get() = Rect(MARGIN, MARGIN, width - 2 * MARGIN, BUTTON_HEIGHT)
    private val bodyTop get() = MARGIN + BUTTON_HEIGHT + GAP
    private val bottomY get() = height - MARGIN - BUTTON_HEIGHT
    private val sidebarRect get() = Rect(MARGIN, bodyTop, (width * 0.2).toInt().coerceIn(100, 150), bottomY - GAP - bodyTop)
    private val contentRect get() =
        Rect(
            sidebarRect.right + GAP,
            bodyTop,
            width - MARGIN - sidebarRect.right - GAP,
            bottomY - GAP - bodyTop,
        )
    private val resetAllRect get() = Rect(MARGIN, bottomY, font.width(RESET_ALL) + 20, BUTTON_HEIGHT)

    /** The width of Cancel, Apply and Done: 80, or less if the window is too narrow for all four buttons. */
    private val bottomButtonWidth get() = ((width - MARGIN - resetAllRect.right - 4 * GAP) / 3).coerceAtMost(80)
    private val doneRect get() = Rect(width - MARGIN - bottomButtonWidth, bottomY, bottomButtonWidth, BUTTON_HEIGHT)
    private val applyRect get() = Rect(doneRect.x - 4 - bottomButtonWidth, bottomY, bottomButtonWidth, BUTTON_HEIGHT)
    private val cancelRect get() = Rect(applyRect.x - 4 - bottomButtonWidth, bottomY, bottomButtonWidth, BUTTON_HEIGHT)

    private fun pageRect(index: Int) =
        Rect(sidebarRect.x, sidebarRect.y + SIDEBAR_HEADER + index * PAGE_HEIGHT, sidebarRect.width, PAGE_HEIGHT)

    /** An entry of the option list: a group title or an option row. */
    private sealed interface Item

    private class Header(
        val title: Component,
    ) : Item

    private class RowItem(
        val row: OptionRow,
    ) : Item

    /** An item with its position on screen. */
    private class Placed(
        val item: Item,
        val rect: Rect,
    ) {
        val row: OptionRow? get() = (item as? RowItem)?.row

        /** Where the control of the row is drawn. */
        val control: Rect get() = Rect(rect.right - 8 - (row?.controlWidth ?: 0), rect.y, row?.controlWidth ?: 0, rect.height)
        val resetIcon: Rect get() = Rect(control.x - ICON_SIZE - 6, rect.y + (rect.height - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE)
        val undoIcon: Rect get() = Rect(resetIcon.x - ICON_SIZE - 2, resetIcon.y, ICON_SIZE, ICON_SIZE)
    }

    /** The groups of the selected page, or all matching options grouped by page while searching. */
    private fun items(): List<Item> =
        if (query.isEmpty()) {
            pages[selectedPage].groups.flatMap { group -> listOf(Header(group.title)) + group.rows.map(::RowItem) }
        } else {
            pages.flatMap { page ->
                val matches = page.rows.filter { it.matches(query) }
                if (matches.isEmpty()) emptyList() else listOf(Header(page.title)) + matches.map(::RowItem)
            }
        }

    /** Group titles are taller than rows, so the space above a title is larger than the space below it. */
    private fun heightOf(item: Item) = if (item is Header) HEADER_HEIGHT else ITEM_HEIGHT

    private fun totalHeight(items: List<Item>) = items.sumOf(::heightOf)

    private fun maxScroll(items: List<Item>) = max(0, totalHeight(items) - contentRect.height).toDouble()

    /** The items with their current positions; rows outside the visible area are included too. */
    private fun placed(): List<Placed> {
        val content = contentRect
        var y = content.y - scroll.roundToInt()
        return items().map { item ->
            Placed(item, Rect(content.x, y, content.width - SCROLLBAR_SPACE, heightOf(item))).also { y += heightOf(item) }
        }
    }

    /** The thumb of the scrollbar, or `null` if everything fits. */
    private fun scrollThumb(items: List<Item>): Rect? {
        val maxScroll = maxScroll(items)
        if (maxScroll <= 0) return null
        val content = contentRect
        val thumbHeight = max(20, content.height * content.height / totalHeight(items))
        val thumbY = content.y + ((scroll / maxScroll) * (content.height - thumbHeight)).roundToInt()
        return Rect(content.right - 3, thumbY, 3, thumbHeight)
    }

    /** The area that reacts to scrollbar clicks: a bit wider than the thin bar itself. */
    private val scrollbarRect get() = Rect(contentRect.right - 6, contentRect.y, 6, contentRect.height)

    /** Scrolls so that the thumb center follows the mouse at [mouseY]. */
    private fun scrollTo(mouseY: Double) {
        val items = items()
        val thumb = scrollThumb(items) ?: return
        val content = contentRect
        val fraction = (mouseY - content.y - thumb.height / 2.0) / (content.height - thumb.height)
        scroll = (fraction * maxScroll(items)).coerceIn(0.0, maxScroll(items))
    }

    /** How many unsaved changes there are, shown on the Apply button. */
    private val modifiedCount: Int get() = allRows.count { it.isModified }

    /** The visible row at the given position, if any. */
    private fun placedAt(
        x: Double,
        y: Double,
    ): Placed? = if (contentRect.contains(x, y)) placed().firstOrNull { it.row != null && it.rect.contains(x, y) } else null

    override fun extractRenderState(
        g: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.extractRenderState(g, mouseX, mouseY, delta)
        scroll = scroll.coerceIn(0.0, maxScroll(items()))

        extractSearch(g, mouseX, mouseY, delta)
        extractSidebar(g, mouseX, mouseY)
        val placed = placed()
        extractOptions(g, placed, mouseX, mouseY, delta)
        extractButtons(g, mouseX, mouseY)
        extractSuggestions(g, placed, mouseX, mouseY)
        extractTooltips(g, placed, mouseX, mouseY)
        cursorAt(placed, mouseX.toDouble(), mouseY.toDouble())?.let(g::requestCursor)
    }

    /** The mouse cursor for the element under the mouse, like vanilla widgets: a hand on clickable things, a text cursor on fields. */
    private fun cursorAt(
        placed: List<Placed>,
        x: Double,
        y: Double,
    ): CursorType? {
        if (draggedRow != null) return CursorTypes.RESIZE_EW
        if (draggingScrollbar) return CursorTypes.RESIZE_NS
        if (suggestionEntries(placed).any { it.second.contains(x, y) }) return CursorTypes.POINTING_HAND
        if (searchRect.contains(x, y)) return CursorTypes.IBEAM
        if (cancelRect.contains(x, y) || (resetAllRect.contains(x, y) && !allDefault)) return CursorTypes.POINTING_HAND
        if (applyRect.contains(x, y) && isDirty && !hasErrors) return CursorTypes.POINTING_HAND
        if (doneRect.contains(x, y) && !hasErrors) return CursorTypes.POINTING_HAND
        if (pages.indices.any { pageRect(it).contains(x, y) }) return CursorTypes.POINTING_HAND
        if (scrollThumb(items()) != null && scrollbarRect.contains(x, y)) return CursorTypes.RESIZE_NS

        val entry = placedAt(x, y) ?: return null
        val row = entry.row ?: return null
        return when {
            !row.isDefault && entry.resetIcon.contains(x, y) -> CursorTypes.POINTING_HAND
            row.isModified && entry.undoIcon.contains(x, y) -> CursorTypes.POINTING_HAND
            row is ToggleRow -> CursorTypes.POINTING_HAND
            row.editBoxArea(entry.control)?.contains(x, y) == true -> CursorTypes.IBEAM
            entry.control.contains(x, y) -> CursorTypes.POINTING_HAND
            else -> null
        }
    }

    private fun extractSearch(
        g: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val search = searchRect
        g.fill(search.x, search.y, search.right, search.bottom, Theme.PANEL_DARK)
        if (searchBox.isFocused) g.outline(search.x, search.y, search.width, search.height, Theme.ACCENT)
        searchBox.setSize(search.width - 12, 9)
        searchBox.setX(search.x + 6)
        searchBox.setY(search.y + (search.height - 8) / 2)
        searchBox.extractRenderState(g, mouseX, mouseY, delta)
    }

    private fun extractSidebar(
        g: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        val sidebar = sidebarRect
        val pagesBottom = pageRect(pages.size - 1).bottom + 4
        g.fill(sidebar.x, sidebar.y, sidebar.right, pagesBottom, Theme.PANEL_DARK)

        g.blit(RenderPipelines.GUI_TEXTURED, ICON, sidebar.x + 6, sidebar.y + 7, 0f, 0f, 16, 16, 16, 16)
        g.text(font, Component.literal("Gommemode"), sidebar.x + 28, sidebar.y + 6, Theme.TEXT)
        g.text(font, Component.literal(modVersion), sidebar.x + 28, sidebar.y + 17, Theme.TEXT_DIM)

        pages.forEachIndexed { index, page ->
            val rect = pageRect(index)
            val selected = query.isEmpty() && index == selectedPage
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            when {
                selected -> {
                    g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.SELECTED)
                    g.fill(rect.x, rect.y, rect.x + 2, rect.bottom, Theme.TEXT)
                }

                hovered -> {
                    g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.HOVER)
                }
            }
            val matches = if (query.isEmpty()) -1 else page.rows.count { it.matches(query) }
            val titleColor =
                when {
                    selected -> Theme.TEXT
                    matches == 0 -> Theme.TEXT_DISABLED
                    else -> Theme.ACCENT
                }
            val textY = rect.y + (rect.height - 8) / 2
            g.text(font, page.title, rect.x + 10, textY, titleColor)

            // A marker shows pages with invalid values (red) or unsaved changes (yellow), so nothing is hidden on other pages.
            // The two markers also differ in shape ("!" and a dot), so they do not rely on color alone.
            var right = rect.right - 6
            when {
                page.rows.any { it.error() != null } -> {
                    Theme.pattern(g, ERROR_MARK, Rect(right - 4, rect.y, 4, rect.height), Theme.ERROR)
                    right -= 8
                }

                page.rows.any { it.isModified } -> {
                    g.fill(right - 4, rect.y + (rect.height - 4) / 2, right, rect.y + (rect.height + 4) / 2, Theme.MODIFIED)
                    right -= 8
                }
            }
            // While searching, each page shows how many of its options match.
            if (matches > 0) {
                val count = matches.toString()
                g.text(font, count, right - font.width(count), textY, Theme.TEXT_DIM)
            }
        }
    }

    private fun extractOptions(
        g: GuiGraphicsExtractor,
        placed: List<Placed>,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val content = contentRect
        val mouseInContent = content.contains(mouseX.toDouble(), mouseY.toDouble())
        g.enableScissor(content.x, content.y, content.right, content.bottom)

        if (placed.isEmpty()) {
            val centerX = content.x + content.width / 2
            g.centeredText(font, Component.translatable("$KEY_PREFIX.noResults", query), centerX, content.y + 20, Theme.TEXT)
            g.centeredText(font, Component.translatable("$KEY_PREFIX.noResults.hint"), centerX, content.y + 32, Theme.TEXT_DIM)
        }

        for (entry in placed) {
            val rect = entry.rect
            if (rect.bottom < content.y || rect.y > content.bottom) continue
            when (val item = entry.item) {
                is Header -> {
                    g.centeredText(font, item.title, rect.x + rect.width / 2, rect.bottom - 12, Theme.TEXT)
                }

                is RowItem -> {
                    val row = item.row
                    val hovered = mouseInContent && rect.contains(mouseX.toDouble(), mouseY.toDouble())
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
                    g.text(font, fittedLabel(entry, hovered), rect.x + LABEL_X, rect.y + (rect.height - 8) / 2, labelColor)
                    row.extractControl(g, font, entry.control, mouseX, mouseY, delta)

                    // Invalid values get a "!" next to the control, so the state does not rely on red alone.
                    if (row.error() != null && !hovered) Theme.pattern(g, ERROR_MARK, errorMarkRect(entry), Theme.ERROR)
                    if (hovered) extractRowIcons(g, entry, mouseX, mouseY)
                    // The row selected with the arrow keys gets a white frame, unlike the lighter background on hover.
                    if (row === selectedRow) g.outline(rect.x, rect.y, rect.width, rect.height, Theme.TEXT)
                }
            }
        }
        g.disableScissor()

        scrollThumb(items())?.let { thumb ->
            val thumbHovered = draggingScrollbar || scrollbarRect.contains(mouseX.toDouble(), mouseY.toDouble())
            g.fill(thumb.x, content.y, thumb.right, content.bottom, Theme.PANEL)
            g.fill(thumb.x, thumb.y, thumb.right, thumb.bottom, if (thumbHovered) Theme.TEXT else Theme.TEXT_DIM)
        }
    }

    /** The space left of the control (and of the undo/reset buttons while they show). */
    private fun labelSpace(
        entry: Placed,
        hovered: Boolean,
    ): Int {
        val row = entry.row ?: return 0
        val right =
            when {
                hovered && row.isModified -> entry.undoIcon.x
                hovered && !row.isDefault -> entry.resetIcon.x
                row.error() != null -> errorMarkRect(entry).x
                else -> entry.control.x
            }
        return right - 6 - (entry.rect.x + LABEL_X)
    }

    /** Where the "!" of an invalid row is drawn: just left of the control. */
    private fun errorMarkRect(entry: Placed) = Rect(entry.control.x - 10, entry.rect.y, 6, entry.rect.height)

    /** The label, marked with "*" while the row has a change that is not applied yet. */
    private fun labelText(row: OptionRow): String = if (row.isModified) row.label.string + " *" else row.label.string

    /** The label, cut with "…" if it would run into the control (e.g. long translations in small windows). */
    private fun fittedLabel(
        entry: Placed,
        hovered: Boolean,
    ): String {
        val label = entry.row?.let(::labelText) ?: return ""
        val space = labelSpace(entry, hovered)
        if (font.width(label) <= space) return label
        return font.plainSubstrByWidth(label, max(0, space - font.width(ELLIPSIS))).trimEnd() + ELLIPSIS
    }

    /** Draws the undo and reset icons of a hovered row, if they would change something. */
    private fun extractRowIcons(
        g: GuiGraphicsExtractor,
        entry: Placed,
        mouseX: Int,
        mouseY: Int,
    ) {
        val row = entry.row ?: return
        if (!row.isDefault) extractIcon(g, RESET_ICON, entry.resetIcon, mouseX, mouseY)
        if (row.isModified) extractIcon(g, UNDO_ICON, entry.undoIcon, mouseX, mouseY)
    }

    /**
     * Draws a small button with a pixel [icon] (one string per line, `X` = filled) centered in it.
     * Pixel patterns instead of font glyphs, because the arrow glyphs come from the fallback font
     * and are not centered reliably.
     */
    private fun extractIcon(
        g: GuiGraphicsExtractor,
        icon: List<String>,
        rect: Rect,
        mouseX: Int,
        mouseY: Int,
    ) {
        val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
        g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.PANEL_DARK)
        if (hovered) g.outline(rect.x, rect.y, rect.width, rect.height, Theme.ACCENT)
        Theme.pattern(g, icon, rect, if (hovered) Theme.ACCENT else Theme.TEXT)
    }

    private fun extractButtons(
        g: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
    ) {
        val mx = mouseX.toDouble()
        val my = mouseY.toDouble()
        Theme.button(g, font, RESET_ALL, resetAllRect, resetAllRect.contains(mx, my), active = !allDefault)
        // Cancel closes without applying; with unsaved changes it asks first, like Escape.
        Theme.button(g, font, CommonComponents.GUI_CANCEL, cancelRect, cancelRect.contains(mx, my))
        val count = modifiedCount
        val apply = if (count > 0) APPLY.copy().append(" ($count)") else APPLY
        Theme.button(g, font, apply, applyRect, applyRect.contains(mx, my), active = isDirty && !hasErrors)
        Theme.button(g, font, CommonComponents.GUI_DONE, doneRect, doneRect.contains(mx, my), active = !hasErrors)
    }

    /** The id row whose text field is focused, with its position, if its suggestion list should show. */
    private fun suggestionTarget(placed: List<Placed>): Pair<TextRow, Rect>? {
        val box = focusedBox ?: return null
        val entry = placed.firstOrNull { it.row?.editBox === box } ?: return null
        val row = entry.row as? TextRow ?: return null
        if (row.suggestions.isEmpty() || entry.rect.bottom < contentRect.y || entry.rect.y > contentRect.bottom) return null
        return row to entry.control
    }

    /** The suggestion list of the focused id field and the position of each entry. */
    private fun suggestionEntries(placed: List<Placed>): List<Pair<String, Rect>> {
        val (row, control) = suggestionTarget(placed) ?: return emptyList()
        val suggestions = row.matchingSuggestions().filter { it != row.editBox.value.trim() }
        if (suggestions.isEmpty()) return emptyList()
        val listHeight = suggestions.size * SUGGESTION_HEIGHT
        val below = control.bottom - 3
        val top = if (below + listHeight <= height - MARGIN) below else control.y + 3 - listHeight
        return suggestions.mapIndexed { index, id ->
            id to
                Rect(control.x, top + index * SUGGESTION_HEIGHT, control.width, SUGGESTION_HEIGHT)
        }
    }

    private fun extractSuggestions(
        g: GuiGraphicsExtractor,
        placed: List<Placed>,
        mouseX: Int,
        mouseY: Int,
    ) {
        val entries = suggestionEntries(placed)
        if (entries.isEmpty()) return
        suggestionIndex = suggestionIndex.coerceIn(0, entries.size - 1)
        // Draw above the option rows, whose text would otherwise cover the list.
        g.nextStratum()
        val first = entries.first().second
        val last = entries.last().second
        g.fill(first.x, first.y, first.right, last.bottom, Theme.DROPDOWN)
        g.outline(first.x, first.y, first.width, last.bottom - first.y, Theme.TEXT_DIM)
        entries.forEachIndexed { index, (id, rect) ->
            val hovered = rect.contains(mouseX.toDouble(), mouseY.toDouble())
            if (hovered || index == suggestionIndex) g.fill(rect.x + 1, rect.y, rect.right - 1, rect.bottom, Theme.SELECTED)
            g.text(font, font.plainSubstrByWidth(id, rect.width - 8), rect.x + 4, rect.y + 2, if (hovered) Theme.ACCENT else Theme.TEXT)
        }
    }

    private fun extractTooltips(
        g: GuiGraphicsExtractor,
        placed: List<Placed>,
        mouseX: Int,
        mouseY: Int,
    ) {
        val mx = mouseX.toDouble()
        val my = mouseY.toDouble()
        if (suggestionEntries(placed).any { it.second.contains(mx, my) }) return

        if (hasErrors && (applyRect.contains(mx, my) || doneRect.contains(mx, my))) {
            g.setTooltipForNextFrame(font, Component.translatable("$KEY_PREFIX.invalid"), mouseX, mouseY)
            return
        }
        if (resetAllRect.contains(mx, my)) {
            g.setTooltipForNextFrame(
                font,
                font.split(Component.translatable("$KEY_PREFIX.resetAll.tooltip"), TOOLTIP_WIDTH),
                mouseX,
                mouseY,
            )
            return
        }

        // With the arrow keys, the selected row shows its description below itself, since there is no hover.
        if (keyboardMode) {
            val selected = placed.firstOrNull { it.row != null && it.row === selectedRow }
            if (selected != null && selected.rect.bottom > contentRect.y && selected.rect.y < contentRect.bottom) {
                g.setTooltipForNextFrame(font, tooltipLines(selected), selected.rect.x + LABEL_X - 12, selected.rect.bottom + 4)
            }
            return
        }

        val entry = placedAt(mx, my)
        val row = entry?.row
        val now = Util.getMillis()
        if (row != hoveredRow) {
            hoveredRow = row
            hoverStart = now
            // Right after one tooltip, the next row shows its tooltip without the delay.
            hoverInstant = now - lastTooltipAt < TOOLTIP_GRACE_MS
        }
        if (entry == null || row == null) return

        when {
            !row.isDefault && entry.resetIcon.contains(mx, my) -> {
                g.setTooltipForNextFrame(font, Component.translatable("$KEY_PREFIX.reset"), mouseX, mouseY)
            }

            row.isModified && entry.undoIcon.contains(mx, my) -> {
                g.setTooltipForNextFrame(font, Component.translatable("$KEY_PREFIX.undo"), mouseX, mouseY)
            }

            hoverInstant || now - hoverStart >= TOOLTIP_DELAY_MS || (row.error() != null && entry.control.contains(mx, my)) -> {
                g.setTooltipForNextFrame(font, tooltipLines(entry), mouseX, mouseY)
                lastTooltipAt = now
            }
        }
    }

    /** The tooltip of a row: the full label if it is cut, the description, the default value and any error. */
    private fun tooltipLines(entry: Placed): List<FormattedCharSequence> {
        val row = entry.row ?: return emptyList()
        val lines = mutableListOf<FormattedCharSequence>()
        if (font.width(labelText(row)) > labelSpace(entry, hovered = true)) lines += font.split(row.label, TOOLTIP_WIDTH)
        lines += font.split(row.description.copy().withColor(Theme.TEXT_DIM), TOOLTIP_WIDTH)
        // Showing the default lets players judge a change without resetting it first.
        lines += font.split(Component.translatable("$KEY_PREFIX.default", row.defaultText()).withColor(Theme.TEXT_DIM), TOOLTIP_WIDTH)
        row.error()?.let { lines += font.split(it.copy().withColor(Theme.ERROR), TOOLTIP_WIDTH) }
        return lines
    }

    override fun mouseClicked(
        event: MouseButtonEvent,
        doubleClick: Boolean,
    ): Boolean {
        val x = event.x()
        val y = event.y()
        val placed = placed()
        keyboardMode = false

        suggestionEntries(placed).firstOrNull { it.second.contains(x, y) }?.let { (id, _) ->
            (suggestionTarget(placed)?.first)?.accept(id)
            return true
        }

        var newFocus: EditBox? = null
        var keyRowClicked: KeyRow? = null
        when {
            searchRect.contains(x, y) -> {
                newFocus = searchBox
            }

            cancelRect.contains(x, y) -> {
                Theme.playClick()
                onClose()
            }

            sidebarRect.contains(x, y) -> {
                pages.indices.firstOrNull { pageRect(it).contains(x, y) }?.let { index ->
                    Theme.playClick()
                    selectedPage = index
                    searchBox.value = ""
                    scroll = 0.0
                }
            }

            scrollThumb(items()) != null && scrollbarRect.contains(x, y) -> {
                draggingScrollbar = true
                scrollTo(y)
            }

            contentRect.contains(x, y) -> {
                val entry = placedAt(x, y)
                val row = entry?.row
                if (entry != null && row != null) {
                    // The arrow keys continue from the row that was clicked last.
                    selectedRow = row
                    when {
                        !row.isDefault && entry.resetIcon.contains(x, y) -> {
                            Theme.playClick()
                            row.reset()
                        }

                        row.isModified && entry.undoIcon.contains(x, y) -> {
                            Theme.playClick()
                            row.undo()
                        }

                        // A checkbox toggles from anywhere in its row.
                        row is ToggleRow -> {
                            row.clickControl(event, entry.control, doubleClick)
                        }

                        entry.control.contains(x, y) && row.clickControl(event, entry.control, doubleClick) -> {
                            if (row is KeyRow) keyRowClicked = row
                            if (row.editBoxArea(entry.control)?.contains(x, y) == true) {
                                newFocus = row.editBox
                            } else if (row is SliderRow<*>) {
                                draggedRow = row
                            }
                        }
                    }
                }
            }

            resetAllRect.contains(x, y) && !allDefault -> {
                Theme.playClick()
                confirmResetAll()
            }

            applyRect.contains(x, y) && isDirty && !hasErrors -> {
                Theme.playClick()
                apply()
            }

            doneRect.contains(x, y) && !hasErrors -> {
                Theme.playClick()
                apply()
                minecraft.gui.setScreen(parent)
            }
        }

        allRows.filterIsInstance<KeyRow>().filter { it !== keyRowClicked }.forEach { it.listening = false }
        focus(newFocus)
        newFocus?.onClick(event, doubleClick)
        return true
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dx: Double,
        dy: Double,
    ): Boolean {
        if (draggingScrollbar) {
            scrollTo(event.y())
            return true
        }
        val row = draggedRow ?: return super.mouseDragged(event, dx, dy)
        val entry = placed().firstOrNull { it.row === row } ?: return true
        row.dragControl(event.x(), entry.control)
        return true
    }

    /** Moving the mouse hands control back from the keyboard, so tooltips follow the mouse again. */
    override fun mouseMoved(
        mouseX: Double,
        mouseY: Double,
    ) {
        keyboardMode = false
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggedRow = null
        draggingScrollbar = false
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        if (minecraft.hasShiftDown()) {
            val entry = placedAt(mouseX, mouseY)
            if (entry != null && entry.control.contains(mouseX, mouseY) && entry.row?.scrollControl(scrollY) == true) return true
        }
        if (contentRect.contains(mouseX, mouseY)) {
            scroll = (scroll - scrollY * ITEM_HEIGHT).coerceIn(0.0, maxScroll(items()))
            return true
        }
        return false
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        suppressNextChar = false
        allRows.filterIsInstance<KeyRow>().firstOrNull { it.listening }?.let { row ->
            if (event.key() == InputConstants.KEY_ESCAPE) row.listening = false else row.assign(InputConstants.getKey(event))
            // The character of the assigned key must not reach the search or a text field.
            suppressNextChar = true
            return true
        }

        // Ctrl+F jumps to the search field and selects its text.
        if (event.hasControlDown() && event.key() == InputConstants.KEY_F) {
            focus(searchBox)
            searchBox.moveCursorToEnd(false)
            searchBox.setHighlightPos(0)
            return true
        }

        val box = focusedBox
        if (box != null) {
            val row = allRows.firstOrNull { it.editBox === box } as? TextRow
            val suggestions = suggestionEntries(placed())
            when (event.key()) {
                InputConstants.KEY_ESCAPE -> {
                    // The search field is focused by default, so Escape there closes the screen like in vanilla.
                    if (box === searchBox) onClose() else focus(null)
                }

                InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                    if (row != null &&
                        suggestions.isNotEmpty() &&
                        // A valid typed id stays; a suggestion only replaces it when picked or when the id is invalid.
                        (row.error() != null || suggestionNavigated)
                    ) {
                        row.accept(suggestions[suggestionIndex.coerceIn(suggestions.indices)].first)
                    }
                    focus(null)
                }

                InputConstants.KEY_TAB -> {
                    if (row != null &&
                        suggestions.isNotEmpty()
                    ) {
                        row.accept(suggestions[suggestionIndex.coerceIn(suggestions.indices)].first)
                    }
                }

                InputConstants.KEY_DOWN -> {
                    when {
                        suggestions.isNotEmpty() -> {
                            suggestionIndex = (suggestionIndex + 1) % suggestions.size
                            suggestionNavigated = true
                        }
                        // From the search field, Down moves into the option list.
                        box === searchBox -> {
                            focus(null)
                            visibleRows().firstOrNull()?.let(::select)
                        }
                    }
                }

                InputConstants.KEY_UP -> {
                    if (suggestions.isNotEmpty()) {
                        suggestionIndex = (suggestionIndex - 1 + suggestions.size) % suggestions.size
                        suggestionNavigated = true
                    }
                }

                else -> {
                    box.keyPressed(event)
                    suggestionIndex = 0
                    suggestionNavigated = false
                }
            }
            return true
        }

        if (handleNavigation(event)) return true

        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    /** The option rows currently listed (the selected page, or the search results). */
    private fun visibleRows(): List<OptionRow> = items().mapNotNull { (it as? RowItem)?.row }

    /** Selects [row] for the keyboard and scrolls it into view. */
    private fun select(row: OptionRow) {
        selectedRow = row
        keyboardMode = true
        val content = contentRect
        val rect = placed().firstOrNull { it.row === row }?.rect ?: return
        if (rect.y < content.y) scroll -= content.y - rect.y
        if (rect.bottom > content.bottom) scroll += rect.bottom - content.bottom
        scroll = scroll.coerceIn(0.0, maxScroll(items()))
    }

    /**
     * Keyboard control of the option list: Up/Down select a row (Up from the first row returns to the search),
     * Left/Right change its value, Enter/Space toggles, cycles, edits or rebinds it, Tab switches pages.
     */
    private fun handleNavigation(event: KeyEvent): Boolean {
        val rows = visibleRows()
        val current = selectedRow?.takeIf { it in rows }
        when (event.key()) {
            InputConstants.KEY_DOWN -> {
                val next = if (current == null) rows.firstOrNull() else rows.getOrNull(rows.indexOf(current) + 1)
                next?.let(::select)
            }

            InputConstants.KEY_UP -> {
                val index = current?.let(rows::indexOf) ?: 0
                if (index <= 0) {
                    selectedRow = null
                    focus(searchBox)
                } else {
                    select(rows[index - 1])
                }
            }

            InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT -> {
                val row = current ?: return false
                row.adjust(if (event.key() == InputConstants.KEY_LEFT) -1 else 1)
                keyboardMode = true
            }

            InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_SPACE -> {
                val row = current ?: return false
                keyboardMode = true
                if (!row.activate()) {
                    row.editBox?.let { box ->
                        // Space would otherwise also type a space into the field.
                        suppressNextChar = true
                        focus(box)
                        box.moveCursorToEnd(false)
                    }
                }
            }

            InputConstants.KEY_TAB -> {
                if (query.isNotEmpty()) return false
                val step = if (event.hasShiftDown()) -1 else 1
                selectedPage = (selectedPage + step + pages.size) % pages.size
                scroll = 0.0
                visibleRows().firstOrNull()?.let(::select)
            }

            else -> {
                return false
            }
        }
        return true
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (suppressNextChar) {
            suppressNextChar = false
            return true
        }
        val box = focusedBox
        if (box != null) {
            suggestionIndex = 0
            suggestionNavigated = false
            return box.charTyped(event)
        }
        // Typing without a focused field starts a search.
        if (event.isAllowedChatCharacter && !Character.isWhitespace(event.codepoint())) {
            focus(searchBox)
            return searchBox.charTyped(event)
        }
        return false
    }

    /** Moves keyboard focus to [box], or removes it for `null`. */
    private fun focus(box: EditBox?) {
        if (focusedBox === box) return
        focusedBox?.let { old ->
            old.isFocused = false
            allRows.firstOrNull { it.editBox === old }?.onFocusLost()
        }
        focusedBox = box
        box?.isFocused = true
        suggestionIndex = 0
        suggestionNavigated = false
    }

    /** Asks before resetting every option, including the toggle key, to its default; the dialog names how many change. */
    private fun confirmResetAll() {
        minecraft.gui.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) allRows.forEach { it.reset() }
                    minecraft.gui.setScreen(this)
                },
                Component.translatable("$KEY_PREFIX.resetAll.title"),
                Component.translatable("$KEY_PREFIX.resetAll.message", allRows.count { !it.isDefault }),
                Component.translatable("$KEY_PREFIX.resetAll.confirm"),
                Component.translatable("$KEY_PREFIX.resetAll.keep"),
            ),
        )
    }

    /** Makes the edited values the active config, saves it and applies it to a running Gommemode. */
    private fun apply() {
        if (hasErrors) return
        ConfigManager.update(state.pending)
        state.saved = ConfigManager.copyOf(ConfigManager.config)

        val keyMapping = state.keyMapping
        if (keyMapping != null && state.pendingKey != state.savedKey) {
            keyMapping.setKey(state.pendingKey)
            KeyMapping.resetMapping()
            minecraft.options.save()
            state.savedKey = state.pendingKey
        }

        GommemodeManager.onConfigChanged()
        Gommemode.LOGGER.debug("Applied config from the config screen")
    }

    /** Closes the screen; asks first if there are changes that were not applied. */
    override fun onClose() {
        if (!isDirty) {
            minecraft.gui.setScreen(parent)
            return
        }
        minecraft.gui.setScreen(
            ConfirmScreen(
                { discard -> minecraft.gui.setScreen(if (discard) parent else this) },
                Component.translatable("$KEY_PREFIX.discard.title"),
                Component.translatable("$KEY_PREFIX.discard.message", allRows.count { it.isModified }),
                Component.translatable("$KEY_PREFIX.discard.confirm"),
                Component.translatable("$KEY_PREFIX.discard.keep"),
            ),
        )
    }

    private companion object {
        const val MARGIN = 8
        const val GAP = 6
        const val BUTTON_HEIGHT = 20
        const val ITEM_HEIGHT = 20
        const val SIDEBAR_HEADER = 30
        const val PAGE_HEIGHT = 18
        const val SCROLLBAR_SPACE = 5
        const val SUGGESTION_HEIGHT = 12
        const val TOOLTIP_WIDTH = 220
        const val TOOLTIP_DELAY_MS = 500L
        const val TOOLTIP_GRACE_MS = 300L

        /** The page shown when the screen opens; remembered while the game runs. */
        var lastPage = 0

        /** An exclamation mark: the row or page has an invalid value. */
        val ERROR_MARK = listOf("XX", "XX", "XX", "XX", "..", "XX")
        const val ICON_SIZE = 16
        const val HEADER_HEIGHT = 26
        const val LABEL_X = 8
        const val ELLIPSIS = "…"

        /** A circular arrow: back to the default. */
        val RESET_ICON =
            listOf(
                "...XXXX...",
                ".XX....X.X",
                ".X......XX",
                "X.....XXXX",
                "X........X",
                "X........X",
                "X........X",
                ".X......X.",
                ".XX....XX.",
                "...XXXX...",
            )

        /** A hooked arrow pointing left: undo the change. */
        val UNDO_ICON =
            listOf(
                "...X......",
                "..XX......",
                ".XXXXXXX..",
                "..XX....X.",
                "...X.....X",
                ".........X",
                ".........X",
                "........X.",
                "......XX..",
                "....XX....",
            )

        val ICON: Identifier = Identifier.fromNamespaceAndPath(Gommemode.MOD_ID, "icon.png")
        val RESET_ALL: Component = Component.translatable("$KEY_PREFIX.resetAll")
        val APPLY: Component = Component.translatable("$KEY_PREFIX.apply")
    }
}
