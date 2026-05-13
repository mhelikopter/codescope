package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.user.User

/**
 * Interface for login, registration and logout operations.
 *
 * Linked LFs:
 * - LF60 Registrieren
 * - LF61 Login / Logout
 */
interface ILoginSteuerung {

    /** Registers a user via Google and creates a profile if needed. */
    suspend fun registerUser(): Result<User>

    /** Logs in an existing user via Google. */
    suspend fun login(): Result<User>

    /** Logs out the currently authenticated user. */
    suspend fun logout(): Result<Unit>

    /** Returns the currently authenticated user from the local session if available. */
    suspend fun getCurrentUser(): User?
}
