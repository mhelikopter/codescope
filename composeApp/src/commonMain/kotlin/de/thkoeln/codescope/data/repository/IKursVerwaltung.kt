package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.domain.course.Course
import kotlinx.coroutines.flow.Flow

interface IKursVerwaltung {

    suspend fun findById(courseId: String): Result<Course>

    suspend fun save(course: Course): Result<Unit>

    suspend fun delete(courseId: String): Result<Unit>

    // Für LF72 – Dozent sieht seine eigenen Kurse
    suspend fun loadCoursesByLecturer(lecturerId: String): Result<List<Course>>


    // Für LF75 – Student sieht Kurse, in denen er Mitglied ist
    suspend fun loadCoursesByMember(studentId: String): Result<List<Course>>

    /**
     * Liefert alle Kurse im System als Flow.
     */
    fun getAllCourses(): Flow<List<Course>>
}