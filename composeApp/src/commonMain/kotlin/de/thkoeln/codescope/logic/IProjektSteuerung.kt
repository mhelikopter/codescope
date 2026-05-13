package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.domain.googleAuth.DriveFile

/**
 * Interface for project-related operations in CodeScope.
 *
 * Linked LF:
 * - LF10 Projekt hochladen
 */
interface IProjektSteuerung {

    /**
     * Uploads a project and saves its metadata.
     */
    suspend fun uploadProject(
        name: String,
        ownerId: String,
        zipData: ByteArray
    ): Result<Project>

    /**
     * Downloads a project from a Git repository, zips it, and uploads it.
     */
    suspend fun uploadFromGit(
        name: String,
        ownerId: String,
        repoUrl: String
    ): Result<Project>

    /**
     * Downloads a project from Google Drive, and uploads it.
     * Note: Implementation details depend on the used Drive integration.
     */
    suspend fun uploadFromDrive(
        name: String,
        ownerId: String,
        driveFileId: String,
        accessToken: String? = null
    ): Result<Project>

    /**
     * Fetches all the projects for a particular User
     */
    suspend fun getProjectsForUser(userId: String): Result<List<Project>>

    /**
     * Deletes a given project
     */
    suspend fun deleteProject(project: Project): Result<Unit>

    /**
     * Updates the given project
     */
    suspend fun updateProject(project: Project): Result<Unit>

    /**
     * Fetches a list of ZIP files from Google Drive.
     */
    suspend fun getDriveFiles(accessToken: String): Result<List<DriveFile>>

    /**
     * Validates the project structure and returns the number of files.
     *
     * @param path The path to the project directory.
     * @return The number of files in the directory.
     */
    fun validateProjectStructure(path: String): Int

    /**
     *  Fetches a specific project by it's projectId
     */
    suspend fun getProjektById(projectId: String): Result<Project>

}