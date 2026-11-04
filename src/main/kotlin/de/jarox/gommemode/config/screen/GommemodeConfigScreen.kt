package de.jarox.gommemode.config.screen

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.GommemodeManager
import de.jarox.gommemode.config.ConfigManager
import de.jarox.gommemode.config.screen.components.ButtonBar
import de.jarox.gommemode.config.screen.components.ButtonBar.Button
import de.jarox.gommemode.config.screen.components.OptionList
import de.jarox.gommemode.config.screen.components.PlacedRow
import de.jarox.gommemode.config.screen.components.RowIcon
import de.jarox.gommemode.config.screen.components.SearchField
import de.jarox.gommemode.config.screen.components.Sidebar
import de.jarox.gommemode.config.screen.components.SuggestionList
import de.jarox.gommemode.config.screen.components.Tooltips
import de.jarox.gommemode.config.screen.rows.KeyRow
import de.jarox.gommemode.config.screen.rows.OptionRow
import de.jarox.gommemode.config.screen.rows.RowSummary
import de.jarox.gommemode.config.screen.rows.SliderRow
import de.jarox.gommemode.config.screen.rows.ToggleRow
import de.jarox.gommemode.config.screen.theme.Theme
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * The Gommemode config screen: a search field on top, the pages in a [Sidebar] on the left and the
 * [OptionList] of the selected page (or the search results of all pages) on the right.
 *
 * Changes are made to a working copy and only take effect with "Apply" or "Done".
 * This class owns the edit state, routes input to the parts in `components` and applies, resets and closes.
 */
