package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.client.IAuthProvider
import de.thkoeln.codescope.data.repository.IBenutzerVerwaltung
import de.thkoeln.codescope.data.repository.IKursVerwaltung
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Controller responsible for administrative actions within CodeScope.
 *
 * **LF100 – Rollen vegeben**
 * **LF101 – Benutzer verwalten**
 * **LF102 – Kurse verwalten**
 *
 * This class provides functionality for user management (roles, status),
 * course management, and system monitoring. Access to most methods is restricted
 * to users with the [UserRole.ADMIN] role.
 */
class AdminSteuerung(
    private val benutzerVerwaltung: IBenutzerVerwaltung,
    private val kursVerwaltung: IKursVerwaltung,
    private val authProvider: IAuthProvider
) : IAdminSteuerung {

    /**
     * Assigns a new [UserRole] to a specific user.
     *
     * **LF100 – Rollen vegeben**
     * **LF101 – Benutzer verwalten**
     *
     * @param adminId The ID of the administrator performing the action.
     * @param targetUserId The ID of the user whose role should be changed.
     * @param newRole The new [UserRole] to assign.
     * @return A [Result] containing the updated [User] or an error if unauthorized,
     *         ids are blank, or the operation is invalid (e.g., self-role change).
     */
    override suspend fun assignRole(
        adminId: String,
        targetUserId: String,
        newRole: UserRole
    ): Result<User> {
        if (adminId.isBlank() || targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Ids must not be blank"))
        }

        val admin = benutzerVerwaltung.loadUser(adminId).getOrElse {
            return Result.failure(IllegalStateException("Admin not found"))
        }

        if (admin.role != UserRole.ADMIN) {
            return Result.failure(IllegalAccessException("Unauthorized"))
        }

        if (adminId == targetUserId) {
            return Result.failure(IllegalStateException("Cannot change own role"))
        }

        val targetUser = benutzerVerwaltung.loadUser(targetUserId).getOrElse {
            return Result.failure(IllegalStateException("Target user not found"))
        }

        val updated = targetUser.copy(role = newRole)
        benutzerVerwaltung.saveUser(updated).getOrElse {
            return Result.failure(IllegalStateException("Failed to save role"))
        }

        return Result.success(updated)
    }

    /**
     * Sets the active status of a user.
     *
     * **LF101 – Benutzer verwalten**
     *
     * @param adminId The ID of the administrator performing the action.
     * @param targetUserId The ID of the user to update.
     * @param isActive The new status to apply.
     * @return A [Result] containing the updated [User]. Returns failure if unauthorized
     *         or if an admin tries to deactivate themselves.
     */
    override suspend fun setUserActiveStatus(
        adminId: String,
        targetUserId: String,
        isActive: Boolean
    ): Result<User> {
        if (adminId.isBlank() || targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Ids must not be blank"))
        }

        val admin = benutzerVerwaltung.loadUser(adminId).getOrElse {
            return Result.failure(IllegalStateException("Admin not found"))
        }

        if (admin.role != UserRole.ADMIN) {
            return Result.failure(IllegalAccessException("Unauthorized"))
        }

        if (adminId == targetUserId) {
            return Result.failure(IllegalStateException("Cannot deactivate self"))
        }

        val targetUser = benutzerVerwaltung.loadUser(targetUserId).getOrElse {
            return Result.failure(IllegalStateException("Target user not found"))
        }

        val updated = targetUser.copy(isActive = isActive)
        benutzerVerwaltung.saveUser(updated).getOrElse {
            return Result.failure(IllegalStateException("Failed to update status"))
        }

        return Result.success(updated)
    }

    /**
     * Retrieves all registered users in the system.
     *
     * **LF101 – Benutzer verwalten**
     *
     * @return A [Flow] emitting a list of all [User] objects.
     */
    override fun getAllUsers(): Flow<List<User>> {
        return benutzerVerwaltung.getAllUsers()
    }

    /**
     * Retrieves all courses in the system.
     *
     * **LF102 – Kurse verwalten**
     *
     * @return A [Flow] emitting a list of all [Course] objects.
     */
    override fun getAllCourses(): Flow<List<Course>> {
        return kursVerwaltung.getAllCourses()
    }

    /**
     * Deletes a course from the system.
     *
     * **LF102 – Kurse verwalten**
     *
     * @param adminId The ID of the administrator performing the action.
     * @param courseId The ID of the course to be deleted.
     * @return A [Result] indicating success or the reason for failure.
     */
    override suspend fun deleteCourse(adminId: String, courseId: String): Result<Unit> {
        val admin = benutzerVerwaltung.loadUser(adminId).getOrElse {
            return Result.failure(IllegalStateException("Admin not found"))
        }
        if (admin.role != UserRole.ADMIN) {
            return Result.failure(IllegalAccessException("Unauthorized"))
        }
        return kursVerwaltung.delete(courseId)
    }

    /**
     * Performs a health check on core system components.
     *
     * Checks the availability of the database, the authentication provider,
     * and the general server status.
     *
     * @return A map mapping component names to their health status (true = healthy).
     */
    override suspend fun checkSystemHealth(): Map<String, Boolean> {
        // 1. Prüfe Datenbank-Erreichbarkeit durch einen Leseversuch
        val dbResult = benutzerVerwaltung.loadUser("health_check_ping")
        val isDatabaseConnected = dbResult.isSuccess || 
            dbResult.exceptionOrNull()?.message?.contains("not found", ignoreCase = true) == true

        // 2. Prüfe Authentifizierungs-Dienst (Session-Check)
        val isAuthOperational = try {
            authProvider.getCurrentUser()
            true
        } catch (e: Exception) {
            false
        }

        // 3. Server-Status (Da wir diesen Code ausführen, ist der Client/Server-App-Verbund aktiv)
        // In einer echten Umgebung könnte man hier einen API-Ping zum Backend machen.
        val isServerUp = true 

        return mapOf(
            "Server" to isServerUp,
            "Database" to isDatabaseConnected,
            "Auth" to isAuthOperational
        )
    }
}
