package de.thkoeln.codescope.data.repository

import android.content.Context
import android.content.SharedPreferences

class SettingsRepositoryImpl(context: Context) : ISettingsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("codescope_settings", Context.MODE_PRIVATE)

    override fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    override fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }
}
