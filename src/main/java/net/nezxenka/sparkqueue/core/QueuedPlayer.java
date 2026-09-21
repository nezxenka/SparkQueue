package net.nezxenka.sparkqueue.core;

import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

public record QueuedPlayer(UUID uuid, String username, int priorityWeight, long timestamp) implements Comparable<QueuedPlayer> {

    public QueuedPlayer(Player player, int priorityWeight) {
        this(player.getUniqueId(), player.getUsername(), priorityWeight, System.currentTimeMillis());
    }

    @Override
    public int compareTo(QueuedPlayer other) {
        if (this.priorityWeight != other.priorityWeight) {
            return Integer.compare(other.priorityWeight, this.priorityWeight); // higher priority first
        }
        if (this.timestamp != other.timestamp) {
            return Long.compare(this.timestamp, other.timestamp); // FIFO
        }
        return this.uuid.compareTo(other.uuid);
    }
}
