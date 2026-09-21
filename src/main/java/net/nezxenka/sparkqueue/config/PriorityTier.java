package net.nezxenka.sparkqueue.config;

import com.velocitypowered.api.proxy.Player;

public record PriorityTier(int weight, String permission) {

    public boolean appliesTo(Player player) {
        return permission.isEmpty() || player.hasPermission(permission);
    }
}
