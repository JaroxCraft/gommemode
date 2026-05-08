package de.jarox.gommemode.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import net.minecraft.network.chat.Component

object Config {
    val configBuilder: ConfigBuilder = ConfigBuilder.create()
        .setTitle(Component.literal("Gommemode"))

    @Suppress("unused")
    var generalCategory: ConfigCategory = configBuilder.getOrCreateCategory(Component.literal("general"))
}