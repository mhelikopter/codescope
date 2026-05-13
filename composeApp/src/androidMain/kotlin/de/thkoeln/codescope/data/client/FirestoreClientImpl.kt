package de.thkoeln.codescope.data.client

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer

class FirestoreClientImpl(private val db: FirebaseFirestore) : IFirestoreClient {
    companion object {
        private const val TAG = "FirestoreClient"
    }

    override suspend fun <T : Any> setDocument(
        path: String,
        data: T,
        strategy: KSerializer<T>
    ): Result<Unit> {
        try {
            db.document(path).set(data = data, strategy = strategy)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun updateDocument(
        path: String,
        fields: Map<String, Any>
    ): Result<Unit> {
        try {
            val pairs = fields.toList().toTypedArray()
            db.document(path).updateFields { pairs.forEach { (field, value) -> field to value } }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun <T> getDocumentsByQuery(
        collectionPath: String,
        field: String,
        value: Any,
        strategy: KSerializer<T>
    ): Result<List<T>> {
        return try {
            val querySnapshot = db.collection(collectionPath)
                .where { field equalTo value }
                .get()

            val result = querySnapshot.documents.map { document ->
                document.data(strategy)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun <T> observeDocument(path: String, strategy: KSerializer<T>): Flow<T?> {
        return db.document(path).snapshots
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.data(strategy)
                } else {
                    null
                }
            }
    }

    override suspend fun deleteDocument(path: String): Result<Unit> {
        try {
            db.document(path).delete()
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun <T> getDocument(path: String, strategy: KSerializer<T>): T? {
        return try {
            val snapshot = db.document(path).get()
            if (snapshot.exists) {
                snapshot.data(strategy)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun <T> getDocumentsByQueryInArray(collectionPath: String, field: String, value: Any, strategy: KSerializer<T>): Result<List<T>> {
        return try {
            val querySnapshot = db.collection(collectionPath)
                .where { field contains value }
                .get()

            val result = querySnapshot.documents.map { document ->
                document.data(strategy)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun <T> getCollection(path: String, strategy: KSerializer<T>): Flow<List<T>> {
        return db.collection(path).snapshots.map { snapshot ->
            snapshot.documents.map { document ->
                document.data(strategy)
            }
        }
    }

    override fun <T> observeCollectionByQuery(collectionPath: String, field: String, value: Any, strategy: KSerializer<T>): Flow<List<T>> {
        return db.collection(collectionPath)
            .where { field equalTo value }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data(strategy) }
            }
    }
}