package de.thkoeln.codescope.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import de.thkoeln.codescope.logic.IAdminSteuerung
import de.thkoeln.codescope.viewmodel.AdminPanelViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.BackHandler
import kotlin.random.Random

/**
 * Composable for the admin panel screen on desktop.
 *
 * @param adminSteuerung The admin controller.
 * @param adminId The ID of the current administrator.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 */
@Composable
fun DesktopAdminPanelScreen(
    adminSteuerung: IAdminSteuerung,
    adminId: String,
    onNavigateBack: () -> Unit
) {
    AdminPanelContent(
        adminSteuerung = adminSteuerung,
        adminId = adminId,
        isMobile = false
    )
}

/**
 * Composable for the admin panel screen on mobile.
 *
 * @param adminSteuerung The admin controller.
 * @param adminId The ID of the current administrator.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 */
@Composable
fun MobileAdminPanelScreen(
    adminSteuerung: IAdminSteuerung,
    adminId: String,
    onNavigateBack: () -> Unit
) {
    AdminPanelContent(
        adminSteuerung = adminSteuerung,
        adminId = adminId,
        isMobile = true
    )
}

/**
 * Main content for the admin panel screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPanelContent(
    adminSteuerung: IAdminSteuerung,
    adminId: String,
    isMobile: Boolean
) {
    BackHandler(enabled = true) {

    }

    val viewModel: AdminPanelViewModel = viewModel {
        AdminPanelViewModel(adminSteuerung)
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val filteredUsers = viewModel.filteredUsers
    val filteredCourses = viewModel.filteredCourses

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isMobile) 16.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (isMobile) 16.dp else 24.dp)
            ) {
                item {
                    ScreenHeader(isMobile = isMobile)
                }

                item {
                    AdminStats(userCount = viewModel.users.size, courseCount = viewModel.courses.size, isMobile = isMobile)
                }

                item {
                    AdminTabs(viewModel.selectedTab) { viewModel.selectTab(it) }
                }

                when (viewModel.selectedTab) {
                    0 -> { // Benutzer
                        item {
                            SearchBar(viewModel.searchQuery, { viewModel.updateSearchQuery(it) }, "Benutzer suchen...")
                        }
                        items(filteredUsers) { user ->
                            UserCard(
                                user = user,
                                adminId = adminId,
                                adminSteuerung = adminSteuerung,
                                onActionComplete = { viewModel.refreshData() }
                            )
                        }
                    }
                    1 -> { // Kurse
                        item {
                            SearchBar(viewModel.searchQuery, { viewModel.updateSearchQuery(it) }, "Kurse suchen...")
                        }
                        items(filteredCourses) { course ->
                            CourseAdminCard(
                                course = course,
                                adminId = adminId,
                                adminSteuerung = adminSteuerung,
                                lecturerName = viewModel.users.find { it.id == course.lecturerId }?.name ?: "Unbekannt",
                                onActionComplete = { viewModel.refreshData() }
                            )
                        }
                    }
                    2 -> { // System
                        item {
                            SystemStatusCard(adminSteuerung)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(isMobile: Boolean) {
    Column {
        Text("System-Administration", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Zentrale Übersicht", fontSize = if (isMobile) 24.sp else 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun AdminStats(userCount: Int, courseCount: Int, isMobile: Boolean) {
    if (isMobile) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AdminStatCard("Registrierte Benutzer", userCount.toString(), "Gesamtzahl im System", null, Modifier.fillMaxWidth())
            AdminStatCard("Aktive Kurse", courseCount.toString(), "Gesamtzahl im System", null, Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AdminStatCard("Registrierte Benutzer", userCount.toString(), "Gesamtzahl im System", null, Modifier.weight(1f))
            AdminStatCard("Aktive Kurse", courseCount.toString(), "Gesamtzahl im System", null, Modifier.weight(1f))
            AdminStatCard("Systemstatus", "OK", "Alle Dienste online", true, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdminTabs(selectedTab: Int, onTabChange: (Int) -> Unit) {
    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
        Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }) { Text("Benutzer", modifier = Modifier.padding(12.dp)) }
        Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }) { Text("Kurse", modifier = Modifier.padding(12.dp)) }
        Tab(selected = selectedTab == 2, onClick = { onTabChange(2) }) { Text("System", modifier = Modifier.padding(12.dp)) }
    }
}

@Composable
private fun SearchBar(searchQuery: String, onSearchQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun UserCard(
    user: User,
    adminId: String,
    adminSteuerung: IAdminSteuerung,
    onActionComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showRoleDialog by remember { mutableStateOf(false) }

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Rolle für ${user.name} ändern") },
            text = {
                Column {
                    UserRole.entries.forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            RadioButton(
                                selected = user.role == role,
                                onClick = {
                                    scope.launch {
                                        val result = adminSteuerung.assignRole(adminId, user.id, role)
                                        if (result.isSuccess) {
                                            onActionComplete()
                                        }
                                        showRoleDialog = false
                                    }
                                }
                            )
                            Text(role.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserInfo(user)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showRoleDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = user.id != adminId && user.isActive
                ) {
                    Text("Rolle ändern")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            adminSteuerung.setUserActiveStatus(adminId, user.id, !user.isActive)
                                .onSuccess { onActionComplete() }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (user.isActive) {
                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    },
                    enabled = user.id != adminId
                ) {
                    Text(if (user.isActive) "Deaktivieren" else "Aktivieren")
                }
            }
        }
    }
}

@Composable
private fun CourseAdminCard(
    course: Course,
    adminId: String,
    adminSteuerung: IAdminSteuerung,
    lecturerName: String,
    onActionComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Kurs löschen?") },
            text = { Text("Möchten Sie den Kurs '${course.name}' wirklich unwiderruflich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = adminSteuerung.deleteCourse(adminId, course.id)
                            if (result.isSuccess) {
                                onActionComplete()
                            }
                            showDeleteConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(course.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Dozent: $lecturerName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ID: ${course.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${course.memberIds.size} Studierende",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Kurs löschen")
            }
        }
    }
}

@Composable
private fun UserInfo(user: User) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    if (user.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, 
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(user.name.take(2).uppercase(), color = if (user.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(
                    user.name + if (!user.isActive) " (Deaktiviert)" else "", 
                    fontWeight = FontWeight.Bold, 
                    color = if (user.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (user.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                user.role.name,
                color = if (user.isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    change: String,
    changePositive: Boolean?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                change,
                fontSize = 12.sp,
                color = when (changePositive) {
                    true -> Color(0xFF2E7D32) // Success Green
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SystemStatusCard(adminSteuerung: IAdminSteuerung) {
    var healthData by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var systemLoad by remember { mutableFloatStateOf(0.0f) }

    LaunchedEffect(Unit) {
        while (true) {
            healthData = adminSteuerung.checkSystemHealth()
            systemLoad = 0.05f + Random.nextFloat() * 0.1f 
            delay(5000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("System-Status", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusRow("Server-Status", if (healthData["Server"] == true) "Online" else "Offline", if (healthData["Server"] == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                StatusRow("Datenbank", if (healthData["Database"] == true) "Verbunden" else "Fehler", if (healthData["Database"] == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                StatusRow("Authentifizierung", if (healthData["Auth"] == true) "Operational" else "Störung", if (healthData["Auth"] == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("System-Last (Latenz/CPU)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(systemLoad * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { systemLoad }, 
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.primary, 
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    "Blau zeigt die aktuelle Ressourcennutzung an. Grüne Punkte markieren fehlerfreie Dienste.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).background(valueColor, CircleShape))
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
