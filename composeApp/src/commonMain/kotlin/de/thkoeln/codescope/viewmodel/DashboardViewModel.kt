package de.thkoeln.codescope.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.logic.IProjektSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class DashboardViewModel(
    private val analyseSteuerung: IAnalyseSteuerung,
    private val kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    private val studentKursSteuerung: IStudentKursSteuerung,
    private val projectSteuerung: IProjektSteuerung
) : ViewModel() {

    var analysisList by mutableStateOf<List<AnalysisResult>>(emptyList())
        private set

    var analysisCount by mutableIntStateOf(0)
        private set

    var catalogCount by mutableIntStateOf(0)
        private set

    var courseCount by mutableIntStateOf(0)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val projectNames = mutableStateMapOf<String, String>()

    val catalogNames = mutableStateMapOf<String, String>()

    fun loadDashboardData(userId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            // Lade Analysen
            launch {
                analyseSteuerung.getAnalysisHistory(userId).onSuccess { allAnalyses ->
                    analysisCount = allAnalyses.size
                    analysisList = allAnalyses.sortedByDescending { it.updatedAt }
                }.onFailure {
                    errorMessage = it.message
                }
            }

            // Lade Kataloge
            launch {
                kriterienkatalogSteuerung.listCriteriaCatalogs(userId).onSuccess {
                    catalogCount = it.size
                }
            }

            // Lade Kurse (Echtzeit-Flow)
            launch {
                studentKursSteuerung.getStudentCourses(userId)
                    .catch { /* Ignoriere Permission-Fehler nach Logout */ }
                    .collect {
                        courseCount = it.size
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
            projectSteuerung.getProjektById(projectId).onSuccess {
                projectNames[projectId] = it.name
            }.onFailure {
                projectNames[projectId] = projectId
            }
        }
    }
}
