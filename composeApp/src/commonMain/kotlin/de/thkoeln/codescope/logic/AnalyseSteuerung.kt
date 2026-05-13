package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.client.IRestClient
import de.thkoeln.codescope.data.repository.IAnalyseVerwaltung
import de.thkoeln.codescope.data.repository.IProjektVerwaltung
import de.thkoeln.codescope.data.repository.IKursVerwaltung
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.analysis.FeedbackItem
import de.thkoeln.codescope.domain.project.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Controller responsible for managing analysis-related logic within CodeScope.
 */
class AnalyseSteuerung(
    private val projektVerwaltung: IProjektVerwaltung,
    private val analyseVerwaltung: IAnalyseVerwaltung,
    private val restClient: IRestClient,
    private val kursVerwaltung: IKursVerwaltung,
    private val notificationService: INotificationService
) : IAnalyseSteuerung {

    override suspend fun startAnalysis(
        projectId: String,
        catalogId: String,
        model: String,
        courseId: String?
    ): Result<String> {
        if (projectId.isBlank()) return Result.failure(IllegalArgumentException("Project id must not be blank"))
        if (catalogId.isBlank()) return Result.failure(IllegalArgumentException("Catalog id must not be blank"))
        if (model.isBlank()) return Result.failure(IllegalArgumentException("Model must not be blank"))

        val projectResult = projektVerwaltung.findById(projectId)
        val project = projectResult.getOrElse { return Result.failure(IllegalStateException("Project not found", it)) }

        if (project.status == ProjectStatus.ANALYSIS_RUNNING) {
            return Result.failure(IllegalStateException("Analysis is already running for this project"))
        }

        val analysisIdResult = restClient.requestAnalysis(projectId, catalogId, model, courseId)
        val analysisId = analysisIdResult.getOrElse { return Result.failure(IllegalStateException("Failed to start analysis in backend", it)) }

        val updatedProject = project.copy(status = ProjectStatus.ANALYSIS_RUNNING)
        projektVerwaltung.save(updatedProject)

        return Result.success(analysisId)
    }

    override suspend fun getAnalysisResult(analysisId: String): Result<AnalysisResult> {
        if (analysisId.isBlank()) return Result.failure(IllegalArgumentException("Analysis id must not be blank"))
        return analyseVerwaltung.loadAnalysis(analysisId)
    }

    override suspend fun getAnalysisHistory(userId: String): Result<List<AnalysisResult>> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User id must not be blank"))
        return analyseVerwaltung.loadAnalysesForUser(userId)
    }

    override suspend fun getAnalysesForCourse(courseId: String): Result<List<AnalysisResult>> {
        if (courseId.isBlank()) return Result.failure(IllegalArgumentException("Course id must not be blank"))
        return analyseVerwaltung.loadAnalysesForCourse(courseId)
    }

    override suspend fun getAnalysesForAllInstructorCourses(instructorId: String): Result<List<AnalysisResult>> {
        return try {
            val courses = kursVerwaltung.loadCoursesByLecturer(instructorId).getOrThrow()
            val allAnalyses = mutableListOf<AnalysisResult>()
            for (course in courses) {
                analyseVerwaltung.loadAnalysesForCourse(course.id).onSuccess {
                    allAnalyses.addAll(it)
                }
            }
            Result.success(allAnalyses.distinctBy { it.id }.sortedByDescending { it.updatedAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAnalysisResult(analysisId: String, requesterId: String): Result<Unit> {
        val analysis = analyseVerwaltung.loadAnalysis(analysisId).getOrElse { return Result.failure(it) }
        val project = projektVerwaltung.findById(analysis.projectId).getOrElse { return Result.failure(it) }

        if (project.ownerId != requesterId) {
            return Result.failure(IllegalAccessException("Only the owner may delete this analysis"))
        }

        analyseVerwaltung.deleteAnalysis(analysisId).getOrElse { return Result.failure(it) }

        val remaining = analyseVerwaltung.loadAnalysesForUser(requesterId).getOrElse { emptyList() }
        if (remaining.none { it.projectId == project.id }) {
            projektVerwaltung.save(project.copy(status = ProjectStatus.UPLOADED))
        }

        return Result.success(Unit)
    }

    override suspend fun startBatchAnalysis(
        projectIds: List<String>,
        catalogId: String,
        model: String,
        resetFeedback: Boolean,
        notifyStudents: Boolean,
        courseId: String?
    ): Result<List<String>> {
        if (projectIds.isEmpty()) return Result.failure(IllegalArgumentException("Project list must not be empty"))
        
        val results = mutableListOf<String>()
        for (projectId in projectIds) {
            val project = projektVerwaltung.findById(projectId).getOrNull()
            if (project == null) continue

            if (project.status == ProjectStatus.ANALYSIS_RUNNING) {
                projektVerwaltung.save(project.copy(status = ProjectStatus.UPLOADED))
            }

            if (resetFeedback) {
                val existingAnalyses = analyseVerwaltung.loadAnalysesForUser(project.ownerId).getOrDefault(emptyList())
                existingAnalyses.filter { it.projectId == projectId }.forEach { analysis ->
                    val resetted = analysis.copy(
                        status = AnalysisStatus.FINISHED,
                        score = null,
                        instructorComment = null,
                        feedback = null,
                        updatedAt = System.currentTimeMillis()
                    )
                    analyseVerwaltung.saveAnalysis(resetted)
                }
            }

            startAnalysis(projectId, catalogId, model, courseId).onSuccess { analysisId ->
                results.add(analysisId)
                if (notifyStudents) {
                    notificationService.showNotification(
                        "Batch-Analyse gestartet",
                        "Für Ihr Projekt ${project.name} wurde eine neue Analyse initiiert."
                    )
                }
            }
        }
        return Result.success(results)
    }

    override suspend fun updateAnalysisResult(
        analysisId: String,
        instructorId: String,
        newStatus: AnalysisStatus,
        newScore: Int?,
        newFeedback: List<FeedbackItem>?,
        instructorComment: String?
    ): Result<AnalysisResult> {
        val analysis = analyseVerwaltung.loadAnalysis(analysisId).getOrElse { return Result.failure(it) }
        
        val updated = analysis.copy(
            status = newStatus,
            score = newScore,
            feedback = newFeedback,
            instructorComment = instructorComment ?: analysis.instructorComment,
            updatedAt = System.currentTimeMillis()
        )

        analyseVerwaltung.saveAnalysis(updated).getOrElse { return Result.failure(it) }

        // Wenn der Dozent die Bewertung abschließt, den Studenten benachrichtigen
        if (newStatus == AnalysisStatus.REVIEWED) {
            notificationService.showNotification(
                "Neue Bewertung verfügbar",
                "Ihr Professor hat Ihr Projekt ${analysis.projectId} bewertet."
            )
        }

        return Result.success(updated)
    }

    override suspend fun linkAnalysisToCourse(
        analysisId: String,
        userId: String,
        courseId: String
    ): Result<Unit> {
        val analysis = analyseVerwaltung.loadAnalysis(analysisId).getOrElse { return Result.failure(it) }
        
        if (analysis.userId != userId) {
            return Result.failure(IllegalAccessException("Only the owner can link this analysis to a course"))
        }

        val updated = analysis.copy(
            courseId = courseId,
            updatedAt = System.currentTimeMillis()
        )

        analyseVerwaltung.saveAnalysis(updated).getOrElse { return Result.failure(it) }
        return Result.success(Unit)
    }

    override fun observeAnalysesForUser(userId: String): Flow<List<AnalysisResult>> {
        return analyseVerwaltung.observeAnalysesForUser(userId)
    }
}
