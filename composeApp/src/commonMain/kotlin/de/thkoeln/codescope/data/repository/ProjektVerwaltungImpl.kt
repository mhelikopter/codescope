package de.thkoeln.codescope.data.repository

import de.thkoeln.codescope.data.client.IFirebaseStorageClient
import de.thkoeln.codescope.data.client.IFirestoreClient
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.project.Project

class ProjektVerwaltungImpl(private val firestoreClient: IFirestoreClient, private val storageClient: IFirebaseStorageClient): IProjektVerwaltung {
    override suspend fun save(project: Project): Result<Unit> {
        val path = "projects/${project.id}"
        return firestoreClient.setDocument(path, project, Project.serializer())
    }

    override suspend fun findById(id: String): Result<Project> {
        val path = "projects/$id"
        val result = firestoreClient.getDocument(path, Project.serializer())
        return if (result != null) {
            Result.success(result)
        } else {
            Result.failure(Exception("Project not found"))
        }
    }

    override suspend fun getByUser(userId: String): Result<List<Project>> {
        val path = "projects"
        return firestoreClient.getDocumentsByQuery(path, "ownerId", userId, Project.serializer())
    }

    override suspend fun deleteProject(project: Project): Result<Unit> {
        val projectPath = "projects/${project.id}"
        val codePath = project.sourceLocation

        val firestoreResult = firestoreClient.deleteDocument(projectPath)
        if (firestoreResult.isFailure) {
            return firestoreResult
        }

        return storageClient.deleteFile(codePath)

    }

    override suspend fun updateProject(project: Project): Result<Unit> {
        val path = "projects/${project.id}"
        return firestoreClient.setDocument(path, project, Project.serializer())
    }

}