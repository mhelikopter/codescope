package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import kotlinx.coroutines.launch

class CriteriaManagementViewModel(
    private val kriterienkatalogSteuerung: IKriterienkatalogSteuerung
) : ViewModel() {

    var catalogs by mutableStateOf<List<CriteriaCatalog>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var showFilePicker by mutableStateOf(false)
        private set

    var showCreateDialog by mutableStateOf(false)
        private set

    var editingCatalog by mutableStateOf<CriteriaCatalog?>(null)
        private set

    var editingCatalogContent by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val filteredCatalogs: List<CriteriaCatalog>
        get() = if (searchQuery.isBlank()) {
            catalogs
        } else {
            catalogs.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

    fun loadCatalogs(userId: String) {
        viewModelScope.launch {
            kriterienkatalogSteuerung.listCriteriaCatalogs(userId)
                .onSuccess { catalogs = it }
                .onFailure { errorMessage = it.message }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun openFilePicker() {
        showFilePicker = true
    }

    fun closeFilePicker() {
        showFilePicker = false
    }

    fun openCreateDialog() {
        showCreateDialog = true
    }

    fun closeCreateDialog() {
        showCreateDialog = false
    }

    fun openEditDialog(catalog: CriteriaCatalog) {
        editingCatalog = catalog
        viewModelScope.launch {
            kriterienkatalogSteuerung.downloadCriteriaCatalog(catalog.id)
                .onSuccess { bytes ->
                    editingCatalogContent = bytes.decodeToString()
                }
                .onFailure {
                    errorMessage = "Fehler beim Laden des Katalogs: ${it.message}"
                }
        }
    }

    fun closeEditDialog() {
        editingCatalog = null
        editingCatalogContent = null
    }

    fun uploadCatalogFromFile(userId: String, bytes: ByteArray, fileName: String) {
        showFilePicker = false

        // Validierung der Dateiendung
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension != "txt" && extension != "json") {
            errorMessage = "Ungültiges Format: Nur .txt und .json Dateien sind erlaubt."
            return
        }
        viewModelScope.launch {
            val catalogName = fileName.substringBeforeLast('.')
            kriterienkatalogSteuerung.uploadCriteriaCatalog(catalogName, userId, bytes, fileName)
                .onSuccess {
                    successMessage = "Katalog hochgeladen"
                    loadCatalogs(userId)
                }
                .onFailure {
                    errorMessage = "Upload fehlgeschlagen: ${it.message}"
                }
        }
    }

    fun createCatalog(userId: String, name: String, criteriaText: String) {
        showCreateDialog = false
        viewModelScope.launch {
            kriterienkatalogSteuerung.uploadCriteriaCatalog(name, userId, criteriaText.encodeToByteArray(), "$name.txt")
                .onSuccess {
                    successMessage = "Katalog '$name' erstellt"
                    loadCatalogs(userId)
                }
                .onFailure {
                    errorMessage = "Erstellen fehlgeschlagen: ${it.message}"
                }
        }
    }

    fun updateCatalog(userId: String, name: String, criteriaText: String) {
        val catalog = editingCatalog ?: return
        closeEditDialog()
        viewModelScope.launch {
            kriterienkatalogSteuerung.updateCriteriaCatalog(catalog.id, name, criteriaText.encodeToByteArray())
                .onSuccess {
                    successMessage = "Katalog aktualisiert"
                    loadCatalogs(userId)
                }
                .onFailure {
                    errorMessage = "Aktualisieren fehlgeschlagen: ${it.message}"
                }
        }
    }

    fun deleteCatalog(userId: String, catalogId: String) {
        viewModelScope.launch {
            kriterienkatalogSteuerung.deleteCriteriaCatalog(catalogId, userId)
                .onSuccess {
                    successMessage = "Katalog gelöscht"
                    loadCatalogs(userId)
                }
                .onFailure {
                    errorMessage = "Löschen fehlgeschlagen: ${it.message}"
                }
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
}
