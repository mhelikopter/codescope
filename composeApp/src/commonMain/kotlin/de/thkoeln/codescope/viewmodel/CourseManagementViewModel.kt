package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.logic.IDozentKursSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import kotlinx.coroutines.launch

class CourseManagementViewModel(
    private val dozentKursSteuerung: IDozentKursSteuerung,
    private val studentKursSteuerung: IStudentKursSteuerung
) : ViewModel() {

    var courses by mutableStateOf<List<Course>>(emptyList())
        private set

    var showCreateDialog by mutableStateOf(false)
        private set

    var newCourseName by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadCourses(userId: String) {
        viewModelScope.launch {
            isLoading = true
            studentKursSteuerung.getInstructorCourses(userId)
                .onSuccess { courses = it }
                .onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun openCreateDialog() {
        showCreateDialog = true
    }

    fun closeCreateDialog() {
        showCreateDialog = false
        newCourseName = ""
    }

    fun updateNewCourseName(name: String) {
        newCourseName = name
    }

    fun createCourse(userId: String) {
        if (newCourseName.isBlank()) return

        viewModelScope.launch {
            dozentKursSteuerung.createCourse(userId, newCourseName)
            closeCreateDialog()
            loadCourses(userId)
            successMessage = "Kurs '$newCourseName' erstellt"
        }
    }

    fun deleteCourse(userId: String, courseId: String) {
        viewModelScope.launch {
            dozentKursSteuerung.deleteCourse(courseId)
            loadCourses(userId)
            successMessage = "Kurs gelöscht"
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
}
