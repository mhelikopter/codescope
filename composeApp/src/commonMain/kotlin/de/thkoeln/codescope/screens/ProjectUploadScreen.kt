package de.thkoeln.codescope.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.domain.project.Project
import kotlinx.coroutines.launch
import de.thkoeln.codescope.components.PlatformFilePicker
import de.thkoeln.codescope.domain.googleAuth.DriveFile
import de.thkoeln.codescope.getPlatform
import de.thkoeln.codescope.components.StandardScreenLayout
import de.thkoeln.codescope.data.client.GoogleAuth
import de.thkoeln.codescope.domain.project.ProjectStatus
import de.thkoeln.codescope.logic.IProjektSteuerung
import de.thkoeln.codescope.viewmodel.ProjectUploadViewModel
import moe.tlaster.precompose.navigation.BackHandler

/**
 * Composable for the project upload screen on desktop.
 */
@Composable
fun DesktopProjectUploadScreen(
    projectSteuerung: IProjektSteuerung,
    googleAuth: GoogleAuth,
    userId: String,
    onUploadComplete: (Project) -> Unit,
    onBack: () -> Unit
) {
    ProjectUploadScreenContent(projectSteuerung, googleAuth, userId, onUploadComplete, onBack)
}

/**
 * Composable for the project upload screen on mobile.
 */
@Composable
fun MobileProjectUploadScreen(
    projectSteuerung: IProjektSteuerung,
    googleAuth: GoogleAuth,
    userId: String,
    onUploadComplete: (Project) -> Unit,
    onBack: () -> Unit
) {
    ProjectUploadScreenContent(projectSteuerung, googleAuth, userId, onUploadComplete, onBack)
}

