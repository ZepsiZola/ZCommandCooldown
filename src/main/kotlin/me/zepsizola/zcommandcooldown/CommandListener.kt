package me.zepsizola.zcommandcooldown

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.TimeUnit

class CommandListener(private val plugin: ZCommandCooldown) : Listener {
    
    /**
     * Handles player command events to apply cooldowns
     * Using LOWEST priority to intercept commands before other plugins process them
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        if (event.isCancelled) return
        
        val player = event.player
        val message = event.message
        
        // Skip if the player has admin permission
        if (player.hasPermission("zcommandcooldown.admin")) {
            return
        }
        
        // Extract the command from the message
        val parts = message.substring(1).split("\\s+".toRegex(), 2)
        if (parts.isEmpty()) return
        
        val command = parts[0].lowercase()
        
        // Check if this command has a cooldown configuration
        val mainCommand = plugin.cooldownManager.getMainCommand(command)
        val cooldownConfig = plugin.configManager.getCooldownConfig(mainCommand)
        
        if (cooldownConfig != null) {
            plugin.debug("Command $command (main: $mainCommand) has cooldown config")
            
            // Check if player has a cooldown for this command
            if (plugin.cooldownManager.hasCooldown(player, mainCommand)) {
                // Get remaining cooldown time
                val remainingMillis = plugin.cooldownManager.getRemainingCooldown(player, mainCommand)
                val formattedTime = plugin.cooldownManager.formatCooldownTime(remainingMillis)
                
                // Send cooldown message to player
                sendCooldownMessage(player, formattedTime)
                
                // Cancel the command
                event.isCancelled = true
                return
            }
            
            // Get cooldown duration for this player and command
            val cooldownDuration = plugin.cooldownManager.getCooldownDuration(player, mainCommand)
            
            // Set cooldown if duration is greater than 0
            if (cooldownDuration > 0) {
                plugin.cooldownManager.setCooldown(player, mainCommand, cooldownDuration)
                plugin.debug("Set cooldown for ${player.name} on command $mainCommand: $cooldownDuration seconds")
            }
        }
    }
    
    /**
     * Sends a cooldown message to a player
     * @param player the player to send the message to
     * @param time the formatted cooldown time
     */
    private fun sendCooldownMessage(player: Player, time: String) {
        if (plugin.configManager.useActionBar) {
            // Send action bar message
            val message = plugin.configManager.getMessage(
                "cooldown.action-bar",
                mapOf("time" to time)
            )
            player.sendActionBar(message)
        } else {
            // Send chat message
            val message = plugin.configManager.getMessage(
                "cooldown.message",
                mapOf("time" to time)
            )
            player.sendMessage(message)
        }
    }
    
    /**
     * Handles player join events to clean up cooldowns
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Clean up cooldowns when a player joins
        plugin.cooldownManager.cleanupCooldowns()
    }
    
    /**
     * Handles player quit events to clean up cooldowns if configured
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // If clear-on-logout is enabled, clear cooldowns for this player
        if (plugin.configManager.clearOnLogout) {
            plugin.cooldownManager.clearCooldowns(event.player.uniqueId)
            plugin.debug("Cleared cooldowns for ${event.player.name} on logout")
        }
    }
}

