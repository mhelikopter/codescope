package de.thkoeln.codescope.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import de.thkoeln.codescope.util.formatTimestamp
import de.thkoeln.codescope.components.PlatformFilePicker
import de.thkoeln.codescope.components.StandardScreenLayout
import de.thkoeln.codescope.util.PlatformContext
import de.thkoeln.codescope.util.saveFileToDisk
import de.thkoeln.codescope.viewmodel.CriteriaManagementViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.AppContext
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.BackHandler
import org.koin.compose.koinInject

@Composable
fun DesktopCriteriaManagementScreen(
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    CriteriaManagementContent(kriterienkatalogSteuerung, userId, onNavigateBack = onNavigateBack, onNavigateToAnalysis = onNavigateToAnalysis, isMobile = false)
}

@Composable
fun MobileCriteriaManagementScreen(
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    CriteriaManagementContent(kriterienkatalogSteuerung, userId, onNavigateBack = onNavigateBack, onNavigateToAnalysis = onNavigateToAnalysis, isMobile = true)
}

@Composable
private fun CriteriaManagementContent(
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit,
    isMobile: Boolean
) {
    val viewModel: CriteriaManagementViewModel = viewModel {
        CriteriaManagementViewModel(kriterienkatalogSteuerung)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val appContext: AppContext = koinInject()
    val platformContext = PlatformContext(appContext)

    LaunchedEffect(userId) {
        viewModel.loadCatalogs(userId)
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

    PlatformFilePicker(
        show = viewModel.showFilePicker,
        fileExtensions = listOf("txt", "json"),
        onFileSelected = { bytes, name ->
            viewModel.uploadCatalogFromFile(userId, bytes, name)
        },
        onDismiss = { viewModel.closeFilePicker() }
    )

    if (viewModel.showCreateDialog) {
        CatalogEditorDialog(
            title = "Neuer Kriterienkatalog",
            confirmButtonText = "Erstellen",
            kriterienkatalogSteuerung = kriterienkatalogSteuerung,
            onDismiss = { viewModel.closeCreateDialog() },
            onConfirm = { name, criteriaText ->
                viewModel.createCatalog(userId, name, criteriaText)
            }
        )
    }

    if (viewModel.editingCatalog != null && viewModel.editingCatalogContent != null) {
        val catalog = viewModel.editingCatalog!!
        val existingCriteria = viewModel.editingCatalogContent!!
            .lines()
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotEmpty() }

        CatalogEditorDialog(
            title = "Katalog bearbeiten",
            confirmButtonText = "Speichern",
            initialName = catalog.name,
            initialCriteria = existingCriteria,
            kriterienkatalogSteuerung = kriterienkatalogSteuerung,
            onDismiss = { viewModel.closeEditDialog() },
            onConfirm = { name, criteriaText ->
                viewModel.updateCatalog(userId, name, criteriaText)
            }
        )
    }

    BackHandler(enabled = true) {
        onNavigateBack()
    }

    StandardScreenLayout(
        title = "Kriterienkataloge",
        isMobile = isMobile,
        snackbarHostState = snackbarHostState
    ) {
        CatalogSection(
            catalogs = viewModel.filteredCatalogs,
            searchQuery = viewModel.searchQuery,
            onSearchChange = { viewModel.updateSearchQuery(it) },
            isMobile = isMobile,
            onNewClick = { viewModel.openCreateDialog() },
            onImportClick = { viewModel.openFilePicker() },
            onEditClick = { viewModel.openEditDialog(it) },
            kriterienkatalogSteuerung = kriterienkatalogSteuerung,
            userId = userId,
            snackbarHostState = snackbarHostState,
            platformContext = platformContext
        )
    }
}

@Composable
private fun CatalogEditorDialog(
    title: String,
    confirmButtonText: String,
    initialName: String = "",
    initialCriteria: List<String> = listOf(""),
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var criteriaList by remember { mutableStateOf(initialCriteria) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold)
                if (isGenerating) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Thema / Name (z.B. Java Basics)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            kriterienkatalogSteuerung.generateCriteriaFromAi(name)
                                .onSuccess { aiCriteria ->
                                    criteriaList = aiCriteria
                                }
                            isGenerating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && !isGenerating && (criteriaList.isEmpty() || (criteriaList.size == 1 && criteriaList[0].isBlank())),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("KI-Vorschlag generieren")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Kriterien:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(criteriaList) { index, criterion ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = criterion,
                                onValueChange = { newValue ->
                                    val newList = criteriaList.toMutableList()
                                    newList[index] = newValue
                                    criteriaList = newList
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("z.B. Sauberer Code...") }
                            )
                            IconButton(onClick = {
                                if (criteriaList.size > 1) {
                                    criteriaList = criteriaList.filterIndexed { i, _ -> i != index }
                                } else {
                                    criteriaList = listOf("")
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { criteriaList = criteriaList + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Weiteres Kriterium hinzufügen")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullText = criteriaList.filter { it.isNotBlank() }.joinToString("\n") { "- $it" }
                    onConfirm(name, fullText)
                },
                enabled = name.isNotBlank() && criteriaList.any { it.isNotBlank() } && !isGenerating
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun CatalogSection(
    catalogs: List<CriteriaCatalog>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isMobile: Boolean,
    onNewClick: () -> Unit,
    onImportClick: () -> Unit,
    onEditClick: (CriteriaCatalog) -> Unit,
    kriterienkatalogSteuerung: IKriterienkatalogSteuerung,
    userId: String,
    snackbarHostState: SnackbarHostState,
    platformContext: PlatformContext
) {
    val scope = rememberCoroutineScope()
    var currentCatalogs by remember { mutableStateOf(catalogs) }

    LaunchedEffect(catalogs) {
        currentCatalogs = catalogs
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionBar(searchQuery, onSearchChange, isMobile, onNewClick = onNewClick)
        
        val filteredCatalogs = currentCatalogs.filter { it.name.contains(searchQuery, ignoreCase = true) }
        
        filteredCatalogs.forEach { catalog ->
            CatalogCard(
                catalog = catalog,
                onEdit = { onEditClick(catalog) },
                onDelete = {
                    scope.launch {
                        kriterienkatalogSteuerung.deleteCriteriaCatalog(catalog.id, userId)
                            .onSuccess { currentCatalogs = currentCatalogs.filter { it.id != catalog.id } }
                    }
                },
                onExport = {
                    scope.launch {
                        kriterienkatalogSteuerung.downloadCriteriaCatalog(catalog.id).onSuccess { bytes ->
                            val sanitizedName = catalog.name.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                            saveFileToDisk(platformContext, "$sanitizedName.txt", bytes)
                        }.onFailure {
                            snackbarHostState.showSnackbar("Export fehlgeschlagen")
                        }
                    }
                },
                isMobile = isMobile
            )
        }
        
        ImportCatalogCard(onImportClick = onImportClick)
    }
}

@Composable
private fun ActionBar(searchQuery: String, onSearchQueryChange: (String) -> Unit, isMobile: Boolean, onNewClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Kataloge suchen...") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = onNewClick) {
            Icon(Icons.Default.Add, null)
            if (!isMobile) { Text(" Neuer Katalog", modifier = Modifier.padding(start = 4.dp)) }
        }
    }
}

@Composable
private fun CatalogCard(catalog: CriteriaCatalog, onEdit: () -> Unit, onDelete: () -> Unit, onExport: () -> Unit, isMobile: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatalogCardHeader(catalog)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    if (!isMobile) { Text(" Bearbeiten") }
                }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    if (!isMobile) { Text(" Export") }
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    if (!isMobile) { Text(" Löschen") }
                }
            }
        }
    }
}

@Composable
private fun CatalogCardHeader(catalog: CriteriaCatalog) {
    val date = remember(catalog.lastUpdated) { formatTimestamp(catalog.lastUpdated) }
    Column {
        Text(catalog.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text("Zuletzt aktualisiert: $date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ImportCatalogCard(onImportClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Katalog importieren", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("Laden Sie eine bestehende Datei hoch.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
                Text("Datei auswählen")
            }
        }
    }
}
