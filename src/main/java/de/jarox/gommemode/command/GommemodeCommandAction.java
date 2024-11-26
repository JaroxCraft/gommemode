package de.jarox.gommemode.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.concurrent.CompletableFuture;

public class GommemodeCommandAction {
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

    public static class ActionSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
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