@Composable
private fun ProjectUploadScreenContent(
    projectSteuerung: IProjektSteuerung,
    googleAuth: GoogleAuth,
    userId: String,
    onUploadComplete: (Project) -> Unit,
    onBack: () -> Unit
) {
    val viewModel = viewModel { ProjectUploadViewModel(projektSteuerung = projectSteuerung, googleAuth) }
    val platform = remember { getPlatform() }
    val isAndroid = platform.name.contains("Android", ignoreCase = true)
    
    val selectedSource = viewModel.selectedSource
    val projectName = viewModel.projectName
    val projectPath = viewModel.projectPath
    val isUploading = viewModel.isUploading
    val showFilePicker = viewModel.showFilePicker
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val tempFileData = viewModel.tempFileData
    
    val driveAccessToken = viewModel.driveAccessToken
    val driveFiles = viewModel.driveFiles
    val isLoadingDriveFiles = viewModel.isLoadingDriveFiles
    val showDriveFileDialog = viewModel.showDriveFileDialog

    val errorMessage = viewModel.errorMessage
    val successMessage = viewModel.successMessage

    val projects = viewModel.projects

    val showDeleteDialog = viewModel.showDeleteDialog

    LaunchedEffect(Unit){
        viewModel.loadProjects(userId)
    }

    //show error messages from viewmodel
    LaunchedEffect(errorMessage) {
        errorMessage?.let{
            snackbarHostState.showSnackbar(it)
        }
        viewModel.clearMessages()
    }

    // Intelligenter BackHandler
    BackHandler(enabled = true) {
        when {
            showFilePicker -> viewModel.closeFilePicker()
            showDriveFileDialog -> viewModel.closeDriveFileDialog()
            isUploading -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Upload läuft noch...")
                }
            }
            else -> onBack()
        }
    }

    if (showFilePicker) {
        PlatformFilePicker(
            show = showFilePicker,
            fileExtensions = listOf("zip"),
            onFileSelected = { bytes, name ->
                viewModel.closeFilePicker()
                viewModel.onFileSelected(bytes, name)
            },
            onDismiss = { viewModel.closeFilePicker() }
        )
    }

    if (showDriveFileDialog) {
        DriveFilePickerDialog(
            files = driveFiles,
            onFileSelected = { file ->
                viewModel.selectDriveFile(file)
            },
            onDismiss = { viewModel.closeDriveFileDialog() }
        )
    }

    if (showDeleteDialog) {
        DeleteDialog(
            onDismiss = { viewModel.dismissDeleteDialog() },
            onDelete = { viewModel.confirmDeletion(userId) },
            project = viewModel.itemToDelete!!
        )
    }

    StandardScreenLayout(title = "Projekt hochladen", isMobile = isAndroid) {
        Box(modifier = Modifier.weight(1f)) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(if (isAndroid) 16.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (isAndroid) 16.dp else 24.dp)
            ) {
                item {
                    UploadCard(
                        selectedSource = selectedSource,
                        onSourceChange = {
                            viewModel.selectSource(it)
                        },
                        projectName = projectName,
                        onProjectNameChange = { viewModel.updateProjectName(it) },
                        projectPath = projectPath,
                        onProjectPathChange = { viewModel.updateProjectPath(it) },
                        onBrowseClick = { viewModel.openFilePicker() },
                        isUploading = isUploading,
                        isAndroid = isAndroid,
                        driveAccessToken = driveAccessToken,
                        isLoadingDriveFiles = isLoadingDriveFiles,
                        onGoogleLoginClick = {
                            if (!isAndroid) {
                                viewModel.loginToGoogleDrive()
                            }
                        },
                        onOpenDrivePicker = {
                            viewModel.openFilePicker()
                        },
                        onUploadClick = {
                            if (selectedSource == "local" && tempFileData == null) {
                                scope.launch { snackbarHostState.showSnackbar("Bitte wählen Sie zuerst eine ZIP-Datei aus.") }
                                return@UploadCard
                            }

                            viewModel.uploadProject(userId, onUploadComplete)
                        },
                        onCancelClick = onBack,
                        isUploadEnabled = when (selectedSource) {
                            "local" -> projectName.isNotBlank() && tempFileData != null
                            "git" -> projectName.isNotBlank() && projectPath.startsWith("http") && !isAndroid
                            "drive" -> projectName.isNotBlank() && projectPath.isNotEmpty() && (driveAccessToken != null || isAndroid)
                            else -> false
                        }
                    )
                }

                items(
                    items = projects,
                    key = { it.id }
                ) { project ->
                    ProjectCard(project, isAndroid, viewModel, onUploadComplete)
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(alignment = Alignment.BottomCenter)
            )

        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    isMobile: Boolean,
    viewModel: ProjectUploadViewModel,
    onUploadComplete: (Project) -> Unit
) {
    val statusColor = when (project.status) {
        ProjectStatus.UPLOADED -> MaterialTheme.colorScheme.outline
        ProjectStatus.ANALYSIS_RUNNING -> MaterialTheme.colorScheme.primary
        ProjectStatus.ANALYSED -> MaterialTheme.colorScheme.secondary
        ProjectStatus.REVIEWED -> MaterialTheme.colorScheme.tertiary
    }

    val statusLabel = project.status.toString()
        .lowercase()
        .replace("_", " ")
        .replaceFirstChar { it.uppercase() }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp), // Tiny outer padding for shadow
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top // Align top in case text wraps
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.onDeleteProject(project) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Projekt löschen",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            FilledTonalButton(
                onClick = {
                    viewModel.updateProjectStatus(project) //update the project status so that the analysis can be performed
                    onUploadComplete(project)
                          },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Analyse ausführen")
            }
        }
    }
}
@Composable
private fun UploadCard(
    selectedSource: String?,
    onSourceChange: (String) -> Unit,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    projectPath: String,
    onProjectPathChange: (String) -> Unit,
    onBrowseClick: () -> Unit,
    isUploading: Boolean,
    isAndroid: Boolean,
    driveAccessToken: String?,
    isLoadingDriveFiles: Boolean,
    onGoogleLoginClick: () -> Unit,
    onOpenDrivePicker: () -> Unit,
    onUploadClick: () -> Unit,
    onCancelClick: () -> Unit,
    isUploadEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SourceSelection(selectedSource = selectedSource, onSourceChange = onSourceChange)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ProjectDetails(
                selectedSource = selectedSource,
                projectName = projectName,
                onProjectNameChange = onProjectNameChange,
                projectPath = projectPath,
                onProjectPathChange = onProjectPathChange,
                onBrowseClick = onBrowseClick,
                isAndroid = isAndroid,
                driveAccessToken = driveAccessToken,
                isLoadingDriveFiles = isLoadingDriveFiles,
                onGoogleLoginClick = onGoogleLoginClick,
                onOpenDrivePicker = onOpenDrivePicker
            )
            
            if (selectedSource != null) {
                ActionButtons(
                    isUploading = isUploading,
                    onUploadClick = onUploadClick,
                    onCancelClick = onCancelClick,
                    isUploadEnabled = isUploadEnabled && !isUploading
                )
            }
        }
    }
}

