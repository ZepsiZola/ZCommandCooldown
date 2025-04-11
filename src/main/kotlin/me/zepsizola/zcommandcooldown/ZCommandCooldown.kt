package me.zepsizola.zcommandcooldown

import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class ZCommandCooldown : JavaPlugin() {
    
    lateinit var configManager: ConfigManager
        private set
    
    lateinit var cooldownManager: CooldownManager
        private set
    
    private var debugMode = false
    
    override fun onEnable() {
        try {
            // Initialize config manager first
            configManager = ConfigManager(this)
            
            // Load configurations before initializing other components
            configManager.loadConfigs()
            
            // Now initialize cooldown manager after configs are loaded
            cooldownManager = CooldownManager(this)
            
            // Register command handler
            getCommand("zcommandcooldown")?.setExecutor(CommandHandler(this))
            
            // Register event listener
            server.pluginManager.registerEvents(CommandListener(this), this)
            
            logger.info("ZCommandCooldown has been enabled!")
        } catch (e: Exception) {
            logger.severe("Error enabling ZCommandCooldown: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onDisable() {
        try {
            // Save any data if needed, but only if cooldownManager was initialized
            if (::cooldownManager.isInitialized) {
                cooldownManager.saveCooldowns()
            }
            
            logger.info("ZCommandCooldown has been disabled!")
        } catch (e: Exception) {
            logger.severe("Error disabling ZCommandCooldown: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Reloads the plugin configuration
     * @return true if reload was successful, false otherwise
     */
    fun reload(): Boolean {
        return try {
            configManager.loadConfigs()
            cooldownManager.reloadCooldowns()
            true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error reloading plugin configuration", e)
            false
        }
    }
    
    /**
     * Toggles debug mode
     * @return the new debug mode state
     */
    fun toggleDebugMode(): Boolean {
        debugMode = !debugMode
        return debugMode
    }
    
    /**
     * Checks if debug mode is enabled
     * @return true if debug mode is enabled, false otherwise
     */
    fun isDebugMode(): Boolean {
        return debugMode
    }
    
    /**
     * Logs a debug message if debug mode is enabled
     * @param message the message to log
     */
    fun debug(message: String) {
        if (debugMode) {
            logger.info("[DEBUG] $message")
        }
    }
}
