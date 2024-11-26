package de.jarox.gommemode.command;

import com.mojang.brigadier.CommandDispatcher;
import de.jarox.gommemode.GommemodeManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class GommemodeCommand {
    public static final String NAME = "gommemode";

    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal(NAME)
                        .executes(context -> run(context.getSource(), GommemodeCommandAction.Action.TOGGLE))
                        .then(
                                ClientCommandManager.argument("action", new GommemodeCommandAction.ActionArgumentType())
                                        .suggests(new GommemodeCommandAction.ActionSuggestionProvider())
                                        .executes(context -> run(context.getSource(), context.getArgument("action", GommemodeCommandAction.Action.class)))
                        )
        );
    }

    public int run(FabricClientCommandSource source, GommemodeCommandAction.Action action) {
        GommemodeManager manager = GommemodeManager.getInstance();

        switch (action) {
            case START:
                if (manager.isActive()) {
                    source.sendError(Text.translatable("gommemode.already_active"));
                    return 1;
                }
                manager.start(source.getPlayer(), source.getWorld());
                return 0;
            case STOP:
                if (!manager.isActive()) {
                    source.sendError(Text.translatable("gommemode.already_inactive"));
                    return 1;
                }
                manager.stop();
                return 0;
            case TOGGLE:
                if (manager.isActive()) {
                    manager.stop();
                } else {
                    manager.start(source.getPlayer(), source.getWorld());
                }
                return 0;
        }
        return 1;
    }
}
