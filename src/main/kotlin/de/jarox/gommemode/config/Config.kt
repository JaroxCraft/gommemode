package de.jarox.gommemode.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import net.minecraft.text.Text

object Config {
    val configBuilder: ConfigBuilder = ConfigBuilder.create()
        .setTitle(Text.of("Gommemode"))

    var generalCategory: ConfigCategory = configBuilder.getOrCreateCategory(Text.of("general"))
}
