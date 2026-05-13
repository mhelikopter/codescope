package de.thkoeln.codescope.domain.project

import kotlinx.serialization.Serializable

/**
 * Domain model representing a code project that can be analysed.
 */
@Serializable
data class Project(
    val id: String,
    val name: String,
    val ownerId: String,        // References the user who owns and uploaded this project
    var sourceLocation: String,
    val status: ProjectStatus,
    val sizeBytes: Long = 0L
)

/**
 * High-level lifecycle state of a project.
 */
@Serializable
enum class ProjectStatus {
    UPLOADED,
    ANALYSIS_RUNNING,
    ANALYSED,
    REVIEWED
}