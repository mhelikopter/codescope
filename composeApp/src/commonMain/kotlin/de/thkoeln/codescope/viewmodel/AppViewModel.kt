package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.Screen
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.ILoginSteuerung
import de.thkoeln.codescope.logic.INotificationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppViewModel(
    private val loginSteuerung: ILoginSteuerung,
    private val analyseSteuerung: IAnalyseSteuerung,
    private val notificationService: INotificationService
) : ViewModel() {

    var currentUser by mutableStateOf<User?>(null)
        private set

    var currentScreen by mutableStateOf<Screen>(Screen.Login)
        private set

    var isInitializing by mutableStateOf(true)
        private set

    private var observationJob: Job? = null
    private var lastKnownAnalyses = mutableMapOf<String, AnalysisStatus>()

    val isAdmin: Boolean get() = currentUser?.role == UserRole.ADMIN
    val isStudent: Boolean get() = currentUser?.role == UserRole.STUDENT
    val isLecturer: Boolean get() = currentUser?.role == UserRole.LECTURER
    val homeScreen: Screen get() = if (isAdmin) Screen.AdminPanel else Screen.Dashboard

    val currentRoute: String
        get() = when (currentScreen) {
            Screen.Dashboard -> "dashboard"
            Screen.ProjectUpload -> "projects"
            is Screen.AnalysisConfig -> "projects"
            is Screen.Results -> "projects"
            Screen.AdminPanel -> "admin"
            Screen.Courses -> "courses"
            Screen.Settings -> "settings"
            Screen.CriteriaManagement -> "criteria"
            is Screen.CourseDetails -> "courses"
            is Screen.AssessmentManagement -> "grading"
            else -> ""
        }

    init {
        viewModelScope.launch {
            val sessionUser = loginSteuerung.getCurrentUser()
            if (sessionUser != null) login(sessionUser)
            isInitializing = false
        }
    }

    private fun startObservingAnalyses(userId: String) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            analyseSteuerung.observeAnalysesForUser(userId)
                .catch { /* Ignoriere Permission-Fehler nach Logout */ }
                .collectLatest { analyses ->
                    processAnalyses(analyses)
                }
        }
    }

    private fun processAnalyses(analyses: List<AnalysisResult>) {
        analyses.forEach { analysis ->
            val previousStatus = lastKnownAnalyses[analysis.id]
            if (previousStatus != null && previousStatus != AnalysisStatus.REVIEWED && analysis.status == AnalysisStatus.REVIEWED) {
                notificationService.showNotification("Neue Bewertung verfügbar", "Dein Projekt '${analysis.projectId}' wurde bewertet.")
            }
            lastKnownAnalyses[analysis.id] = analysis.status
        }
    }

    fun navigateByRoute(route: String) {
        if (route == "logout") { logout(); return }
        val user = currentUser ?: return
        currentScreen = when (route) {
            "dashboard" -> if (user.role == UserRole.ADMIN) Screen.AdminPanel else Screen.Dashboard
            "upload", "projects" -> Screen.ProjectUpload
            "courses" -> Screen.Courses
            "settings" -> Screen.Settings
            "criteria" -> Screen.CriteriaManagement
            "grading" -> Screen.AssessmentManagement()
            "admin" -> if (user.role == UserRole.ADMIN) Screen.AdminPanel else return
            else -> if (route.startsWith("course/")) Screen.CourseDetails(route.substringAfter("course/"), user.id) else return
        }
    }

    fun navigateToHome() { currentScreen = homeScreen }
    fun navigateToProjectUpload() { currentScreen = Screen.ProjectUpload }
    fun navigateToCourses() { currentScreen = Screen.Courses }
    fun navigateToAnalysisConfig(project: Project) { currentUser?.let { currentScreen = Screen.AnalysisConfig(it.id, project) } }
    fun navigateToResults(projectName: String, analysisId: String, asLecturer: Boolean = false) { currentScreen = Screen.Results(projectName, analysisId, isLecturer = asLecturer) }
    fun navigateToCourseDetails(courseId: String) { currentUser?.let { currentScreen = Screen.CourseDetails(courseId, it.id) } }
    fun navigateToAssessmentManagement(courseId: String? = null) { currentScreen = Screen.AssessmentManagement(courseId) }
    fun navigateBack() {
        currentScreen = when (val screen = currentScreen) {
            is Screen.AnalysisConfig -> Screen.ProjectUpload
            is Screen.Results -> homeScreen
            is Screen.CourseDetails -> Screen.Courses
            is Screen.AssessmentManagement -> if (screen.courseId != null) Screen.CourseDetails(screen.courseId, currentUser?.id ?: "") else Screen.Courses
            else -> homeScreen
        }
    }

    fun login(user: User) {
        currentUser = user
        currentScreen = if (user.role == UserRole.ADMIN) Screen.AdminPanel else Screen.Dashboard
        if (user.role == UserRole.STUDENT) startObservingAnalyses(user.id)
    }

    fun logout() {
        // 1. Alle laufenden Hintergrundprozesse dieses ViewModels hart stoppen
        observationJob?.cancel()
        viewModelScope.coroutineContext.cancelChildren() 
        
        // 2. UI-Zustand sofort bereinigen
        currentScreen = Screen.Login
        currentUser = null
        lastKnownAnalyses.clear()

        // 3. Den eigentlichen Logout-Prozess in einem neuen Scope starten,
        // da wir den viewModelScope gerade bereinigt haben.
        viewModelScope.launch {
            try {
                loginSteuerung.logout()
            } catch (e: Exception) {
                // Stiller Fehler beim Logout ist verkraftbar, da App-State bereits Login zeigt
            }
        }
    }
}
