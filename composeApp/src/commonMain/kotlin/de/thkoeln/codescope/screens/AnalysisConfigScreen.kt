package de.thkoeln.codescope.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.domain.ai.AiModel
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import kotlinx.coroutines.launch
import de.thkoeln.codescope.components.PlatformFilePicker
import de.thkoeln.codescope.util.calculateProjectSize
import de.thkoeln.codescope.components.StandardScreenLayout
import de.thkoeln.codescope.viewmodel.AnalysisConfigViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import moe.tlaster.precompose.navigation.BackHandler

/**
 * Composable for the analysis configuration screen on desktop.
 */
@Composable
fun DesktopAnalysisConfigScreen(
    analyseSteuerung: IAnalyseSteuerung,
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    project: Project,
    userId: String,
    onStartAnalysis: (analysisId: String) -> Unit,
    onBack: () -> Unit
) {
    AnalysisConfigScreenContent(
        analyseSteuerung = analyseSteuerung,
        kriterienkatalogSteuerung = kriterienkatalogSteuerung,
        project = project,
        userId = userId,
        onStartAnalysis = onStartAnalysis,
        onBack = onBack,
        isMobile = false
    )
}

/**
 * Composable for the analysis configuration screen on mobile.
 */
@Composable
fun MobileAnalysisConfigScreen(
    analyseSteuerung: IAnalyseSteuerung,
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    project: Project,
    userId: String,
    onStartAnalysis: (analysisId: String) -> Unit,
    onBack: () -> Unit
) {
    AnalysisConfigScreenContent(
        analyseSteuerung = analyseSteuerung,
        kriterienkatalogSteuerung = kriterienkatalogSteuerung,
        project = project,
        userId = userId,
        onStartAnalysis = onStartAnalysis,
        onBack = onBack,
        isMobile = true
    )
}

@Composable
private fun AnalysisConfigScreenContent(
    analyseSteuerung: IAnalyseSteuerung,
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    project: Project,
    userId: String,
    onStartAnalysis: (analysisId: String) -> Unit,
    onBack: () -> Unit,
    isMobile: Boolean
) {
    val viewModel: AnalysisConfigViewModel = viewModel {
        AnalysisConfigViewModel(analyseSteuerung, kriterienkatalogSteuerung)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Sicherer BackHandler: Verhindert Absturz durch Hardware-Back-Button
    BackHandler(enabled = true) {
        onBack()
    }

    LaunchedEffect(userId) {
        viewModel.loadCatalogs(userId)
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    PlatformFilePicker(
        show = viewModel.showFilePicker,
        fileExtensions = listOf("txt", "json"),
        onFileSelected = { bytes, name ->
            viewModel.onFileSelected(bytes, name)
        },
        onDismiss = {
            viewModel.closeFilePicker()
        }
    )

    if (viewModel.showNewCatalogDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeNewCatalogDialog() },
            title = { Text("Neuen Kriterienkatalog erstellen") },
            text = {
                OutlinedTextField(
                    value = viewModel.newCatalogName,
                    onValueChange = { viewModel.updateNewCatalogName(it) },
                    label = { Text("Name des Katalogs") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = viewModel.newCatalogName.isNotBlank(),
                    onClick = { viewModel.createNewCatalog(userId) }
                ) {
                    Text("Hochladen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeNewCatalogDialog() }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    StandardScreenLayout(
        title = "Analyse konfigurieren", 
        isMobile = isMobile, 
        showBackButton = true, 
        onNavigateBack = onBack,
        snackbarHostState = snackbarHostState
    ) {
        ConfigCard(
            project = project,
            criteriaCatalogs = viewModel.criteriaCatalogs,
            selectedCatalog = viewModel.selectedCatalog,
            onCatalogChange = { viewModel.selectCatalog(it) },
            selectedModel = viewModel.selectedModel,
            onModelChange = { viewModel.selectModel(it) },
            isStartingAnalysis = viewModel.isStartingAnalysis,
            onStartAnalysis = {
                viewModel.startAnalysis(project, userId, onStartAnalysis)
            },
            onNewCatalog = { viewModel.openFilePicker() },
            onBack = onBack,
            isMobile = isMobile
        )
    }
}

@Composable
private fun ConfigCard(
    project: Project,
    criteriaCatalogs: List<CriteriaCatalog>,
    selectedCatalog: CriteriaCatalog?,
    onCatalogChange: (CriteriaCatalog) -> Unit,
    selectedModel: AiModel,
    onModelChange: (AiModel) -> Unit,
    isStartingAnalysis: Boolean,
    onStartAnalysis: () -> Unit,
    onNewCatalog: () -> Unit,
    onBack: () -> Unit,
    isMobile: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(if (isMobile) 16.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SelectedProjectSection(project = project)
            HorizontalDivider()
            CatalogSelectionSection(
                criteriaCatalogs = criteriaCatalogs,
                selectedCatalog = selectedCatalog,
                onCatalogChange = onCatalogChange,
                onNewCatalog = onNewCatalog
            )
            HorizontalDivider()
            AIModelSelectionSection(selectedModel = selectedModel, onModelChange = onModelChange, isMobile = isMobile)
            Spacer(modifier = Modifier.height(8.dp))
            ActionButtons(
                onStartAnalysis = onStartAnalysis,
                onBack = onBack,
                isMobile = isMobile,
                isLoading = isStartingAnalysis
            )
        }
    }
}

@Composable
private fun SelectedProjectSection(project: Project) {
    val projectSizeFromModel = project.sizeBytes
    val projectSize = if (projectSizeFromModel > 0L) projectSizeFromModel else calculateProjectSize(project.sourceLocation)
    val sizeText = formatFileSize(projectSize)

    Column {
        Text(
            text = "Ausgewähltes Projekt",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = project.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = sizeText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSelectionSection(
    criteriaCatalogs: List<CriteriaCatalog>,
    selectedCatalog: CriteriaCatalog?,
    onCatalogChange: (CriteriaCatalog) -> Unit,
    onNewCatalog: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kriterienkatalog auswählen",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onNewCatalog) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("upload")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedCatalog?.name ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                criteriaCatalogs.forEach { catalog ->
                    DropdownMenuItem(
                        text = { Text(catalog.name) },
                        onClick = {
                            onCatalogChange(catalog)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Der ausgewählte Katalog besteht aus einer einzelnen Bewertungsdatei.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AIModelSelectionSection(selectedModel: AiModel, onModelChange: (AiModel) -> Unit, isMobile: Boolean) {
    Column {
        Text(
            text = "KI-Modell auswählen",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isMobile) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AiModel.entries.forEach { model ->
                    AIModelCard(model = model, isSelected = selectedModel == model, onClick = { onModelChange(model) })
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AiModel.entries.forEach { model ->
                    AIModelCard(model = model, isSelected = selectedModel == model, onClick = { onModelChange(model) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AIModelCard(model: AiModel, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(width = 2.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(12.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isSelected) RadioButton(selected = true, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = model.id, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionButtons(onStartAnalysis: () -> Unit, onBack: () -> Unit, isMobile: Boolean, isLoading: Boolean = false) {
    if (isMobile) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStartAnalysis, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Analyse starten")
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, enabled = !isLoading, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zurück")
            }
            Button(onClick = onStartAnalysis, modifier = Modifier.weight(1f), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Analyse starten")
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(units.lastIndex)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    val formatted = String.format(Locale.GERMAN, "%.1f", value).replace(",0", "")
    return "$formatted ${units[digitGroups]}"
}
