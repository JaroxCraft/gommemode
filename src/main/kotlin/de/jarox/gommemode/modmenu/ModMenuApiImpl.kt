package de.jarox.gommemode.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.config.Config
import net.fabricmc.loader.api.FabricLoader

/**
 * ModMenu API implementation that provides a configuration screen factory.
 *
 * The screen is built with Cloth Config, which is optional; without it, no config screen is offered
 * (the config file `config/gommemode.json` can still be edited by hand).
 */
class ModMenuApiImpl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        if (!FabricLoader.getInstance().isModLoaded(CLOTH_CONFIG_MOD_ID)) {
            Gommemode.LOGGER.debug("Cloth Config is not installed, no config screen available")
            return ConfigScreenFactory { null }
        }
        return ConfigScreenFactory { parent ->
            Gommemode.LOGGER.debug("Providing ModMenu config screen factory")
            Config.buildScreen(parent)
        }
    }

    private companion object {
        const val CLOTH_CONFIG_MOD_ID = "cloth-config"
    }
}
