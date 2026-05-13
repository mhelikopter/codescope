package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.data.client.IFirestoreClient
import de.thkoeln.codescope.domain.course.Course
import kotlinx.coroutines.flow.Flow

class KursVerwaltungImpl(private val firestoreClient: IFirestoreClient): IKursVerwaltung {
    override suspend fun findById(courseId: String): Result<Course> {
        val path = "courses/$courseId"
        val result = firestoreClient.getDocument(path, Course.serializer())
        return if (result != null) {
            Result.success(result)
        } else {
            Result.failure(Exception("Course not found"))
        }
    }

    override suspend fun save(course: Course): Result<Unit> {
        val path = "courses/${course.id}"
        return firestoreClient.setDocument(path, course, Course.serializer())
    }

    override suspend fun delete(courseId: String): Result<Unit> {
        val path = "courses/$courseId"
        return firestoreClient.deleteDocument(path)
    }

    override suspend fun loadCoursesByLecturer(lecturerId: String): Result<List<Course>> {
        val path = "courses"
        return firestoreClient.getDocumentsByQuery(path, "lecturerId", lecturerId, Course.serializer())
    }

    override suspend fun loadCoursesByMember(studentId: String): Result<List<Course>> {
        val path = "courses"
        return firestoreClient.getDocumentsByQueryInArray(path, "memberIds", studentId, Course.serializer())
    }

    override fun getAllCourses(): Flow<List<Course>> {
        return firestoreClient.getCollection("courses", Course.serializer())
    }
}