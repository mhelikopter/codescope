package de.thkoeln.codescope.data.client

import de.thkoeln.codescope.domain.googleAuth.GoogleAuthCredential

expect class GoogleAuth(context: Any = Unit) {
    suspend fun getGoogleIdToken(): GoogleAuthCredential
    suspend fun signOut()
}