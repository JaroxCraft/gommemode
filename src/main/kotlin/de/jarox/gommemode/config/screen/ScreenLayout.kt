package de.jarox.gommemode.config.screen

import de.jarox.gommemode.config.screen.theme.Rect

/**
 * Where everything on the config screen goes for a screen of [width] × [height]:
 * the search field on top, the sidebar and the option list below it, the buttons at the bottom.
 * [resetAllWidth] is the width of the Reset All button, which depends on its translated label.
 */
internal class ScreenLayout(
    val width: Int,
    val height: Int,
    resetAllWidth: Int,
) {
    val search = Rect(MARGIN, MARGIN, width - 2 * MARGIN, BUTTON_HEIGHT)

    /** The top of the button row. */
    val bottomY = height - MARGIN - BUTTON_HEIGHT

    private val bodyTop = MARGIN + BUTTON_HEIGHT + GAP
    private val bodyHeight = bottomY - GAP - bodyTop

    val sidebar = Rect(MARGIN, bodyTop, (width * 0.2).toInt().coerceIn(100, 150), bodyHeight)
    val content = Rect(sidebar.right + GAP, bodyTop, width - MARGIN - sidebar.right - GAP, bodyHeight)

    val resetAll = Rect(MARGIN, bottomY, resetAllWidth, BUTTON_HEIGHT)

    /** The width of Cancel, Apply and Done: 80, or less if the window is too narrow for all four buttons. */
    private val buttonWidth = ((width - MARGIN - resetAll.right - 4 * GAP) / 3).coerceAtMost(80)
    val done = Rect(width - MARGIN - buttonWidth, bottomY, buttonWidth, BUTTON_HEIGHT)
    val apply = Rect(done.x - BUTTON_SPACING - buttonWidth, bottomY, buttonWidth, BUTTON_HEIGHT)
    val cancel = Rect(apply.x - BUTTON_SPACING - buttonWidth, bottomY, buttonWidth, BUTTON_HEIGHT)

    /** Where floating panels (tooltips, suggestions) may go: inside the margins and above the buttons. */
    val floatingTop = MARGIN
    val floatingBottom = bottomY - GAP

    /** The sidebar entry of the page at [index]. */
    fun page(index: Int) = Rect(sidebar.x, sidebar.y + SIDEBAR_HEADER + index * PAGE_HEIGHT, sidebar.width, PAGE_HEIGHT)

    companion object {
        const val MARGIN = 8
        const val GAP = 6
        const val BUTTON_HEIGHT = 20
        const val BUTTON_SPACING = 4

        /** The space for the mod icon, name and version above the pages. */
        const val SIDEBAR_HEADER = 30
        const val PAGE_HEIGHT = 18
    }
}
