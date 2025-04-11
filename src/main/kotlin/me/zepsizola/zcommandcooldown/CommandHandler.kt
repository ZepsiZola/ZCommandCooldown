package me.zepsizola.zcommandcooldown

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

class CommandHandler(private val plugin: ZCommandCooldown) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            // Show help message if no arguments provided
            sendHelpMessage(sender)
            return true
        }
        
        when (args[0].lowercase()) {
            "reload" -> handleReloadCommand(sender)
            "clear" -> handleClearCommand(sender, args)
            "debug" -> handleDebugCommand(sender)
            "help" -> sendHelpMessage(sender)
            else -> {
                // Unknown command
                sender.sendMessage(plugin.configManager.getMessage("error.unknown-command"))
            }
        }
        
        return true
    }
    
    /**
     * Handles the reload command
     * @param sender the command sender
     */
    private fun handleReloadCommand(sender: CommandSender) {
        if (!sender.hasPermission("zcommandcooldown.reload")) {
            sender.sendMessage(plugin.configManager.getMessage("error.no-permission"))
            return
        }
        
        val success = plugin.reload()
        
        if (success) {
            sender.sendMessage(plugin.configManager.getMessage("admin.reload.success"))
        } else {
            sender.sendMessage(plugin.configManager.getMessage("admin.reload.error"))
        }
    }
    
    /**
     * Handles the clear command
     * @param sender the command sender
     * @param args the command arguments
     */
    private fun handleClearCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("zcommandcooldown.clear")) {
            sender.sendMessage(plugin.configManager.getMessage("error.no-permission"))
            return
        }
        
        if (args.size < 2) {
            sender.sendMessage(plugin.configManager.getMessage("error.invalid-args"))
            return
        }
        
        if (args[1].equals("all", ignoreCase = true)) {
            // Clear all cooldowns
            plugin.cooldownManager.clearAllCooldowns()
            sender.sendMessage(plugin.configManager.getMessage("admin.clear.all-success"))
            return
        }
        
        // Clear cooldowns for a specific player
        val targetName = args[1]
        val targetPlayer = Bukkit.getPlayer(targetName)
        
        if (targetPlayer != null) {
            plugin.cooldownManager.clearCooldowns(targetPlayer.uniqueId)
            sender.sendMessage(
                plugin.configManager.getMessage(
                    "admin.clear.success",
                    mapOf("player" to targetPlayer.name)
                )
            )
        } else {
            // Try to find player by UUID
            try {
                val targetUUID = UUID.fromString(targetName)
                plugin.cooldownManager.clearCooldowns(targetUUID)
                sender.sendMessage(
                    plugin.configManager.getMessage(
                        "admin.clear.success",
                        mapOf("player" to targetName)
                    )
                )
            } catch (e: IllegalArgumentException) {
                // Not a valid UUID
                sender.sendMessage(
                    plugin.configManager.getMessage(
                        "admin.clear.player-not-found",
                        mapOf("player" to targetName)
                    )
                )
            }
        }
    }
    
    /**
     * Handles the debug command
     * @param sender the command sender
     */
    private fun handleDebugCommand(sender: CommandSender) {
        if (!sender.hasPermission("zcommandcooldown.admin")) {
            sender.sendMessage(plugin.configManager.getMessage("error.no-permission"))
            return
        }
        
        val debugEnabled = plugin.toggleDebugMode()
        
        if (debugEnabled) {
            sender.sendMessage(plugin.configManager.getMessage("admin.debug.enabled"))
        } else {
            sender.sendMessage(plugin.configManager.getMessage("admin.debug.disabled"))
        }
    }
    
    /**
     * Sends the help message to a command sender
     * @param sender the command sender
     */
    private fun sendHelpMessage(sender: CommandSender) {
        sender.sendMessage(plugin.configManager.getMessage("admin.help.header"))
        sender.sendMessage(plugin.configManager.getMessage("admin.help.reload"))
        sender.sendMessage(plugin.configManager.getMessage("admin.help.clear"))
        sender.sendMessage(plugin.configManager.getMessage("admin.help.clear-all"))
        sender.sendMessage(plugin.configManager.getMessage("admin.help.debug"))
        sender.sendMessage(plugin.configManager.getMessage("admin.help.help"))
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("zcommandcooldown.admin")) {
            return emptyList()
        }
        
        if (args.size == 1) {
            val completions = mutableListOf("reload", "clear", "debug", "help")
            return completions.filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].equals("clear", ignoreCase = true)) {
            val completions = mutableListOf("all")
            
            // Add online player names
            completions.addAll(Bukkit.getOnlinePlayers().map { it.name })
            
            return completions.filter { it.startsWith(args[1], ignoreCase = true) }
        }
        
        return emptyList()
    }
}

