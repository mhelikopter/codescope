package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.analysis.FeedbackItem
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the analysis logic component.
 */
interface IAnalyseSteuerung {

    suspend fun startAnalysis(
        projectId: String, 
        catalogId: String, 
        model: String,
        courseId: String? = null
    ): Result<String>

    suspend fun getAnalysisResult(analysisId: String): Result<AnalysisResult>

    suspend fun getAnalysisHistory(userId: String): Result<List<AnalysisResult>>

    suspend fun getAnalysesForCourse(courseId: String): Result<List<AnalysisResult>>

    suspend fun getAnalysesForAllInstructorCourses(instructorId: String): Result<List<AnalysisResult>>

    suspend fun deleteAnalysisResult(analysisId: String, requesterId: String): Result<Unit>

    suspend fun startBatchAnalysis(
        projectIds: List<String>,
        catalogId: String,
        model: String,
        resetFeedback: Boolean = false,
        notifyStudents: Boolean = true,
        courseId: String? = null
    ): Result<List<String>>

    suspend fun updateAnalysisResult(
        analysisId: String,
        instructorId: String,
        newStatus: AnalysisStatus,
        newScore: Int?,
        newFeedback: List<FeedbackItem>?,
        instructorComment: String? = null
    ): Result<AnalysisResult>

    suspend fun linkAnalysisToCourse(
        analysisId: String,
        userId: String,
        courseId: String
    ): Result<Unit>

    fun observeAnalysesForUser(userId: String): Flow<List<AnalysisResult>>
}
