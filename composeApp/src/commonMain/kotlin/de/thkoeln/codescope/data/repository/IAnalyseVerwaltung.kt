package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.domain.analysis.AnalysisResult
import kotlinx.coroutines.flow.Flow

interface IAnalyseVerwaltung {

    suspend fun saveAnalysis(result: AnalysisResult): Result<Unit>

    suspend fun loadAnalysis(analysisId: String): Result<AnalysisResult>

    suspend fun loadAnalysesForUser(userId: String): Result<List<AnalysisResult>>

    suspend fun loadAnalysesForCourse(courseId: String): Result<List<AnalysisResult>>

    suspend fun deleteAnalysis(analysisId: String): Result<Unit>

    fun observeAnalysesForUser(userId: String): Flow<List<AnalysisResult>>
}
