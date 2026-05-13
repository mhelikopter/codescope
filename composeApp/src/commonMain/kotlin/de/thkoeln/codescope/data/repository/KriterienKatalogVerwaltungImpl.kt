package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.data.client.IFirebaseStorageClient
import de.thkoeln.codescope.data.client.IFirestoreClient
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.util.UUID

class KriterienKatalogVerwaltungImpl(
    private val firestoreClient: IFirestoreClient,
    private val storageClient: IFirebaseStorageClient,
    private val httpClient: HttpClient
): IKriterienKatalogVerwaltung {
    override suspend fun saveCatalog(catalog: CriteriaCatalog): Result<Unit> {
        val path = "Catalogs/${catalog.id}"
        val result = firestoreClient.setDocument(path, catalog, CriteriaCatalog.serializer())
        return result
    }

    override suspend fun loadCatalog(catalogId: String): Result<CriteriaCatalog> {
        val path = "Catalogs/$catalogId"
        val result = firestoreClient.getDocument(path, CriteriaCatalog.serializer())
        return if (result != null) {
            Result.success(result)
        } else {
            Result.failure(Exception("Criteria catalog not found"))
        }
    }

    override suspend fun loadCatalogsByUser(userId: String): Result<List<CriteriaCatalog>> {
        val path = "Catalogs"
        return firestoreClient.getDocumentsByQuery(path, "ownerId", userId, CriteriaCatalog.serializer())
    }

    override suspend fun deleteCatalog(catalog: CriteriaCatalog): Result<Unit> {
        val storageResult = storageClient.deleteFile(catalog.sourceLocation)
        if (storageResult.isFailure) {
            return storageResult
        }
        val documentPath = "Catalogs/${catalog.id}"
        val firestoreResult = firestoreClient.deleteDocument(documentPath)
        return firestoreResult
    }

    override suspend fun uploadCatalog(byteArray: ByteArray, catalogName: String, ownerId: String): Result<Unit> {
        val catalogId = UUID.randomUUID().toString()
        val storagePath = "Catalogs/${ownerId}/${catalogId}.txt"

        val storageResult = storageClient.uploadFile(storagePath, byteArray)
        if (storageResult.isFailure) {
            return storageResult
        }

        val catalog = CriteriaCatalog(
            id = catalogId,
            name = catalogName,
            ownerId = ownerId,
            sourceLocation = storagePath,
            lastUpdated = System.currentTimeMillis()
        )

        return saveCatalog(catalog)
    }

    override suspend fun updateCatalog(catalog: CriteriaCatalog, byteArray: ByteArray): Result<Unit> {
        // 1. Datei im Storage überschreiben
        val storageResult = storageClient.uploadFile(catalog.sourceLocation, byteArray)
        if (storageResult.isFailure) {
            return storageResult
        }

        // 2. Metadaten aktualisieren (Timestamp)
        val updatedCatalog = catalog.copy(lastUpdated = System.currentTimeMillis())
        return saveCatalog(updatedCatalog)
    }

    override suspend fun getCatalogUrl(catalogId: String): Result<String> {
        return try {
            val catalog = loadCatalog(catalogId).getOrThrow()
            storageClient.getFile(catalog.sourceLocation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCatalogBytes(catalogId: String): Result<ByteArray> {
        return try {
            val catalog = loadCatalog(catalogId).getOrThrow()
            val url = storageClient.getFile(catalog.sourceLocation).getOrThrow()
            val response = httpClient.get(url)
            val bytes = response.body<ByteArray>()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}