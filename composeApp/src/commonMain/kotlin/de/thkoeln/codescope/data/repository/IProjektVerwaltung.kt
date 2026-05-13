package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.domain.project.Project

/**
 * Abstraction for persisting and loading projects.
 *
 * Implementations will typically talk to Firebase / Firestore / Storage,
 * but the controller only knows this interface.
 */
interface IProjektVerwaltung {

    /**
     * Saves or updates the given project.
     *
     * @return Result.success(Unit) if successful, Result.failure(...) otherwise.
     */
    suspend fun save(project: Project): Result<Unit>

    /**
     * Loads a project by its id.
     */
    suspend fun findById(id: String): Result<Project>

    suspend fun getByUser(userId: String): Result<List<Project>>

    suspend fun deleteProject(project: Project): Result<Unit>

    suspend fun updateProject(project: Project): Result<Unit>
}