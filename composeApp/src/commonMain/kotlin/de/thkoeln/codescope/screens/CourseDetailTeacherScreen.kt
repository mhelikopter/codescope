package de.thkoeln.codescope.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.components.*
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.*
import de.thkoeln.codescope.viewmodel.CourseDetailTeacherViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.BackHandler
import org.koin.compose.koinInject

@Composable
fun DesktopCourseDetailTeacherScreen(
    dozentKursSteuerung: IDozentKursSteuerung,
    analyseSteuerung: IAnalyseSteuerung,
    courseId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onManageAssessments: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    CourseDetailTeacherContent(dozentKursSteuerung, analyseSteuerung, courseId, userId, onNavigateBack, onManageAssessments, onNavigateToAnalysis, isMobile = false)
}

@Composable
fun MobileCourseDetailTeacherScreen(
    dozentKursSteuerung: IDozentKursSteuerung,
    analyseSteuerung: IAnalyseSteuerung,
    courseId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onManageAssessments: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    CourseDetailTeacherContent(dozentKursSteuerung, analyseSteuerung, courseId, userId, onNavigateBack, onManageAssessments, onNavigateToAnalysis, isMobile = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDetailTeacherContent(
    dozentKursSteuerung: IDozentKursSteuerung,
    analyseSteuerung: IAnalyseSteuerung,
    courseId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onManageAssessments: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit,
    isMobile: Boolean
) {
    val adminSteuerung: IAdminSteuerung = koinInject()
    val kriterienSteuerung: IKriterienkatalogSteuerung = koinInject()
    val studentKursSteuerung: IStudentKursSteuerung = koinInject()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val viewModel: CourseDetailTeacherViewModel = viewModel {
        CourseDetailTeacherViewModel(dozentKursSteuerung, analyseSteuerung, adminSteuerung, kriterienSteuerung, studentKursSteuerung)
    }

    LaunchedEffect(courseId, userId) {
        viewModel.loadData(courseId, userId)
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (viewModel.showShareDialog) {
        CatalogShareDialog(
            catalogs = viewModel.catalogsToShare,
            onDismiss = { viewModel.closeShareDialog() },
            onShare = { catalogId ->
                viewModel.shareCatalog(catalogId, courseId, userId)
            }
        )
    }

    BackHandler(enabled = true) {
        onNavigateBack()
    }

    StandardScreenLayout(
        title = viewModel.course?.name ?: "Kurs-Details",
        isMobile = isMobile,
        showBackButton = true,
        onNavigateBack = onNavigateBack
    ) {
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Kriterienkataloge
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Kriterienkataloge", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { viewModel.openShareDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "Katalog hinzufügen", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (viewModel.sharedCatalogs.isEmpty()) {
                        Text("Keine Kataloge geteilt.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.sharedCatalogs.forEach { catalog ->
                                SharedCatalogCard(catalog = catalog) {
                                    IconButton(onClick = {
                                        viewModel.unshareCatalog(catalog.id, courseId, userId)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Entfernen", tint = MaterialTheme.colorScheme.error) 
                                    }
                                }
                            }
                        }
                    }
                }

                // Analysen
                Column {
                    Text(text = "Abgegebene Analysen (${viewModel.submittedAnalyses.size})", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (viewModel.submittedAnalyses.isEmpty()) {
                        Text("Noch keine Abgaben vorhanden.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.submittedAnalyses.forEach { analysis ->
                                val studentName = viewModel.students.find { it.id == analysis.userId }?.name ?: "Unbekannt"
                                AnalysisInfoCard(
                                    analysis = analysis, 
                                    subtitle = "Student: $studentName",
                                    onClick = { onNavigateToAnalysis(analysis.projectId, analysis.id) }
                                )
                            }
                        }
                    }
                }

                // Mitglieder
                Column {
                    Text(text = "Mitglieder", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Dozent", style = MaterialTheme.typography.labelLarge)
                        viewModel.lecturer?.let { UserCard(user = it, icon = Icons.Default.School) }

                        Text("Studierende (${viewModel.students.size})", style = MaterialTheme.typography.labelLarge)
                        if (viewModel.students.isEmpty()) {
                            Text("Keine Studierenden.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            viewModel.students.forEach { student ->
                                UserCard(user = student, icon = Icons.Default.Person) {
                                    IconButton(onClick = {
                                        viewModel.removeStudent(student.id, courseId, userId)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Entfernen", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onManageAssessments,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bewertungen verwalten")
                }
            }
        }
    }
}

@Composable
private fun CatalogShareDialog(
    catalogs: List<CriteriaCatalog>,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Katalog im Kurs teilen") },
        text = {
            if (catalogs.isEmpty()) {
                Text("Keine weiteren Kataloge zum Teilen verfügbar.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(catalogs) { catalog ->
                        ListItem(
                            headlineContent = { Text(catalog.name) },
                            modifier = Modifier.fillMaxWidth().clickable { onShare(catalog.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}
