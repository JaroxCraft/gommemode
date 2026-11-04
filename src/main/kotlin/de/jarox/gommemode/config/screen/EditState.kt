package de.jarox.gommemode.config.screen

import com.mojang.blaze3d.platform.InputConstants
import de.jarox.gommemode.Gommemode
import de.jarox.gommemode.config.ConfigManager
import de.jarox.gommemode.config.GommemodeConfig
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft

/** The prefix of all translation keys of the config screen. */
internal const val KEY_PREFIX = "config.${Gommemode.MOD_ID}"

/**
 * The values the config screen works with: [pending] is edited by the screen, [saved] is what was
 * last applied, and [defaults] are the built-in defaults. The toggle key lives in the vanilla key
 * mapping, so it is tracked here separately.
 */
internal class EditState(
    val keyMapping: KeyMapping?,
) {
    val defaults = GommemodeConfig()
    val pending: GommemodeConfig = ConfigManager.copyOf(ConfigManager.config)
    var saved: GommemodeConfig = ConfigManager.copyOf(ConfigManager.config)
    var pendingKey: InputConstants.Key = keyMapping?.let(KeyMappingHelper::getBoundKeyOf) ?: InputConstants.UNKNOWN
    var savedKey: InputConstants.Key = pendingKey

    /** Makes the pending values the active config and saves them; a changed toggle key is bound and saved too. */
    fun apply() {
        ConfigManager.update(pending)
        saved = ConfigManager.copyOf(ConfigManager.config)

        val mapping = keyMapping ?: return
        if (pendingKey == savedKey) return
        mapping.setKey(pendingKey)
        KeyMapping.resetMapping()
        Minecraft.getInstance().options.save()
        savedKey = pendingKey
    }
}
