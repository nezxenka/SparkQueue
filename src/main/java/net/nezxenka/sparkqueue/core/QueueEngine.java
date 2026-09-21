package net.nezxenka.sparkqueue.core;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.nezxenka.sparkqueue.config.PluginConfig;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public final class QueueEngine {

    private final ProxyServer proxy;
    private final Logger logger;
    private final PluginConfig config;
    private final Map<String, PriorityBlockingQueue<QueuedPlayer>> queues = new ConcurrentHashMap<>();
    private final Map<UUID, String> activePlayerQueues = new ConcurrentHashMap<>();
    private final Set<String> pendingPings = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService worker;

    public QueueEngine(ProxyServer proxy, Logger logger, PluginConfig config) {
        this.proxy = proxy;
        this.logger = logger;
        this.config = config;
    }

    public void start() {
        this.worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SparkQueue-DispatchThread");
            t.setDaemon(true);
            return t;
        });
        long interval = config.checkIntervalSeconds();
        this.worker.scheduleAtFixedRate(this::tick, interval, interval, TimeUnit.SECONDS);
    }

    public synchronized void enqueue(Player player, String serverName) {
        Optional<RegisteredServer> optServer = proxy.getServer(serverName);
        if (optServer.isEmpty()) {
            config.send(player, "server-not-found", Map.of("server", serverName));
            return;
        }

        String target = optServer.get().getServerInfo().getName();
        UUID uuid = player.getUniqueId();

        if (target.equals(activePlayerQueues.get(uuid))) {
            status(uuid).ifPresent(status -> config.send(player, "queue-position", status.placeholders()));
            return;
        }

        PriorityBlockingQueue<QueuedPlayer> queue = queues.computeIfAbsent(target, k -> new PriorityBlockingQueue<>());
        int maxQueueSize = config.maxQueueSize();
        if (maxQueueSize > 0 && queue.size() >= maxQueueSize) {
            config.send(player, "queue-full", Map.of("server", target));
            return;
        }

        dequeue(uuid);
        queue.offer(new QueuedPlayer(player, config.priorityWeight(player)));
        activePlayerQueues.put(uuid, target);

        status(uuid).ifPresent(status -> config.send(player, "queue-joined", status.placeholders()));
    }

    public synchronized Optional<String> dequeue(UUID uuid) {
        String server = activePlayerQueues.remove(uuid);
        if (server == null) {
            return Optional.empty();
        }
        PriorityBlockingQueue<QueuedPlayer> queue = queues.get(server);
        if (queue != null) {
            queue.removeIf(p -> p.uuid().equals(uuid));
        }
        return Optional.of(server);
    }

    public Optional<QueueStatus> status(UUID uuid) {
        String server = activePlayerQueues.get(uuid);
        if (server == null) {
            return Optional.empty();
        }
        PriorityBlockingQueue<QueuedPlayer> queue = queues.get(server);
        if (queue == null) {
            return Optional.empty();
        }

        QueuedPlayer[] snapshot = queue.toArray(new QueuedPlayer[0]);
        QueuedPlayer self = null;
        for (QueuedPlayer queued : snapshot) {
            if (queued.uuid().equals(uuid)) {
                self = queued;
                break;
            }
        }
        if (self == null) {
            return Optional.empty();
        }

        int position = 1;
        for (QueuedPlayer queued : snapshot) {
            if (queued.compareTo(self) < 0) {
                position++;
            }
        }
        return Optional.of(new QueueStatus(server, position, snapshot.length));
    }

    private void tick() {
        try {
            for (Map.Entry<String, PriorityBlockingQueue<QueuedPlayer>> entry : queues.entrySet()) {
                String serverName = entry.getKey();
                PriorityBlockingQueue<QueuedPlayer> queue = entry.getValue();

                if (queue.isEmpty()) continue;

                Optional<RegisteredServer> optServer = proxy.getServer(serverName);
                if (optServer.isEmpty()) {
                    showOffline(serverName, queue);
                    continue;
                }
                if (!pendingPings.add(serverName)) continue;
                RegisteredServer target = optServer.get();

                target.ping().handleAsync((ping, error) -> {
                    pendingPings.remove(serverName);
                    if (error != null) {
                        showOffline(serverName, queue);
                    } else {
                        dispatch(target, queue, freeSlots(target, ping));
                        showProgress(serverName, queue);
                    }
                    return null;
                }, worker).exceptionally(ex -> {
                    logger.error("Failed to process queue for {}", serverName, ex);
                    return null;
                });
            }
        } catch (RuntimeException e) {
            logger.error("Queue tick failed", e);
        }
    }

    private int freeSlots(RegisteredServer target, ServerPing ping) {
        Optional<ServerPing.Players> players = ping.getPlayers();
        int max = players.map(ServerPing.Players::getMax).orElse(config.fallbackMaxPlayers());
        int online = players.map(ServerPing.Players::getOnline).orElse(target.getPlayersConnected().size());
        return max - online;
    }

    private void dispatch(RegisteredServer target, PriorityBlockingQueue<QueuedPlayer> queue, int freeSlots) {
        int count = Math.min(Math.min(freeSlots, queue.size()), maxSendsPerTick());
        long delay = config.sendDelayMs();
        for (int i = 0; i < count; i++) {
            worker.schedule(() -> sendNext(target, queue), i * delay, TimeUnit.MILLISECONDS);
        }
    }

    private int maxSendsPerTick() {
        long delay = config.sendDelayMs();
        if (delay <= 0) {
            return Integer.MAX_VALUE;
        }
        long intervalMs = TimeUnit.SECONDS.toMillis(config.checkIntervalSeconds());
        return (int) Math.max(1, intervalMs / delay);
    }

    private synchronized void sendNext(RegisteredServer target, PriorityBlockingQueue<QueuedPlayer> queue) {
        String serverName = target.getServerInfo().getName();
        QueuedPlayer next;
        while ((next = queue.poll()) != null) {
            activePlayerQueues.remove(next.uuid(), serverName);
            Optional<Player> player = proxy.getPlayer(next.uuid());
            if (player.isPresent()) {
                config.send(player.get(), "connecting", Map.of("server", serverName));
                player.get().createConnectionRequest(target).fireAndForget();
                return;
            }
        }
    }

    private void showProgress(String serverName, PriorityBlockingQueue<QueuedPlayer> queue) {
        QueuedPlayer[] sorted = queue.toArray(new QueuedPlayer[0]);
        Arrays.sort(sorted);
        for (int i = 0; i < sorted.length; i++) {
            QueueStatus status = new QueueStatus(serverName, i + 1, sorted.length);
            proxy.getPlayer(sorted[i].uuid())
                    .ifPresent(player -> config.sendActionBar(player, "actionbar-progress", status.placeholders()));
        }
    }

    private void showOffline(String serverName, PriorityBlockingQueue<QueuedPlayer> queue) {
        config.message("server-offline", Map.of("server", serverName)).ifPresent(message -> {
            for (QueuedPlayer queued : queue) {
                proxy.getPlayer(queued.uuid()).ifPresent(player -> player.sendActionBar(message));
            }
        });
    }

    public void shutdown() {
        if (worker != null) {
            worker.shutdownNow();
        }
        queues.clear();
        activePlayerQueues.clear();
        pendingPings.clear();
    }
}
