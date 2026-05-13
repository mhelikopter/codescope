package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CourseDetailStudentViewModel(
    private val studentKursSteuerung: IStudentKursSteuerung,
    private val dozentKursSteuerung: IDozentKursSteuerung,
    private val analyseSteuerung: IAnalyseSteuerung,
    private val adminSteuerung: IAdminSteuerung,
    private val kriterienSteuerung: IKriterienkatalogSteuerung
) : ViewModel() {

    var course by mutableStateOf<Course?>(null)
        private set

    var sharedCatalogs by mutableStateOf<List<CriteriaCatalog>>(emptyList())
        private set

    var lecturer by mutableStateOf<User?>(null)
        private set

    var students by mutableStateOf<List<User>>(emptyList())
        private set

    var analyses by mutableStateOf<List<AnalysisResult>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var addedCatalogIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadData(courseId: String, userId: String) {
        viewModelScope.launch {
            isLoading = true

            // 1. Kurs laden
            dozentKursSteuerung.getCourse(courseId).onSuccess { loadedCourse ->
                course = loadedCourse

                // 2. Kataloge parallel laden
                launch {
                    kriterienSteuerung.listCriteriaCatalogs(loadedCourse.lecturerId).onSuccess { catalogs ->
                        sharedCatalogs = catalogs.filter { loadedCourse.sharedCatalogIds.contains(it.id) }
                    }
                }

                // 3. Benutzer (Echtzeit-Flow) in eigener Coroutine starten, damit loadData nicht hängen bleibt
                launch {
                    adminSteuerung.getAllUsers()
                        .catch { /* Logout Schutz */ }
                        .collectLatest { allUsers ->
                            lecturer = allUsers.find { it.id == loadedCourse.lecturerId }
                            students = allUsers.filter { loadedCourse.memberIds.contains(it.id) }
                        }
                }
            }

            // 4. Analysen laden
            analyseSteuerung.getAnalysisHistory(userId).onSuccess { allAnalyses ->
                analyses = allAnalyses.filter { it.courseId == courseId }
            }
            
            isLoading = false
        }
    }

    fun addCatalogToMyCollection(catalog: CriteriaCatalog, userId: String) {
        viewModelScope.launch {
            kriterienSteuerung.downloadCriteriaCatalog(catalog.id).onSuccess { bytes ->
                kriterienSteuerung.uploadCriteriaCatalog(
                    name = catalog.name,
                    ownerId = userId,
                    fileData = bytes,
                    fileName = "${catalog.name}.txt"
                ).onSuccess {
                    addedCatalogIds = addedCatalogIds + catalog.id
                    successMessage = "Katalog hinzugefügt"
                }.onFailure {
                    errorMessage = "Fehler beim Hinzufügen"
                }
            }
        }
    }

    fun downloadCatalog(catalogId: String, onUrl: (String) -> Unit) {
        viewModelScope.launch {
            kriterienSteuerung.exportCriteriaCatalog(catalogId).onSuccess { url ->
                onUrl(url)
            }.onFailure {
                errorMessage = "Download fehlgeschlagen"
            }
        }
    }

    fun leaveCourse(courseId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            studentKursSteuerung.leaveCourse(courseId, userId).onSuccess {
                onSuccess()
            }.onFailure {
                errorMessage = "Verlassen fehlgeschlagen: ${it.message}"
            }
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
}
