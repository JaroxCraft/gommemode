package de.jarox.gommemode.config.screen.rows

/** Totals over all option [rows], for the buttons and the confirmation dialogs. */
internal class RowSummary(
    val rows: List<OptionRow>,
) {
    /** Whether any row holds a value that cannot be applied. */
    val hasErrors: Boolean get() = rows.any { it.error() != null }

    /** How many rows have a change that is not applied yet. */
    val modifiedCount: Int get() = rows.count { it.isModified }

    val isDirty: Boolean get() = rows.any { it.isModified }

    /** How many rows differ from their default, i.e. what Reset All would change. */
    val nonDefaultCount: Int get() = rows.count { !it.isDefault }
}
