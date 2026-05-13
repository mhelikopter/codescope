package de.thkoeln.codescope.data.client
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseJvmConfig(
    val apiKey: String,
    val projectId: String,
    val storageBucket: String,
    val functionsRegion: String = "europe-west3"
)

/**
 * Hält den aktuellen Firebase Auth Zustand (idToken/refreshToken/localId).
 * Wird von AuthProviderImpl.jvm gesetzt und von Firestore/Storage/Functions genutzt.
 */
@Serializable
class FirebaseSession {
    var idToken: String? = null
    var refreshToken: String? = null
    var localId: String? = null

    fun clear() {
        idToken = null
        refreshToken = null
        localId = null
    }

    fun requireIdToken(): String =
        idToken ?: error("No Firebase idToken in session. User not authenticated on JVM.")
}
