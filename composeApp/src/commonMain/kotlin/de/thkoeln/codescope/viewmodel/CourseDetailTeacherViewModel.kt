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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CourseDetailTeacherViewModel(
    private val dozentKursSteuerung: IDozentKursSteuerung,
    private val analyseSteuerung: IAnalyseSteuerung,
    private val adminSteuerung: IAdminSteuerung,
    private val kriterienSteuerung: IKriterienkatalogSteuerung,
    private val studentKursSteuerung: IStudentKursSteuerung
) : ViewModel() {

    var course by mutableStateOf<Course?>(null)
        private set

    var sharedCatalogs by mutableStateOf<List<CriteriaCatalog>>(emptyList())
        private set

    var myCatalogs by mutableStateOf<List<CriteriaCatalog>>(emptyList())
        private set

    var lecturer by mutableStateOf<User?>(null)
        private set

    var students by mutableStateOf<List<User>>(emptyList())
        private set

    var submittedAnalyses by mutableStateOf<List<AnalysisResult>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var showShareDialog by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val catalogsToShare: List<CriteriaCatalog>
        get() = myCatalogs.filter { catalog ->
            course?.sharedCatalogIds?.contains(catalog.id) != true
        }

    fun loadData(courseId: String, userId: String) {
        viewModelScope.launch {
            isLoading = true

            dozentKursSteuerung.getCourse(courseId).onSuccess { loadedCourse ->
                course = loadedCourse

                // Lade eingereichte Analysen
                analyseSteuerung.getAnalysesForCourse(courseId).onSuccess {
                    submittedAnalyses = it
                }

                // Lade geteilte Kataloge
                kriterienSteuerung.listCriteriaCatalogs(loadedCourse.lecturerId).onSuccess { catalogs ->
                    sharedCatalogs = catalogs.filter { loadedCourse.sharedCatalogIds.contains(it.id) }
                }

                // Lade eigene Kataloge
                kriterienSteuerung.listCriteriaCatalogs(userId).onSuccess { catalogs ->
                    myCatalogs = catalogs
                }

                // Lade Benutzer
                adminSteuerung.getAllUsers().collectLatest { allUsers ->
                    lecturer = allUsers.find { it.id == loadedCourse.lecturerId }
                    students = allUsers.filter { loadedCourse.memberIds.contains(it.id) }
                    isLoading = false
                }
            }
        }
    }

    fun refreshData(courseId: String, userId: String) {
        loadData(courseId, userId)
    }

    fun openShareDialog() {
        showShareDialog = true
    }

    fun closeShareDialog() {
        showShareDialog = false
    }

    fun shareCatalog(catalogId: String, courseId: String, userId: String) {
        viewModelScope.launch {
            kriterienSteuerung.shareCatalogWithCourse(catalogId, courseId, userId).onSuccess {
                closeShareDialog()
                refreshData(courseId, userId)
                successMessage = "Katalog geteilt"
            }.onFailure {
                errorMessage = "Teilen fehlgeschlagen: ${it.message}"
            }
        }
    }

    fun unshareCatalog(catalogId: String, courseId: String, userId: String) {
        viewModelScope.launch {
            kriterienSteuerung.unshareCatalogFromCourse(catalogId, courseId, userId).onSuccess {
                refreshData(courseId, userId)
                successMessage = "Katalog entfernt"
            }.onFailure {
                errorMessage = "Entfernen fehlgeschlagen: ${it.message}"
            }
        }
    }

    fun removeStudent(studentId: String, courseId: String, userId: String) {
        viewModelScope.launch {
            studentKursSteuerung.leaveCourse(courseId, studentId).onSuccess {
                refreshData(courseId, userId)
                successMessage = "Student entfernt"
            }.onFailure {
                errorMessage = "Entfernen fehlgeschlagen: ${it.message}"
            }
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
}
