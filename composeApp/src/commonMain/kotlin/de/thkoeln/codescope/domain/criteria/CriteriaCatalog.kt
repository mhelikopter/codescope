package de.thkoeln.codescope.domain.criteria

import kotlinx.serialization.Serializable

/**
 * Domain model for a criteria catalog uploaded by a user.
 *
 * A criteria catalog consists of one or more text files that describe
 * how a project should be evaluated by the AI.
 */
@Serializable
data class CriteriaCatalog(
    val id: String,
    val name: String,
    val ownerId: String,              // User who created and owns this catalog
    var sourceLocation: String,       // References to uploaded .txt files containing the criteria
    val lastUpdated: Long
)

@Serializable
data class CriteriaItem(
    val question: String,
    val weight: Int                 // Weight of this criterion used for scoring and evaluation
)