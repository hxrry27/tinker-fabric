package dev.hxrry.tinker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tinker", "tinker"));

    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.tinker.toggle", GLFW.GLFW_KEY_G, CATEGORY);

    private static final long REPEAT_MILLIS = 10_000L;

    private static long lastExplained;

    private Keybinds() {
    }

    public static void register() {
        KeyMappingHelper.registerKeyMapping(TOGGLE);
        ClientTickEvents.END_CLIENT_TICK.register(Keybinds::tick);
    }

    private static void tick(Minecraft client) {
        while (TOGGLE.consumeClick()) {
            if (ClientState.available()) {
                ClientNetworking.sendToggle();
            } else {
                explainWhyNothingHappened(client);
            }
        }
    }

    private static void explainWhyNothingHappened(Minecraft client) {
        if (client.player == null || System.currentTimeMillis() - lastExplained < REPEAT_MILLIS) {
            return;
        }
        lastExplained = System.currentTimeMillis();
        client.player.sendSystemMessage(Component.literal(reason()).withStyle(ChatFormatting.GRAY));
    }

    private static String reason() {
        if (ClientState.unsupported()) {
            return "[Tinker] This server's tinker speaks a different protocol version, "
                    + "so the toggle key is off.";
        }
        return "[Tinker] There is no tinker backend on this server, so the toggle key does nothing.";
    }
}
