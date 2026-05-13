package de.thkoeln.codescope.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.components.*
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import de.thkoeln.codescope.logic.IAdminSteuerung
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import de.thkoeln.codescope.util.formatTimestamp
import de.thkoeln.codescope.viewmodel.DashboardViewModel
import org.koin.compose.koinInject
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.logic.IProjektSteuerung
import moe.tlaster.precompose.navigation.BackHandler

@Composable
fun DesktopDashboardScreen(
    user: User,
    onNewProject: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    DashboardStudentContent(
        user = user,
        onNavigateToProjects = onNewProject,
        onNavigateToCourses = { onNavigate("courses") },
        onNavigateToAnalysis = onNavigateToAnalysis,
        onNavigateToSettings = { onNavigate("settings") },
        isMobile = false
    )
}

@Composable
fun MobileDashboardScreen(
    user: User,
    onNavigate: (String) -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    when (user.role) {
        UserRole.STUDENT -> {
            DashboardStudentContent(
                user = user,
                onNavigateToProjects = { onNavigate("projects") },
                onNavigateToCourses = { onNavigate("courses") },
                onNavigateToAnalysis = onNavigateToAnalysis,
                onNavigateToSettings = { onNavigate("settings") },
                isMobile = true
            )
        }
        UserRole.LECTURER -> {
            DashboardTeacherScreen(
                user = user,
                onNavigateToCourses = { onNavigate("courses") },
                onNavigateToAssessments = { onNavigate("grading") },
                onNavigateToSettings = { onNavigate("settings") },
                onNavigateToAnalysis = onNavigateToAnalysis,
                isMobile = true
            )
        }
        UserRole.ADMIN -> {
            val adminSteuerung: IAdminSteuerung = koinInject()
            MobileAdminPanelScreen(
                adminSteuerung = adminSteuerung,
                adminId = user.id,
                onNavigateBack = { onNavigate("dashboard") }
            )
        }
    }
}

@Composable
private fun DashboardStudentContent(
    user: User,
    onNavigateToProjects: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    isMobile: Boolean,
) {
    val analyseSteuerung: IAnalyseSteuerung = koinInject()
    val kriterienkatalogSteuerung: IKriterienkatalogSteuerung = koinInject()
    val studentKursSteuerung: IStudentKursSteuerung = koinInject()
    val projektSteuerung: IProjektSteuerung = koinInject()
    
    val viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(analyseSteuerung, kriterienkatalogSteuerung, studentKursSteuerung, projektSteuerung)
    }

    LaunchedEffect(user.id) {
        viewModel.loadDashboardData(user.id)
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
                    subtitle = "Verbessere deine Projekte mit KI-gestütztem Feedback.",
                    buttonText = "Neue Analyse starten",
                    buttonIcon = Icons.Default.Add,
                    onButtonClick = onNavigateToProjects,
                    isMobile = isMobile
                )
            }
            item {
                DashboardStatistics(
                    stats = listOf(
                        DashboardStat("Analysierte Projekte", viewModel.analysisCount.toString(), MaterialTheme.colorScheme.primary),
                        DashboardStat("Aktive Kurse", viewModel.courseCount.toString(), MaterialTheme.colorScheme.secondary),
                        DashboardStat("Kriterienkataloge", viewModel.catalogCount.toString(), MaterialTheme.colorScheme.tertiary)
                    ),
                    isMobile = isMobile
                )
            }
            item {
                Text(
                    text = "Letzte Analysen",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(viewModel.analysisList) { item ->
                AnalysisHistoryCard(
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

@Composable
private fun AnalysisHistoryCard(
    analysis: AnalysisResult,
    viewModel: DashboardViewModel,
    onClick: (projectName: String) -> Unit,
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
