package de.thkoeln.codescope.data.client

import dev.gitlive.firebase.functions.FirebaseFunctions

class RestClientImpl(
    private val functions: FirebaseFunctions
) : IRestClient {

    override suspend fun requestAnalysis(
        projectId: String,
        catalogId: String,
        modelId: String,
        courseId: String?
    ): Result<String> {
        return try {
            val requestData = mutableMapOf(
                "projectId" to projectId,
                "catalogId" to catalogId,
                "modelId" to modelId
            )
            courseId?.let { requestData["courseId"] = it }

            val function = functions.httpsCallable("analyseCode")
            val result = function.invoke(requestData)

            val fullResponse = result.data<Map<String, String>>()
            val analysisId = fullResponse["analysisId"]

            if (analysisId != null) {
                Result.success(analysisId)
            } else {
                Result.failure(Exception("Backend returned valid response but missing 'analysisId'"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateCriteria(topic: String): Result<List<String>> {
        return try {
            val requestData = mapOf("topic" to topic)
            val function = functions.httpsCallable("generateCriteria")
            val result = function.invoke(requestData)

            val fullResponse = result.data<Map<String, List<String>>>()
            val criteria = fullResponse["criteria"]

            if (criteria != null) {
                Result.success(criteria)
            } else {
                Result.failure(Exception("Backend returned valid response but missing 'criteria'"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
