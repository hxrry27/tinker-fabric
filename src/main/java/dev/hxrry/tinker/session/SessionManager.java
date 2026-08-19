package dev.hxrry.tinker.session;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SessionManager {

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public PlayerSession get(ServerPlayer player) {
        return sessions.computeIfAbsent(player.getUUID(), id -> new PlayerSession());
    }

    public PlayerSession peek(ServerPlayer player) {
        return sessions.get(player.getUUID());
    }

    public void clear() {
        sessions.clear();
    }

    public void remove(ServerPlayer player) {
        sessions.remove(player.getUUID());
    }
}
