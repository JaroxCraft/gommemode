package de.jarox.gommemode.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Configuration screen factory for the Gommemode mod.
 *
 * Provides a ClothConfig-based configuration screen with a single "General" category.
 */
object Config {
    /**
     * Builds and returns the configuration screen.
     *
     * @param parentScreen The screen to return to when pressing Escape
     * @return The constructed configuration screen
     */
    fun buildScreen(parentScreen: Screen?): Screen {
        val builder =
            ConfigBuilder.create()
                .setParentScreen(parentScreen)
                .setTitle(Component.literal("Gommemode"))

        val generalCategory = builder.getOrCreateCategory(Component.literal("general"))
        addGeneralOptions(generalCategory)

        return builder.build()
    }

    @Suppress("EmptyMethod")
    private fun addGeneralOptions(category: ConfigCategory) {
        // Placeholder for future configuration entries.
        // Add options to [category] here as the mod gains configurable settings.
    }
}
