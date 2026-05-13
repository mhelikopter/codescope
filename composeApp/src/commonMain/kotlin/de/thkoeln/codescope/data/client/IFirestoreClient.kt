package de.thkoeln.codescope.data.client

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer

interface IFirestoreClient {
    suspend fun <T : Any> setDocument(path: String, data: T, strategy: KSerializer<T>): Result<Unit>

    suspend fun updateDocument(path: String, fields: Map<String, Any>): Result<Unit>

    fun <T> observeDocument(path: String, strategy: KSerializer<T>): Flow<T?>

    suspend fun <T> getDocumentsByQuery(collectionPath: String, field: String, value: Any, strategy: KSerializer<T>): Result<List<T>>

    suspend fun deleteDocument(path: String): Result<Unit>

    suspend fun <T> getDocument(path: String, strategy: KSerializer<T>): T?

    suspend fun <T> getDocumentsByQueryInArray(collectionPath: String, field: String, value: Any, strategy: KSerializer<T>): Result<List<T>>

    fun <T> getCollection(path: String, strategy: KSerializer<T>): Flow<List<T>>

    fun <T> observeCollectionByQuery(collectionPath: String, field: String, value: Any, strategy: KSerializer<T>): Flow<List<T>>
}