@Composable
private fun SourceSelection(selectedSource: String?, onSourceChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "1", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = "Quelle auswählen", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceCard(
                title = "Lokales ZIP",
                subtitle = "",
                icon = Icons.Default.FolderZip,
                isSelected = selectedSource == "local",
                onClick = { onSourceChange("local") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            SourceCard(
                title = "Git Repository",
                subtitle = "",
                icon = Icons.Default.Link,
                isSelected = selectedSource == "git",
                onClick = { onSourceChange("git") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            SourceCard(
                title = "Google Drive",
                subtitle = "",
                icon = Icons.Default.Cloud,
                isSelected = selectedSource == "drive",
                onClick = { onSourceChange("drive") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProjectDetails(
    selectedSource: String?,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    projectPath: String,
    onProjectPathChange: (String) -> Unit,
    onBrowseClick: () -> Unit,
    isAndroid: Boolean,
    driveAccessToken: String?,
    isLoadingDriveFiles: Boolean,
    onGoogleLoginClick: () -> Unit,
    onOpenDrivePicker: () -> Unit
) {
    if (selectedSource != null) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "2", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = "Projektdetails", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

            when (selectedSource) {
                "local" -> {
                    OutlinedTextField(
                        value = projectPath,
                        onValueChange = {},
                        label = { Text("Pfad") },
                        placeholder = { Text("Lokale Datei wählen...") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.FolderZip, null) },
                        trailingIcon = {
                            Button(onClick = onBrowseClick, modifier = Modifier.padding(end = 8.dp)) {
                                Text("Suchen")
                            }
                        }
                    )
                }
                "git" -> {
                    if (isAndroid) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Git-Uploads werden derzeit nur auf dem Desktop unterstützt. Bitte laden Sie das Repository als ZIP herunter und nutzen Sie 'Lokales ZIP'.", fontSize = 13.sp)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = projectPath,
                            onValueChange = onProjectPathChange,
                            label = { Text("Repository URL (HTTPS)") },
                            placeholder = { Text("https://github.com/user/repo.git") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Link, null) }
                        )
                    }
                }
                "drive" -> {
                    if (isAndroid) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Nutzen Sie 'Lokales ZIP' -> 'Google Drive' auf Android.", fontSize = 13.sp)
                            }
                        }
                    } else if (driveAccessToken == null) {
                        Button(
                            onClick = onGoogleLoginClick,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            if (isLoadingDriveFiles) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Login, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Mit Google anmelden")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = projectPath,
                            onValueChange = {},
                            label = { Text("Drive Datei ID") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.CloudQueue, null) },
                            trailingIcon = {
                                Button(onClick = onOpenDrivePicker, modifier = Modifier.padding(end = 8.dp)) {
                                    if (isLoadingDriveFiles) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    } else {
                                        Text("Wählen")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = projectName,
                onValueChange = onProjectNameChange,
                label = { Text("Projektname *") },
                placeholder = { Text("z.B. Softwaretechnik Hausarbeit") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isUploading: Boolean,
    onUploadClick: () -> Unit,
    onCancelClick: () -> Unit,
    isUploadEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onCancelClick,
            enabled = !isUploading,
            modifier = Modifier.weight(1f).height(50.dp)
        ) {
            Text("Abbrechen")
        }

        Button(
            onClick = onUploadClick,
            enabled = isUploadEnabled && !isUploading,
            modifier = Modifier.weight(1f).height(50.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Hochladen")
            }
        }
    }
}

@Composable
private fun DeleteDialog(onDismiss: () -> Unit, onDelete: (project: Project) -> Unit, project: Project){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sind Sie sicher das Sie das Projekt löschen wollen?") },
        text = { Text("Sind Sie sicher das Sie das Projekt löschen wollen?") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete(project)
                          },
                colors = ButtonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.Transparent
                )
            ) {
                Text("Löschen")
            }
                        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun DriveFilePickerDialog(
    files: List<DriveFile>,
    onFileSelected: (DriveFile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Google Drive ZIP auswählen") },
        text = {
            if (files.isEmpty()) {
                Text("Keine ZIP-Dateien gefunden.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(files) { file ->
                        ListItem(
                            headlineContent = { Text(file.name) },
                            leadingContent = { Icon(Icons.Default.FolderZip, null) },
                            modifier = Modifier.clickable { onFileSelected(file) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}
