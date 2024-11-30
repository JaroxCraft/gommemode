package de.jarox.gommemode;

import de.jarox.gommemode.entity.GommeEntity;
import de.jarox.gommemode.entity.GommeEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GommeMode implements ModInitializer, ClientModInitializer {

    public static final String MOD_ID = "gommemode";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final EntityType<GommeEntity> GOMME_ENTITY_TYPE = EntityType.Builder.<GommeEntity>create(SpawnGroup.CREATURE).build();
    public static Identifier GOMMEMODE_SONG = Identifier.of(MOD_ID, "song");
    public static SoundEvent GOMMEMODE_SOUND_EVENT = Registry.register(Registries.SOUND_EVENT, GOMMEMODE_SONG, SoundEvent.of(GOMMEMODE_SONG));
    public static GommeMode INSTANCE;
    private KeyBinding toggleKey;

    public GommeMode() {
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
        Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(GommeMode.MOD_ID, "gomme"),
                GOMME_ENTITY_TYPE
        );
        FabricDefaultAttributeRegistry.register(GOMME_ENTITY_TYPE, GommeEntity.createLivingAttributes());
    }

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GOMME_ENTITY_TYPE, GommeEntityRenderer::new);

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gommemode.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.gommemode.general"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GommemodeManager manager = GommemodeManager.getInstance();
            manager.updateActive();

            while (toggleKey.wasPressed()) {
                if (manager.isActive()) {
                    manager.stop();
                } else {
                    assert client.player != null;
                    assert client.world != null;
                    manager.start(client.player, client.world);
                }
            }
        });
    }
}
