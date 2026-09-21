package net.nezxenka.sparkqueue.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.regex.Pattern;

final class MessageTemplate {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-z-]+)%");

    private final String raw;
    private final Component parsed;
    private final boolean preParsed;

    MessageTemplate(String raw) {
        this.raw = raw;
        this.parsed = MINI_MESSAGE.deserialize(raw);
        this.preParsed = countPlaceholders(raw) == countPlaceholders(parsed);
    }

    boolean isEmpty() {
        return raw.isEmpty();
    }

    Component render(Map<String, String> placeholders) {
        if (placeholders.isEmpty()) {
            return parsed;
        }
        if (preParsed) {
            return parsed.replaceText(TextReplacementConfig.builder()
                    .match(PLACEHOLDER)
                    .replacement((match, original) -> {
                        String value = placeholders.get(match.group(1));
                        return value == null ? original : Component.text(value);
                    })
                    .build());
        }

        String text = raw;
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            text = text.replace("%" + placeholder.getKey() + "%", MINI_MESSAGE.escapeTags(placeholder.getValue()));
        }
        return MINI_MESSAGE.deserialize(text);
    }

    private static int countPlaceholders(String text) {
        return (int) PLACEHOLDER.matcher(text).results().count();
    }

    private static int countPlaceholders(Component component) {
        int count = component instanceof TextComponent text ? countPlaceholders(text.content()) : 0;
        for (Component child : component.children()) {
            count += countPlaceholders(child);
        }
        return count;
    }
}
