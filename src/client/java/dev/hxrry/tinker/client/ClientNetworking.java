package dev.hxrry.tinker.client;

import dev.hxrry.tinker.Tinker;
import dev.hxrry.tinker.net.Protocol;
import dev.hxrry.tinker.net.TinkerPayload;
import dev.hxrry.tinker.net.Wire;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public final class ClientNetworking {

    private static final int[] HELLO_TICKS = {10, 40, 100, 200};

    private static final int GIVE_UP_AFTER = HELLO_TICKS[HELLO_TICKS.length - 1] + 40;

    private static int ticksSinceJoin = -1;
    private static int attempts;

    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TinkerPayload.TYPE,
                (payload, context) -> receive(payload.data()));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientState.reset();
            ticksSinceJoin = 0;
            attempts = 0;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientState.reset();
            ticksSinceJoin = -1;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        if (ticksSinceJoin < 0) {
            return;
        }
        if (ClientState.acknowledged()) {
            ticksSinceJoin = -1;
            return;
        }

        ticksSinceJoin++;
        if (attempts < HELLO_TICKS.length && ticksSinceJoin >= HELLO_TICKS[attempts]) {
            attempts++;
            send(Wire.hello(Protocol.PROTOCOL_VERSION));
            return;
        }
        if (ticksSinceJoin >= GIVE_UP_AFTER) {
            ticksSinceJoin = -1;
            Tinker.LOGGER.info("No tinker backend answered on this server after {} attempts; "
                    + "the toggle key stays off.", attempts);
        }
    }

    public static void sendToggle() {
        send(Wire.toggle(Protocol.MODE_TOGGLE));
    }

    private static void receive(byte[] message) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            switch (in.readByte()) {
                case Protocol.HELLO_ACK -> handleAck(Wire.readHelloAck(in));
                case Protocol.STATE -> handleState(Wire.readState(in));
                default -> Tinker.LOGGER.debug("Ignoring an unknown tinker message id");
            }
        } catch (IOException | RuntimeException e) {
            Tinker.LOGGER.debug("Ignoring a malformed tinker message", e);
        }
    }

    private static void handleAck(Wire.HelloAck ack) {
        int serverVersion = ack.protocolVersion();
        ClientState.acknowledge(ack.supported());
        if (ack.supported()) {
            Tinker.LOGGER.info("Tinker backend found, speaking protocol v{}.", serverVersion);
            return;
        }
        Tinker.LOGGER.warn("Tinker backend speaks protocol v{}, this mod speaks v{}; standing down.",
                serverVersion, Protocol.PROTOCOL_VERSION);
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(
                        "[Tinker] This server's tinker is protocol v" + serverVersion
                                + " and this mod speaks v" + Protocol.PROTOCOL_VERSION
                                + ". The tinker key does nothing until the versions match.")
                        .withStyle(ChatFormatting.RED));
            }
        });
    }

    private static void handleState(Wire.State state) {
        if (!ClientState.tinkerEnabled(state.tinkerEnabled())) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.hud.setOverlayMessage(
                Component.translatable(state.tinkerEnabled()
                        ? "tinker.message.enabled" : "tinker.message.disabled")
                        .withStyle(state.tinkerEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED),
                false));
    }

    private static void send(byte[] message) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        connection.send(ClientPlayNetworking.createServerboundPacket(new TinkerPayload(message)));
    }
}
