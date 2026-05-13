package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import de.thkoeln.codescope.theme.ThemeSettings

class SettingsViewModel : ViewModel() {

    var notifications by mutableStateOf(true)
        private set

    val isDarkMode: Boolean
        get() = ThemeSettings.isDarkMode

    fun setDarkMode(enabled: Boolean) {
        ThemeSettings.isDarkMode = enabled
    }

    fun updateNotifications(enabled: Boolean) {
        notifications = enabled
    }
}
