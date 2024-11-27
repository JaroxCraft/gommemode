package de.jarox.gommemode;

import de.jarox.gommemode.command.GommemodeCommand;
import de.jarox.gommemode.command.GommemodeCommandAction;
import de.jarox.gommemode.entity.GommeEntity;
import de.jarox.gommemode.entity.GommeEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class GommeMode implements ModInitializer, ClientModInitializer {

    public static final String MOD_ID = "gommemode";
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
        SharedConstants.isDevelopment = true;

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
            while (toggleKey.wasPressed()) {
                if (GommemodeManager.getInstance().isActive()) {
                    GommemodeManager.getInstance().stop();
                } else {
                    assert client.player != null;
                    assert client.world != null;
                    GommemodeManager.getInstance().start(client.player, client.world);
                }
            }
        });

        ArgumentTypeRegistry.registerArgumentType(
                Identifier.of(GommeMode.MOD_ID, "action"),
                GommemodeCommandAction.ActionArgumentType.class,
                ConstantArgumentSerializer.of(GommemodeCommandAction.ActionArgumentType::new)
        );
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> new GommemodeCommand().register(dispatcher));
    }
}
