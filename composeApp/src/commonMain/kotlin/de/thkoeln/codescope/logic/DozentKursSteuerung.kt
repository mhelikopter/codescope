package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.repository.IKursVerwaltung
import de.thkoeln.codescope.domain.course.Course
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/**
 * Controller responsible for course-related operations performed by instructors.
 *
 * **LF70 – Kurs anlegen**
 * **LF72 – Dozenten Kursansicht anzeigen**
 *
 * This class provides functionality for creating, retrieving, and managing courses.
 * It acts as an orchestration layer between the UI and the [IKursVerwaltung] repository.
 */
class DozentKursSteuerung(
    private val kursVerwaltung: IKursVerwaltung
) : IDozentKursSteuerung {

    /**
     * Creates a new course with the specified name and assigns it to a lecturer.
     *
     * **LF70 – Kurs anlegen**
     *
     * @param lecturerId The unique identifier of the instructor creating the course.
     * @param courseName The name of the new course. Must not be empty or blank.
     * @return A [Result] containing the created [Course] on success, or an [IllegalArgumentException]
     *         if the input validation fails.
     */
    override suspend fun createCourse(
        lecturerId: String,
        courseName: String
    ): Result<Course> {
        if (lecturerId.isBlank() || courseName.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid input"))
        }

        val newCourse = Course(
            id = generateCourseId(),
            name = courseName.trim(),
            lecturerId = lecturerId,
            memberIds = emptyList(),
            sharedCatalogIds = emptyList()
        )

        return kursVerwaltung.save(newCourse).map { newCourse }
    }

    /**
     * Retrieves the details of a specific course by its ID.
     *
     * **LF72 – Dozenten Kursansicht anzeigen**
     *
     * @param courseId The unique identifier of the course.
     * @return A [Result] containing the [Course] if found.
     */
    override suspend fun getCourse(courseId: String): Result<Course> {
        return kursVerwaltung.findById(courseId)
    }

    /**
     * Deletes a course from the system.
     *
     * @param courseId The unique identifier of the course to be removed.
     * @return A [Result] indicating success or failure of the deletion process.
     */
    override suspend fun deleteCourse(courseId: String): Result<Unit> {
        return kursVerwaltung.delete(courseId)
    }

    /**
     * Provides a stream of all available courses in the system.
     *
     * @return A [Flow] emitting the current list of all [Course] objects.
     */
    override fun getAllCourses(): Flow<List<Course>> {
        return kursVerwaltung.getAllCourses()
    }

    /**
     * Generates a unique course identifier based on a random numeric part.
     * Format: "COURSE-<10-digit-number>"
     *
     * @return A unique string ID for a new course.
     */
    private fun generateCourseId(): String {
        val randomPart = Random.nextLong(1_000_000_000L, 9_999_999_999L)
        return "COURSE-$randomPart"
    }

}
