package de.jarox.gommemode

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.Command
import de.jarox.gommemode.config.ConfigManager
import de.jarox.gommemode.config.GommemodeConfig
import de.jarox.gommemode.config.screen.GommemodeConfigScreen
import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.entity.GommeEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Main entry point for the Gommemode mod.
 *
 * Implements both [ModInitializer] for server-side initialization
 * and [ClientModInitializer] for client-side setup.
 */
class Gommemode :
    ModInitializer,
    ClientModInitializer {
    /** Registers the entity type, its attributes and the song sound event. */
    override fun onInitialize() {
        LOGGER.info("Registering entity type and sound event")
        Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "gomme"),
            GOMME_ENTITY_TYPE,
        )
        Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            GOMMEMODE_SONG_IDENTIFIER,
            GOMMEMODE_SOUND_EVENT,
        )
        FabricDefaultAttributeRegistry.register(GOMME_ENTITY_TYPE, LivingEntity.createLivingAttributes())
        LOGGER.info("Entity and sound registrations complete")
    }

    /** Loads the config and sets up the renderer, key mapping and client events. */
    override fun onInitializeClient() {
        LOGGER.info("Initializing client-side config, renderer, key mapping, and events")
        ConfigManager.load()
        registerEntityRenderer()
        registerKeyMapping()
        registerTickEvents()
        registerConnectionEvents()
        registerCommand()
        LOGGER.info("Gommemode initialized!")
    }

    /** Registers [GommeEntityRenderer] for the Gomme entity type. */
    private fun registerEntityRenderer() {
        EntityRenderers.register(GOMME_ENTITY_TYPE) { context: EntityRendererProvider.Context ->
            GommeEntityRenderer(context)
        }
    }

    /** Registers the toggle key mapping (default: `G`) in its own category. */
    private fun registerKeyMapping() {
        val categoryIdentifier = Identifier.fromNamespaceAndPath(MOD_ID, "general")
        val category = KeyMapping.Category.register(categoryIdentifier)

        toggleKeyMapping =
            KeyMappingHelper.registerKeyMapping(
                KeyMapping(
                    "key.gommemode.toggle",
                    InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_G,
                    category,
                ),
            )
    }

    /** Runs the manager tick and handles key presses at the end of every client tick. */
    private fun registerTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register { client: Minecraft ->
            client.player?.let(GommemodeManager::tick)
            handleKeyMappingClick(client)
        }
    }

    /**
     * Toggles Gommemode once per queued key press while a world is loaded.
     * With Ctrl held and [GommemodeConfig.ctrlOpensConfig] on, the key opens the config screen instead (default: Ctrl+G).
     */
    private fun handleKeyMappingClick(client: Minecraft) {
        val keyMapping = toggleKeyMapping ?: return
        while (keyMapping.consumeClick()) {
            if (ConfigManager.config.ctrlOpensConfig && client.hasControlDown()) {
                // Clicks queued along with this one are dropped, so they do not toggle after the screen closes.
                while (keyMapping.consumeClick()) Unit
                client.gui.setScreen(GommemodeConfigScreen(null))
                return
            }
            val player = client.player ?: return
            val level = client.level ?: return
            GommemodeManager.toggleActive(player, level)
        }
    }

    /** Resets the Gommemode state whenever a world is joined or left. */
    private fun registerConnectionEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            GommemodeManager.reset()
        }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            GommemodeManager.reset()
        }
    }

    /**
     * Registers `/gommemode` which opens the config screen.
     * This is the way in for players without ModMenu; the command only runs on the client.
     */
    private fun registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal(MOD_ID).executes { openConfigScreen() },
            )
        }
    }

    /** Opens the config screen and returns the command result. */
    private fun openConfigScreen(): Int {
        val minecraft = Minecraft.getInstance()
        // The chat screen closes right after the command runs, which would also close a screen opened now.
        // As a scheduled task, the config screen opens after the chat is gone.
        minecraft.schedule { minecraft.gui.setScreen(GommemodeConfigScreen(null)) }
        return Command.SINGLE_SUCCESS
    }

    companion object {
        const val MOD_ID: String = "gommemode"
        internal val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

        /** The toggle key mapping; set during client initialization. */
        internal var toggleKeyMapping: KeyMapping? = null
            private set

        val GOMME_ENTITY_TYPE: EntityType<GommeEntity> =
            EntityType.Builder
                .of(
                    { entityType, level -> GommeEntity(entityType, level) },
                    MobCategory.CREATURE,
                ).build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "gomme")))

        val GOMMEMODE_SONG_IDENTIFIER: Identifier = Identifier.fromNamespaceAndPath(MOD_ID, "song")

        val GOMMEMODE_SOUND_EVENT: SoundEvent = SoundEvent.createVariableRangeEvent(GOMMEMODE_SONG_IDENTIFIER)
    }
}
