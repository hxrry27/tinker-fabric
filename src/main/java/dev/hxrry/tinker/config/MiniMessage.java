package dev.hxrry.tinker.config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;

public final class MiniMessage {

    private MiniMessage() {
    }

    public static Component parse(String input, Map<String, String> placeholders) {
        MutableComponent root = Component.empty();
        Deque<Style> styles = new ArrayDeque<>();
        styles.push(Style.EMPTY);

        StringBuilder run = new StringBuilder();
        int index = 0;
        while (index < input.length()) {
            char current = input.charAt(index);

            if (current == '\\' && index + 1 < input.length() && input.charAt(index + 1) == '<') {
                run.append('<');
                index += 2;
                continue;
            }
            if (current != '<') {
                run.append(current);
                index++;
                continue;
            }

            int close = input.indexOf('>', index);
            if (close < 0) {
                run.append(current);
                index++;
                continue;
            }

            String tag = input.substring(index + 1, close);
            String name = tag.toLowerCase(Locale.ROOT);
            index = close + 1;

            String replacement = placeholders.get(name);
            if (replacement != null) {
                run.append(replacement);
                continue;
            }

            if (name.startsWith("/")) {
                flush(root, run, styles.peek());
                if (styles.size() > 1) {
                    styles.pop();
                }
                continue;
            }

            Style pushed = apply(styles.peek(), name);
            if (pushed == null) {
                run.append('<').append(tag).append('>');
                continue;
            }
            flush(root, run, styles.peek());
            styles.push(pushed);
        }

        flush(root, run, styles.peek());
        return root;
    }

    private static void flush(MutableComponent root, StringBuilder run, Style style) {
        if (run.isEmpty()) {
            return;
        }
        root.append(Component.literal(run.toString()).setStyle(style));
        run.setLength(0);
    }

    private static Style apply(Style current, String name) {
        switch (name) {
            case "reset":
                return Style.EMPTY;
            case "bold", "b":
                return current.withBold(true);
            case "italic", "i", "em":
                return current.withItalic(true);
            case "underlined", "u":
                return current.withUnderlined(true);
            case "strikethrough", "st":
                return current.withStrikethrough(true);
            case "obfuscated", "obf":
                return current.withObfuscated(true);
            default:
                break;
        }

        if (name.startsWith("#")) {
            TextColor hex = TextColor.parseColor(name).result().orElse(null);
            return hex == null ? null : current.withColor(hex);
        }

        ChatFormatting colour = named(name);
        return colour == null ? null : current.withColor(colour);
    }

    private static final Map<String, ChatFormatting> COLOURS = Map.ofEntries(
            Map.entry("black", ChatFormatting.BLACK),
            Map.entry("dark_blue", ChatFormatting.DARK_BLUE),
            Map.entry("dark_green", ChatFormatting.DARK_GREEN),
            Map.entry("dark_aqua", ChatFormatting.DARK_AQUA),
            Map.entry("dark_red", ChatFormatting.DARK_RED),
            Map.entry("dark_purple", ChatFormatting.DARK_PURPLE),
            Map.entry("gold", ChatFormatting.GOLD),
            Map.entry("gray", ChatFormatting.GRAY),
            Map.entry("dark_gray", ChatFormatting.DARK_GRAY),
            Map.entry("blue", ChatFormatting.BLUE),
            Map.entry("green", ChatFormatting.GREEN),
            Map.entry("aqua", ChatFormatting.AQUA),
            Map.entry("red", ChatFormatting.RED),
            Map.entry("light_purple", ChatFormatting.LIGHT_PURPLE),
            Map.entry("yellow", ChatFormatting.YELLOW),
            Map.entry("white", ChatFormatting.WHITE));

    private static ChatFormatting named(String name) {
        return COLOURS.get(name);
    }
}
