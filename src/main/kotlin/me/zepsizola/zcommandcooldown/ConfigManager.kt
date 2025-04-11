package me.zepsizola.zcommandcooldown

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.logging.Level

class ConfigManager(private val plugin: ZCommandCooldown) {
    
    private val miniMessage = MiniMessage.miniMessage()
    
    // Configuration files
    private lateinit var configFile: File
    private lateinit var messagesFile: File
    private lateinit var cooldownsFile: File
    
    // Configuration objects
    var config: FileConfiguration = YamlConfiguration()
        private set
    
    var messages: FileConfiguration = YamlConfiguration()
        private set
    
    var cooldowns: FileConfiguration = YamlConfiguration()
        private set
    
    // Config values
    var useActionBar: Boolean = true
    var clearOnLogout: Boolean = false
    
    /**
     * Loads all configuration files
     */
    fun loadConfigs() {
        // Create plugin directory if it doesn't exist
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdir()
        }
        
        // Load main config
        loadMainConfig()
        
        // Load messages config
        loadMessagesConfig()
        
        // Load cooldowns config
        loadCooldownsConfig()
        
        // Parse config values
        parseConfigValues()
    }
    
    /**
     * Loads the main configuration file
     */
    private fun loadMainConfig() {
        configFile = File(plugin.dataFolder, "config.yml")
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
        
        config = YamlConfiguration.loadConfiguration(configFile)
    }
    
    /**
     * Loads the messages configuration file
     */
    private fun loadMessagesConfig() {
        messagesFile = File(plugin.dataFolder, "messages.yml")
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }
    
    /**
     * Loads the cooldowns configuration file
     */
    private fun loadCooldownsConfig() {
        cooldownsFile = File(plugin.dataFolder, "cooldowns.yml")
        
        if (!cooldownsFile.exists()) {
            plugin.saveResource("cooldowns.yml", false)
        }
        
        cooldowns = YamlConfiguration.loadConfiguration(cooldownsFile)
    }
    
    /**
     * Parses configuration values from the loaded config files
     */
    private fun parseConfigValues() {
        // Parse main config values
        useActionBar = config.getBoolean("use-action-bar", true)
        clearOnLogout = config.getBoolean("clear-on-logout", false)
    }
    
    /**
     * Gets a message from the messages.yml file and formats it with MiniMessage
     * @param path the path to the message in the messages.yml file
     * @param replacements a map of placeholders to replace in the message
     * @return the formatted message as a Component
     */
    fun getMessage(path: String, replacements: Map<String, String> = emptyMap()): Component {
        var message = messages.getString(path) ?: "&cMissing message: $path"
        
        // Replace prefix placeholder with actual prefix
        if (message.contains("<prefix>")) {
            val prefix = messages.getString("prefix") ?: "&8[&6ZCC&8]&r "
            message = message.replace("<prefix>", prefix)
        }
        
        // Replace custom placeholders
        replacements.forEach { (placeholder, value) ->
            message = message.replace("<$placeholder>", value)
        }
        
        return miniMessage.deserialize(message)
    }
    
    /**
     * Gets a raw message string from the messages.yml file
     * @param path the path to the message in the messages.yml file
     * @param replacements a map of placeholders to replace in the message
     * @return the raw message string
     */
    fun getRawMessage(path: String, replacements: Map<String, String> = emptyMap()): String {
        var message = messages.getString(path) ?: "&cMissing message: $path"
        
        // Replace prefix placeholder with actual prefix
        if (message.contains("<prefix>")) {
            val prefix = messages.getString("prefix") ?: "&8[&6ZCC&8]&r "
            message = message.replace("<prefix>", prefix)
        }
        
        // Replace custom placeholders
        replacements.forEach { (placeholder, value) ->
            message = message.replace("<$placeholder>", value)
        }
        
        return message
    }
    
    /**
     * Gets a cooldown configuration section for a command
     * @param command the command to get the cooldown for
     * @return the cooldown configuration section, or null if not found
     */
    fun getCooldownConfig(command: String): ConfigurationSection? {
        return cooldowns.getConfigurationSection("cooldowns.$command")
    }
    
    /**
     * Gets all configured commands with cooldowns
     * @return a set of command names that have cooldowns configured
     */
    fun getCooldownCommands(): Set<String> {
        val cooldownsSection = cooldowns.getConfigurationSection("cooldowns") ?: return emptySet()
        return cooldownsSection.getKeys(false)
    }
    
    /**
     * Saves the main configuration file
     */
    fun saveConfig() {
        try {
            config.save(configFile)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save config to $configFile", e)
        }
    }
    
    /**
     * Saves the messages configuration file
     */
    fun saveMessages() {
        try {
            messages.save(messagesFile)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save messages to $messagesFile", e)
        }
    }
    
    /**
     * Saves the cooldowns configuration file
     */
    fun saveCooldowns() {
        try {
            cooldowns.save(cooldownsFile)
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save cooldowns to $cooldownsFile", e)
        }
    }
}
