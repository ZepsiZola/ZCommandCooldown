# ZCommandCooldown

A lightweight Minecraft plugin that adds customizable, permission-based cooldowns to any command on your server.

[![Paper Compatible](https://img.shields.io/badge/Paper-Compatible-brightgreen)](https://papermc.io/)
[![Folia Compatible](https://img.shields.io/badge/Folia-Compatible-brightgreen)](https://github.com/PaperMC/Folia)
[![Minecraft 1.20+](https://img.shields.io/badge/Minecraft-1.20%2B-blue)](https://www.minecraft.net/)

## Features

- **Permission-Based Cooldowns**: Set different cooldown durations based on player permissions
- **Command Aliases**: Define aliases for commands that share the same cooldown
- **Customizable Messages**: Fully customizable messages with MiniMessage format support
- **Action Bar Support**: Display cooldown messages in the action bar instead of chat
- **Admin Commands**: Reload configuration, clear cooldowns, and more
- **Persistent Cooldowns**: Cooldowns persist across server restarts
- **Folia Compatible**: Works with both Paper and Folia servers

## Installation

1. Download the latest release from the [Releases](https://github.com/zepsizola/ZCommandCooldown/releases) page
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Edit the configuration files in the `plugins/ZCommandCooldown` directory to customize the plugin

## Building from Source

If you want to build the plugin from source:

```bash
# Clone the repository
git clone https://github.com/zepsizola/ZCommandCooldown.git
cd ZCommandCooldown

# Build the plugin
./gradlew jar
```

## Configuration (`cooldowns.yml`)
[View cooldowns.yml](./src/main/resources/cooldowns.yml)
