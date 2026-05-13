package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.domain.criteria.CriteriaCatalog

interface IKriterienKatalogVerwaltung {

    suspend fun saveCatalog(catalog: CriteriaCatalog): Result<Unit>

    suspend fun loadCatalog(catalogId: String): Result<CriteriaCatalog>

    suspend fun loadCatalogsByUser(userId: String): Result<List<CriteriaCatalog>>

    suspend fun deleteCatalog(catalog: CriteriaCatalog): Result<Unit>

    suspend fun uploadCatalog(byteArray: ByteArray, catalogName: String, ownerId: String): Result<Unit>

    suspend fun updateCatalog(catalog: CriteriaCatalog, byteArray: ByteArray): Result<Unit>

    suspend fun getCatalogUrl(catalogId: String): Result<String>

    suspend fun getCatalogBytes(catalogId: String): Result<ByteArray>
}