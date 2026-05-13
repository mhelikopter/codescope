package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.logic.IDozentKursSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CourseStudentViewModel(
    private val studentKursSteuerung: IStudentKursSteuerung,
    private val dozentKursSteuerung: IDozentKursSteuerung
) : ViewModel() {

    var allCourses by mutableStateOf<List<Course>>(emptyList())
        private set

    var studentCourses by mutableStateOf<List<Course>>(emptyList())
        private set

    var selectedTab by mutableIntStateOf(0)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isJoining by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val myCourses: List<Course>
        get() = studentCourses

    val availableCourses: List<Course>
        get() = allCourses.filter { course ->
            !studentCourses.any { it.id == course.id }
        }

    val displayCourses: List<Course>
        get() {
            val courses = when (selectedTab) {
                0 -> myCourses
                1 -> availableCourses
                else -> emptyList()
            }
            return if (searchQuery.isBlank()) courses
            else courses.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.id.contains(searchQuery, ignoreCase = true)
            }
        }

    fun loadCourses(userId: String) {
        viewModelScope.launch {
            launch {
                dozentKursSteuerung.getAllCourses()
                    .catch { /* Ignoriere Permission-Fehler nach Logout */ }
                    .collectLatest {
                        allCourses = it
                    }
            }
            launch {
                studentKursSteuerung.getStudentCourses(userId)
                    .catch { /* Ignoriere Permission-Fehler nach Logout */ }
                    .collectLatest {
                        studentCourses = it
                    }
            }
        }
    }

    fun selectTab(index: Int) {
        selectedTab = index
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun joinCourse(courseId: String, userId: String) {
        viewModelScope.launch {
            isJoining = true
            studentKursSteuerung.joinCourse(courseId, userId)
                .onSuccess {
                    successMessage = "Erfolgreich beigetreten!"
                    selectedTab = 0
                    loadCourses(userId)
                }
                .onFailure {
                    errorMessage = "Fehler beim Beitritt."
                }
            isJoining = false
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
}
