package de.jarox.gommemode.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.jarox.gommemode.GommeMode;
import de.jarox.gommemode.util.ParticleSpawner;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.concurrent.CompletableFuture;

public class GommemodeCommand {

    public final String NAME = "gommemode";

    private SoundInstance currentSound = new PositionedSoundInstance(
            GommeMode.GOMMEMODE_SOUND_EVENT,
            SoundCategory.MASTER,
            1f,
            1f,
            Random.create(),
            new BlockPos(0, 0, 0));


    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal(NAME)
                        .executes(context -> run(context.getSource(), Action.TOGGLE))
                        .then(
                                ClientCommandManager.argument("action", new ActionArgumentType())
                                        .suggests(new ActionSuggestionProvider())
                                        .executes(context -> run(context.getSource(), context.getArgument("action", Action.class)))
                        )
        );
    }

    public int run(FabricClientCommandSource source, Action action) {
        PlayerEntity player = source.getPlayer();
        BlockPos pos = player.getBlockPos();
        ClientWorld world = source.getWorld();
        MinecraftClient client = source.getClient();

        switch (action) {
            case START:
                if (client.getSoundManager().isPlaying(currentSound)) {
                    source.sendError(Text.translatable("gommemode.already_active"));
                    return 1;
                }
                currentSound = new PositionedSoundInstance(
                        GommeMode.GOMMEMODE_SOUND_EVENT,
                        SoundCategory.MASTER,
                        1f,
                        1f,
                        Random.create(),
                        player.getBlockPos());
                client.getSoundManager().play(currentSound);

                world.addParticle(ParticleTypes.ELDER_GUARDIAN, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0);
                ParticleSpawner.spawnSphere(world, Vec3d.of(player.getBlockPos()), 3, ParticleTypes.ENCHANT, 0.1);
                return 0;
            case STOP:
                if (!client.getSoundManager().isPlaying(currentSound)) {
                    source.sendError(Text.translatable("gommemode.already_inactive"));
                    return 1;
                }
                client.getSoundManager().stop(currentSound);
                break;
            case TOGGLE:
                if (client.getSoundManager().isPlaying(currentSound)) {
                    run(source, Action.STOP);
                } else {
                    run(source, Action.START);
                }
                return 0;
        }
        return 1;
    }

    public enum Action {
        START,
        STOP,
        TOGGLE
    }

    public static class ActionArgumentType implements ArgumentType<Action> {
        @Override
        public Action parse(StringReader reader) {
            String string = reader.readUnquotedString().toUpperCase();
            return Action.valueOf(string);
        }
    }

    private static class ActionSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
            String input = builder.getRemaining();
            boolean isUpperCase = !input.isEmpty() && Character.isUpperCase(input.charAt(0));

            for (Action value : Action.values()) {
                String suggestion = isUpperCase ? value.name().toUpperCase() : value.name().toLowerCase();

                if (input.isEmpty() || suggestion.startsWith(input)) {
                    builder.suggest(suggestion);
                }
            }
            return builder.buildFuture();
        }
    }
}
