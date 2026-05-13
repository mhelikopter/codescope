package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.googleAuth.DriveFile
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.logic.IProjektSteuerung
import de.thkoeln.codescope.data.client.GoogleAuth
import de.thkoeln.codescope.domain.project.ProjectStatus
import kotlinx.coroutines.launch

class ProjectUploadViewModel(
    private val projektSteuerung: IProjektSteuerung,
    private val googleAuth: GoogleAuth
) : ViewModel() {

    var projects by mutableStateOf<List<Project>>(emptyList())

    var selectedSource by mutableStateOf<String?>(null)
        private set

    var projectName by mutableStateOf("")
        private set

    var projectPath by mutableStateOf("")
        private set

    var isUploading by mutableStateOf(false)
        private set

    var showFilePicker by mutableStateOf(false)
        private set

    var showDeleteDialog by mutableStateOf(false)
        private set

    var tempFileData by mutableStateOf<ByteArray?>(null)
        private set

    var driveAccessToken by mutableStateOf<String?>(null)
        private set

    var driveFiles by mutableStateOf<List<DriveFile>>(emptyList())
        private set

    var isLoadingDriveFiles by mutableStateOf(false)
        private set

    var showDriveFileDialog by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var itemToDelete by mutableStateOf<Project?>(null)
        private set

    fun selectSource(source: String) {
        selectedSource = source
        projectPath = ""
        tempFileData = null
    }

    fun updateProjectName(name: String) {
        projectName = name
    }

    fun updateProjectPath(path: String) {
        projectPath = path
    }

    fun openFilePicker() {
        showFilePicker = true
    }

    fun closeFilePicker() {
        showFilePicker = false
    }

    fun onFileSelected(bytes: ByteArray, name: String) {
        showFilePicker = false
        tempFileData = bytes
        projectPath = name
        if (projectName.isEmpty()) projectName = name.substringBeforeLast(".")
    }

    fun loginToGoogleDrive() {
        viewModelScope.launch {
            try {
                val credential = googleAuth.getGoogleIdToken()
                driveAccessToken = credential.accessToken

                if (driveAccessToken != null) {
                    successMessage = "Erfolgreich bei Google angemeldet"
                    loadDriveFiles()
                }
            } catch (e: Exception) {
                errorMessage = "Login fehlgeschlagen: ${e.message}"
            }
        }
    }

    private fun loadDriveFiles() {
        val token = driveAccessToken ?: return
        viewModelScope.launch {
            isLoadingDriveFiles = true
            projektSteuerung.getDriveFiles(token)
                .onSuccess {
                    driveFiles = it
                    showDriveFileDialog = true
                }
                .onFailure {
                    errorMessage = "Fehler beim Laden der Drive-Dateien"
                }
            isLoadingDriveFiles = false
        }
    }

    fun closeDriveFileDialog() {
        showDriveFileDialog = false
    }

    fun selectDriveFile(file: DriveFile) {
        projectPath = file.id
        if (projectName.isEmpty()) projectName = file.name.substringBeforeLast(".")
        showDriveFileDialog = false
    }

    fun uploadProject(userId: String, onUploadComplete: (Project) -> Unit) {
        if (projectName.isBlank()) {
            errorMessage = "Bitte geben Sie einen Projektnamen ein"
            return
        }

        viewModelScope.launch {
            isUploading = true
            errorMessage = null

            val result = when (selectedSource) {
                "local" -> {
                    val data = tempFileData
                    if (data != null) {
                        projektSteuerung.uploadProject(
                            name = projectName,
                            ownerId = userId,
                            zipData = data
                        )
                    } else {
                        Result.failure(Exception("Keine Datei ausgewählt"))
                    }
                }
                "drive" -> {
                    val token = driveAccessToken
                    if (token != null && projectPath.isNotBlank()) {
                        projektSteuerung.uploadFromDrive(
                            name = projectName,
                            ownerId = userId,
                            driveFileId = projectPath,
                            accessToken = token
                        )
                    } else {
                        Result.failure(Exception("Keine Datei ausgewählt"))
                    }
                }
                else -> Result.failure(Exception("Bitte wählen Sie eine Upload-Quelle"))
            }

            result
                .onSuccess {
                    isUploading = false
                    onUploadComplete(result.getOrNull()!!)
                }
                .onFailure { error ->
                    isUploading = false
                    errorMessage = "Upload fehlgeschlagen: ${error.message}"
                }
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }

    fun loadProjects(userId: String) {
        viewModelScope.launch {
            val result = projektSteuerung.getProjectsForUser(userId)

            result
                .onFailure {
                    errorMessage = "Fehler beim laden von Projekte"
                }
                .onSuccess {
                    successMessage = "Nutzer Projekte geladen"
                    projects = result.getOrNull()?.toList()?:emptyList()
                }
        }
    }

    private fun deleteProject(project: Project) {
        viewModelScope.launch {
            val result = projektSteuerung.deleteProject(project)

            result
                .onFailure {
                    errorMessage = "Fehler beim löschen eines Projekt"
                }
                .onSuccess {
                    successMessage = "Projekt erfolgreich gelöscht"
                }
        }
    }

    fun confirmDeletion(userId: String) {
        itemToDelete?.let { item ->
            deleteProject(itemToDelete!!)
        }
        dismissDeleteDialog()
        loadProjects(userId)
    }

    fun dismissDeleteDialog(){
        itemToDelete = null
        showDeleteDialog = false
    }

    fun onDeleteProject(project: Project) {
        showDeleteDialog = true
        itemToDelete = project
    }

    fun updateProjectStatus(project: Project) {
        val updatedProject = project.copy(status = ProjectStatus.UPLOADED)
        viewModelScope.launch {
            projektSteuerung.updateProject(updatedProject)
        }
    }

}
