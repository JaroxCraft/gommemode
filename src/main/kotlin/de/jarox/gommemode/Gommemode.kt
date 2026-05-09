package de.jarox.gommemode

import com.mojang.blaze3d.platform.InputConstants
import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.entity.GommeEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
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
import org.lwjgl.glfw.GLFW

/**
 * Main entry point for the Gommemode mod.
 *
 * Implements both [ModInitializer] for server-side initialization
 * and [ClientModInitializer] for client-side setup.
 */
class Gommemode : ModInitializer, ClientModInitializer {
    private var toggleKeyMapping: KeyMapping? = null

    override fun onInitialize() {
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
    }

    override fun onInitializeClient() {
        registerEntityRenderer()
        registerKeyMapping()
        registerTickEvents()
        registerConnectionEvents()
    }

    private fun registerEntityRenderer() {
        EntityRenderers.register(GOMME_ENTITY_TYPE) { context: EntityRendererProvider.Context ->
            GommeEntityRenderer(context)
        }
    }

    private fun registerKeyMapping() {
        val categoryIdentifier = Identifier.fromNamespaceAndPath(MOD_ID, "general")
        val category = KeyMapping.Category.register(categoryIdentifier)

        toggleKeyMapping =
            KeyMappingHelper.registerKeyMapping(
                KeyMapping(
                    "key.gommemode.toggle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    category,
                ),
            )
    }

    private fun registerTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register { client: Minecraft ->
            handleAutoDeactivateOnSongEnd(client)
            handleKeyMappingClick(client)
        }
    }

    private fun handleAutoDeactivateOnSongEnd(client: Minecraft) {
        if (GommemodeManager.isPlaying() != GommemodeManager.isActive) {
            val player = client.player ?: return
            val level = client.level ?: return
            GommemodeManager.toggleActive(player, level)
        }
    }

    private fun handleKeyMappingClick(client: Minecraft) {
        val keyMapping = toggleKeyMapping ?: return
        while (keyMapping.consumeClick()) {
            val player = client.player ?: return
            val level = client.level ?: return
            GommemodeManager.toggleActive(player, level)
        }
    }

    private fun registerConnectionEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            GommemodeManager.reset()
        }
    }

    companion object {
        const val MOD_ID: String = "gommemode"

        val GOMME_ENTITY_TYPE: EntityType<GommeEntity> =
            EntityType.Builder.of(
                { entityType, level -> GommeEntity(entityType, level) },
                MobCategory.CREATURE,
            ).build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "gomme")))

        val GOMMEMODE_SONG_IDENTIFIER: Identifier =
            Identifier.fromNamespaceAndPath(MOD_ID, "song")

        val GOMMEMODE_SOUND_EVENT: SoundEvent = SoundEvent.createVariableRangeEvent(GOMMEMODE_SONG_IDENTIFIER)
    }
}
