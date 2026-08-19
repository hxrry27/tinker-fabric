package dev.hxrry.tinker.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import dev.hxrry.tinker.Tinker;
import dev.hxrry.tinker.property.Category;
import dev.hxrry.tinker.property.TinkerProperty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TinkerConfig {

    private final boolean requireToolItem;
    private final Item toolItem;
    private final int defaultPermissionLevel;
    private final boolean singleplayerOwnerAllowed;
    private final Set<String> allowedProperties;
    private final Set<Category> allowedCategories;
    private final Messages messages;

    private TinkerConfig(boolean requireToolItem,
            Item toolItem,
            int defaultPermissionLevel,
            boolean singleplayerOwnerAllowed,
            Set<String> allowedProperties,
            Set<Category> allowedCategories,
            Messages messages) {
        this.requireToolItem = requireToolItem;
        this.toolItem = toolItem;
        this.defaultPermissionLevel = defaultPermissionLevel;
        this.singleplayerOwnerAllowed = singleplayerOwnerAllowed;
        this.allowedProperties = allowedProperties;
        this.allowedCategories = allowedCategories;
        this.messages = messages;
    }

    public static TinkerConfig load(Path file) {
        return load(read(file));
    }

    private static JsonObject read(Path file) {
        String bundled = bundled();
        try {
            if (!Files.isRegularFile(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, bundled, StandardCharsets.UTF_8);
                Tinker.LOGGER.info("Wrote a default config to {}", file.toAbsolutePath());
                return parse(bundled);
            }
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            Tinker.LOGGER.error("Could not read or write {} - running on the bundled defaults, "
                    + "so nothing you put in that file will apply.", file.toAbsolutePath(), e);
            return parse(bundled);
        }
    }

    private static JsonObject parse(String text) {
        JsonReader reader = new JsonReader(new StringReader(text));
        reader.setStrictness(Strictness.LENIENT);   
        return JsonParser.parseReader(reader).getAsJsonObject();
    }

    private static String bundled() {
        try (InputStream stream = TinkerConfig.class.getResourceAsStream("/tinker/config.jsonc")) {
            if (stream == null) {
                throw new IllegalStateException("the bundled config.jsonc is missing from the jar");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("could not read the bundled config.jsonc", e);
        }
    }

    static TinkerConfig load(JsonObject config) {
        JsonObject tool = section(config, "tool");
        boolean requireToolItem = bool(tool, "require-item", false);
        Item toolItem = resolveToolItem(string(tool, "item", "minecraft:golden_axe"));

        JsonObject permissions = section(config, "permissions");
        int defaultLevel = integer(permissions, "default-level", 2);
        boolean ownerAllowed = bool(permissions, "singleplayer-owner-always-allowed", true);

        Set<String> allowedProperties = new HashSet<>();
        Set<Category> allowedCategories = EnumSet.noneOf(Category.class);
        readAllowlist(config, allowedProperties, allowedCategories);
        warnAboutUnsafeCategories(allowedCategories);

        return new TinkerConfig(requireToolItem, toolItem, defaultLevel, ownerAllowed,
                Set.copyOf(allowedProperties), Set.copyOf(allowedCategories),
                new Messages(config));
    }

    private static void readAllowlist(JsonObject config,
            Set<String> properties,
            Set<Category> categories) {
        JsonObject allowlist = section(config, "allowlist");
        for (Category category : Category.values()) {
            JsonObject section = allowlist.getAsJsonObject(category.key());
            if (section == null) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
                if (bool(section, entry.getKey(), false)) {
                    properties.add(category.key() + "." + entry.getKey());
                    categories.add(category);
                }
            }
        }
    }

    private static void warnAboutUnsafeCategories(Set<Category> categories) {
        Set<Category> unsafe = EnumSet.noneOf(Category.class);
        for (Category category : categories) {
            if (!category.safeTier()) {
                unsafe.add(category);
            }
        }
        if (!unsafe.isEmpty()) {
            Tinker.LOGGER.warn("Tinker: {} allowlisted, which is supported but NOT ADVISED - "
                    + "expect dupes, bugs and weird situations.", unsafe);
        }
    }

    private static Item resolveToolItem(String raw) {
        Identifier id = raw == null ? null : Identifier.tryParse(raw);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) {
            Tinker.LOGGER.warn("tool.item '{}' is not a valid item; falling back to golden_axe.", raw);
            return Items.GOLDEN_AXE;
        }
        return item;
    }

    public boolean requireToolItem() {
        return requireToolItem;
    }

    public Item toolItem() {
        return toolItem;
    }

    public int defaultPermissionLevel() {
        return defaultPermissionLevel;
    }

    public boolean singleplayerOwnerAllowed() {
        return singleplayerOwnerAllowed;
    }

    public boolean isAllowed(TinkerProperty property) {
        return allowedProperties.contains(property.key());
    }

    public boolean isAllowed(Category category, String property) {
        return allowedProperties.contains(category.key() + "." + property);
    }

    public boolean isCategoryEnabled(Category category) {
        return allowedCategories.contains(category);
    }

    public Messages messages() {
        return messages;
    }

    public String summary() {
        return allowedProperties.size() + " properties across " + allowedCategories.size()
                + " categories allowlisted, tool "
                + (requireToolItem ? BuiltInRegistries.ITEM.getKey(toolItem) : "not required");
    }

    private static JsonObject section(JsonObject parent, String key) {
        JsonObject section = parent == null ? null : parent.getAsJsonObject(key);
        return section == null ? new JsonObject() : section;
    }

    private static boolean bool(JsonObject parent, String key, boolean fallback) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
    }

    private static int integer(JsonObject parent, String key, int fallback) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsInt() : fallback;
    }

    private static String string(JsonObject parent, String key, String fallback) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }
}
