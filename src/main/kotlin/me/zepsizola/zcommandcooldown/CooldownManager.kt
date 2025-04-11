package me.zepsizola.zcommandcooldown

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import java.io.IOException
import java.util.logging.Level

class CooldownManager(private val plugin: ZCommandCooldown) {
    
    // Map of player UUID to a map of command to cooldown end time
    private val cooldowns = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>()
    
    // Map of command aliases to their main command
    private val commandAliases = ConcurrentHashMap<String, String>()
    
    // File to store cooldowns
    private val cooldownsFile = File(plugin.dataFolder, "data/cooldowns.yml")
    
    init {
        try {
            // Create data directory if it doesn't exist
            val dataDir = File(plugin.dataFolder, "data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }
            
            // Load cooldowns from file
            loadCooldowns()
            
            // Load command aliases from config
            loadCommandAliases()
        } catch (e: Exception) {
            plugin.logger.warning("Error initializing CooldownManager: ${e.message}")
            // Continue with empty cooldowns if there's an error
        }
    }
    
    /**
     * Loads command aliases from the cooldowns.yml file
     */
    private fun loadCommandAliases() {
        try {
            val cooldownsSection = plugin.configManager.cooldowns.getConfigurationSection("cooldowns")
            if (cooldownsSection != null) {
                for (commandName in cooldownsSection.getKeys(false)) {
                    val commandSection = cooldownsSection.getConfigurationSection(commandName)
                    if (commandSection != null) {
                        val aliases = commandSection.getStringList("aliases")
                        for (alias in aliases) {
                            commandAliases[alias.lowercase()] = commandName.lowercase()
                        }
                    }
                }
            }
            
            plugin.debug("Loaded ${commandAliases.size} command aliases")
        } catch (e: Exception) {
            plugin.logger.warning("Error loading command aliases: ${e.message}")
            // Continue without aliases if there's an error
        }
    }
    
    /**
     * Reloads cooldowns and command aliases from config
     */
    fun reloadCooldowns() {
        commandAliases.clear()
        loadCommandAliases()
    }
    
    /**
     * Loads cooldowns from file
     */
    private fun loadCooldowns() {
        if (!cooldownsFile.exists()) {
            return
        }
        
        try {
            val config = YamlConfiguration.loadConfiguration(cooldownsFile)
            
            for (playerUUID in config.getKeys(false)) {
                val playerSection = config.getConfigurationSection(playerUUID) ?: continue
                val playerCooldowns = ConcurrentHashMap<String, Long>()
                
                for (command in playerSection.getKeys(false)) {
                    val cooldownEnd = playerSection.getLong(command)
                    // Only load cooldowns that haven't expired yet
                    if (cooldownEnd > System.currentTimeMillis()) {
                        playerCooldowns[command] = cooldownEnd
                    }
                }
                
                if (playerCooldowns.isNotEmpty()) {
                    cooldowns[UUID.fromString(playerUUID)] = playerCooldowns
                }
            }
            
            plugin.debug("Loaded cooldowns for ${cooldowns.size} players")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error loading cooldowns from file", e)
        }
    }
    
    /**
     * Saves cooldowns to file
     */
    fun saveCooldowns() {
        try {
            val config = YamlConfiguration()
            
            // Remove expired cooldowns before saving
            cleanupExpiredCooldowns()
            
            for ((playerUUID, playerCooldowns) in cooldowns) {
                if (playerCooldowns.isEmpty()) continue
                
                val playerSection = config.createSection(playerUUID.toString())
                for ((command, cooldownEnd) in playerCooldowns) {
                    playerSection.set(command, cooldownEnd)
                }
            }
            
            config.save(cooldownsFile)
            plugin.debug("Saved cooldowns for ${cooldowns.size} players")
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Error saving cooldowns to file", e)
        }
    }
    
    /**
     * Removes expired cooldowns from memory
     */
    private fun cleanupExpiredCooldowns() {
        val currentTime = System.currentTimeMillis()
        val playersToRemove = mutableListOf<UUID>()
        
        for ((playerUUID, playerCooldowns) in cooldowns) {
            val commandsToRemove = mutableListOf<String>()
            
            for ((command, cooldownEnd) in playerCooldowns) {
                if (cooldownEnd <= currentTime) {
                    commandsToRemove.add(command)
                }
            }
            
            for (command in commandsToRemove) {
                playerCooldowns.remove(command)
            }
            
            if (playerCooldowns.isEmpty()) {
                playersToRemove.add(playerUUID)
            }
        }
        
        for (playerUUID in playersToRemove) {
            cooldowns.remove(playerUUID)
        }
    }
    
    /**
     * Gets the cooldown duration for a player and command
     * @param player the player to get the cooldown for
     * @param command the command to get the cooldown for
     * @return the cooldown duration in seconds, or 0 if no cooldown
     */
    fun getCooldownDuration(player: Player, command: String): Int {
        // Check if player has bypass permission
        if (player.hasPermission("zcommandcooldown.bypass")) {
            return 0
        }
        
        // Get the main command if this is an alias
        val mainCommand = getMainCommand(command)
        
        // Get the cooldown config for this command
        val cooldownSection = plugin.configManager.getCooldownConfig(mainCommand) ?: return 0
        
        // Get the durations section
        val durationsSection = cooldownSection.getConfigurationSection("durations") ?: return 0
        
        // Get all permission-based cooldowns
        val permissionCooldowns = mutableMapOf<String, Int>()
        
        // Process the durations section to extract permission nodes and cooldown values
        processPermissionCooldowns(durationsSection, "", permissionCooldowns)
        
        plugin.debug("Permission cooldowns for $mainCommand: $permissionCooldowns")
        
        // Find the lowest cooldown that the player has permission for
        var lowestCooldown: Int? = null
        for ((permission, cooldown) in permissionCooldowns) {
            if (permission == "default") continue
            
            if (player.hasPermission(permission)) {
                plugin.debug("Player ${player.name} has permission $permission with cooldown $cooldown")
                if (lowestCooldown == null || cooldown < lowestCooldown) {
                    lowestCooldown = cooldown
                }
            }
        }
        
        // If no permission matched, use default or highest cooldown
        if (lowestCooldown == null) {
            lowestCooldown = permissionCooldowns["default"] 
                ?: permissionCooldowns.values.maxOrNull() 
                ?: 0
            
            plugin.debug("Using default/highest cooldown for ${player.name}: $lowestCooldown")
        }
        
        return lowestCooldown
    }
    
    /**
     * Recursively processes a configuration section to extract permission nodes and cooldown values
     * @param section the configuration section to process
     * @param prefix the current permission node prefix
     * @param result the map to store the results in
     */
    private fun processPermissionCooldowns(
        section: ConfigurationSection,
        prefix: String,
        result: MutableMap<String, Int>
    ) {
        for (key in section.getKeys(false)) {
            val fullPath = if (prefix.isEmpty()) key else "$prefix.$key"
            
            if (section.isConfigurationSection(key)) {
                // This is a nested section, recurse into it
                val subSection = section.getConfigurationSection(key)
                if (subSection != null) {
                    processPermissionCooldowns(subSection, fullPath, result)
                }
            } else {
                // This is a value, add it to the result
                val value = section.getInt(key)
                result[fullPath] = value
            }
        }
    }
    
    /**
     * Gets the main command for an alias
     * @param command the command or alias
     * @return the main command, or the original command if not an alias
     */
    fun getMainCommand(command: String): String {
        return commandAliases[command.lowercase()] ?: command.lowercase()
    }
    
    /**
     * Sets a cooldown for a player and command
     * @param player the player to set the cooldown for
     * @param command the command to set the cooldown for
     * @param durationSeconds the cooldown duration in seconds
     */
    fun setCooldown(player: Player, command: String, durationSeconds: Int) {
        if (durationSeconds <= 0) return
        
        val mainCommand = getMainCommand(command)
        val playerCooldowns = cooldowns.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
        val cooldownEnd = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(durationSeconds.toLong())
        
        playerCooldowns[mainCommand] = cooldownEnd
        plugin.debug("Set cooldown for ${player.name} on command $mainCommand: $durationSeconds seconds")
    }
    
    /**
     * Checks if a player has a cooldown for a command
     * @param player the player to check
     * @param command the command to check
     * @return true if the player has a cooldown, false otherwise
     */
    fun hasCooldown(player: Player, command: String): Boolean {
        // Check if player has bypass permission
        if (player.hasPermission("zcommandcooldown.bypass")) {
            return false
        }
        
        val mainCommand = getMainCommand(command)
        val playerCooldowns = cooldowns[player.uniqueId] ?: return false
        val cooldownEnd = playerCooldowns[mainCommand] ?: return false
        
        return cooldownEnd > System.currentTimeMillis()
    }
    
    /**
     * Gets the remaining cooldown time for a player and command
     * @param player the player to check
     * @param command the command to check
     * @return the remaining cooldown time in milliseconds, or 0 if no cooldown
     */
    fun getRemainingCooldown(player: Player, command: String): Long {
        val mainCommand = getMainCommand(command)
        val playerCooldowns = cooldowns[player.uniqueId] ?: return 0
        val cooldownEnd = playerCooldowns[mainCommand] ?: return 0
        
        val remaining = cooldownEnd - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }
    
    /**
     * Formats a cooldown time in a human-readable format
     * @param timeMillis the time in milliseconds
     * @return a formatted string like "1m 30s"
     */
    fun formatCooldownTime(timeMillis: Long): String {
        val seconds = timeMillis / 1000
        
        if (seconds < 60) {
            return "${seconds}s"
        }
        
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        if (minutes < 60) {
            return if (remainingSeconds > 0) {
                "${minutes}m ${remainingSeconds}s"
            } else {
                "${minutes}m"
            }
        }
        
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        
        return if (remainingMinutes > 0) {
            "${hours}h ${remainingMinutes}m"
        } else {
            "${hours}h"
        }
    }
    
    /**
     * Clears all cooldowns for a player
     * @param playerUUID the UUID of the player
     */
    fun clearCooldowns(playerUUID: UUID) {
        cooldowns.remove(playerUUID)
    }
    
    /**
     * Clears all cooldowns for all players
     */
    fun clearAllCooldowns() {
        cooldowns.clear()
    }
    
    /**
     * Clears expired cooldowns and removes cooldowns for players who have logged out
     * if clear-on-logout is enabled
     */
    fun cleanupCooldowns() {
        cleanupExpiredCooldowns()
        
        // If clear-on-logout is enabled, remove cooldowns for offline players
        if (plugin.configManager.clearOnLogout) {
            val playersToRemove = mutableListOf<UUID>()
            
            for (playerUUID in cooldowns.keys) {
                if (plugin.server.getPlayer(playerUUID) == null) {
                    playersToRemove.add(playerUUID)
                }
            }
            
            for (playerUUID in playersToRemove) {
                cooldowns.remove(playerUUID)
            }
            
            if (playersToRemove.isNotEmpty()) {
                plugin.debug("Cleared cooldowns for ${playersToRemove.size} offline players")
            }
        }
    }
}

