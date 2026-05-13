package de.thkoeln.codescope.data.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirebaseStorageClientImpl(
    private val http: HttpClient,
    private val config: FirebaseJvmConfig,
    private val session: FirebaseSession
) : IFirebaseStorageClient {

    // Helper für konsistentes Pfad-Encoding
    private fun encodePath(path: String): String =
        java.net.URLEncoder.encode(path.trim('/'), "UTF-8").replace("+", "%20")

    override suspend fun uploadFile(path: String, data: ByteArray): Result<Unit> = try {
        val bucket = config.storageBucket.trim()
        if (bucket.isEmpty()) throw IllegalStateException("Storage Bucket ist leer!")

        val encodedName = encodePath(path)
        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o?uploadType=media&name=$encodedName"

        println("JVM Storage: Starte Upload zu $url")

        val response = http.post(url) {
            header("Authorization", "Bearer ${session.requireIdToken()}")
            contentType(ContentType.Application.OctetStream)
            setBody(data)
        }

        if (response.status.isSuccess()) {
            println("JVM Storage: Upload erfolgreich -> $path")
            Result.success(Unit)
        } else {
            val errorBody = response.bodyAsText()
            println("JVM Storage: Upload FEHLGESCHLAGEN (${response.status}) -> $errorBody")
            Result.failure(IllegalStateException("Upload failed: ${response.status} $errorBody"))
        }
    } catch (e: Exception) {
        println("JVM Storage: Exception beim Upload -> ${e.message}")
        Result.failure(e)
    }

    override suspend fun deleteFile(path: String): Result<Unit> = try {
        val encodedName = encodePath(path)
        val url = "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o/$encodedName"

        val response = http.delete(url) {
            header("Authorization", "Bearer ${session.requireIdToken()}")
        }

        if (response.status.isSuccess()) Result.success(Unit)
        else Result.failure(IllegalStateException("Delete failed: ${response.status}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFile(path: String): Result<String> = try {
        val encodedName = encodePath(path)
        val metaUrl = "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o/$encodedName"

        val response = http.get(metaUrl) {
            header("Authorization", "Bearer ${session.requireIdToken()}")
        }

        if (response.status.isSuccess()) {
            val meta: JsonObject = response.body()
            val tokens = meta["downloadTokens"]?.jsonPrimitive?.content
            val token = tokens?.split(",")?.firstOrNull()

            val downloadUrl = if (token != null) {
                "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o/$encodedName?alt=media&token=$token"
            } else {
                "https://firebasestorage.googleapis.com/v0/b/${config.storageBucket}/o/$encodedName?alt=media"
            }
            Result.success(downloadUrl)
        } else {
            Result.failure(IllegalStateException("Get meta failed: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
