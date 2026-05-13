package de.thkoeln.codescope.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.util.formatTimestamp
import kotlinx.coroutines.launch
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import org.koin.compose.koinInject
import androidx.compose.foundation.clickable
import de.thkoeln.codescope.util.shareAnalysisResult
import de.thkoeln.codescope.viewmodel.AnalysisResultsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.AppContext
import de.thkoeln.codescope.util.PlatformContext
import de.thkoeln.codescope.util.saveFileToDisk
import moe.tlaster.precompose.navigation.BackHandler

@Composable
fun DesktopResultsScreen(
    projectName: String,
    analysisId: String,
    analyseSteuerung: IAnalyseSteuerung,
    userId: String,
    isLecturer: Boolean = false,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    val viewModel: AnalysisResultsViewModel = viewModel {
        AnalysisResultsViewModel(analyseSteuerung)
    }

    LaunchedEffect(analysisId) {
        viewModel.loadAnalysisResult(analysisId)
    }

    LaunchedEffect(viewModel.deleteSuccess) {
        if (viewModel.deleteSuccess) {
            onBack()
        }
    }

    AnalysisResultsScreenContent(
        projectName = projectName,
        analysisResult = viewModel.analysisResult,
        errorMessage = viewModel.errorMessage,
        onBack = onBack,
        onExport = onExport,
        onDelete = { viewModel.deleteAnalysis(analysisId, userId) },
        isMobile = false,
        analysisId = analysisId,
        userId = userId,
        analyseSteuerung = analyseSteuerung,
        isLecturer = isLecturer
    )
}

@Composable
fun MobileResultsScreen(
    projectName: String,
    analysisId: String,
    analyseSteuerung: IAnalyseSteuerung,
    userId: String,
    isLecturer: Boolean = false,
    onBack: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val viewModel: AnalysisResultsViewModel = viewModel {
        AnalysisResultsViewModel(analyseSteuerung)
    }

    LaunchedEffect(analysisId) {
        viewModel.loadAnalysisResult(analysisId)
    }

    LaunchedEffect(viewModel.deleteSuccess) {
        if (viewModel.deleteSuccess) {
            onBack()
        }
    }

    AnalysisResultsScreenContent(
        projectName = projectName,
        analysisResult = viewModel.analysisResult,
        errorMessage = viewModel.errorMessage,
        onBack = onBack,
        onShare = onShare ?: {
            val res = viewModel.analysisResult
            if (res != null) {
                shareAnalysisResult(projectName, res)
            }
        },
        onDelete = { viewModel.deleteAnalysis(analysisId, userId) },
        isMobile = true,
        analysisId = analysisId,
        userId = userId,
        analyseSteuerung = analyseSteuerung,
        isLecturer = isLecturer
    )
}

