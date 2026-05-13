package de.thkoeln.codescope.data.client

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RestClientImpl(
    private val http: HttpClient,
    private val config: FirebaseJvmConfig,
    private val session: FirebaseSession
) : IRestClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun requestAnalysis(
        projectId: String,
        catalogId: String,
        modelId: String,
        courseId: String?
    ): Result<String> {
        return try {
            val url = "https://${config.functionsRegion}-${config.projectId}.cloudfunctions.net/analyseCode"

            val response = http.post(url) {
                header("Authorization", "Bearer ${session.requireIdToken()}")
                contentType(ContentType.Application.Json)
                // Das Firebase Callable Protokoll erwartet die Argumente im "data"-Feld
                setBody(mapOf("data" to AnalyseCodeRequest(projectId, catalogId, modelId, courseId)))
            }

            val status = response.status
            val raw = response.bodyAsText()

            println("🔥 JVM RestClient analyseCode HTTP ${status.value}")
            println("🔥 JVM RestClient RAW: $raw")

            if (!status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("analyseCode failed: HTTP ${status.value} $raw")
                )
            }

            val analysisId = extractAnalysisId(raw)
                ?: return Result.failure(
                    IllegalStateException("Backend response missing analysisId. RAW=$raw")
                )

            Result.success(analysisId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateCriteria(topic: String): Result<List<String>> {
        return try {
            val url = "https://${config.functionsRegion}-${config.projectId}.cloudfunctions.net/generateCriteria"

            val response = http.post(url) {
                header("Authorization", "Bearer ${session.requireIdToken()}")
                contentType(ContentType.Application.Json)
                // Firebase Callable Protokoll: Daten in "data" Feld
                setBody(mapOf("data" to mapOf("topic" to topic)))
            }

            val status = response.status
            val raw = response.bodyAsText()

            println("🔥 JVM RestClient generateCriteria HTTP ${status.value}")
            println("🔥 JVM RestClient RAW: $raw")

            if (!status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("generateCriteria failed: HTTP ${status.value} $raw")
                )
            }

            val criteria = extractCriteria(raw)
                ?: return Result.failure(
                    IllegalStateException("Backend response missing criteria. RAW=$raw")
                )

            Result.success(criteria)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Supports these common response shapes:
     * 1) {"analysisId":"..."}
     * 2) {"data":{"analysisId":"..."}}
     * 3) {"result":{"analysisId":"..."}}
     */
    private fun extractAnalysisId(raw: String): String? {
        return try {
            val root = json.parseToJsonElement(raw).jsonObject

            // direct
            root["result"]?.jsonObject?.get("analysisId")?.jsonPrimitive?.contentOrNull
                ?: root["data"]?.jsonObject?.get("analysisId")?.jsonPrimitive?.contentOrNull
                ?: root["analysisId"]?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) {
            // fallback (wenn response kein JSON ist)
            "\"analysisId\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                .find(raw)?.groupValues?.getOrNull(1)
        }
    }

    private fun extractCriteria(raw: String): List<String>? {
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            // Firebase Functions wrappen die Rückgabe oft in "result" oder "data"
            val data = root["result"]?.jsonObject ?: root["data"]?.jsonObject ?: root
            data["criteria"]?.jsonArray?.map { it.jsonPrimitive.content }
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class AnalyseCodeRequest(
        val projectId: String,
        val catalogId: String,
        val modelId: String,
        val courseId: String?
    )
}
