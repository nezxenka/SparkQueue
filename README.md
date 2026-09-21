# SparkQueue

High-performance, multi-threaded priority queue and player matchmaking plugin for **Velocity** Minecraft proxy networks.

## Overview
Rebranded and modern Java 21 re-architecture of the open-source `ajQueue` concept, engineered specifically for high-concurrency Minecraft proxy setups and Folia backend servers.

### Features
* **Priority Matching:** Weighted multi-tier queues (`sparkqueue.priority.admin`, `sparkqueue.priority.donor`) with atomic FIFO ordering for equal weights.
* **Non-Blocking Health Pinging:** Prevents player drops by polling backend server capacity asynchronously.
* **Zero Main-Thread Overhead:** Asynchronous scheduled dispatcher thread prevents proxy latency spikes during peak player rushes.
* **Seamless Transfer:** Compatible with both Paper and Folia backend servers.
* **Fully Configurable:** Timings, queue limits, priority tiers and every message (MiniMessage format) live in `config.yml`.

## Commands & Permissions
* `/queue <server>` - Enqueue for target backend server.
* `/queue leave` - Exit queue.
* `/sparkqueue` - Show current queue position and status.
* `sparkqueue.priority.admin` - Top priority bypass tier (default weight: 100).
* `sparkqueue.priority.donor` - Donor priority tier (default weight: 50).

## Configuration
On first start SparkQueue creates `plugins/sparkqueue/config.yml`. Missing keys fall back to the bundled defaults, and setting a message to `""` disables it.

## Build
Requires JDK 21.
```bash
./gradlew build
```
The plugin jar is written to `build/libs/SparkQueue-1.0.jar`.

## Contributors
Thanks to everyone who helps make SparkQueue better!

<a href="https://github.com/nezxenka/SparkQueue/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=nezxenka/SparkQueue" alt="SparkQueue contributors" />
</a>

The grid above updates automatically from the commit history.

**Want to contribute?** Bug reports, ideas and pull requests are all welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.
