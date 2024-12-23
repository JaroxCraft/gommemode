package de.jarox.gommemode.config

import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.minecraft.text.Text

object Config {
    val configBuilder: ConfigBuilder = ConfigBuilder.create()
        .setTitle(Text.of("Gommemode"))
}
