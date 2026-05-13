package de.thkoeln.codescope.domain.analysis

import kotlinx.serialization.Serializable

/**
 * Domain model representing an analysis job and its result.
 */
@Serializable
data class AnalysisResult(
    val id: String,
    val userId: String,                     // owner of the analysis
    val projectId: String,
    val courseId: String? = null,           // optional link to a course
    val criteriaCatalogId: String,          // References the CriteriaCatalog used for this analysis
    val model: String,
    val status: AnalysisStatus,
    val score: Int?,
    val feedback: List<FeedbackItem>?,
    val instructorComment: String? = null,  // Manual feedback from the lecturer
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class FeedbackItem(
    val criterion: String,
    val comment: String,
    val rating: Int
)

@Serializable
enum class AnalysisStatus {
    PENDING,
    RUNNING,
    FINISHED,
    FAILED,
    REVIEWED // New status when lecturer has graded it
}