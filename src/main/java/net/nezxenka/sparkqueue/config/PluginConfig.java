package net.nezxenka.sparkqueue.config;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PluginConfig {

    private static final String FILE_NAME = "config.yml";

    private final int checkIntervalSeconds;
    private final int maxQueueSize;
    private final long sendDelayMs;
    private final int fallbackMaxPlayers;
    private final List<PriorityTier> priorityTiers;
    private final Map<String, MessageTemplate> messages;

    private PluginConfig(ConfigurationNode root, ConfigurationNode defaults) {
        ConfigurationNode settings = root.node("settings");
        this.checkIntervalSeconds = Math.max(1, settings.node("check-interval-seconds").getInt());
        this.maxQueueSize = settings.node("max-queue-size").getInt();
        this.sendDelayMs = Math.max(0, settings.node("send-delay-ms").getLong());
        this.fallbackMaxPlayers = settings.node("fallback-max-players").getInt();

        List<PriorityTier> tiers = new ArrayList<>();
        for (ConfigurationNode tier : root.node("priority-tiers").childrenMap().values()) {
            tiers.add(new PriorityTier(tier.node("weight").getInt(), tier.node("permission").getString("")));
        }
        this.priorityTiers = List.copyOf(tiers);

        Map<String, String> rawMessages = new HashMap<>();
        for (ConfigurationNode source : List.of(defaults, root)) {
            source.node("messages").childrenMap().forEach((key, node) -> rawMessages.put(String.valueOf(key), node.getString("")));
        }
        Map<String, MessageTemplate> templates = new HashMap<>();
        rawMessages.forEach((key, raw) -> templates.put(key, new MessageTemplate(raw)));
        this.messages = Map.copyOf(templates);
    }

    public static PluginConfig load(Path dataDirectory) throws IOException {
        URL bundled = PluginConfig.class.getResource("/" + FILE_NAME);
        if (bundled == null) {
            throw new IOException("Bundled " + FILE_NAME + " is missing from the plugin jar");
        }

        Path file = dataDirectory.resolve(FILE_NAME);
        if (Files.notExists(file)) {
            Files.createDirectories(dataDirectory);
            try (InputStream in = bundled.openStream()) {
                Files.copy(in, file);
            }
        }

        ConfigurationNode defaults = YamlConfigurationLoader.builder().url(bundled).build().load();
        ConfigurationNode root = YamlConfigurationLoader.builder().path(file).build().load();

        root.node("settings").mergeFrom(defaults.node("settings"));
        if (root.node("priority-tiers").virtual()) {
            root.node("priority-tiers").mergeFrom(defaults.node("priority-tiers"));
        }
        return new PluginConfig(root, defaults);
    }

    public int checkIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public int maxQueueSize() {
        return maxQueueSize;
    }

    public long sendDelayMs() {
        return sendDelayMs;
    }

    public int fallbackMaxPlayers() {
        return fallbackMaxPlayers;
    }

    public int priorityWeight(Player player) {
        return priorityTiers.stream()
                .filter(tier -> tier.appliesTo(player))
                .mapToInt(PriorityTier::weight)
                .max()
                .orElse(0);
    }

    public void send(Audience audience, String key) {
        send(audience, key, Map.of());
    }

    public void send(Audience audience, String key, Map<String, String> placeholders) {
        message(key, placeholders).ifPresent(audience::sendMessage);
    }

    public void sendActionBar(Audience audience, String key, Map<String, String> placeholders) {
        message(key, placeholders).ifPresent(audience::sendActionBar);
    }

    public Optional<Component> message(String key, Map<String, String> placeholders) {
        MessageTemplate template = messages.get(key);
        if (template == null || template.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(template.render(placeholders));
    }
}
