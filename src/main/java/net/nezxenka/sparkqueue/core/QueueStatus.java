package net.nezxenka.sparkqueue.core;

import java.util.Map;

public record QueueStatus(String server, int position, int total) {

    public Map<String, String> placeholders() {
        return Map.of(
                "server", server,
                "position", String.valueOf(position),
                "total", String.valueOf(total)
        );
    }
}
