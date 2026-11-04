package de.jarox.gommemode.config.screen.components

import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.config.screen.Page
import de.jarox.gommemode.config.screen.ScreenLayout
import de.jarox.gommemode.config.screen.theme.Icons
import de.jarox.gommemode.config.screen.theme.Rect
import de.jarox.gommemode.config.screen.theme.Theme
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * The sidebar: the mod icon, name and version, and the pages. Each page shows whether it has invalid values
 * or unsaved changes, and while a search runs how many of its options match.
 */
internal class Sidebar(
    private val pages: List<Page>,
    private val list: OptionList,
) {
    private val modVersion: String =
        FabricLoader
            .getInstance()
            .getModContainer(Gommemode.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("")

    /** The index of the page entry at ([x], [y]), if any. */
    fun pageAt(
        layout: ScreenLayout,
        x: Double,
        y: Double,
    ): Int? = pages.indices.firstOrNull { layout.page(it).contains(x, y) }

    fun extract(
        g: GuiGraphicsExtractor,
        font: Font,
        layout: ScreenLayout,
        mouseX: Int,
        mouseY: Int,
    ) {
        val sidebar = layout.sidebar
        g.fill(sidebar.x, sidebar.y, sidebar.right, layout.page(pages.size - 1).bottom + 4, Theme.PANEL_DARK)

        g.blit(RenderPipelines.GUI_TEXTURED, ICON, sidebar.x + 6, sidebar.y + 7, 0f, 0f, 16, 16, 16, 16)
        g.text(font, Component.literal("Gommemode"), sidebar.x + 28, sidebar.y + 6, Theme.TEXT)
        g.text(font, Component.literal(modVersion), sidebar.x + 28, sidebar.y + 17, Theme.TEXT_DIM)

        val searching = list.query.isNotEmpty()
        pages.forEachIndexed { index, page ->
            val rect = layout.page(index)
            val selected = !searching && index == list.selectedPage
            when {
                selected -> {
                    g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.SELECTED)
                    g.fill(rect.x, rect.y, rect.x + 2, rect.bottom, Theme.TEXT)
                }

                rect.contains(mouseX.toDouble(), mouseY.toDouble()) -> {
                    g.fill(rect.x, rect.y, rect.right, rect.bottom, Theme.HOVER)
                }
            }
            val matches = if (searching) list.matchCount(page) else -1
            val titleColor =
                when {
                    selected -> Theme.TEXT
                    matches == 0 -> Theme.TEXT_DISABLED
                    else -> Theme.ACCENT
                }
            val textY = rect.y + (rect.height - 8) / 2
            g.text(font, page.title, rect.x + 10, textY, titleColor)

            var right = rect.right - 6
            if (extractMarker(g, page, rect, right)) right -= 8
            // While searching, each page shows how many of its options match.
            if (matches > 0) {
                val count = matches.toString()
                g.text(font, count, right - font.width(count), textY, Theme.TEXT_DIM)
            }
        }
    }

    /**
     * Marks a page with invalid values (red "!") or unsaved changes (yellow dot), so nothing is hidden on other pages.
     * The two markers also differ in shape, so they do not rely on color alone. Returns whether a marker was drawn.
     */
    private fun extractMarker(
        g: GuiGraphicsExtractor,
        page: Page,
        rect: Rect,
        right: Int,
    ): Boolean {
        val marker = Rect(right - 4, rect.y, 4, rect.height)
        when {
            page.rows.any { it.error() != null } -> {
                Theme.pattern(g, Icons.ERROR_MARK, marker, Theme.ERROR)
            }

            page.rows.any { it.isModified } -> {
                val dotY = marker.y + (marker.height - 4) / 2
                g.fill(marker.x, dotY, marker.right, dotY + 4, Theme.MODIFIED)
            }

            else -> {
                return false
            }
        }
        return true
    }

    private companion object {
        val ICON: Identifier = Identifier.fromNamespaceAndPath(Gommemode.MOD_ID, "icon.png")
    }
}
