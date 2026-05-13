package de.thkoeln.codescope.data.client

import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class AuthProviderImpl(
    private val http: HttpClient,
    private val googleAuth: GoogleAuth,
    private val config: FirebaseJvmConfig,
    private val session: FirebaseSession,
    private val firestoreClient: IFirestoreClient
) : IAuthProvider {

    private val sessionFile = File(System.getProperty("user.home"), ".codescope_session.json")

    private fun saveSession() {
        try {
            val jsonText = Json.encodeToString(session)
            sessionFile.writeText(jsonText)
            println("JVM Auth: Sitzung gespeichert unter ${sessionFile.absolutePath}")
        } catch (e: Exception) {
            println("JVM Auth ERROR: Sitzung konnte nicht gespeichert werden: ${e.message}")
        }
    }

    override suspend fun signInWithGoogle(): Result<User> = try {
        println(
            "JVM Firebase Config → " +
                    "apiKeyLen=${config.apiKey.length}, " +
                    "apiKeyLast4=${config.apiKey.takeLast(4)}, " +
                    "projectId=${config.projectId}, " +
                    "bucket=${config.storageBucket}"
        )
        val credential = googleAuth.getGoogleIdToken()

        val postBody = when {
            credential.idToken?.isNotBlank() == true ->
                "id_token=${credential.idToken}&providerId=google.com"

            credential.accessToken?.isNotBlank() == true ->
                "access_token=${credential.accessToken}&providerId=google.com"

            else ->
                return Result.failure(
                    IllegalStateException(
                        "GoogleAuthCredential has neither idToken nor accessToken"
                    )
                )
        }

        val resp = firebaseSignInWithGooglePostBody(postBody)
        println("JVM Auth: firebase signInWithIdp OK uid=${resp.localId}")

        session.idToken = resp.idToken
        session.refreshToken = resp.refreshToken
        session.localId = resp.localId

        saveSession()

        // Versuche das Profil aus Firestore zu laden
        val appUser = try {
            firestoreClient.getDocument("users/${resp.localId}", User.serializer())
        } catch (e: Exception) {
            println("JVM Auth: Fehler beim Laden des Users aus Firestore (vielleicht neu?): ${e.message}")
            null
        }

        // Falls der User nicht existiert, gib ein temporäres Objekt zurück (für registerUser flow)
        val finalUser = appUser ?: User(
            id = resp.localId,
            name = resp.displayName ?: "Unbekannter Nutzer",
            email = resp.email ?: "",
            role = UserRole.STUDENT
        )

        println("JVM Auth: final user profile prepared (exists in firestore: ${appUser != null})")
        Result.success(finalUser)
    } catch (e: Exception) {
        println("JVM Auth ERROR: ${e.message}")
        Result.failure(Exception("JVM sign-in failed: ${e.message}", e))
    }

    override suspend fun signOut(): Result<Unit> = try {
        session.clear()
        if (sessionFile.exists()) sessionFile.delete()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCurrentUser(): User? {
        val uid = session.localId ?: return null

        if (session.refreshToken != null) {
            refreshFirebaseToken()
        }

        return try {
            firestoreClient.getDocument("users/$uid", User.serializer())
        } catch (e: Exception) {
            println("JVM Auth: Fehler beim Laden des Profils: ${e.message}")
            null
        }
    }

    private suspend fun refreshFirebaseToken() {
        try {
            val url = "https://securetoken.googleapis.com/v1/token?key=${config.apiKey}"

            val response = http.post(url) {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to session.refreshToken
                ))
            }

            if (response.status.isSuccess()) {
                val data = response.body<Map<String, String>>()
                session.idToken = data["id_token"]
                session.refreshToken = data["refresh_token"]
                saveSession()
            }
        } catch (e: Exception) {
            println("JVM Auth: Fehler beim Token-Refresh: ${e.message}")
        }
    }

    private suspend fun firebaseSignInWithGooglePostBody(postBody: String): FirebaseSignInResponse {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=${config.apiKey}"

        val payload = FirebaseSignInWithIdpRequest(
            postBody = postBody,
            requestUri = "http://localhost",
            returnIdpCredential = true,
            returnSecureToken = true
        )

        val response = http.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        val status = response.status
        val rawBody = response.bodyAsText()

        if (!status.isSuccess()) {
            throw IllegalStateException("Firebase sign-in failed: $rawBody")
        }

        return Json { ignoreUnknownKeys = true }.decodeFromString(rawBody)
    }

    @Serializable
    private data class FirebaseSignInWithIdpRequest(
        @SerialName("postBody") val postBody: String,
        @SerialName("requestUri") val requestUri: String,
        @SerialName("returnIdpCredential") val returnIdpCredential: Boolean,
        @SerialName("returnSecureToken") val returnSecureToken: Boolean
    )

    @Serializable
    private data class FirebaseSignInResponse(
        @SerialName("idToken") val idToken: String,
        @SerialName("refreshToken") val refreshToken: String,
        @SerialName("localId") val localId: String,
        @SerialName("email") val email: String? = null,
        @SerialName("displayName") val displayName: String? = null
    )
}
