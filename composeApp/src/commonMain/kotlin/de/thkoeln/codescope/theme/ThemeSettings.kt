package de.thkoeln.codescope.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.thkoeln.codescope.data.repository.ISettingsRepository

object ThemeSettings {
    private var repository: ISettingsRepository? = null

    private var _isDarkMode = mutableStateOf(false)
    
    var isDarkMode: Boolean
        get() = _isDarkMode.value
        set(value) {
            _isDarkMode.value = value
            repository?.setDarkMode(value)
        }

    fun initialize(repo: ISettingsRepository) {
        repository = repo
        _isDarkMode.value = repo.isDarkMode()
    }
}