class GommemodeConfigScreen(
    private val parent: Screen?,
) : Screen(Component.translatable("$KEY_PREFIX.title")) {
    private val state = EditState(Gommemode.toggleKeyMapping)
    private val pages = buildPages(state)
    private val summary = RowSummary(pages.flatMap { it.rows })
    private val allRows = summary.rows

    private val list = OptionList(pages)
    private val sidebar = Sidebar(pages, list)
    private val suggestions = SuggestionList()
    private val tooltips = Tooltips(minecraft.font)
    private val buttons = ButtonBar(summary)
    private var layout = ScreenLayout(0, 0, 0)

    private val search = SearchField(minecraft.font) { text -> list.query = text }
    private val searchBox = search.box

    /** The text field that receives keyboard input, if any. */
    private var focusedBox: EditBox? = null

    /** The slider that is being dragged, if any. */
    private var draggedRow: OptionRow? = null

    /** Set when a key press was consumed; the character event that follows it is then ignored. */
    private var suppressNextChar = false

    /** The row selected with the arrow keys (or last clicked); Enter, Space and the arrow keys act on it. */
    private var selectedRow: OptionRow? = null

    /** Whether the last input was the keyboard; the selected row then shows its tooltip. Moving the mouse ends it. */
    private var keyboardMode = false

    /** Whether [init] ran before; the search field only gets the initial focus on the first opening. */
    private var opened = false

    /**
     * Runs whenever this screen is shown or resized, including after returning from a confirmation dialog.
     *
     * Switching screens turns off Minecraft's text input, and only focusing a field turns it on again,
     * so the focus is applied here rather than in the constructor. Like vanilla's world selection, the
     * search field is focused when the screen first opens.
     */
    override fun init() {
        super.init()
        layout = ScreenLayout(width, height, ButtonBar.resetAllWidth(font))
        list.area = layout.content

        val box = focusedBox ?: searchBox.takeUnless { opened }
        opened = true
        focusedBox?.isFocused = false
        focusedBox = null
        focus(box)
    }

    private fun press(button: Button) {
        Theme.playClick()
        when (button) {
            Button.RESET_ALL -> {
                confirmResetAll()
            }

            // Cancel closes without applying; with unsaved changes it asks first, like Escape.
            Button.CANCEL -> {
                onClose()
            }

            Button.APPLY -> {
                apply()
            }

            Button.DONE -> {
                apply()
                minecraft.gui.setScreen(parent)
            }
        }
    }

    // Drawing

    override fun extractRenderState(
        g: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.extractRenderState(g, mouseX, mouseY, delta)
        list.clampScroll()

        search.extract(g, layout.search, mouseX, mouseY, delta)
        sidebar.extract(g, font, layout, mouseX, mouseY)
        list.extract(g, font, mouseX, mouseY, delta, selectedRow)
        buttons.extract(g, font, layout, mouseX, mouseY)
        val open = openSuggestions()
        open?.let { suggestions.extract(g, font, it, mouseX, mouseY) }
        extractTooltips(g, open, mouseX.toDouble(), mouseY.toDouble())
        cursorAt(open, mouseX.toDouble(), mouseY.toDouble())?.let(g::requestCursor)
    }

    /** The suggestion list of the focused id field, if it shows. */
    private fun openSuggestions(): SuggestionList.Open? {
        val box = focusedBox ?: return null
        val target = list.placedRows().firstOrNull { it.row.editBox === box }?.takeIf(list::isOnScreen)
        return suggestions.open(target, height - ScreenLayout.MARGIN)
    }

    private fun extractTooltips(
        g: GuiGraphicsExtractor,
        open: SuggestionList.Open?,
        x: Double,
        y: Double,
    ) {
        if (open?.idAt(x, y) != null) return

        buttons.buttonAt(layout, x, y)?.let { button ->
            val tip = buttons.tooltip(button) ?: return@let
            tooltips.extract(g, layout, tooltips.lines(tip.text, tip.color), buttons.rectOf(layout, button), centered = true)
            return
        }

        // With the arrow keys, the selected row shows its description below itself, since there is no hover.
        if (keyboardMode) {
            val selected = list.placedRows().firstOrNull { it.row === selectedRow }?.takeIf(list::isOnScreen) ?: return
            tooltips.extract(g, layout, tooltips.rowLines(selected), selected.rect, selected.labelX, preferBelow = true)
            return
        }

        val entry = list.rowAt(x, y)
        // An invalid value explains itself at once when the mouse is on its control.
        val force = entry != null && entry.row.error() != null && entry.control.contains(x, y)
        val rowTipDue = tooltips.rowTooltipDue(entry?.row, force)
        if (entry == null) return

        // The row tooltip stays while the reset or undo icon shows its own one; the two go on opposite sides of the row.
        var iconTipBelow = false
        if (rowTipDue) {
            val rowTipBelow = tooltips.extract(g, layout, tooltips.rowLines(entry), entry.rect, entry.labelX, preferBelow = true)
            iconTipBelow = !rowTipBelow
        }
        val icon = entry.iconAt(x, y) ?: return
        val iconLines = tooltips.lines(icon.tooltip, Theme.ACCENT)
        tooltips.extract(g, layout, iconLines, entry.rectOf(icon), centered = true, preferBelow = iconTipBelow)
    }

    /** The mouse cursor for the element under the mouse, like vanilla widgets: a hand on clickable things, a text cursor on fields. */
    private fun cursorAt(
        open: SuggestionList.Open?,
        x: Double,
        y: Double,
    ): CursorType? {
        if (draggedRow != null) return CursorTypes.RESIZE_EW
        if (list.draggingScrollbar) return CursorTypes.RESIZE_NS
        if (open?.idAt(x, y) != null) return CursorTypes.POINTING_HAND
        if (layout.search.contains(x, y)) return CursorTypes.IBEAM
        buttons.buttonAt(layout, x, y)?.let { return if (buttons.isActive(it)) CursorTypes.POINTING_HAND else null }
        if (sidebar.pageAt(layout, x, y) != null) return CursorTypes.POINTING_HAND
        if (list.isOnScrollbar(x, y)) return CursorTypes.RESIZE_NS

        val entry = list.rowAt(x, y) ?: return null
        return when {
            entry.iconAt(x, y) != null -> CursorTypes.POINTING_HAND
            entry.row is ToggleRow -> CursorTypes.POINTING_HAND
            entry.row.editBoxArea(entry.control)?.contains(x, y) == true -> CursorTypes.IBEAM
            entry.control.contains(x, y) -> CursorTypes.POINTING_HAND
            else -> null
        }
    }

    // Mouse

    override fun mouseClicked(
        event: MouseButtonEvent,
        doubleClick: Boolean,
    ): Boolean {
        val x = event.x()
        val y = event.y()
        keyboardMode = false
        openSuggestions()?.let { if (suggestions.click(it, x, y)) return true }

        var newFocus: EditBox? = null
        var keyRowClicked: KeyRow? = null
        val button = buttons.buttonAt(layout, x, y)
        when {
            layout.search.contains(x, y) -> {
                newFocus = searchBox
            }

            button != null -> {
                if (buttons.isActive(button)) press(button)
            }

            layout.sidebar.contains(x, y) -> {
                sidebar.pageAt(layout, x, y)?.let { index ->
                    Theme.playClick()
                    list.selectedPage = index
                    searchBox.value = ""
                }
            }

            list.isOnScrollbar(x, y) -> {
                list.startScrollbarDrag(y)
            }

            else -> {
                list.rowAt(x, y)?.let { entry ->
                    // The arrow keys continue from the row that was clicked last.
                    selectedRow = entry.row
                    clickRow(entry, event)?.let { clicked ->
                        if (clicked is KeyRow) keyRowClicked = clicked
                        if (clicked.editBoxArea(entry.control)?.contains(x, y) == true) {
                            newFocus = clicked.editBox
                        } else if (clicked is SliderRow<*>) {
                            draggedRow = clicked
                        }
                    }
                }
            }
        }

        allRows.filterIsInstance<KeyRow>().filter { it !== keyRowClicked }.forEach { it.listening = false }
        focus(newFocus)
        newFocus?.onClick(event, doubleClick)
        return true
    }

    /** Handles a click on a row; returns the row if the click went to its control. */
    private fun clickRow(
        entry: PlacedRow,
        event: MouseButtonEvent,
    ): OptionRow? {
        val row = entry.row
        when (entry.iconAt(event.x(), event.y())) {
            RowIcon.RESET -> {
                Theme.playClick()
                row.reset()
            }

            RowIcon.UNDO -> {
                Theme.playClick()
                row.undo()
            }

            null -> {
                // A checkbox toggles from anywhere in its row.
                if (row is ToggleRow) {
                    row.clickControl(event, entry.control)
                } else if (entry.control.contains(event.x(), event.y()) && row.clickControl(event, entry.control)) {
                    return row
                }
            }
        }
        return null
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dx: Double,
        dy: Double,
    ): Boolean {
        if (list.draggingScrollbar) {
            list.dragScrollbar(event.y())
            return true
        }
        val row = draggedRow ?: return super.mouseDragged(event, dx, dy)
        list.placedRows().firstOrNull { it.row === row }?.let { row.dragControl(event.x(), it.control) }
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
        list.stopScrollbarDrag()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        // Shift + wheel over a control changes its value instead of scrolling.
        if (minecraft.hasShiftDown()) {
            val entry = list.rowAt(mouseX, mouseY)
            if (entry != null && entry.control.contains(mouseX, mouseY) && entry.row.scrollControl(scrollY)) return true
        }
        if (!layout.content.contains(mouseX, mouseY)) return false
        list.scrollBy(scrollY)
        return true
    }

    // Keyboard

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

        // Ctrl+S saves and closes, like "Done"; with invalid values it does nothing, like the disabled button.
        if (event.hasControlDown() && event.key() == InputConstants.KEY_S) {
            if (buttons.isActive(Button.DONE)) press(Button.DONE)
            return true
        }

        // Ctrl + the toggle key closes the screen like Escape, as it opened it (see Gommemode.handleKeyMappingClick).
        if (event.hasControlDown() &&
            ConfigManager.config.ctrlOpensConfig &&
            Gommemode.toggleKeyMapping?.matches(event) == true
        ) {
            // The key's character must not reach the search or a text field if the discard dialog keeps the screen open.
            suppressNextChar = true
            onClose()
            return true
        }

        focusedBox?.let { box ->
            fieldKeyPressed(box, event)
            return true
        }
        if (navigationKeyPressed(event)) return true

        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose()
            return true
        }
        return super.keyPressed(event)
    }

    /** Keys while the text field [box] has focus; Up, Down, Enter and Tab also work the suggestion list. */
    private fun fieldKeyPressed(
        box: EditBox,
        event: KeyEvent,
    ) {
        val open = openSuggestions()
        when (event.key()) {
            InputConstants.KEY_ESCAPE -> {
                // The search field is focused by default, so Escape there closes the screen like in vanilla.
                if (box === searchBox) onClose() else focus(null)
            }

            InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                open?.let(suggestions::acceptOnEnter)
                focus(null)
            }

            InputConstants.KEY_TAB -> {
                when {
                    open != null -> {
                        suggestions.acceptHighlighted(open)
                    }

                    // From the search field, Tab leaves it: into the results while searching, otherwise to the next page.
                    box === searchBox -> {
                        focus(null)
                        if (list.query.isEmpty()) {
                            navigationKeyPressed(event)
                        } else {
                            list.visibleRows().firstOrNull()?.let(::select)
                        }
                    }
                }
            }

            InputConstants.KEY_DOWN -> {
                when {
                    open != null -> {
                        suggestions.move(open, 1)
                    }

                    // From the search field, Down moves into the option list.
                    box === searchBox -> {
                        focus(null)
                        list.visibleRows().firstOrNull()?.let(::select)
                    }
                }
            }

            InputConstants.KEY_UP -> {
                open?.let { suggestions.move(it, -1) }
            }

            else -> {
                box.keyPressed(event)
                suggestions.reset()
            }
        }
    }

    /** Selects [row] for the keyboard and scrolls it into view. */
    private fun select(row: OptionRow) {
        selectedRow = row
        keyboardMode = true
        list.scrollIntoView(row)
    }

    /**
     * Keyboard control of the option list: Up/Down select a row (Up from the first row returns to the search),
     * Left/Right change its value, Enter/Space toggles, cycles, edits or rebinds it, Tab switches pages.
     */
    private fun navigationKeyPressed(event: KeyEvent): Boolean {
        val rows = list.visibleRows()
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
                if (list.query.isNotEmpty()) return false
                val step = if (event.hasShiftDown()) -1 else 1
                list.selectedPage = Math.floorMod(list.selectedPage + step, pages.size)
                list.visibleRows().firstOrNull()?.let(::select)
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
        focusedBox?.let { box ->
            suggestions.reset()
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
        suggestions.reset()
    }

    // Applying and closing

    /** Asks before resetting every option, including the toggle key, to its default; the dialog names how many change. */
    private fun confirmResetAll() {
        confirm(
            "resetAll",
            summary.nonDefaultCount,
            onConfirm = { allRows.forEach { it.reset() } },
            next = { this },
        )
    }

    /** Makes the edited values the active config, saves it and applies it to a running Gommemode. */
    private fun apply() {
        if (summary.hasErrors) return
        state.apply()

        GommemodeManager.onConfigChanged()
        Gommemode.LOGGER.debug("Applied config from the config screen")
    }

    /** Closes the screen; asks first if there are changes that were not applied. */
    override fun onClose() {
        if (!summary.isDirty) {
            minecraft.gui.setScreen(parent)
            return
        }
        confirm("discard", summary.modifiedCount, onConfirm = {}, next = { confirmed -> if (confirmed) parent else this })
    }

    /**
     * Shows the confirmation dialog [key] (with title, message, confirm and keep texts) naming [count] options.
     * On confirmation it runs [onConfirm]; either way it then shows the screen [next] returns.
     */
    private fun confirm(
        key: String,
        count: Int,
        onConfirm: () -> Unit,
        next: (confirmed: Boolean) -> Screen?,
    ) {
        minecraft.gui.setScreen(
            ConfirmScreen(
                { confirmed ->
                    if (confirmed) onConfirm()
                    minecraft.gui.setScreen(next(confirmed))
                },
                Component.translatable("$KEY_PREFIX.$key.title"),
                Component.translatable("$KEY_PREFIX.$key.message", count),
                Component.translatable("$KEY_PREFIX.$key.confirm"),
                Component.translatable("$KEY_PREFIX.$key.keep"),
            ),
        )
    }
}
