package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.ai.AiModel
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import kotlinx.coroutines.launch

class AnalysisConfigViewModel(
    private val analyseSteuerung: IAnalyseSteuerung,
    private val kriterienkatalogSteuerung: IKriterienkatalogSteuerung
) : ViewModel() {

    var criteriaCatalogs by mutableStateOf<List<CriteriaCatalog>>(emptyList())
        private set

    var selectedCatalog by mutableStateOf<CriteriaCatalog?>(null)
        private set

    var selectedModel by mutableStateOf(AiModel.GEMINI_25_PRO)
        private set

    var isStartingAnalysis by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var showNewCatalogDialog by mutableStateOf(false)
        private set

    var showFilePicker by mutableStateOf(false)
        private set

    var newCatalogName by mutableStateOf("")
        private set

    private var tempFileData: ByteArray? = null
    private var tempFileName: String? = null

    fun loadCatalogs(userId: String) {
        viewModelScope.launch {
            kriterienkatalogSteuerung.listCriteriaCatalogs(userId)
                .onSuccess { catalogs ->
                    criteriaCatalogs = catalogs
                    if (selectedCatalog == null && catalogs.isNotEmpty()) {
                        selectedCatalog = catalogs.first()
                    }
                }
                .onFailure {
                    errorMessage = it.message
                }
        }
    }

    fun selectCatalog(catalog: CriteriaCatalog) {
        selectedCatalog = catalog
    }

    fun selectModel(model: AiModel) {
        selectedModel = model
    }

    fun openFilePicker() {
        showFilePicker = true
    }

    fun closeFilePicker() {
        showFilePicker = false
    }

    fun onFileSelected(bytes: ByteArray, name: String) {
        showFilePicker = false

        // Validierung der Dateiendung
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension != "txt" && extension != "json") {
            errorMessage = "Ungültiges Format: Nur .txt und .json Dateien sind erlaubt."
            return
        }

        tempFileData = bytes
        tempFileName = name
        newCatalogName = name.substringBeforeLast('.')
        showNewCatalogDialog = true
    }

    fun updateNewCatalogName(name: String) {
        newCatalogName = name
    }

    fun closeNewCatalogDialog() {
        showNewCatalogDialog = false
        tempFileData = null
        tempFileName = null
    }

    fun createNewCatalog(userId: String) {
        val fileData = tempFileData ?: return
        val fileName = tempFileName ?: return
        if (newCatalogName.isBlank()) return

        viewModelScope.launch {
            kriterienkatalogSteuerung.uploadCriteriaCatalog(
                name = newCatalogName,
                ownerId = userId,
                fileData = fileData,
                fileName = fileName
            ).onSuccess {
                loadCatalogs(userId)
                criteriaCatalogs.find { it.name == newCatalogName }?.let {
                    selectedCatalog = it
                }
                showNewCatalogDialog = false
                successMessage = "Kriterienkatalog erfolgreich erstellt"
            }.onFailure {
                errorMessage = "Fehler beim Erstellen: ${it.message}"
            }
        }
    }

    fun startAnalysis(project: Project, userId: String, onSuccess: (String) -> Unit) {
        val catalog = selectedCatalog
        if (catalog == null) {
            errorMessage = "Bitte wählen Sie einen Kriterienkatalog aus"
            return
        }

        viewModelScope.launch {
            isStartingAnalysis = true
            errorMessage = null

            analyseSteuerung.startAnalysis(
                projectId = project.id,
                catalogId = catalog.id,
                model = selectedModel.id
            ).onSuccess { analysisId ->
                isStartingAnalysis = false
                onSuccess(analysisId)
            }.onFailure { error ->
                isStartingAnalysis = false
                errorMessage = "Fehler beim Starten der Analyse: ${error.message}"
            }
        }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
