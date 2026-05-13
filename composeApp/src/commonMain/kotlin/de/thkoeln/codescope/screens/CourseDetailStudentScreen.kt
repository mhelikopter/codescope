package de.thkoeln.codescope.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.components.*
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.*
import de.thkoeln.codescope.viewmodel.CourseDetailStudentViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.util.PlatformContext
import de.thkoeln.codescope.util.saveFileToDisk
import de.thkoeln.codescope.AppContext
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.BackHandler
import org.koin.compose.koinInject

@Composable
fun DesktopCourseDetailStudentScreen(
    studentKursSteuerung: IStudentKursSteuerung,
    analyseSteuerung: IAnalyseSteuerung,
    courseId: String,
    userId: String,
    onNavigateBack: () -> Unit
) {
    CourseDetailStudentContent(studentKursSteuerung, analyseSteuerung, courseId, userId, onNavigateBack, isMobile = false)
}

@Composable
fun MobileCourseDetailStudentScreen(
    studentKursSteuerung: IStudentKursSteuerung,
    analyseSteuerung: IAnalyseSteuerung,
    courseId: String,
    userId: String,
    onNavigateBack: () -> Unit
) {
    CourseDetailStudentContent(studentKursSteuerung, analyseSteuerung, courseId, userId, onNavigateBack, isMobile = true)
}

@Composable
private fun CourseDetailStudentContent(
    studentKursSteuerung: IStudentKursSteuerung,
    analyseSteuerung: IAnalyseSteuerung,
    courseId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    isMobile: Boolean
) {
    val dozentKursSteuerung: IDozentKursSteuerung = koinInject()
    val adminSteuerung: IAdminSteuerung = koinInject()
    val kriterienSteuerung: IKriterienkatalogSteuerung = koinInject()
    // Den AppContext (Android Context) via Koin holen
    val appContext: AppContext = koinInject()
    val platformContext = PlatformContext(appContext)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val viewModel: CourseDetailStudentViewModel = viewModel {
        CourseDetailStudentViewModel(studentKursSteuerung, dozentKursSteuerung, analyseSteuerung, adminSteuerung, kriterienSteuerung)
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

    BackHandler(enabled = true) {
        onNavigateBack()
    }

    StandardScreenLayout(
        title = viewModel.course?.name ?: "Kurs-Details",
        isMobile = isMobile,
        showBackButton = true,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState
    ) {
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Kataloge
                Column {
                    Text(text = "Kriterienkataloge", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (viewModel.sharedCatalogs.isEmpty()) {
                        Text("Keine Kataloge geteilt.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.sharedCatalogs.forEach { catalog ->
                                SharedCatalogCard(catalog = catalog) {
                                    val isAdded = viewModel.addedCatalogIds.contains(catalog.id)
                                    IconButton(
                                        onClick = {
                                            if (!isAdded) {
                                                viewModel.addCatalogToMyCollection(catalog, userId)
                                            }
                                        },
                                        enabled = !isAdded
                                    ) {
                                        Icon(
                                            imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = if (isAdded) "Hinzugefügt" else "Hinzufügen",
                                            tint = if (isAdded) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                        )
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            kriterienSteuerung.downloadCriteriaCatalog(catalog.id).onSuccess { bytes ->
                                                val sanitizedName = catalog.name.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                                                saveFileToDisk(platformContext, "$sanitizedName.txt", bytes)
                                            }.onFailure {
                                                snackbarHostState.showSnackbar("Export fehlgeschlagen")
                                            }
                                        }
                                    }) { Icon(Icons.Default.Download, contentDescription = "Download") }
                                }
                            }
                        }
                    }
                }

                // Analysen
                Column {
                    Text(text = "Meine Analysen", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (viewModel.analyses.isEmpty()) {
                        Text("Noch keine Analysen zu diesem Kurs vorhanden.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.analyses.forEach { analysis ->
                                AnalysisInfoCard(analysis = analysis, subtitle = "Status: ${analysis.status}")
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
                        viewModel.students.forEach { student ->
                            UserCard(user = student, icon = Icons.Default.Person)
                        }
                    }
                }

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.leaveCourse(courseId, userId, onNavigateBack)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kurs verlassen")
                }
            }
        }
    }
}
