package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.criteria.CriteriaCatalog

interface IKriterienkatalogSteuerung {

    suspend fun uploadCriteriaCatalog(
        name: String,
        ownerId: String,
        fileData: ByteArray,
        fileName: String
    ): Result<Unit>

    suspend fun updateCriteriaCatalog(
        catalogId: String,
        name: String,
        fileData: ByteArray
    ): Result<Unit>

    suspend fun listCriteriaCatalogs(
        userId: String
    ): Result<List<CriteriaCatalog>>

    suspend fun shareCatalogWithCourse(
        catalogId: String,
        courseId: String,
        lecturerId: String
    ): Result<Unit>

    suspend fun unshareCatalogFromCourse(
        catalogId: String,
        courseId: String,
        lecturerId: String
    ): Result<Unit>

    suspend fun deleteCriteriaCatalog(
        catalogId: String,
        requesterId: String
    ): Result<Unit>

    suspend fun exportCriteriaCatalog(catalogId: String): Result<String>

    suspend fun downloadCriteriaCatalog(catalogId: String): Result<ByteArray>

    suspend fun generateCriteriaFromAi(topic: String): Result<List<String>>

    suspend fun getCriteriaCatalog(catalogId: String): Result<CriteriaCatalog>
}