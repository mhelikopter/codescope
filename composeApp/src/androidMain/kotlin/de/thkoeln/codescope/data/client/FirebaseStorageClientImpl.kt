package de.thkoeln.codescope.data.client

// NOTE: android.util.Log is currently used for logging in this implementation.
// In a production-ready multiplatform setup, logging would be abstracted
// via a platform-independent logging mechanism.
// For MS3, logging is kept simple and platform-specific.

import de.thkoeln.codescope.helper.toData
import dev.gitlive.firebase.storage.FirebaseStorage

class FirebaseStorageClientImpl(private val storage: FirebaseStorage) : IFirebaseStorageClient {

    companion object {
        private const val TAG = "StorageClient"
    }

    override suspend fun uploadFile(path: String, data: ByteArray): Result<Unit> {
        return try {
            // 1. Create a reference to the specific path (e.g., "uploads/user1/project.zip")
            val reference = storage.reference(path)

            // 2. Upload the raw data (suspend function)
            // Note: You can also look into 'putFile' if you have platform-specific file paths
            reference.putData(data.toData())

            println("$TAG: Upload successful: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            println("$TAG: Upload failed at $path: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Unit> {
        try {
            storage.reference(path).delete()
            println("$TAG: File deleted: $path")
            return Result.success(Unit)
        } catch (e: Exception) {
            println("$TAG: Error deleting file: $path")
            return Result.failure(e)
        }
    }

    override suspend fun getFile(path: String): Result<String> {
        return try {
            val reference = storage.reference(path).getDownloadUrl()
            println("$TAG: File URL retrieved: $reference")
            Result.success(reference)
        } catch (e: Exception) {
            println("$TAG: Could not get URL for $path")
            return Result.failure(e)
        }
    }

}