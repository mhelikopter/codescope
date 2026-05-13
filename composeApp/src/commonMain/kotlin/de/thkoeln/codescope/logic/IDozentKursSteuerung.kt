package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.course.Course
import kotlinx.coroutines.flow.Flow

/**
 * Interface for course operations performed by lecturers.
 */
interface IDozentKursSteuerung {

    /**
     * Creates a new course owned by a lecturer.
     */
    suspend fun createCourse(
        lecturerId: String,
        courseName: String
    ): Result<Course>

    /**
     * Retrieves a specific course by its ID.
     */
    suspend fun getCourse(courseId: String): Result<Course>

    /**
     * Deletes a course.
     */
    suspend fun deleteCourse(courseId: String): Result<Unit>

    /**
     * Retrieves all courses.
     */
    fun getAllCourses():  Flow<List<Course>>
}