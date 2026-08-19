package dev.hxrry.tinker;

import dev.hxrry.tinker.command.TinkerCommand;
import dev.hxrry.tinker.config.TinkerConfig;
import dev.hxrry.tinker.listener.StairVoxel;
import dev.hxrry.tinker.listener.TinkerToolListener;
import dev.hxrry.tinker.net.TinkerChannel;
import dev.hxrry.tinker.net.TinkerPayload;
import dev.hxrry.tinker.property.PropertyResolver;
import dev.hxrry.tinker.session.SessionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class Tinker implements ModInitializer {

    public static final String MOD_ID = "tinker";

    public static final Logger LOGGER = LoggerFactory.getLogger("Tinker");

    private static final SessionManager SESSIONS = new SessionManager();
    private static final PropertyResolver RESOLVER = new PropertyResolver(Tinker::config);
    private static final TinkerChannel CHANNEL = new TinkerChannel();

    private static TinkerConfig config;
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        config = TinkerConfig.load(configFile());
        LOGGER.info("Tinker ready: {}. Config: {}", config.summary(),
                configFile().toAbsolutePath());

        TinkerPayload.register();
        CHANNEL.register();
        TinkerToolListener.register();
        TinkerCommand.register();

        ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> {
            server = null;
            SESSIONS.clear();
            CHANNEL.clear();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, ignored) -> {
            CHANNEL.forget(handler.player);
            SESSIONS.remove(handler.player);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                CHANNEL.sendState(newPlayer));
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("config.jsonc");
    }

    public static void reload() {
        config = TinkerConfig.load(configFile());
        SESSIONS.clear();
        StairVoxel.clearCache();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CHANNEL.sendState(player);
        }
    }

    public static TinkerConfig config() {
        return config;
    }

    public static SessionManager sessions() {
        return SESSIONS;
    }

    public static PropertyResolver resolver() {
        return RESOLVER;
    }

    public static TinkerChannel channel() {
        return CHANNEL;
    }
}
