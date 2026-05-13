package de.thkoeln.codescope.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.components.DashboardStat
import de.thkoeln.codescope.components.DashboardStatistics
import de.thkoeln.codescope.components.DashboardTopBar
import de.thkoeln.codescope.components.DashboardWelcomeCard
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.logic.IProjektSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import de.thkoeln.codescope.util.formatTimestamp
import de.thkoeln.codescope.viewmodel.DashboardTeacherViewModel
import de.thkoeln.codescope.viewmodel.DashboardViewModel
import moe.tlaster.precompose.navigation.BackHandler
import org.koin.compose.koinInject

/**
 * Composable for the teacher dashboard screen on desktop.
 *
 * @param user The user object.
 * @param onNavigate Callback for navigation actions.
 * @param onNavigateToAnalysis Callback for when an analysis is clicked.
 */
@Composable
fun DesktopDashboardTeacherScreen(
    user: User,
    onNavigate: (String) -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    DashboardTeacherScreen(
        user = user,
        onNavigateToCourses = { onNavigate("courses") },
        onNavigateToAssessments = { onNavigate("grading") },
        onNavigateToSettings = { onNavigate("settings") },
        onNavigateToAnalysis = onNavigateToAnalysis,
        isMobile = false
    )
}

/**
 * Composable for the teacher dashboard screen.
 *
 * @param user The user object.
 * @param onNavigateToCourses Callback for when the courses button is clicked.
 * @param onNavigateToAssessments Callback for when the assessments button is clicked.
 * @param onNavigateToSettings Callback for when the settings is clicked.
 * @param onNavigateToAnalysis Callback for when an analysis is clicked.
 * @param isMobile True if the screen is for a mobile device, false otherwise.
 */
@Composable
fun DashboardTeacherScreen(
    user: User,
    onNavigateToCourses: () -> Unit,
    onNavigateToAssessments: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit,
    isMobile: Boolean
) {
    val studentKursSteuerung: IStudentKursSteuerung = koinInject()
    val kriterienkatalogSteuerung: IKriterienkatalogSteuerung = koinInject()
    val analyseSteuerung: IAnalyseSteuerung = koinInject()
    val projektSteuerung: IProjektSteuerung = koinInject()
    
    val viewModel: DashboardTeacherViewModel = viewModel {
        DashboardTeacherViewModel(studentKursSteuerung, kriterienkatalogSteuerung, analyseSteuerung, projektSteuerung)
    }

    LaunchedEffect(user.id) {
        viewModel.loadData(user.id)
    }

    BackHandler(enabled = true) {

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DashboardTopBar(userName = user.name, onNavigateToSettings = onNavigateToSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(if (isMobile) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 16.dp else 24.dp)
        ) {
            item {
                DashboardWelcomeCard(
                    title = "Willkommen zurück, ${user.name.split(" ").first()}!",
                    subtitle = "Verwalten Sie Ihre Kurse und Bewertungen effizient.",
                    buttonText = "Bewertungen ansehen",
                    buttonColor = MaterialTheme.colorScheme.tertiary,
                    onButtonClick = onNavigateToAssessments,
                    isMobile = isMobile
                )
            }
            item {
                DashboardStatistics(
                    stats = listOf(
                        DashboardStat("Aktive Kurse", viewModel.courses.size.toString(), MaterialTheme.colorScheme.secondary),
                        DashboardStat("Studierende", viewModel.totalStudents.toString(), MaterialTheme.colorScheme.primary),
                        DashboardStat("Kriterienkataloge", viewModel.catalogCount.toString(), MaterialTheme.colorScheme.tertiary)
                    ),
                    isMobile = isMobile
                )
            }

            // Eigene Analysen des Dozenten
            if (viewModel.ownAnalysisList.isNotEmpty()) {
                item {
                    Text(
                        text = "Ihre eigenen Analysen",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(viewModel.ownAnalysisList) { item ->
                    TeacherAnalysisHistoryCard(
                        analysis = item,
                        viewModel = viewModel,
                        onClick = { projectId ->
                            onNavigateToAnalysis(projectId, item.id)
                                  },
                        isMobile = isMobile
                    )
                }
            }

            // Analysen der Studierenden
            item {
                Text(
                    text = "Letzte Analysen Ihrer Studierenden",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (viewModel.studentAnalysisList.isEmpty()) {
                item {
                    Text(
                        text = "Noch keine Analysen vorhanden.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(viewModel.studentAnalysisList) { item ->
                    TeacherAnalysisHistoryCard(
                        analysis = item,
                        viewModel = viewModel,
                        onClick = { projectName ->
                            onNavigateToAnalysis(projectName, item.id)
                                  },
                        isMobile = isMobile
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherAnalysisHistoryCard(
    analysis: AnalysisResult,
    viewModel: DashboardTeacherViewModel,
    onClick: (String) -> Unit,
    isMobile: Boolean
) {
    val score = analysis.score ?: 0
    val scoreColor = when {
        score >= 80 -> MaterialTheme.colorScheme.secondary
        score >= 35 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val statusColor = when (analysis.status) {
        AnalysisStatus.PENDING -> MaterialTheme.colorScheme.outline
        AnalysisStatus.RUNNING -> MaterialTheme.colorScheme.primary
        AnalysisStatus.FINISHED -> MaterialTheme.colorScheme.secondary
        AnalysisStatus.FAILED -> MaterialTheme.colorScheme.error
        AnalysisStatus.REVIEWED -> MaterialTheme.colorScheme.tertiary
    }

    val catalogName = viewModel.catalogNames[analysis.criteriaCatalogId]?:analysis.criteriaCatalogId
    val projectName = viewModel.projectNames[analysis.projectId]?:analysis.projectId

    LaunchedEffect(Unit) {
        viewModel.getCatalogName(analysis.criteriaCatalogId)
        viewModel.getProjectName(analysis.projectId)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = { onClick(projectName) }),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(if (isMobile) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = projectName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = analysis.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "Katalog: $catalogName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Aktualisiert am ${formatTimestamp(analysis.updatedAt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                if (analysis.status == AnalysisStatus.FINISHED || analysis.status == AnalysisStatus.REVIEWED) {
                    Text(text = "$score%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                }
            }
        }
    }
}
