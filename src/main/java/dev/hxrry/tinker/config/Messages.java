package dev.hxrry.tinker.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hxrry.tinker.Tinker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Messages {

    private final Map<String, String> raw = new HashMap<>();
    private final Component prefix;

    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    Messages(JsonObject config) {
        JsonObject messages = config.getAsJsonObject("messages");
        if (messages != null) {
            for (Map.Entry<String, JsonElement> entry : messages.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    raw.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        this.prefix = MiniMessage.parse(raw.getOrDefault("prefix", ""), Map.of());
    }

    public Component render(String key, Map<String, String> placeholders) {
        String template = raw.get(key);
        if (template == null) {
            if (warned.add(key)) {
                Tinker.LOGGER.warn("Missing message key 'messages.{}' in the tinker config.", key);
            }
            return Component.empty();
        }
        return MiniMessage.parse(template, placeholders);
    }

    public Component render(String key) {
        return render(key, Map.of());
    }

    public void actionBar(ServerPlayer player, String key, Map<String, String> placeholders) {
        player.sendSystemMessage(render(key, placeholders), true);
    }

    public void send(CommandSourceStack source, String key) {
        source.sendSystemMessage(Component.empty().append(prefix).append(render(key)));
    }

    public static Map<String, String> placeholders(String... pairs) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        return values;
    }
}
