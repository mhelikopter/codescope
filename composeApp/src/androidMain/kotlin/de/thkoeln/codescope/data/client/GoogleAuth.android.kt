package de.thkoeln.codescope.data.client

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import de.thkoeln.codescope.R
import de.thkoeln.codescope.domain.googleAuth.GoogleAuthCredential

actual class GoogleAuth actual constructor(val context: Any) {

    private fun context(): Context = context as Context
    private val credentialManager = CredentialManager.Companion.create(context())

    actual suspend fun getGoogleIdToken(): GoogleAuthCredential {
        // Create Google ID option with your web client ID
        val googleIdOption = GetGoogleIdOption.Builder()
            // IMPORTANT: Use your server's client ID, not Android client ID
            .setServerClientId(context().getString(R.string.default_web_client_id))
            // Set to false to show all Google accounts
            .setFilterByAuthorizedAccounts(false)
            .build()
        // Create credential request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        try {
            // Launch Google Sign-In UI
            val result = credentialManager.getCredential(
                context = context(),
                request = request
            )

            val credential = result.credential

            // Verify credential type
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleIdTokenCredential = GoogleIdTokenCredential.Companion
                    .createFrom(credential.data)

                return GoogleAuthCredential(
                    idToken = googleIdTokenCredential.idToken,
                    accessToken = null // Credential Manager doesn't provide access token
                )
            } else {
                return GoogleAuthCredential()
            }
        } catch (e: GetCredentialException) {
            println("GoogleAuth: GetCredentialException: ${e.message}")
            return GoogleAuthCredential()
        }
    }

    actual suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            println("GoogleAuth: Error clearing credential state: ${e.message}")
        }
    }
}