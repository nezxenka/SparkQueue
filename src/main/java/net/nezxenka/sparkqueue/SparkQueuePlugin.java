package net.nezxenka.sparkqueue;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.nezxenka.sparkqueue.command.QueueCommand;
import net.nezxenka.sparkqueue.config.PluginConfig;
import net.nezxenka.sparkqueue.core.QueueEngine;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
    id = "sparkqueue",
    name = "SparkQueue",
    version = "1.0",
    description = "Next-generation priority matchmaking queue for Velocity.",
    authors = {"nezxenka"}
)
public final class SparkQueuePlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private QueueEngine queueEngine;

    @Inject
    public SparkQueuePlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Starting SparkQueue engine...");

        PluginConfig config;
        try {
            config = PluginConfig.load(dataDirectory);
        } catch (IOException e) {
            logger.error("Failed to load config.yml, SparkQueue will stay disabled.", e);
            return;
        }

        this.queueEngine = new QueueEngine(proxy, logger, config);
        this.queueEngine.start();

        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta meta = commandManager.metaBuilder("queue").aliases("sparkqueue").plugin(this).build();
        commandManager.register(meta, new QueueCommand(queueEngine, config));
        logger.info("SparkQueue initialized with priority dispatching and health polling.");
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (queueEngine != null) {
            queueEngine.dequeue(event.getPlayer().getUniqueId());
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (queueEngine != null) {
            queueEngine.shutdown();
        }
        logger.info("SparkQueue shutdown complete.");
    }

    public QueueEngine getQueueEngine() {
        return queueEngine;
    }
}
