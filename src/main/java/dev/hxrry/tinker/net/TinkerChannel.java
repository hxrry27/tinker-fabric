package dev.hxrry.tinker.net;

import dev.hxrry.tinker.Permissions;
import dev.hxrry.tinker.Tinker;
import dev.hxrry.tinker.TinkerService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TinkerChannel {

    private final Map<UUID, Boolean> handshaken = new HashMap<>();

    private final Map<UUID, RateLimit> limits = new HashMap<>();

    public void register() {
        ServerPlayNetworking.registerGlobalReceiver(TinkerPayload.TYPE,
                (payload, context) -> receive(context.player(), payload.data()));
    }

    private void receive(ServerPlayer player, byte[] message) {
        if (message.length == 0 || message.length > Protocol.MAX_PAYLOAD_BYTES) {
            reject(player, "payload length " + message.length);
            return;
        }
        if (!limit(player).allow()) {
            reject(player, "rate limit");
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            switch (in.readByte()) {
                case Protocol.HELLO -> handleHello(player, in.readInt());
                case Protocol.TOGGLE_TINKER -> handleToggle(player, in.readByte());
                case Protocol.CYCLE_PROPERTY -> handleCycle(player, in.readByte());
                default -> reject(player, "unknown message id");
            }
        } catch (IOException | RuntimeException e) {
            reject(player, "malformed payload: " + e.getClass().getSimpleName());
        }
    }

    private void handleHello(ServerPlayer player, int clientVersion) {
        boolean supported = clientVersion == Protocol.PROTOCOL_VERSION;
        handshaken.put(player.getUUID(), supported);

        send(player, Wire.helloAck(Protocol.PROTOCOL_VERSION, supported));

        if (supported) {
            sendState(player);
        } else {
            Tinker.LOGGER.debug("Tinker client v{} from {} is not supported (server speaks v{})",
                    clientVersion, player.getGameProfile().name(), Protocol.PROTOCOL_VERSION);
        }
    }

    // permission is re-checked here rather than trusted from the client's last known state
    private void handleToggle(ServerPlayer player, byte mode) {
        if (!ready(player) || !Permissions.canUse(player)) {
            sendState(player);
            return;
        }
        switch (mode) {
            case Protocol.MODE_OFF -> TinkerService.setTinkerMode(player, false);
            case Protocol.MODE_ON -> TinkerService.setTinkerMode(player, true);
            case Protocol.MODE_TOGGLE -> TinkerService.toggleTinkerMode(player);
            default -> reject(player, "unknown toggle mode " + mode);
        }
    }

    private void handleCycle(ServerPlayer player, byte direction) {
        if (!ready(player) || !Permissions.canUse(player)) {
            sendState(player);
            return;
        }
        if (direction != Protocol.DIRECTION_BACKWARD && direction != Protocol.DIRECTION_FORWARD) {
            reject(player, "unknown cycle direction " + direction);
            return;
        }
        TinkerService.cycleSelection(player, direction == Protocol.DIRECTION_FORWARD);
    }

    public void sendState(ServerPlayer player) {
        if (!ready(player)) {
            return;
        }
        send(player, encodeState(player));
    }

    private byte[] encodeState(ServerPlayer player) {
        return Wire.state(TinkerService.tinkerMode(player), selectedProperty(player),
                Permissions.canUse(player));
    }

    private String selectedProperty(ServerPlayer player) {
        if (!TinkerService.tinkerMode(player)) {
            return null;
        }
        BlockPos pos = TinkerService.targetBlock(player);
        if (pos == null) {
            return null;
        }
        String key = TinkerService.selectedProperty(player,
                player.level().getBlockState(pos).getBlock());
        if (key == null) {
            return null;
        }
        int dot = key.indexOf('.');
        return dot < 0 ? key : key.substring(dot + 1);
    }

    private void send(ServerPlayer player, byte[] data) {
        TinkerPayload payload = new TinkerPayload(data);
        if (ServerPlayNetworking.canSend(player, TinkerPayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        } else {
            player.connection.send(ServerPlayNetworking.createClientboundPacket(payload));
        }
    }

    public boolean isListening(ServerPlayer player) {
        return ready(player);
    }

    private boolean ready(ServerPlayer player) {
        return Boolean.TRUE.equals(handshaken.get(player.getUUID()));
    }

    private RateLimit limit(ServerPlayer player) {
        return limits.computeIfAbsent(player.getUUID(), id -> new RateLimit());
    }

    private void reject(ServerPlayer player, String reason) {
        Tinker.LOGGER.debug("Rejected {} message from {}: {}",
                Protocol.CHANNEL, player.getGameProfile().name(), reason);
    }

    public void forget(ServerPlayer player) {
        handshaken.remove(player.getUUID());
        limits.remove(player.getUUID());
    }

    public void clear() {
        handshaken.clear();
        limits.clear();
    }

    private static final class RateLimit {
        private long windowStart;
        private int count;

        boolean allow() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 1000L) {
                windowStart = now;
                count = 0;
            }
            return ++count <= Protocol.MAX_MESSAGES_PER_SECOND;
        }
    }
}
