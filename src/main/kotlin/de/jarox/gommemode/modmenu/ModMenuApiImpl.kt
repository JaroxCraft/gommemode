package de.jarox.gommemode.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import de.jarox.gommemode.config.screen.GommemodeConfigScreen

/** ModMenu API implementation that opens the Gommemode config screen. */
class ModMenuApiImpl : ModMenuApi {
    /** Opens [GommemodeConfigScreen], returning to ModMenu when it is closed. */
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory(::GommemodeConfigScreen)
}
