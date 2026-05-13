package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.domain.user.User
import kotlinx.coroutines.flow.Flow

interface IBenutzerVerwaltung {

    suspend fun saveUser(user: User): Result<Unit>

    suspend fun loadUser(userId: String): Result<User>

    fun getAllUsers(): Flow<List<User>>
}