package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.logic.IProjektSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import kotlinx.coroutines.launch

class DashboardTeacherViewModel(
    private val studentKursSteuerung: IStudentKursSteuerung,
    private val kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    private val analyseSteuerung: IAnalyseSteuerung,
    private val projektSteuerung: IProjektSteuerung
) : ViewModel() {

    var courses by mutableStateOf<List<Course>>(emptyList())
        private set

    var studentAnalysisList by mutableStateOf<List<AnalysisResult>>(emptyList())
        private set

    var ownAnalysisList by mutableStateOf<List<AnalysisResult>>(emptyList())
        private set

    var catalogCount by mutableIntStateOf(0)
        private set

    var isLoading by mutableStateOf(true)
        private set

    val totalStudents: Int
        get() = courses.sumOf { it.memberIds.size }

    val projectNames = mutableStateMapOf<String, String>()

    val catalogNames = mutableStateMapOf<String, String>()

    fun loadData(userId: String) {
        viewModelScope.launch {
            isLoading = true

            // Lade Kurse
            launch {
                studentKursSteuerung.getInstructorCourses(userId).onSuccess {
                    courses = it
                }
            }

            // Lade Kataloge
            launch {
                kriterienkatalogSteuerung.listCriteriaCatalogs(userId).onSuccess {
                    catalogCount = it.size
                }
            }

            // Lade eigene Analysen
            launch {
                analyseSteuerung.getAnalysisHistory(userId).onSuccess {
                    ownAnalysisList = it.sortedByDescending { analysis -> analysis.updatedAt }
                }
            }

            // Lade Analysen aus allen Kursen des Dozenten (Studierende)
            launch {
                analyseSteuerung.getAnalysesForAllInstructorCourses(userId).onSuccess {
                    studentAnalysisList = it.sortedByDescending { analysis -> analysis.updatedAt }
                }
            }

            isLoading = false
        }
    }

    fun getCatalogName(catalogId: String) {
        viewModelScope.launch {
            kriterienkatalogSteuerung.getCriteriaCatalog(catalogId).onSuccess {
                catalogNames[catalogId] = it.name
            }.onFailure {
                catalogNames[catalogId] = catalogId
            }
        }
    }

    fun getProjectName(projectId: String) {
        viewModelScope.launch {
            projektSteuerung.getProjektById(projectId).onSuccess {
                projectNames[projectId] = it.name
            }.onFailure {
                projectNames[projectId] = projectId
            }
        }
    }

}
