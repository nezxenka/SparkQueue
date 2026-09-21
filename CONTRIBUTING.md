Thanks for taking the time to contribute to SparkQueue! Every bug report, idea and pull request helps make the plugin better.

This document explains how to get help, report problems and get your changes merged.

### :speech_balloon: Looking for support?

If you have a question about installing or configuring SparkQueue, first read the [README](README.md) and look through the default `config.yml`. If that doesn't answer it, feel free to open an issue and ask.

### :bug: Reporting bugs?

Before reporting a bug, please make sure it is actually caused by SparkQueue and not by another plugin, the backend server or the proxy itself. Try to reproduce it with SparkQueue as the only plugin on the proxy if you can.

Bugs should be reported using the [GitHub Issues tab](https://github.com/nezxenka/SparkQueue/issues). A good report includes:

* The SparkQueue, Velocity and Java versions you are running.
* Your `config.yml` (remove anything private first).
* The steps needed to reproduce the problem.
* The full error from the proxy console, if there is one.

### :bulb: Suggesting features?

Feature requests are welcome in the [issue tracker](https://github.com/nezxenka/SparkQueue/issues) too. Describe the problem you want to solve, not only the solution, so we can find the best way to fit it into the plugin.

### :pencil: Want to contribute code?

#### Pull Requests

If you made a change or improvement that could be useful for others, please open a pull request to merge it back into the project. Bug fixes are especially appreciated!

If you're planning a large change, please open an issue first so we can discuss it before you start working. Most pull requests are happily accepted, but bigger changes affect how easy the project is to maintain and need more consideration.

When opening a pull request:

* Keep it focused on one change. Several small pull requests are easier to review than one huge one.
* Explain what you changed and why.
* Make sure the project builds and that you tested your change on a real Velocity proxy.

#### Setting up a development environment

All you need is JDK 21 and an IDE such as IntelliJ IDEA. Clone the repository, open it as a Gradle project and build it:

```bash
./gradlew build
```

The plugin jar is written to `build/libs/`. To test it, drop the jar into the `plugins` folder of a Velocity 3.3+ proxy and set up one or more backend servers behind it.

#### Code style

SparkQueue loosely follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). In general, copy the style of the code in the class you're editing.

A few project rules:

* Never block the proxy's threads. Queue work runs on the dispatcher thread, and server pings are asynchronous.
* Don't hardcode settings or player-facing text. Add a key to `config.yml` and read it through `PluginConfig`, with a sensible default.
* Messages use the [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format. Anything that comes from a player, such as a server name typed in a command, must never be parsed as MiniMessage tags.

#### Project Layout

The plugin lives in the `net.nezxenka.sparkqueue` package and is split into a few parts:

* **`SparkQueuePlugin`** - The plugin entry point. Loads the config, starts the queue engine and registers commands and listeners.
* **`command`** - The `/queue` command.
* **`config`** - Loading `config.yml`, priority tiers and pre-parsed message templates.
* **`core`** - The queue engine itself: player ordering, server health polling and sending players to their target server.

### :heart: Contributors

Everyone whose pull request is merged appears automatically in the [contributors list](https://github.com/nezxenka/SparkQueue/graphs/contributors) and in the Contributors section of the [README](README.md#contributors). Thank you!
