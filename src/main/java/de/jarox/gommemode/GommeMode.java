package de.jarox.gommemode;

import de.jarox.gommemode.command.GommemodeCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class GommeMode implements ModInitializer, ClientModInitializer {

    public static final String MOD_ID = "gommemode";

    public static Identifier GOMMEMODE_SONG = Identifier.of(MOD_ID, "song");
    public static SoundEvent GOMMEMODE_SOUND_EVENT = Registry.register(Registries.SOUND_EVENT, GOMMEMODE_SONG, SoundEvent.of(GOMMEMODE_SONG));

    public static GommeMode INSTANCE;

    public GommeMode() {
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
    }

    @Override
    public void onInitializeClient() {
        ArgumentTypeRegistry.registerArgumentType(
                Identifier.of(GommeMode.MOD_ID, "action"),
                GommemodeCommand.ActionArgumentType.class,
                ConstantArgumentSerializer.of(GommemodeCommand.ActionArgumentType::new)
        );
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> new GommemodeCommand().register(dispatcher));
    }
}
