package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.ILoginSteuerung
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginSteuerung: ILoginSteuerung
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun login(onSuccess: (User) -> Unit) {
        if (isLoading) return
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = loginSteuerung.registerUser()
            if (result.isSuccess) {
                isLoading = false
                result.getOrNull()?.let { onSuccess(it) }
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Login fehlgeschlagen"
                isLoading = false
            }
        }
    }

    fun resetState() {
        isLoading = false
        errorMessage = null
    }

    fun clearError() {
        errorMessage = null
    }
}
