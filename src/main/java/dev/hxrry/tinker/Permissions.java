package dev.hxrry.tinker;

import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

public final class Permissions {

    public static final PermissionNode<Boolean> USE_NODE = PermissionNode.of("tinker", "use");

    public static final PermissionNode<Boolean> RELOAD_NODE = PermissionNode.of("tinker", "reload");

    private static final PermissionCheck[] LEVELS = {
        Commands.LEVEL_ALL, Commands.LEVEL_MODERATORS, Commands.LEVEL_GAMEMASTERS,
        Commands.LEVEL_ADMINS, Commands.LEVEL_OWNERS
    };

    private static final Predicate<Entity> USE_IF_TRUE = PermissionPredicates.require(USE_NODE, true);
    private static final Predicate<Entity> USE_IF_FALSE = PermissionPredicates.require(USE_NODE, false);
    private static final Predicate<CommandSourceStack> RELOAD_IF_TRUE =
            PermissionPredicates.require(RELOAD_NODE, true);
    private static final Predicate<CommandSourceStack> RELOAD_IF_FALSE =
            PermissionPredicates.require(RELOAD_NODE, false);

    private Permissions() {
    }

    public static boolean canUse(ServerPlayer player) {
        boolean granted = USE_IF_TRUE.test(player);
        if (granted == USE_IF_FALSE.test(player)) {
            return granted;
        }
        return fallback(player, player.permissions());
    }

    public static boolean canReload(CommandSourceStack source) {
        boolean granted = RELOAD_IF_TRUE.test(source);
        if (granted == RELOAD_IF_FALSE.test(source)) {
            return granted;
        }
        return fallback(source.getPlayer(), source.permissions());
    }

    private static boolean fallback(ServerPlayer player, PermissionSet permissions) {
        if (player != null && Tinker.config().singleplayerOwnerAllowed()) {
            MinecraftServer server = player.level().getServer();
            if (server != null && server.isSingleplayerOwner(player.nameAndId())) {
                return true;    // the person whose world it is, with or without cheats
            }
        }
        int level = Tinker.config().defaultPermissionLevel();
        return level >= 0 && level < LEVELS.length && LEVELS[level].check(permissions);
    }
}
