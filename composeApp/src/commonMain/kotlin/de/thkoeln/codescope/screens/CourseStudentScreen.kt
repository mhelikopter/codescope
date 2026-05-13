package de.thkoeln.codescope.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thkoeln.codescope.components.StandardScreenLayout
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.logic.IDozentKursSteuerung
import de.thkoeln.codescope.logic.IStudentKursSteuerung
import de.thkoeln.codescope.viewmodel.CourseStudentViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.tlaster.precompose.navigation.BackHandler

@Composable
fun DesktopCourseStudentScreen(
    studentKursSteuerung: IStudentKursSteuerung,
    dozentKursSteuerung: IDozentKursSteuerung,
    user: User,
    onCourseClick: (String) -> Unit
) {
    CourseStudentContent(studentKursSteuerung, dozentKursSteuerung, user, onCourseClick, isMobile = false)
}

@Composable
fun MobileCourseStudentScreen(
    studentKursSteuerung: IStudentKursSteuerung,
    dozentKursSteuerung: IDozentKursSteuerung,
    user: User,
    onCourseClick: (String) -> Unit
) {
    CourseStudentContent(studentKursSteuerung, dozentKursSteuerung, user, onCourseClick, isMobile = true)
}

@Composable
private fun CourseStudentContent(
    studentKursSteuerung: IStudentKursSteuerung,
    dozentKursSteuerung: IDozentKursSteuerung,
    user: User,
    onCourseClick: (String) -> Unit,
    isMobile: Boolean
) {
    val viewModel: CourseStudentViewModel = viewModel {
        CourseStudentViewModel(studentKursSteuerung, dozentKursSteuerung)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle System Back Button
    BackHandler(enabled = true) {
        // Da wir im Dashboard starten, gehen wir hier standardmäßig nicht weiter zurück
        // aber wir stellen sicher, dass kein Crash passiert.
    }

    LaunchedEffect(user.id) {
        viewModel.loadCourses(user.id)
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

    StandardScreenLayout(
        title = "Kurse",
        isMobile = isMobile,
        snackbarHostState = snackbarHostState
    ) {
        ActionBar(viewModel.selectedTab, { viewModel.selectTab(it) }, viewModel.searchQuery, { viewModel.updateSearchQuery(it) })

        if (viewModel.isJoining) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (viewModel.displayCourses.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (viewModel.selectedTab == 0) "Du bist noch in keinem Kurs." else "Keine weiteren Kurse verfügbar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                viewModel.displayCourses.forEach { course ->
                    CourseCard(
                        course = course,
                        isEnrolled = viewModel.selectedTab == 0,
                        onAction = {
                            if (viewModel.selectedTab == 1) {
                                viewModel.joinCourse(course.id, user.id)
                            } else {
                                onCourseClick(course.id)
                            }
                        },
                        isMobile = isMobile
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }) { Text("Meine Kurse", modifier = Modifier.padding(12.dp)) }
            Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }) { Text("Verfügbar", modifier = Modifier.padding(12.dp)) }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Kurse suchen...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun CourseCard(
    course: Course,
    isEnrolled: Boolean,
    onAction: () -> Unit,
    isMobile: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onAction() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("ID: ${course.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isEnrolled) {
                Button(onClick = onAction) {
                    Text("Beitreten")
                }
            } else if (!isMobile) {
                TextButton(onClick = onAction) {
                    Text("Öffnen →")
                }
            }
        }
    }
}
