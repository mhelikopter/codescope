package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.course.Course
import kotlinx.coroutines.flow.Flow

/**
 * Interface for all course interactions performed by students.
 */
interface IStudentKursSteuerung {

    suspend fun joinCourse(courseId: String, studentId: String): Result<Course>

    suspend fun getInstructorCourses(lecturerId: String): Result<List<Course>>

    suspend fun removeStudentFromCourse(
        courseId: String,
        instructorId: String,
        studentId: String
    ): Result<Unit>

    suspend fun leaveCourse(courseId: String, studentId: String): Result<Unit>

    fun getStudentCourses(studentId: String): Flow<List<Course>>

    /**
     * Links an existing analysis to a course.
     */
    suspend fun submitAnalysisToCourse(analysisId: String, studentId: String, courseId: String): Result<Unit>
}
