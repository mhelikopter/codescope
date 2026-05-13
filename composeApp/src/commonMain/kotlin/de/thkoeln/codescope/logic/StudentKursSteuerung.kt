package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.repository.IKursVerwaltung
import de.thkoeln.codescope.domain.course.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Controller responsible for all course interactions performed by students.
 *
 * **LF71 – Kurs beitreten**
 *
 * This class handles student-facing operations such as joining/leaving courses,
 * retrieving enrolled courses, and submitting analysis results for evaluation.
 */
class StudentKursSteuerung(
    private val kursVerwaltung: IKursVerwaltung,
    private val analyseSteuerung: IAnalyseSteuerung
) : IStudentKursSteuerung {

    /**
     * Enrolls a student in a specific course.
     *
     * **LF71 – Kurs beitreten**
     * **LF73 – Kursmitglied entfernen**
     * **LF74 – Kursmitgliedschaft beenden**
     * **LF75 – Studentenkursansicht anzeigen**
     *
     * Validates that the student is not the lecturer of the course. If the student
     * is already a member, it returns the current course state as success.
     *
     * @param courseId The unique identifier of the course to join.
     * @param studentId The unique identifier of the student.
     * @return A [Result] containing the updated [Course] object on success.
     *         Failure occurs if IDs are blank, the course is not found, or a lecturer
     *         tries to join their own course.
     */
    override suspend fun joinCourse(
        courseId: String,
        studentId: String
    ): Result<Course> {
        if (courseId.isBlank() || studentId.isBlank()) {
            return Result.failure(IllegalArgumentException("Ids must not be blank"))
        }

        val course = kursVerwaltung.findById(courseId).getOrElse {
            return Result.failure(IllegalStateException("Course not found"))
        }

        // Prevent lecturer from joining their own course as a student
        if (course.lecturerId == studentId) {
            return Result.failure(IllegalStateException("Als Dozent dieses Kurses kannst du ihm nicht als Student beitreten."))
        }

        if (course.memberIds.contains(studentId)) {
            return Result.success(course)
        }

        val updatedCourse = course.copy(
            memberIds = course.memberIds + studentId
        )

        return kursVerwaltung.save(updatedCourse).map { updatedCourse }
    }

    /**
     * Retrieves all courses managed by a specific instructor.
     *
     * @param lecturerId The unique identifier of the lecturer.
     * @return A [Result] containing a list of [Course] objects.
     */
    override suspend fun getInstructorCourses(
        lecturerId: String
    ): Result<List<Course>> {
        return kursVerwaltung.loadCoursesByLecturer(lecturerId)
    }

    /**
     * Removes a student from a course. This action is performed by an instructor.
     *
     * **LF73 – Kursmitglied entfernen**
     *
     * @param courseId The unique identifier of the course.
     * @param instructorId The ID of the instructor authorizing the removal.
     * @param studentId The ID of the student to be removed.
     * @return A [Result] indicating success or failure (e.g., unauthorized or course not found).
     */
    override suspend fun removeStudentFromCourse(
        courseId: String,
        instructorId: String,
        studentId: String
    ): Result<Unit> {
        val course = kursVerwaltung.findById(courseId).getOrElse {
            return Result.failure(IllegalStateException("Course not found"))
        }

        if (course.lecturerId != instructorId) {
            return Result.failure(IllegalAccessException("Unauthorized"))
        }

        val updatedCourse = course.copy(
            memberIds = course.memberIds - studentId
        )

        return kursVerwaltung.save(updatedCourse).map { Unit }
    }

    /**
     * Allows a student to voluntarily leave a course.
     *
     * **LF74 – Kursmitgliedschaft beenden**
     *
     * @param courseId The unique identifier of the course.
     * @param studentId The ID of the student leaving the course.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun leaveCourse(
        courseId: String,
        studentId: String
    ): Result<Unit> {
        val course = kursVerwaltung.findById(courseId).getOrElse {
            return Result.failure(IllegalStateException("Course not found"))
        }

        val updatedCourse = course.copy(
            memberIds = course.memberIds - studentId
        )

        return kursVerwaltung.save(updatedCourse).map { Unit }
    }

    /**
     * Provides a real-time stream of all courses the specified student is enrolled in.
     *
     * **LF75 – Studentenkursansicht anzeigen**
     *
     * @param studentId The unique identifier of the student.
     * @return A [Flow] emitting a list of [Course] objects the student belongs to.
     */
    override fun getStudentCourses(studentId: String): Flow<List<Course>> {
        // Wir nutzen hier den Flow aus dem Repository für Live-Updates
        return kursVerwaltung.getAllCourses().map { allCourses ->
            allCourses.filter { it.memberIds.contains(studentId) }
        }
    }

    /**
     * Submits a completed analysis to a course for evaluation.
     *
     * This essentially links the analysis record to the specified course.
     *
     * @param analysisId The unique identifier of the analysis.
     * @param studentId The ID of the student (owner of the analysis).
     * @param courseId The ID of the course to submit to.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun submitAnalysisToCourse(
        analysisId: String,
        studentId: String,
        courseId: String
    ): Result<Unit> {
        return analyseSteuerung.linkAnalysisToCourse(analysisId, studentId, courseId)
    }
}
