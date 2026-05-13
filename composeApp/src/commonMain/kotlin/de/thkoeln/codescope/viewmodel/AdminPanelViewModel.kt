package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.IAdminSteuerung
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminPanelViewModel(
    private val adminSteuerung: IAdminSteuerung
) : ViewModel() {

    var users by mutableStateOf<List<User>>(emptyList())
        private set

    var courses by mutableStateOf<List<Course>>(emptyList())
        private set

    var selectedTab by mutableIntStateOf(0)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(true)
        private set

    val filteredUsers: List<User>
        get() = if (searchQuery.isBlank()) users
        else users.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }

    val filteredCourses: List<Course>
        get() = if (searchQuery.isBlank()) courses
        else courses.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, ignoreCase = true)
        }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            launch {
                adminSteuerung.getAllUsers()
                    .catch { /* Ignoriere Permission-Fehler nach Logout */ }
                    .collectLatest {
                        users = it
                    }
            }
            launch {
                adminSteuerung.getAllCourses()
                    .catch { /* Ignoriere Permission-Fehler nach Logout */ }
                    .collectLatest {
                        courses = it
                        isLoading = false
                    }
            }
        }
    }

    fun refreshData() {
        loadData()
    }

    fun selectTab(index: Int) {
        selectedTab = index
        searchQuery = ""
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun setUserActiveStatus(adminId: String, userId: String, isActive: Boolean) {
        viewModelScope.launch {
            adminSteuerung.setUserActiveStatus(adminId, userId, isActive)
            refreshData()
        }
    }

    fun changeUserRole(adminId: String, userId: String, newRole: de.thkoeln.codescope.domain.user.UserRole) {
        viewModelScope.launch {
            adminSteuerung.assignRole(adminId, userId, newRole)
            refreshData()
        }
    }

    fun deleteCourse(adminId: String, courseId: String) {
        viewModelScope.launch {
            adminSteuerung.deleteCourse(adminId, courseId)
            refreshData()
        }
    }
}
