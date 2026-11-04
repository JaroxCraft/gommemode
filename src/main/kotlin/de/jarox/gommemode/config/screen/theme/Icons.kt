package de.jarox.gommemode.config.screen.theme

/** Pixel icons for [Theme.pattern]: one string per line, `X` = filled. */
internal object Icons {
    /** An exclamation mark: the row or page has an invalid value. */
    val ERROR_MARK = listOf("XX", "XX", "XX", "XX", "..", "XX")

    /** A circular arrow: back to the default. */
    val RESET =
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
    val UNDO =
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
}
