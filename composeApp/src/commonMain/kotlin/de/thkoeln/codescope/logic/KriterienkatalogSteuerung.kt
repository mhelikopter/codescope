package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.client.IRestClient
import de.thkoeln.codescope.data.repository.IKriterienKatalogVerwaltung
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.data.repository.IKursVerwaltung

/**
 * Controller responsible for managing criteria catalogs within CodeScope.
 *
 * **LF20 – Kriterienkatalog Hochladen**
 * **LF21 – Kriterienkataloge Anzeigen**
 * **LF22 – Kriterienkatalog im Kurs teilen**
 * **LF23 – Kriterienkatalog verwalten**
 * **LF24 – KI-Kriterienkatalog erstellen**
 *
 * This class handles the lifecycle of criteria catalogs (creation, update, deletion),
 * sharing catalogs with courses, and integrating with AI services to generate criteria suggestions.
 */
class KriterienkatalogSteuerung(
    private val kriterienKatalogVerwaltung: IKriterienKatalogVerwaltung,
    private val kursVerwaltung: IKursVerwaltung,
    private val restClient: IRestClient
) : IKriterienkatalogSteuerung {

    /**
     * Uploads a new criteria catalog to the system.
     *
     * **LF20 – Kriterienkatalog Hochladen**
     *
     * @param name The display name of the catalog.
     * @param ownerId The ID of the user (typically a lecturer) who owns the catalog.
     * @param fileData The binary content of the catalog file (e.g., JSON or CSV).
     * @param fileName The original name of the uploaded file.
     * @return A [Result] indicating success or failure of the upload.
     */
    override suspend fun uploadCriteriaCatalog(
        name: String,
        ownerId: String,
        fileData: ByteArray,
        fileName: String
    ): Result<Unit> {
        if (name.isBlank() || ownerId.isBlank() || fileData.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid input data"))
        }

        return kriterienKatalogVerwaltung.uploadCatalog(
            byteArray = fileData,
            catalogName = name.trim(),
            ownerId = ownerId
        )
    }

    /**
     * Updates an existing criteria catalog with new metadata or file content.
     *
     * **LF23 – Kriterienkatalog verwalten**
     *
     * @param catalogId The unique identifier of the catalog to update.
     * @param name The new display name for the catalog.
     * @param fileData The new binary content for the catalog.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun updateCriteriaCatalog(
        catalogId: String,
        name: String,
        fileData: ByteArray
    ): Result<Unit> {
        if (catalogId.isBlank() || name.isBlank() || fileData.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid input data"))
        }

        val catalog = kriterienKatalogVerwaltung.loadCatalog(catalogId).getOrElse {
            return Result.failure(it)
        }

        val updatedCatalog = catalog.copy(name = name.trim())
        return kriterienKatalogVerwaltung.updateCatalog(updatedCatalog, fileData)
    }

    /**
     * Lists all criteria catalogs belonging to a specific user.
     *
     * **LF21 – Kriterienkataloge Anzeigen**
     *
     * @param userId The ID of the owner.
     * @return A [Result] containing a list of [CriteriaCatalog] objects.
     */
    override suspend fun listCriteriaCatalogs(userId: String): Result<List<CriteriaCatalog>> {
        return kriterienKatalogVerwaltung.loadCatalogsByUser(userId)
    }

    /**
     * Makes a criteria catalog available for a specific course.
     *
     * **LF22 – Kriterienkatalog im Kurs teilen**
     *
     * Both the catalog and the course must be owned/managed by the same instructor.
     *
     * @param catalogId The ID of the catalog to share.
     * @param courseId The ID of the course to share with.
     * @param lecturerId The ID of the instructor authorizing the share.
     * @return A [Result] indicating success or failure (e.g., unauthorized access).
     */
    override suspend fun shareCatalogWithCourse(
        catalogId: String,
        courseId: String,
        lecturerId: String
    ): Result<Unit> {
        val catalog = kriterienKatalogVerwaltung.loadCatalog(catalogId).getOrElse { return Result.failure(it) }
        if (catalog.ownerId != lecturerId) return Result.failure(IllegalAccessException("Not owner of catalog"))

        val course = kursVerwaltung.findById(courseId).getOrElse { return Result.failure(it) }
        if (course.lecturerId != lecturerId) return Result.failure(IllegalAccessException("Not lecturer of course"))

        if (course.sharedCatalogIds.contains(catalogId)) return Result.success(Unit)

        return kursVerwaltung.save(course.copy(sharedCatalogIds = course.sharedCatalogIds + catalogId))
    }

    /**
     * Removes a criteria catalog from a course's shared list.
     *
     * @param catalogId The ID of the catalog to unshare.
     * @param courseId The ID of the course.
     * @param lecturerId The ID of the instructor authorizing the action.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun unshareCatalogFromCourse(
        catalogId: String,
        courseId: String,
        lecturerId: String
    ): Result<Unit> {
        val course = kursVerwaltung.findById(courseId).getOrElse { return Result.failure(it) }
        if (course.lecturerId != lecturerId) return Result.failure(IllegalAccessException("Unauthorized"))

        return kursVerwaltung.save(course.copy(sharedCatalogIds = course.sharedCatalogIds - catalogId))
    }

    /**
     * Deletes a criteria catalog from the system. Only the owner is allowed to delete it.
     *
     * **LF23 – Kriterienkatalog verwalten**
     *
     * @param catalogId The ID of the catalog to delete.
     * @param requesterId The ID of the user requesting the deletion.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun deleteCriteriaCatalog(
        catalogId: String,
        requesterId: String
    ): Result<Unit> {
        val existing = kriterienKatalogVerwaltung.loadCatalog(catalogId).getOrElse { return Result.failure(it) }
        if (existing.ownerId != requesterId) return Result.failure(IllegalAccessException("Not owner"))
        return kriterienKatalogVerwaltung.deleteCatalog(existing)
    }

    /**
     * Provides a public download URL for a criteria catalog.
     *
     * @param catalogId The unique identifier of the catalog.
     * @return A [Result] containing the URL string.
     */
    override suspend fun exportCriteriaCatalog(catalogId: String): Result<String> {
        return kriterienKatalogVerwaltung.getCatalogUrl(catalogId)
    }

    /**
     * Downloads the binary content of a criteria catalog.
     *
     * @param catalogId The unique identifier of the catalog.
     * @return A [Result] containing the [ByteArray] of the file content.
     */
    override suspend fun downloadCriteriaCatalog(catalogId: String): Result<ByteArray> {
        return kriterienKatalogVerwaltung.getCatalogBytes(catalogId)
    }

    /**
     * Requests AI-generated criteria suggestions based on a specific topic.
     *
     * **LF24 – KI-Kriterienkatalog erstellen**
     *
     * @param topic The subject matter for which criteria should be generated.
     * @return A [Result] containing a list of suggested criteria as strings.
     */
    override suspend fun generateCriteriaFromAi(topic: String): Result<List<String>> {
        return restClient.generateCriteria(topic)
    }

    /**
     * Retrieves the metadata of a specific criteria catalog.
     *
     * @param catalogId The ID of the catalog.
     * @return A [Result] containing the [CriteriaCatalog] object.
     */
    override suspend fun getCriteriaCatalog(catalogId: String): Result<CriteriaCatalog> {
        if (catalogId.isBlank()) {
            return Result.failure(IllegalArgumentException("Catalog ID must not be blank"))
        }
        return kriterienKatalogVerwaltung.loadCatalog(catalogId)
    }
}
