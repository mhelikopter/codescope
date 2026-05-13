package de.thkoeln.codescope.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.components.StatCard
import de.thkoeln.codescope.components.StandardScreenLayout
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.ai.AiModel
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.viewmodel.AssessmentManagementViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.logic.IAdminSteuerung
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.BackHandler
import org.koin.compose.koinInject

@Composable
fun DesktopAssessmentManagementScreen(
    analyseSteuerung: IAnalyseSteuerung,
    userId: String,
    courseId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    AssessmentManagementContent(analyseSteuerung, userId, courseId, onNavigateBack = onNavigateBack, onNavigateToAnalysis = onNavigateToAnalysis, isMobile = false)
}

@Composable
fun MobileAssessmentManagementScreen(
    analyseSteuerung: IAnalyseSteuerung,
    userId: String,
    courseId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    AssessmentManagementContent(analyseSteuerung, userId, courseId, onNavigateBack = onNavigateBack, onNavigateToAnalysis = onNavigateToAnalysis, isMobile = true)
}

@Composable
private fun AssessmentManagementContent(
    analyseSteuerung: IAnalyseSteuerung,
    userId: String,
    courseId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit,
    isMobile: Boolean
) {
    val kriterienSteuerung: IKriterienkatalogSteuerung = koinInject()
    val adminSteuerung: IAdminSteuerung = koinInject()
    
    val viewModel: AssessmentManagementViewModel = viewModel {
        AssessmentManagementViewModel(analyseSteuerung, kriterienSteuerung, adminSteuerung)
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, courseId) {
        viewModel.loadData(userId, courseId)
    }

    if (viewModel.showBatchDialog) {
        BatchAnalysisDialog(
            count = viewModel.selectedIds.size,
            catalogs = viewModel.availableCatalogs,
            onDismiss = { viewModel.closeBatchDialog() },
            onConfirm = { catalogId, model, notifyStudents ->
                viewModel.startBatchAnalysis(catalogId, model, notifyStudents, courseId, userId)
            }
        )
    }

    BackHandler(enabled = true) {
        if (courseId != null) {
            onNavigateBack()
        }
    }

    StandardScreenLayout(
        title = "Bewertungs-Management",
        isMobile = isMobile,
        showBackButton = courseId != null,
        onNavigateBack = onNavigateBack,
        bottomBar = {
            if (viewModel.selectedIds.isNotEmpty()) {
                BatchActionBar(
                    selectedCount = viewModel.selectedIds.size,
                    onStartBatch = { viewModel.openBatchDialog() },
                    onCancel = { viewModel.clearSelection() }
                )
            }
        }
    ) {
        Statistics(viewModel.assessments, isMobile = isMobile)
        Tabs(selectedTab = viewModel.selectedTab, onTabChange = { viewModel.selectTab(it) }, isMobile = isMobile)
        Filters(searchQuery = viewModel.searchQuery, onSearchQueryChange = { viewModel.updateSearchQuery(it) }, isMobile = isMobile)

        if (viewModel.isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(if (isMobile) 16.dp else 24.dp)) {
                viewModel.filteredAssessments.forEach { assessment ->
                    val studentName = viewModel.allUsers.find { it.id == assessment.userId }?.name ?: assessment.userId

                    AssessmentCard(
                        assessment = assessment,
                        studentName = studentName,
                        isMobile = isMobile,
                        isSelected = viewModel.selectedIds.contains(assessment.id),
                        onToggleSelect = { viewModel.toggleSelection(assessment.id) },
                        onNavigateToDetails = {
                            onNavigateToAnalysis(assessment.projectId, assessment.id)
                        },
                        onReview = { comment, score ->
                            scope.launch {
                                analyseSteuerung.updateAnalysisResult(
                                    analysisId = assessment.id,
                                    instructorId = userId,
                                    newStatus = AnalysisStatus.REVIEWED,
                                    newScore = score,
                                    newFeedback = assessment.feedback,
                                    instructorComment = comment
                                ).onSuccess { 
                                    viewModel.refreshData(userId, courseId)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchActionBar(selectedCount: Int, onStartBatch: () -> Unit, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$selectedCount ausgewählt", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) { Text("Abbrechen") }
                Button(onClick = onStartBatch) { Text("Batch Analyse starten") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchAnalysisDialog(
    count: Int, 
    catalogs: List<CriteriaCatalog>,
    onDismiss: () -> Unit, 
    onConfirm: (String, String, Boolean) -> Unit
) {
    var selectedCatalog by remember { mutableStateOf(catalogs.firstOrNull()) }
    var selectedModel by remember { mutableStateOf(AiModel.GEMINI_2_FLASH) }
    var notifyStudents by remember { mutableStateOf(true) }
    
    var catalogExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Analyse starten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Möchten Sie eine Batch-Analyse für $count Projekte starten?")
                
                // Catalog Selector
                ExposedDropdownMenuBox(
                    expanded = catalogExpanded,
                    onExpandedChange = { catalogExpanded = !catalogExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCatalog?.name ?: "Kein Katalog verfügbar",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kriterienkatalog") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catalogExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catalogExpanded,
                        onDismissRequest = { catalogExpanded = false }
                    ) {
                        catalogs.forEach { catalog ->
                            DropdownMenuItem(
                                text = { Text(catalog.name) },
                                onClick = {
                                    selectedCatalog = catalog
                                    catalogExpanded = false
                                }
                            )
                        }
                    }
                }

                // AI Model Selector
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedModel.id,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("KI Modell") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        AiModel.entries.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.id) },
                                onClick = {
                                    selectedModel = model
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notifyStudents, onCheckedChange = { notifyStudents = it })
                    Text("Studenten über Aktualisierung benachrichtigen", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCatalog != null,
                onClick = { 
                    selectedCatalog?.let { 
                        onConfirm(it.id, selectedModel.id, notifyStudents) 
                    }
                }
            ) { 
                Text("Starten") 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Abbrechen") } 
        }
    )
}

@Composable
private fun Statistics(assessments: List<AnalysisResult>, isMobile: Boolean) {
    val pending = assessments.count { it.status == AnalysisStatus.FINISHED }
    val reviewed = assessments.count { it.status == AnalysisStatus.REVIEWED }
    
    if (isMobile) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Zu bewerten", pending.toString(), Modifier.fillMaxWidth(), MaterialTheme.colorScheme.primary)
            StatCard("Abgeschlossen", reviewed.toString(), Modifier.fillMaxWidth(), MaterialTheme.colorScheme.tertiary)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Zu bewerten", pending.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            StatCard("Abgeschlossen", reviewed.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun Tabs(selectedTab: Int, onTabChange: (Int) -> Unit, isMobile: Boolean) {
    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
        Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }) { Text("Ausstehend", modifier = Modifier.padding(12.dp)) }
        Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }) { Text("Abgeschlossen", modifier = Modifier.padding(12.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Filters(searchQuery: String, onSearchQueryChange: (String) -> Unit, isMobile: Boolean) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Suchen...") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun AssessmentCard(
    assessment: AnalysisResult,
    studentName: String,
    isMobile: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onNavigateToDetails: () -> Unit,
    onReview: (String, Int) -> Unit
) {
    var showReviewDialog by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(assessment.score?.toString() ?: "") }

    val isScoreValid = score.isNotEmpty() && score.toIntOrNull() in 0..100

    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Bewertung abgeben") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = score,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || (newValue.all { it.isDigit() } && (newValue.toIntOrNull() ?: 0) <= 100)) {
                                score = newValue
                            }
                        },
                        label = { Text("Score (0-100)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = score.isNotEmpty() && !isScoreValid,
                        supportingText = {
                            if (score.isNotEmpty() && !isScoreValid) {
                                Text("Bitte einen Wert zwischen 0 und 100 eingeben")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Kommentar") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = isScoreValid,
                    onClick = {
                        onReview(comment, score.toIntOrNull() ?: 0)
                        showReviewDialog = false
                    }
                ) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { showReviewDialog = false }) { Text("Abbrechen") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggleSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = onToggleSelect) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Select",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(Modifier.weight(1f).clickable { onNavigateToDetails() }) {
                    Text(assessment.projectId, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Student: $studentName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                IconButton(onClick = onNavigateToDetails) {
                    Icon(Icons.Default.Info, contentDescription = "Details ansehen", tint = MaterialTheme.colorScheme.primary)
                }

                if (assessment.status == AnalysisStatus.REVIEWED) {
                    Text("${assessment.score}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                }
            }
            
            if (assessment.status == AnalysisStatus.FINISHED) {
                Button(onClick = { showReviewDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Jetzt bewerten")
                }
            } else if (assessment.instructorComment != null) {
                HorizontalDivider()
                Text("Dozent Kommentar:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(assessment.instructorComment, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
