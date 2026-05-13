package de.thkoeln.codescope.data.client

import de.thkoeln.codescope.domain.user.User

interface IAuthProvider {

    /**
     * Performs Google sign-in and returns basic user information.
     */
    suspend fun signInWithGoogle(): Result<User>

    /**
     * Signs out the current user.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Returns the currently authenticated user from the local session if available.
     */
    suspend fun getCurrentUser(): User?
}
