package de.thkoeln.codescope.domain.user

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT,
    LECTURER,
    ADMIN
}

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean = true
)
