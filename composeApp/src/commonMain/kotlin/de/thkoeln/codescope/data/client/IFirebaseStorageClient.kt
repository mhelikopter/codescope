package de.thkoeln.codescope.data.client

interface IFirebaseStorageClient {
    suspend fun getFile(path: String): Result<String>

    suspend fun uploadFile(path: String, data: ByteArray): Result<Unit>

    suspend fun deleteFile(path: String): Result<Unit>
}