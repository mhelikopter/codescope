package de.thkoeln.codescope.domain.course

import kotlinx.serialization.Serializable

/**
 * Domain model representing a course in CodeScope.
 */
@Serializable
data class Course(
    val id: String,
    val name: String,
    val lecturerId: String,             // References the lecturer (User) who owns this course
    val memberIds: List<String>,        // IDs of users enrolled in this course (students)
    val sharedCatalogIds: List<String>
)