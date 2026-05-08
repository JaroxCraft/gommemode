package de.jarox.gommemode

import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.entity.GommeEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

class Gommemode : ModInitializer, ClientModInitializer {
    private var toggleKey: KeyMapping? = null

    override fun onInitialize() {
        Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "gomme"),
            GOMME_ENTITY_TYPE
        )
        FabricDefaultAttributeRegistry.register(GOMME_ENTITY_TYPE, LivingEntity.createLivingAttributes())
    }

    override fun onInitializeClient() {
        EntityRendererRegistry.register<GommeEntity>(
            GOMME_ENTITY_TYPE
        ) { ctx: EntityRendererProvider.Context -> GommeEntityRenderer(ctx) }

        val gommeCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "general"))
        toggleKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.gommemode.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                gommeCategory
            )
        )
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: Minecraft ->
            if (GommemodeManager.isPlaying() != GommemodeManager.active) {
                client.player?.let { player ->
                    client.level?.let { world ->
                        GommemodeManager.toggleActive(player, world)
                    }
                }
            }

            while (toggleKey!!.consumeClick()) {
                client.player?.let { player ->
                    client.level?.let { world ->
                        GommemodeManager.toggleActive(player, world)
                    }
                }
            }
        })
    }

    companion object {
        const val MOD_ID: String = "gommemode"

        val GOMME_ENTITY_TYPE: EntityType<GommeEntity> =
            EntityType.Builder.of<GommeEntity>({ type, level -> GommeEntity(type, level) }, MobCategory.CREATURE)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "gomme")))

        private val GOMMEMODE_SONG: Identifier = Identifier.fromNamespaceAndPath(MOD_ID, "song")
        var GOMMEMODE_SOUND_EVENT: SoundEvent = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            GOMMEMODE_SONG,
            SoundEvent.createVariableRangeEvent(GOMMEMODE_SONG)
        )
    }
}
