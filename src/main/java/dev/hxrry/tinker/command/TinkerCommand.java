package dev.hxrry.tinker.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.hxrry.tinker.Permissions;
import dev.hxrry.tinker.Tinker;
import dev.hxrry.tinker.config.Messages;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

// admin only, purely for reloading config
public final class TinkerCommand {

    private TinkerCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            LiteralCommandNode<CommandSourceStack> node = dispatcher.register(
                Commands.literal("tinker")
                    .requires(Permissions::canReload)
                    .then(Commands.literal("reload")
                        .executes(ctx -> {
                            reload(ctx.getSource());
                            return Command.SINGLE_SUCCESS;
                        })));

            dispatcher.register(Commands.literal("tk")
                    .requires(Permissions::canReload)
                    .redirect(node));
        });
    }

    private static void reload(CommandSourceStack source) {
        try {
            Tinker.reload();
        } catch (RuntimeException e) {
            Tinker.LOGGER.error("Failed to reload the tinker config.", e);
            messages().send(source, "reload-failed");
            return;
        }
        messages().send(source, "reloaded");
    }

    private static Messages messages() {
        return Tinker.config().messages();
    }
}
