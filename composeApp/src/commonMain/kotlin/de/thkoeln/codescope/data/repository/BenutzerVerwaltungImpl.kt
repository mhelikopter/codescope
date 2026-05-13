package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.data.client.IFirestoreClient
import de.thkoeln.codescope.domain.user.User
import kotlinx.coroutines.flow.Flow

class BenutzerVerwaltungImpl(private val firestoreClient: IFirestoreClient) : IBenutzerVerwaltung {
    override suspend fun saveUser(user: User): Result<Unit> {
        val path = "users/${user.id}"
        return firestoreClient.setDocument(path, user, User.serializer())
    }

    override suspend fun loadUser(userId: String): Result<User> {
        val path = "users/$userId"
        val result = firestoreClient.getDocument(path, User.serializer())
        return if (result != null) {
            Result.success(result)
        } else {
            Result.failure(Exception("User not found"))
        }
    }

    override fun getAllUsers(): Flow<List<User>> {
        return firestoreClient.getCollection("users", User.serializer())
    }
}