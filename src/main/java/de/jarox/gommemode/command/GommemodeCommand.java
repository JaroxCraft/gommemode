package de.jarox.gommemode.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.jarox.gommemode.Gommemode;
import de.jarox.gommemode.util.ParticleSpawner;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.concurrent.CompletableFuture;

public class GommemodeCommand implements Command<FabricClientCommandSource> {

    public final String NAME = "gommemode";
    private SoundInstance currentSound = new PositionedSoundInstance(
            Gommemode.GOMMEMODE_SOUND_EVENT,
            SoundCategory.MASTER,
            1f,
            1f,
            Random.create(),
            new BlockPos(0, 0, 0));


    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal(NAME)
                .then(ClientCommandManager.argument("action", StringArgumentType.word())
                        .suggests(new ActionSuggestionProvider()).executes(this)));
    }

    @Override
    public int run(CommandContext<FabricClientCommandSource> context) {
        var source = context.getSource();
        var player = source.getPlayer();
        var world = source.getWorld();
        var client = source.getClient();

        String actionString = context.getArgument("action", String.class).toUpperCase();
        Action action;
        try {
            action = Action.valueOf(actionString);
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Invalid action! Use either 'start' or 'stop'."));
            return 1;
        }

        switch (action) {
            case START:
                if (client.getSoundManager().isPlaying(currentSound)) {
                    source.sendError(Text.literal("The Gommemode is already active!"));
                    return 1;
                }
                currentSound = new PositionedSoundInstance(
                        Gommemode.GOMMEMODE_SOUND_EVENT,
                        SoundCategory.MASTER,
                        1f,
                        1f,
                        Random.create(),
                        player.getBlockPos());
                client.getSoundManager().play(currentSound);
                ParticleSpawner.spawnSphere(world, Vec3d.of(player.getBlockPos()), 3, ParticleTypes.CHERRY_LEAVES, 0.1);
                return 0;
            case STOP:
                if (!client.getSoundManager().isPlaying(currentSound)) {
                    source.sendError(Text.literal("The Gommemode is already stopped!"));
                    return 1;
                }
                client.getSoundManager().stop(currentSound);
                break;

            default:
                source.sendError(Text.literal("Invalid action! Use either 'start' or 'stop'."));
                return 1;
        }
        return 1;
    }

    private enum Action {
        START,
        STOP,
    }

    private static class ActionSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
            String input = builder.getRemaining().toLowerCase();
            for (Action value : Action.values()) {
                String suggestion = value.name().toLowerCase();
                if (suggestion.startsWith(input)) {
                    builder.suggest(suggestion);
                }
            }
            return builder.buildFuture();
        }
    }
}
