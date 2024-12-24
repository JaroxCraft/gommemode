package de.jarox.gommemode

import de.jarox.gommemode.entity.GommeEntity
import de.jarox.gommemode.entity.GommeEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

class Gommemode : ModInitializer, ClientModInitializer {
    private var toggleKey: KeyBinding? = null

    init {
        INSTANCE = this
    }

    override fun onInitialize() {
        Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "gomme"),
            GOMME_ENTITY_TYPE
        )
        FabricDefaultAttributeRegistry.register(GOMME_ENTITY_TYPE, LivingEntity.createLivingAttributes())
    }

    override fun onInitializeClient() {
        EntityRendererRegistry.register<GommeEntity?>(
            GOMME_ENTITY_TYPE
        ) { ctx: EntityRendererFactory.Context? -> GommeEntityRenderer(ctx!!) }

        toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.gommemode.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.gommemode.general"
            )
        )
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            while (toggleKey!!.wasPressed()) {
                GommemodeManager.toggleActive(client.player!!, client.world!!)
            }
        })
    }

    companion object {

        lateinit var INSTANCE: Gommemode

        const val MOD_ID: String = "gommemode"

        val GOMME_ENTITY_TYPE: EntityType<GommeEntity?> =
            EntityType.Builder.create<GommeEntity?>(SpawnGroup.CREATURE).build()

        private val GOMMEMODE_SONG: Identifier = Identifier.of(MOD_ID, "song")
        var GOMMEMODE_SOUND_EVENT: SoundEvent = Registry.register(
            Registries.SOUND_EVENT, GOMMEMODE_SONG, SoundEvent.of(
                GOMMEMODE_SONG
            )
        )
    }
}
