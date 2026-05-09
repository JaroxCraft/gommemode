package de.jarox.gommemode.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import de.jarox.gommemode.config.Config

/**
 * ModMenu API implementation that provides a configuration screen factory.
 */
class ModMenuApiImpl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { parent -> Config.buildScreen(parent) }
}
