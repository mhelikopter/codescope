package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.IAdminSteuerung
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AssessmentManagementViewModel(
    private val analyseSteuerung: IAnalyseSteuerung,
    private val kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    private val adminSteuerung: IAdminSteuerung
) : ViewModel() {

    var assessments by mutableStateOf<List<AnalysisResult>>(emptyList())
        private set

    var availableCatalogs by mutableStateOf<List<CriteriaCatalog>>(emptyList())
        private set

    var allUsers by mutableStateOf<List<User>>(emptyList())
        private set

    var selectedTab by mutableIntStateOf(0)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(true)
        private set

    var selectedIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var showBatchDialog by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val filteredAssessments: List<AnalysisResult>
        get() = assessments.filter {
            val statusMatch = when (selectedTab) {
                0 -> it.status == AnalysisStatus.FINISHED
                1 -> it.status == AnalysisStatus.REVIEWED
                else -> true
            }
            val searchMatch = it.projectId.contains(searchQuery, ignoreCase = true) ||
                    it.userId.contains(searchQuery, ignoreCase = true)
            statusMatch && searchMatch
        }

    fun loadData(userId: String, courseId: String?) {
        viewModelScope.launch {
            isLoading = true

            val result = if (courseId != null) {
                analyseSteuerung.getAnalysesForCourse(courseId)
            } else {
                analyseSteuerung.getAnalysesForAllInstructorCourses(userId)
            }

            result.onSuccess {
                assessments = it
            }

            kriterienkatalogSteuerung.listCriteriaCatalogs(userId).onSuccess {
                availableCatalogs = it
            }

            adminSteuerung.getAllUsers().collectLatest {
                allUsers = it
                isLoading = false
            }
        }
    }

    fun refreshData(userId: String, courseId: String?) {
        loadData(userId, courseId)
    }

    fun selectTab(index: Int) {
        selectedTab = index
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun toggleSelection(analysisId: String) {
        selectedIds = if (selectedIds.contains(analysisId)) {
            selectedIds - analysisId
        } else {
            selectedIds + analysisId
        }
    }

    fun selectAll() {
        selectedIds = filteredAssessments.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedIds = emptySet()
    }

    fun openBatchDialog() {
        showBatchDialog = true
    }

    fun closeBatchDialog() {
        showBatchDialog = false
    }

    fun startBatchAnalysis(
        catalogId: String,
        model: String,
        notifyStudents: Boolean,
        courseId: String?,
        userId: String
    ) {
        viewModelScope.launch {
            val projectIds = assessments.filter { selectedIds.contains(it.id) }.map { it.projectId }
            analyseSteuerung.startBatchAnalysis(
                projectIds = projectIds,
                catalogId = catalogId,
                model = model,
                resetFeedback = false,
                notifyStudents = notifyStudents,
                courseId = courseId
            ).onSuccess {
                closeBatchDialog()
                clearSelection()
                refreshData(userId, courseId)
                successMessage = "Batch-Analyse gestartet"
            }.onFailure {
                errorMessage = "Batch-Analyse fehlgeschlagen: ${it.message}"
            }
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
}
