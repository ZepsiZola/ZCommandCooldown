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

## Dependencies

- [MCKotlin](https://modrinth.com/plugin/mckotlin)

## Installation

1. Download the latest release from [Modrinth](https://modrinth.com/plugin/zcommandcooldown/versions)
2. Download the appropriate version of MCKotlin from [Modrinth](https://modrinth.com/plugin/mckotlin)
3. Place the JAR file in your server's `plugins` folder
4. Restart your server
5. Edit the configuration files in the `plugins/ZCommandCooldown/` directory to customize the plugin

## Building from Source

If you want to build the plugin from source:

```bash
# Clone the repository
git clone https://github.com/zepsizola/ZCommandCooldown.git
cd ZCommandCooldown

# Build the plugin
./gradlew shadowJar
```

## Configuration ([`cooldowns.yml`](./src/main/resources/cooldowns.yml))
```yaml
cooldowns:
  repair: # Command that a cooldown will be applied to.
    aliases: # Other aliases for the command. All share the same cooldown.
    - fix
    - erepair
    durations: # Set durations of cooldown according to permission. If player has multiple of these perms, the lowest duration is used.
      default: 600             # 600 seconds for players by default
      group.group1: 120        # 2 minutes for players with group.group1 permission
      group.group2: 60         # 1 minute for players with group.group2 permission
      custom.repair.fast: 30   # 30 secods for players with the custom.repair.fast permission
```
