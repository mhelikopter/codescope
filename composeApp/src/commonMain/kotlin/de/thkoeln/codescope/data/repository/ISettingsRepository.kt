package de.thkoeln.codescope.data.repository

/**
 * Repository for managing application-wide settings and their persistence.
 */
interface ISettingsRepository {
    /**
     * Returns true if dark mode is enabled.
     */
    fun isDarkMode(): Boolean

    /**
     * Persists the dark mode setting.
     */
    fun setDarkMode(enabled: Boolean)
}
