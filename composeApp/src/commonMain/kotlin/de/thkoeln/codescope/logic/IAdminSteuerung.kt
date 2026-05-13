package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.user.User
import de.thkoeln.codescope.domain.user.UserRole
import kotlinx.coroutines.flow.Flow

interface IAdminSteuerung {
    suspend fun assignRole(
        adminId: String,
        targetUserId: String,
        newRole: UserRole
    ): Result<User>

    suspend fun setUserActiveStatus(
        adminId: String,
        targetUserId: String,
        isActive: Boolean
    ): Result<User>

    fun getAllUsers(): Flow<List<User>>

    /**
     * Returns a flow of all courses in the system.
     */
    fun getAllCourses(): Flow<List<Course>>

    /**
     * Deletes a course from the system.
     */
    suspend fun deleteCourse(adminId: String, courseId: String): Result<Unit>

    /**
     * Checks if the backend services are reachable.
     */
    suspend fun checkSystemHealth(): Map<String, Boolean>
}
