package de.thkoeln.codescope.data.repository

import java.io.File
import java.util.Properties

class SettingsRepositoryImpl : ISettingsRepository {

    private val configFile = File(System.getProperty("user.home"), ".codescope_settings.properties")
    private val properties = Properties()

    init {
        if (configFile.exists()) {
            try {
                configFile.inputStream().use { properties.load(it) }
            } catch (e: Exception) {
                println("Settings: Failed to load properties: ${e.message}")
            }
        }
    }

    override fun isDarkMode(): Boolean = properties.getProperty("dark_mode", "false").toBoolean()

    override fun setDarkMode(enabled: Boolean) {
        properties.setProperty("dark_mode", enabled.toString())
        try {
            configFile.outputStream().use { properties.store(it, "CodeScope Settings") }
        } catch (e: Exception) {
            println("Settings: Failed to save properties: ${e.message}")
        }
    }
}
