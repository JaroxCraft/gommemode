package de.jarox.gommemode;

import de.jarox.gommemode.command.GommemodeCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class Gommemode implements ModInitializer, ClientModInitializer {

    public static Identifier GOMMEMODE_SONG = Identifier.of("gommemode:song");
    public static SoundEvent GOMMEMODE_SOUND_EVENT = Registry.register(Registries.SOUND_EVENT, GOMMEMODE_SONG, SoundEvent.of(GOMMEMODE_SONG));

    public static Gommemode INSTANCE;

    public Gommemode() {
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
    }

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                new GommemodeCommand().register(dispatcher));
        ClientTickEvents.START_WORLD_TICK.register(world -> {
        });
    }
}
