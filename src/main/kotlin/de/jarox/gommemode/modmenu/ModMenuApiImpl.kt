package de.jarox.gommemode.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import de.jarox.gommemode.config.Config
import net.minecraft.client.gui.screen.Screen

class ModMenuApiImpl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent: Screen? -> Config.configBuilder.setParentScreen(parent).build() }
    }
}
