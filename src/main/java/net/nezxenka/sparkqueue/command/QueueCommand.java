package net.nezxenka.sparkqueue.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.nezxenka.sparkqueue.config.PluginConfig;
import net.nezxenka.sparkqueue.core.QueueEngine;

import java.util.Map;

public final class QueueCommand implements SimpleCommand {

    private final QueueEngine engine;
    private final PluginConfig config;

    public QueueCommand(QueueEngine engine, PluginConfig config) {
        this.engine = engine;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            config.send(invocation.source(), "players-only");
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            engine.status(player.getUniqueId()).ifPresentOrElse(
                    status -> config.send(player, "queue-position", status.placeholders()),
                    () -> config.send(player, "usage")
            );
            return;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            engine.dequeue(player.getUniqueId()).ifPresentOrElse(
                    server -> config.send(player, "queue-left", Map.of("server", server)),
                    () -> config.send(player, "not-in-queue")
            );
            return;
        }

        engine.enqueue(player, args[0]);
    }
}
