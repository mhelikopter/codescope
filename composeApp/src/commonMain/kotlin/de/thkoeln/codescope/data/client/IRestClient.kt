package de.thkoeln.codescope.data.client

/**
 * Abstraction over the backend (Cloud Functions) that actually starts an analysis.
 */
interface IRestClient {

    /**
     * Requests the backend to start an analysis for the given project and catalog.
     *
     * @return Result with the backend-generated analysis id.
     */
    suspend fun requestAnalysis(
        projectId: String,
        catalogId: String,
        modelId: String,
        courseId: String? = null
    ): Result<String>

    /**
     * Generates criteria suggestions for a given topic using AI via backend.
     */
    suspend fun generateCriteria(topic: String): Result<List<String>>
}
