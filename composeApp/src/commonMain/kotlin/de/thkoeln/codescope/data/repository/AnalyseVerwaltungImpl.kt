package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.data.client.IFirestoreClient
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import kotlinx.coroutines.flow.Flow

class AnalyseVerwaltungImpl(private val firestoreClient: IFirestoreClient) : IAnalyseVerwaltung {

    override suspend fun saveAnalysis(result: AnalysisResult): Result<Unit> {
        val path = "analysis/${result.id}"
        return firestoreClient.setDocument(path, result, AnalysisResult.serializer())
    }

    override suspend fun loadAnalysis(analysisId: String): Result<AnalysisResult> {
        val path = "analysis/$analysisId"
        val result = firestoreClient.getDocument(path, AnalysisResult.serializer())
        return if (result != null) {
            Result.success(result)
        } else {
            Result.failure(Exception("Analysis not found"))
        }
    }

    override suspend fun loadAnalysesForUser(userId: String): Result<List<AnalysisResult>> {
        val path = "analysis"
        return firestoreClient.getDocumentsByQuery(path, "userId", userId, AnalysisResult.serializer())
    }

    override suspend fun loadAnalysesForCourse(courseId: String): Result<List<AnalysisResult>> {
        val path = "analysis"
        return firestoreClient.getDocumentsByQuery(path, "courseId", courseId, AnalysisResult.serializer())
    }

    override suspend fun deleteAnalysis(analysisId: String): Result<Unit> {
        val path = "analysis/$analysisId"
        return firestoreClient.deleteDocument(path)
    }

    override fun observeAnalysesForUser(userId: String): Flow<List<AnalysisResult>> {
        return firestoreClient.observeCollectionByQuery("analysis", "userId", userId, AnalysisResult.serializer())
    }
}