@Composable
private fun AnalysisResultsScreenContent(
    projectName: String,
    analysisResult: AnalysisResult?,
    errorMessage: String?,
    onBack: () -> Unit,
    onExport: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    isMobile: Boolean,
    analysisId: String,
    userId: String,
    analyseSteuerung: IAnalyseSteuerung,
    isLecturer: Boolean
) {
    val studentKursSteuerung: IStudentKursSteuerung = koinInject()
    val kriterienkatalogSteuerung: IKriterienkatalogSteuerung = koinInject()
    val appContext: AppContext = koinInject()
    val platformContext = PlatformContext(appContext)
    
    val scope = rememberCoroutineScope()
    var showSubmitDialog by remember { mutableStateOf(false) }
    var userCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var currentAnalysis by remember { mutableStateOf(analysisResult) }

    // Handle System Back Button
    BackHandler(enabled = true) {
        onBack()
    }

    val catalogName by produceState(initialValue = "", analysisResult) {
        analysisResult?.let {
            kriterienkatalogSteuerung.getCriteriaCatalog(it.criteriaCatalogId)
                .onSuccess { catalog ->
                    value = catalog.name
                }
        }
    }

    LaunchedEffect(analysisResult) {
        currentAnalysis = analysisResult
    }

    LaunchedEffect(userId) {
        if (!isLecturer) {
            studentKursSteuerung.getStudentCourses(userId).collect { userCourses = it }
        }
    }

    val courseName by remember(currentAnalysis, userCourses) {
        derivedStateOf {
            currentAnalysis?.courseId?.let { id ->
                userCourses.firstOrNull { it.id == id }?.name ?: ""
            } ?: ""
        }
    }

    if (showSubmitDialog) {
        SubmitToCourseDialog(
            courses = userCourses,
            onDismiss = { showSubmitDialog = false },
            onConfirm = { courseId ->
                scope.launch {
                    studentKursSteuerung.submitAnalysisToCourse(analysisId, userId, courseId)
                        .onSuccess {
                            showSubmitDialog = false
                            analyseSteuerung.getAnalysisResult(analysisId).onSuccess { currentAnalysis = it }
                        }
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(if (isMobile) 16.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 16.dp else 24.dp)
        ) {
            item {
                val finalOnShare: (() -> Unit)? = onShare ?: currentAnalysis?.let { res ->
                    { shareAnalysisResult(projectName, res) }
                }
                
                ScreenHeader(
                    onBack = onBack,
                    onExport = if (isMobile) null else {
                        {
                            currentAnalysis?.let { res ->
                                val text = generateResultText(projectName, res, catalogName, courseName)
                                saveFileToDisk(platformContext, "Analyse_${projectName.replace(" ", "_")}.txt", text.encodeToByteArray())
                            }
                        }
                    },
                    onDelete = onDelete,
                    onShare = finalOnShare,
                    isMobile = isMobile,
                    showSubmit = !isLecturer && currentAnalysis?.courseId == null && currentAnalysis?.status == AnalysisStatus.FINISHED,
                    onOpenSubmitDialog = { showSubmitDialog = true }
                )
            }

            if (errorMessage != null) {
                item {
                    ErrorDisplay(errorMessage, onBack)
                }
            } else if (currentAnalysis != null) {
                item {
                    ProjectInfoCard(projectName, currentAnalysis!!, catalogName, isMobile, courseName)
                }
                item {
                    Text(text = "Bewertung nach Kategorien", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                items(currentAnalysis!!.feedback ?: emptyList()) { feedbackItem ->
                    CategoryScoreCard(feedbackItem.criterion, feedbackItem.rating, feedbackItem.comment)
                }
                item {
                    FeedbackCard(
                        analysisResult = currentAnalysis!!,
                        isMobile = isMobile,
                        isLecturer = isLecturer,
                        onSaveFeedback = { comment ->
                            scope.launch {
                                analyseSteuerung.updateAnalysisResult(
                                    analysisId = analysisId,
                                    instructorId = userId,
                                    newStatus = AnalysisStatus.REVIEWED,
                                    newScore = currentAnalysis!!.score,
                                    newFeedback = currentAnalysis!!.feedback,
                                    instructorComment = comment
                                ).onSuccess { currentAnalysis = it }
                            }
                        }
                    )
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

private fun generateResultText(projectName: String, res: AnalysisResult, catalogName: String, courseName: String): String {
    val sb = StringBuilder()
    sb.appendLine("CodeScope Analysebericht")
    sb.appendLine("========================")
    sb.appendLine("Projekt: $projectName")
    sb.appendLine("Datum: ${formatTimestamp(res.createdAt)}")
    sb.appendLine("Modell: ${res.model}")
    sb.appendLine("Katalog: $catalogName")
    if (courseName.isNotBlank()) sb.appendLine("Kurs: $courseName")
    sb.appendLine("Gesamt-Score: ${res.score ?: 0}/100")
    sb.appendLine()
    sb.appendLine("Bewertungen:")
    res.feedback?.forEach { item ->
        sb.appendLine("- ${item.criterion}: ${item.rating}/100")
        if (item.comment.isNotBlank()) sb.appendLine("  Feedback: ${item.comment}")
    }
    sb.appendLine()
    sb.appendLine("Dozenten-Feedback:")
    sb.appendLine(res.instructorComment ?: "Noch kein Feedback vorhanden.")
    return sb.toString()
}

@Composable
private fun ErrorDisplay(message: String, onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Zurück zum Dashboard", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    onBack: () -> Unit,
    onExport: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onShare: (() -> Unit)?,
    isMobile: Boolean,
    showSubmit: Boolean,
    onOpenSubmitDialog: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = if (isMobile) 16.dp else 0.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(text = "Analyseergebnis", fontSize = if (isMobile) 24.sp else 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.weight(1f))

        if (showSubmit) {
            IconButton(onClick = onOpenSubmitDialog) {
                Icon(Icons.Default.School, contentDescription = "Zum Kurs hinzufügen", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (!isMobile) {
            IconButton(onClick = onExport ?: {}) {
                Icon(Icons.Default.Download, contentDescription = "Exportieren", tint = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            onShare?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.Share, contentDescription = "Teilen", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        onDelete?.let {
            IconButton(onClick = it) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ProjectInfoCard(projectName: String, analysisResult: AnalysisResult, catalogName: String, isMobile: Boolean, courseName: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(if (isMobile) 16.dp else 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (isMobile) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularScoreIndicator(analysisResult.score ?: 0, true)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ProjectDetails(projectName, analysisResult, catalogName, isMobile, courseName)
                if (!isMobile) {
                    CircularScoreIndicator(analysisResult.score ?: 0, false)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ProjectDetails(projectName: String, analysisResult: AnalysisResult, catalogName: String, isMobile: Boolean, courseName: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = projectName, fontSize = if (isMobile) 20.sp else 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = "Analysiert am ${formatTimestamp(analysisResult.createdAt)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Text(text = "Modell: ${analysisResult.model}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "Katalog: $catalogName", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (analysisResult.courseId != null) {
            Text(text = "Kurs: ${if (courseName.isNotBlank()) courseName else analysisResult.courseId}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        val score = analysisResult.score ?: 0
        val bestanden = score >= 35
        val badgeColor = if (bestanden) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
        Surface(shape = RoundedCornerShape(8.dp), color = badgeColor.copy(alpha = 0.1f)) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (bestanden) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (bestanden) "Bestanden" else "Kritisch", color = badgeColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CircularScoreIndicator(score: Int, isMobile: Boolean) {
    val scoreColor = when {
        score >= 80 -> MaterialTheme.colorScheme.secondary
        score >= 35 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(if (isMobile) 120.dp else 160.dp)) {
        CircularProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxSize(), color = scoreColor, strokeWidth = if (isMobile) 10.dp else 12.dp, trackColor = MaterialTheme.colorScheme.outlineVariant)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = score.toString(), fontSize = if (isMobile) 36.sp else 48.sp, fontWeight = FontWeight.Bold, color = scoreColor)
            Text(text = "/100", fontSize = if (isMobile) 14.sp else 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryScoreCard(name: String, score: Int, description: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                val catScoreColor = when {
                    score >= 80 -> MaterialTheme.colorScheme.secondary
                    score >= 35 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                Text(text = "$score/100", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = catScoreColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progressColor = when {
                score >= 80 -> MaterialTheme.colorScheme.secondary
                score >= 35 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = progressColor, trackColor = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeedbackCard(analysisResult: AnalysisResult, isMobile: Boolean, isLecturer: Boolean, onSaveFeedback: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(analysisResult.instructorComment ?: "") }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(if (isMobile) 16.dp else 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Dozenten-Feedback", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                if (isLecturer && !isEditing) {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isEditing) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false }) { Text("Abbrechen") }
                    Button(onClick = { onSaveFeedback(text); isEditing = false }) { Text("Speichern") }
                }
            } else {
                Text(text = if (text.isBlank()) "Noch kein Feedback vorhanden." else text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, lineHeight = 20.sp)
                if (analysisResult.status == AnalysisStatus.REVIEWED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Status: Bewertet ✓", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun SubmitToCourseDialog(courses: List<Course>, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Kurs auswählen") }, text = {
        if (courses.isEmpty()) {
            Text("Du bist in keinem Kurs eingeschrieben.")
        } else {
            LazyColumn {
                items(courses) { course ->
                    ListItem(headlineContent = { Text(course.name) }, modifier = Modifier.clickable { onConfirm(course.id) })
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}
