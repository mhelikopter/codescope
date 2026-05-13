package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.client.IAuthProvider
import de.thkoeln.codescope.data.repository.IBenutzerVerwaltung
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole

/**
 * Controller responsible for all user-related authentication and profile logic.
 *
 * **LF60 – Registrieren**
 * **LF61 – Einloggen/Ausloggen**
 *
 * This class orchestrates the interaction between the authentication provider (e.g., Google Auth)
 * and the user repository to handle registration, login, logout, and session management.
 */
class LoginSteuerung(
    private val authProvider: IAuthProvider,
    private val benutzerVerwaltung: IBenutzerVerwaltung
) : ILoginSteuerung{

    /**
     * Registers a new user or logs in an existing one via Google.
     *
     * **LF60 – Registrieren**
     *
     * If the user already exists, their profile is loaded. If they are new, a default profile
     * with the [UserRole.STUDENT] role is created.
     *
     * @return A [Result] containing the [User] object.
     *         Failure occurs if the Google sign-in fails, the account is deactivated,
     *         or the profile cannot be saved.
     */
    override suspend fun registerUser(): Result<User> {
        val loginResult = authProvider.signInWithGoogle()
        val user = loginResult.getOrElse { error ->
            return Result.failure(IllegalStateException("Google sign-in failed", error))
        }

        val existingUser = benutzerVerwaltung.loadUser(user.id).getOrNull()
        
        // Wenn der Nutzer existiert, aber deaktiviert ist -> Registrierung/Login abbrechen
        if (existingUser != null && !existingUser.isActive) {
            return Result.failure(IllegalStateException("Account is deactivated. Please contact an admin."))
        }

        val finalUser = existingUser ?: user.copy(role = UserRole.STUDENT, isActive = true)

        val saveResult = benutzerVerwaltung.saveUser(finalUser)
        saveResult.getOrElse { error ->
            return Result.failure(IllegalStateException("Could not save user profile", error))
        }

        return Result.success(finalUser)
    }

    /**
     * Logs in an existing user via Google.
     *
     * **LF61 – Einloggen/Ausloggen**
     *
     * Unlike [registerUser], this method ensures the user already has a registered profile
     * in the system before allowing access.
     *
     * @return A [Result] containing the [User] object.
     *         Failure occurs if Google sign-in fails, the user is not registered,
     *         or the account is deactivated.
     */
    override suspend fun login(): Result<User> {
        val loginResult = authProvider.signInWithGoogle()
        val account = loginResult.getOrElse { error ->
            return Result.failure(IllegalStateException("Google sign-in failed", error))
        }

        val userResult = benutzerVerwaltung.loadUser(account.id)
        return userResult.mapCatching { user ->
            if (!user.isActive) {
                throw IllegalStateException("Account is deactivated.")
            }
            user
        }.recoverCatching { error ->
            if (error.message?.contains("deactivated") == true) throw error
            throw IllegalStateException("User not registered. Please register first.")
        }
    }

    /**
     * Signs the current user out of the application.
     *
     * **LF61 – Einloggen/Ausloggen**
     *
     * @return A [Result] indicating success or failure.
     */
    override suspend fun logout(): Result<Unit> {
        return authProvider.signOut()
    }

    /**
     * Retrieves the currently authenticated user's profile.
     *
     * This method performs an additional check against the database to ensure the
     * account hasn't been deactivated since the session started.
     *
     * @return The [User] object if authenticated and active, or null otherwise.
     */
    override suspend fun getCurrentUser(): User? {
        val user = authProvider.getCurrentUser()
        return if (user != null) {
            // Nochmal prüfen ob im Hintergrund deaktiviert wurde
            val profile = benutzerVerwaltung.loadUser(user.id).getOrNull()
            if (profile?.isActive == false) null else profile ?: user
        } else null
    }
}
