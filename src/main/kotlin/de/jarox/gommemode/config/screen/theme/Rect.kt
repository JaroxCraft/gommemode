package de.jarox.gommemode.config.screen.theme

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
