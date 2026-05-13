package de.thkoeln.codescope

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.thkoeln.codescope.theme.CodeScopeTheme
import de.thkoeln.codescope.theme.ThemeSettings
import de.thkoeln.codescope.screens.*
import de.thkoeln.codescope.components.*
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import de.thkoeln.codescope.logic.*
import de.thkoeln.codescope.data.repository.ISettingsRepository
import de.thkoeln.codescope.viewmodel.AppViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.thkoeln.codescope.data.client.GoogleAuth
import org.koin.compose.koinInject

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object ProjectUpload : Screen()
    data class AnalysisConfig(val userId: String, val project: Project) : Screen()
    data class Results(val projectName: String, val analysisId: String, val isLecturer: Boolean = false) : Screen()
    object AdminPanel : Screen()
    object Courses : Screen()
    object Settings : Screen()
    object CriteriaManagement : Screen()
    data class CourseDetails(val courseId: String, val userId: String) : Screen()
    data class AssessmentManagement(val courseId: String? = null) : Screen()
}

@Composable
fun App(isMobile: Boolean) {
    val loginSteuerung: ILoginSteuerung = koinInject()
    val analyseSteuerung: IAnalyseSteuerung = koinInject()
    val notificationService: INotificationService = koinInject()
    val settingsRepository: ISettingsRepository = koinInject()

    // Initialize ThemeSettings once
    LaunchedEffect(settingsRepository) {
        ThemeSettings.initialize(settingsRepository)
    }

    val viewModel: AppViewModel = viewModel {
        AppViewModel(loginSteuerung, analyseSteuerung, notificationService)
    }

    // Nutze den globalen Dark Mode State
    CodeScopeTheme(darkTheme = ThemeSettings.isDarkMode) {
        if (viewModel.isInitializing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val user = viewModel.currentUser
            if (user == null || viewModel.currentScreen == Screen.Login) {
                if (isMobile) {
                    MobileLoginScreen(
                        loginSteuerung = loginSteuerung,
                        onLoginSuccess = { loggedInUser ->
                            viewModel.login(loggedInUser)
                        }
                    )
                } else {
                    DesktopLoginScreen(
                        loginSteuerung = loginSteuerung,
                        onLoginSuccess = { loggedInUser ->
                            viewModel.login(loggedInUser)
                        }
                    )
                }
            } else {
                if (isMobile) {
                    MobileApp(
                        user = user,
                        viewModel = viewModel
                    )
                } else {
                    DesktopApp(
                        user = user,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopApp(
    user: User,
    viewModel: AppViewModel
) {
    val currentScreen = viewModel.currentScreen

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar nur anzeigen, wenn wir nicht im Login sind und ein User existiert
        if (viewModel.currentUser != null && currentScreen != Screen.Login) {
            CSSidebar(
                currentRoute = viewModel.currentRoute,
                onNavigate = viewModel::navigateByRoute,
                userRole = user.role
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                Screen.Dashboard -> {
                    when (user.role) {
                        UserRole.ADMIN -> {
                            val adminSteuerung: IAdminSteuerung = koinInject()
                            DesktopAdminPanelScreen(
                                adminSteuerung = adminSteuerung,
                                adminId = user.id,
                                onNavigateBack = {}
                            )
                        }
                        UserRole.LECTURER -> {
                            DesktopDashboardTeacherScreen(
                                user = user,
                                onNavigate = viewModel::navigateByRoute,
                                onNavigateToAnalysis = { projectName, analysisId ->
                                    viewModel.navigateToResults(projectName, analysisId, asLecturer = true)
                                }
                            )
                        }
                        UserRole.STUDENT -> {
                            DesktopDashboardScreen(
                                user = user,
                                onNewProject = viewModel::navigateToProjectUpload,
                                onNavigate = viewModel::navigateByRoute,
                                onNavigateToAnalysis = { projectName, analysisId ->
                                    viewModel.navigateToResults(projectName, analysisId)
                                }
                            )
                        }
                    }
                }

                Screen.ProjectUpload -> {
                    val projektSteuerung: IProjektSteuerung = koinInject()
                    val googleAuth: GoogleAuth = koinInject()
                    DesktopProjectUploadScreen(
                        projectSteuerung = projektSteuerung,
                        googleAuth = googleAuth,
                        userId = user.id,
                        onUploadComplete = viewModel::navigateToAnalysisConfig,
                        onBack = viewModel::navigateToHome
                    )
                }

                is Screen.AnalysisConfig -> {
                    val analyseSteuerung: IAnalyseSteuerung = koinInject()
                    val kriterienkatalogSteuerung: IKriterienkatalogSteuerung = koinInject()
                    DesktopAnalysisConfigScreen(
                        analyseSteuerung = analyseSteuerung,
                        kriterienkatalogSteuerung = kriterienkatalogSteuerung,
                        project = currentScreen.project,
                        userId = currentScreen.userId,
                        onStartAnalysis = { analysisId ->
                            viewModel.navigateToResults(currentScreen.project.name, analysisId)
                        },
                        onBack = viewModel::navigateToProjectUpload
                    )
                }

                is Screen.Results -> {
                    val analyseSteuerung: IAnalyseSteuerung = koinInject()
                    DesktopResultsScreen(
                        projectName = currentScreen.projectName,
                        analysisId = currentScreen.analysisId,
                        analyseSteuerung = analyseSteuerung,
                        userId = user.id,
                        isLecturer = currentScreen.isLecturer,
                        onBack = viewModel::navigateToHome,
                        onExport = { /* Export logic */ }
                    )
                }

                Screen.AdminPanel -> {
                    val adminSteuerung: IAdminSteuerung = koinInject()
                    DesktopAdminPanelScreen(
                        adminSteuerung = adminSteuerung,
                        adminId = user.id,
                        onNavigateBack = viewModel::navigateToHome
                    )
                }

                Screen.Courses -> {
                    if(viewModel.isStudent) {
                        val studentKursSteuerung: IStudentKursSteuerung = koinInject()
                        DesktopCourseStudentScreen(
                            studentKursSteuerung = studentKursSteuerung,
                            dozentKursSteuerung = koinInject(),
                            user = user,
                            onCourseClick = viewModel::navigateToCourseDetails
                        )
                    } else {
                        val dozentKursSteuerung: IDozentKursSteuerung = koinInject()
                        val studentKursSteuerung: IStudentKursSteuerung = koinInject()
                        DesktopCourseManagementScreen(
                            dozentKursSteuerung = dozentKursSteuerung,
                            studentKursSteuerung = studentKursSteuerung,
                            user = user,
                            onManageAssessments = { viewModel.navigateToAssessmentManagement() },
                            onCourseClick = viewModel::navigateToCourseDetails
                        )
                    }
                }

                Screen.Settings -> {
                    DesktopSettingsScreen(
                        user = user,
                        onNavigateBack = viewModel::navigateToHome,
                        onLogout = viewModel::logout
                    )
                }

                Screen.CriteriaManagement -> {
                    val kriterienkatalogSteuerung: IKriterienkatalogSteuerung = koinInject()
                    DesktopCriteriaManagementScreen(
                        kriterienkatalogSteuerung = kriterienkatalogSteuerung,
                        userId = user.id,
                        onNavigateBack = viewModel::navigateToHome,
                        onNavigateToAnalysis = { projectName, analysisId ->
                            viewModel.navigateToResults(projectName, analysisId, viewModel.isLecturer)
                        }
                    )
                }

                is Screen.CourseDetails -> {
                    if (viewModel.isStudent) {
                        val studentKursSteuerung: IStudentKursSteuerung = koinInject()
                        val analyseSteuerung: IAnalyseSteuerung = koinInject()
                        DesktopCourseDetailStudentScreen(
                            studentKursSteuerung = studentKursSteuerung,
                            analyseSteuerung = analyseSteuerung,
                            courseId = currentScreen.courseId,
                            userId = currentScreen.userId,
                            onNavigateBack = viewModel::navigateToCourses
                        )
                    } else {
                        val dozentKursSteuerung: IDozentKursSteuerung = koinInject()
                        val analyseSteuerung: IAnalyseSteuerung = koinInject()
                        DesktopCourseDetailTeacherScreen(
                            dozentKursSteuerung = dozentKursSteuerung,
                            analyseSteuerung = analyseSteuerung,
                            courseId = currentScreen.courseId,
                            userId = user.id,
                            onNavigateBack = viewModel::navigateToCourses,
                            onManageAssessments = { viewModel.navigateToAssessmentManagement(currentScreen.courseId) },
                            onNavigateToAnalysis = { projectName, analysisId ->
                                viewModel.navigateToResults(projectName, analysisId, asLecturer = true)
                            }
                        )
                    }
                }

                is Screen.AssessmentManagement -> {
                    val analyseSteuerung: IAnalyseSteuerung = koinInject()
                    DesktopAssessmentManagementScreen(
                        analyseSteuerung = analyseSteuerung,
                        userId = user.id,
                        courseId = currentScreen.courseId,
                        onNavigateBack = viewModel::navigateBack,
                        onNavigateToAnalysis = { projectName, analysisId ->
                            viewModel.navigateToResults(projectName, analysisId, viewModel.isLecturer)
                        }
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun MobileApp(
    user: User,
    viewModel: AppViewModel
) {
    val currentScreen = viewModel.currentScreen

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    Screen.Dashboard -> {
                        when (user.role) {
                            UserRole.ADMIN -> {
                                val adminSteuerung: IAdminSteuerung = koinInject()
                                MobileAdminPanelScreen(
                                    adminSteuerung = adminSteuerung,
                                    adminId = user.id,
                                    onNavigateBack = {}
                                )
                            }
                            UserRole.LECTURER -> {
                                DashboardTeacherScreen(
                                    user = user,
                                    onNavigateToCourses = { viewModel.navigateByRoute("courses") },
                                    onNavigateToAssessments = { viewModel.navigateByRoute("grading") },
                                    onNavigateToSettings = { viewModel.navigateByRoute("settings") },
                                    onNavigateToAnalysis = { projectName, analysisId ->
                                        viewModel.navigateToResults(projectName, analysisId, asLecturer = true)
                                    },
                                    isMobile = true
                                )
                            }
                            UserRole.STUDENT -> {
                                MobileDashboardScreen(
                                    user = user,
                                    onNavigate = viewModel::navigateByRoute,
                                    onNavigateToAnalysis = { projectName, analysisId ->
                                        viewModel.navigateToResults(projectName, analysisId)
                                    }
                                )
                            }
                        }
                    }

                    Screen.AdminPanel -> {
                        val adminSteuerung: IAdminSteuerung = koinInject()
                        MobileAdminPanelScreen(
                            adminSteuerung = adminSteuerung,
                            adminId = user.id,
                            onNavigateBack = viewModel::navigateToHome
                        )
                    }

                    is Screen.Results -> {
                        val analyseSteuerung: IAnalyseSteuerung = koinInject()
                        MobileResultsScreen(
                            projectName = currentScreen.projectName,
                            analysisId = currentScreen.analysisId,
                            analyseSteuerung = analyseSteuerung,
                            userId = user.id,
                            isLecturer = currentScreen.isLecturer,
                            onBack = viewModel::navigateToHome
                        )
                    }

                    Screen.ProjectUpload -> {
                        val projektSteuerung: IProjektSteuerung = koinInject()
                        val googleAuth: GoogleAuth = koinInject()
                        MobileProjectUploadScreen(
                            projectSteuerung = projektSteuerung,
                            googleAuth = googleAuth,
                            userId = user.id,
                            onUploadComplete = viewModel::navigateToAnalysisConfig,
                            onBack = viewModel::navigateToHome
                        )
                    }

                    is Screen.AnalysisConfig -> {
                        val analyseSteuerung: IAnalyseSteuerung = koinInject()
                        val kriterienkatalogSteuerung: IKriterienkatalogSteuerung = koinInject()
                        MobileAnalysisConfigScreen(
                            analyseSteuerung = analyseSteuerung,
                            kriterienkatalogSteuerung = kriterienkatalogSteuerung,
                            project = currentScreen.project,
                            userId = currentScreen.userId,
                            onStartAnalysis = { analysisId ->
                                viewModel.navigateToResults(currentScreen.project.name, analysisId)
                            },
                            onBack = viewModel::navigateToProjectUpload
                        )
                    }

                    Screen.CriteriaManagement -> {
                        val kriterienkatalogSteuerung: IKriterienkatalogSteuerung = koinInject()
                        MobileCriteriaManagementScreen(
                            kriterienkatalogSteuerung = kriterienkatalogSteuerung,
                            userId = user.id,
                            onNavigateBack = viewModel::navigateToHome,
                            onNavigateToAnalysis = { projectName, analysisId ->
                                viewModel.navigateToResults(projectName, analysisId, viewModel.isLecturer)
                            }
                        )
                    }

                    Screen.Courses -> {
                        if (viewModel.isStudent) {
                            val studentKursSteuerung: IStudentKursSteuerung = koinInject()
                            MobileCourseStudentScreen(
                                studentKursSteuerung = studentKursSteuerung,
                                dozentKursSteuerung = koinInject(),
                                user = user,
                                onCourseClick = viewModel::navigateToCourseDetails
                            )
                        } else {
                            val dozentKursSteuerung: IDozentKursSteuerung = koinInject()
                            val studentKursSteuerung: IStudentKursSteuerung = koinInject()
                            MobileCourseManagementScreen(
                                dozentKursSteuerung = dozentKursSteuerung,
                                studentKursSteuerung = studentKursSteuerung,
                                user = user,
                                onManageAssessments = { viewModel.navigateToAssessmentManagement() },
                                onCourseClick = viewModel::navigateToCourseDetails
                            )
                        }
                    }

                    is Screen.CourseDetails -> {
                        if (viewModel.isStudent) {
                            val studentKursSteuerung: IStudentKursSteuerung = koinInject()
                            val analyseSteuerung: IAnalyseSteuerung = koinInject()
                            MobileCourseDetailStudentScreen(
                                studentKursSteuerung = studentKursSteuerung,
                                analyseSteuerung = analyseSteuerung,
                                courseId = currentScreen.courseId,
                                userId = currentScreen.userId,
                                onNavigateBack = viewModel::navigateToCourses
                            )
                        } else {
                            val dozentKursSteuerung: IDozentKursSteuerung = koinInject()
                            val analyseSteuerung: IAnalyseSteuerung = koinInject()
                            MobileCourseDetailTeacherScreen(
                                dozentKursSteuerung = dozentKursSteuerung,
                                analyseSteuerung = analyseSteuerung,
                                courseId = currentScreen.courseId,
                                userId = user.id,
                                onNavigateBack = viewModel::navigateToCourses,
                                onManageAssessments = { viewModel.navigateToAssessmentManagement(currentScreen.courseId) },
                                onNavigateToAnalysis = { projectName, analysisId ->
                                    viewModel.navigateToResults(projectName, analysisId, asLecturer = true)
                                }
                            )
                        }
                    }

                    is Screen.AssessmentManagement -> {
                        val analyseSteuerung: IAnalyseSteuerung = koinInject()
                        MobileAssessmentManagementScreen(
                            analyseSteuerung = analyseSteuerung,
                            userId = user.id,
                            courseId = currentScreen.courseId,
                            onNavigateBack = viewModel::navigateBack,
                            onNavigateToAnalysis = { projectName, analysisId ->
                                viewModel.navigateToResults(projectName, analysisId, viewModel.isLecturer)
                            }
                        )
                    }

                    Screen.Settings -> {
                        MobileSettingsScreen(
                            user = user,
                            onNavigateBack = viewModel::navigateToHome,
                            onLogout = viewModel::logout
                        )
                    }

                    else -> {}
                }
            }
            if (currentScreen !is Screen.Results && viewModel.currentUser != null && currentScreen != Screen.Login) {
                MobileBottomNav(
                    currentRoute = viewModel.currentRoute,
                    userRole = user.role,
                    onNavigate = viewModel::navigateByRoute
                )
            }
        }
    }
}
