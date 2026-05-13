package de.thkoeln.codescope.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import de.thkoeln.codescope.viewmodel.CourseManagementViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.tlaster.precompose.navigation.BackHandler

/**
 * Composable for the course management screen on desktop.
 */
@Composable
fun DesktopCourseManagementScreen(
    dozentKursSteuerung: IDozentKursSteuerung,
    studentKursSteuerung: IStudentKursSteuerung,
    user: User,
    onManageAssessments: () -> Unit,
    onCourseClick: (String) -> Unit
) {
    CourseManagementContent(dozentKursSteuerung, studentKursSteuerung, user, onManageAssessments, onCourseClick, isMobile = false)
}

/**
 * Composable for the course management screen on mobile.
 */
@Composable
fun MobileCourseManagementScreen(
    dozentKursSteuerung: IDozentKursSteuerung,
    studentKursSteuerung: IStudentKursSteuerung,
    user: User,
    onManageAssessments: () -> Unit,
    onCourseClick: (String) -> Unit
) {
    CourseManagementContent(dozentKursSteuerung, studentKursSteuerung, user, onManageAssessments, onCourseClick, isMobile = true)
}

@Composable
private fun CourseManagementContent(
    dozentKursSteuerung: IDozentKursSteuerung,
    studentKursSteuerung: IStudentKursSteuerung,
    user: User,
    onManageAssessments: () -> Unit,
    onCourseClick: (String) -> Unit,
    isMobile: Boolean
) {
    val viewModel: CourseManagementViewModel = viewModel {
        CourseManagementViewModel(dozentKursSteuerung, studentKursSteuerung)
    }

    // Handle System Back Button
    BackHandler(enabled = true) {
        // Return to dashboard/home is the default expectation for this screen
    }

    LaunchedEffect(user.id) {
        viewModel.loadCourses(user.id)
    }

    if (viewModel.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeCreateDialog() },
            title = { Text("Neuen Kurs erstellen") },
            text = {
                OutlinedTextField(
                    value = viewModel.newCourseName,
                    onValueChange = { viewModel.updateNewCourseName(it) },
                    label = { Text("Kursname") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.createCourse(user.id) }) { Text("Erstellen") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeCreateDialog() }) { Text("Abbrechen") }
            }
        )
    }

    StandardScreenLayout(title = "Kursverwaltung", isMobile = isMobile) {
        ActionBar(
            onCreateClick = { viewModel.openCreateDialog() },
            isMobile = isMobile
        )
        CourseList(
            courses = viewModel.courses,
            isMobile = isMobile,
            user = user,
            onCourseClick = onCourseClick,
            onDeleteCourse = { courseId ->
                viewModel.deleteCourse(user.id, courseId)
            }
        )
    }
}

@Composable
private fun ActionBar(
    onCreateClick: () -> Unit,
    isMobile: Boolean
) {
    if (isMobile) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Neuer Kurs")
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Neuer Kurs")
            }
        }
    }
}

@Composable
private fun CourseList(
    courses: List<Course>,
    isMobile: Boolean,
    user: User,
    onCourseClick: (String) -> Unit,
    onDeleteCourse: (String) -> Unit
) {
    if (courses.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Noch keine Kurse erstellt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            courses.forEach { course ->
                CourseManagementCard(
                    course = course,
                    isMobile = isMobile,
                    user = user,
                    onManage = { onCourseClick(course.id) },
                    onDelete = { onDeleteCourse(course.id) }
                )
            }
        }
    }
}

@Composable
private fun CourseManagementCard(
    course: Course,
    isMobile: Boolean,
    user: User,
    onManage: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = course.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ID: ${course.id} • Studierende: ${course.memberIds.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onManage, modifier = Modifier.weight(1f)) { Text("Verwalten") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Löschen") }
            }
        }
    }
}
