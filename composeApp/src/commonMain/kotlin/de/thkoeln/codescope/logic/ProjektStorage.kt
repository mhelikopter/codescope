package de.thkoeln.codescope.logic.storage

import de.thkoeln.codescope.data.client.IFirebaseStorageClient

/**
 * Implementation of [IProjektStorage] that manages project files in cloud storage.
 *
 * This class uses [IFirebaseStorageClient] to handle the physical storage of project artifacts,
 * specifically source code archives.
 */
class ProjektStorage(
    private val firebaseStorageClient: IFirebaseStorageClient
) : IProjektStorage {

    /**
     * Uploads the project's source code as a ZIP archive to Firebase Storage.
     *
     * The file is stored at the path: "projects/{projektId}/source.zip".
     *
     * @param projektId The unique identifier of the project.
     * @param zipDaten The binary content of the ZIP archive.
     * @return A [Result] containing the storage path (string) where the file was uploaded,
     *         or a failure if the upload process encountered an error.
     */
    override suspend fun uploadProjektQuellcode(
        projektId: String,
        zipDaten: ByteArray
    ): Result<String> {

        val pfad = "projects/$projektId/source.zip"

        val uploadResult = firebaseStorageClient.uploadFile(pfad, zipDaten)

        if (uploadResult.isFailure) {
            return Result.failure(uploadResult.exceptionOrNull()!!)
        }

        return Result.success(pfad)
    }
}
