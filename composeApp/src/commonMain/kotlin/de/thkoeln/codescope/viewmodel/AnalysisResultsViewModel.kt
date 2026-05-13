package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AnalysisResultsViewModel(
    private val analyseSteuerung: IAnalyseSteuerung
) : ViewModel() {

    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var deleteSuccess by mutableStateOf(false)
        private set

    fun loadAnalysisResult(analysisId: String) {
        if (analysisId.isBlank() || analysisId == "null") {
            errorMessage = "Ungültige Analyse-ID übergeben."
            isLoading = false
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            var attempts = 0
            val maxAttempts = 30 // Max 2.5 Minuten
            var isFinished = false

            while (!isFinished && attempts < maxAttempts) {
                attempts++
                analyseSteuerung.getAnalysisResult(analysisId)
                    .onSuccess { result ->
                        if (result.status == AnalysisStatus.FINISHED ||
                            result.status == AnalysisStatus.FAILED ||
                            result.status == AnalysisStatus.REVIEWED) {
                            analysisResult = result
                            isFinished = true
                        } else {
                            delay(5000)
                        }
                    }
                    .onFailure { error ->
                        val msg = error.message ?: ""
                        if (msg.contains("PERMISSION_DENIED") || msg.contains("UNAUTHENTICATED")) {
                            errorMessage = "Zugriffsfehler: $msg"
                            isFinished = true
                        } else {
                            println("Analyse noch nicht bereit (Versuch $attempts): $msg")
                            delay(5000)
                        }
                    }
            }

            if (!isFinished && errorMessage == null) {
                errorMessage = "Die Analyse dauert ungewöhnlich lange. Bitte versuchen Sie es später erneut."
            }

            isLoading = false
        }
    }

    fun deleteAnalysis(analysisId: String, userId: String) {
        viewModelScope.launch {
            analyseSteuerung.deleteAnalysisResult(analysisId, userId)
                .onSuccess {
                    deleteSuccess = true
                }
                .onFailure { error ->
                    errorMessage = "Löschen fehlgeschlagen: ${error.message}"
                }
        }
    }


    fun clearError() {
        errorMessage = null
    }
}
