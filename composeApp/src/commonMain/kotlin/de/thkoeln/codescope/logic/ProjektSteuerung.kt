package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.repository.IProjektVerwaltung
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.domain.project.ProjectStatus
import de.thkoeln.codescope.logic.storage.IProjektStorage
import de.thkoeln.codescope.domain.googleAuth.DriveFile
import kotlin.random.Random
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

/**
 * Controller responsible for project management workflows.
 *
 * **LF10 – Quellcode Hochladen**
 *
 * This class handles the creation and uploading of projects from various sources:
 * direct ZIP uploads, Git repositories (via cloning), and Google Drive.
 * It also manages project identification and storage orchestration.
 */
class ProjektSteuerung(
    private val projektVerwaltung: IProjektVerwaltung,
    private val projektStorage: IProjektStorage
) : IProjektSteuerung {

    /**
     * Creates and uploads a new project from a provided ZIP archive.
     *
     * **LF10 – Quellcode Hochladen**
     *
     * @param name The display name of the project.
     * @param ownerId The ID of the user who owns the project.
     * @param zipData The binary content of the ZIP archive.
     * @return A [Result] containing the created [Project] on success.
     *         Failure occurs if the name/owner is blank, the data is empty,
     *         or if the data does not appear to be a valid ZIP file.
     */
    override suspend fun uploadProject(
        name: String,
        ownerId: String,
        zipData: ByteArray
    ): Result<Project> {
        val bereinigterName = name.trim()
        if (bereinigterName.isEmpty()) return Result.failure(IllegalArgumentException("Projektname darf nicht leer sein"))
        if (ownerId.isBlank()) return Result.failure(IllegalArgumentException("Owner-ID darf nicht leer sein"))
        if (zipData.isEmpty()) return Result.failure(IllegalArgumentException("Projektquellcode darf nicht leer sein"))

        // Check for ZIP magic number (PK..)
        if (zipData.size < 4 || zipData[0] != 'P'.code.toByte() || zipData[1] != 'K'.code.toByte()) {
            return Result.failure(IllegalArgumentException("Nur ZIP-Dateien sind als Upload erlaubt"))
        }

        val projektId = generiereProjektId()
        val uploadResult = projektStorage.uploadProjektQuellcode(projektId, zipData)

        if (uploadResult.isFailure) return Result.failure(uploadResult.exceptionOrNull()!!)

        val projekt = Project(
            id = projektId,
            name = bereinigterName,
            ownerId = ownerId,
            sourceLocation = uploadResult.getOrThrow(),
            status = ProjectStatus.UPLOADED,
            sizeBytes = zipData.size.toLong()
        )

        return projektVerwaltung.save(projekt).map { projekt }
    }

    /**
     * Clones a remote Git repository, archives its content, and uploads it as a project.
     *
     * @param name The project name.
     * @param ownerId The owner's user ID.
     * @param repoUrl The public URL of the Git repository.
     * @return A [Result] containing the [Project] or an error if the clone or ZIP process failed.
     */
    override suspend fun uploadFromGit(
        name: String,
        ownerId: String,
        repoUrl: String
    ): Result<Project> = try {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "codescope_git_${Random.nextInt(10000)}")
        tempDir.mkdirs()

        // Git Clone ausführen
        val process = ProcessBuilder("git", "clone", "--depth", "1", repoUrl, tempDir.absolutePath)
            .redirectErrorStream(true)
            .start()
        
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.inputStream.bufferedReader().readText()
            tempDir.deleteRecursively()
            Result.failure(Exception("Git clone fehlgeschlagen: $error"))
        } else {
            // .git Verzeichnis entfernen um Platz zu sparen
            File(tempDir, ".git").deleteRecursively()
            
            // Zippen
            val zipFile = File(tempDir.parent, "${tempDir.name}.zip")
            zipDirectory(tempDir, zipFile)
            
            val zipData = zipFile.readBytes()
            
            // Aufräumen
            tempDir.deleteRecursively()
            zipFile.delete()
            
            // Normaler Upload
            uploadProject(name, ownerId, zipData)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Downloads a file from Google Drive and uploads it as a project.
     *
     * @param name The project name.
     * @param ownerId The owner's user ID.
     * @param driveFileId The ID or sharing URL of the Google Drive file.
     * @param accessToken A valid OAuth2 access token with Drive scope.
     * @return A [Result] containing the [Project].
     */
    override suspend fun uploadFromDrive(
        name: String,
        ownerId: String,
        driveFileId: String,
        accessToken: String?
    ): Result<Project> = try {
        if (accessToken.isNullOrBlank()) {
            Result.failure(Exception("Google Drive Zugriff erfordert Authentifizierung."))
        } else {
            // Bereinigen der Drive File ID falls ein ganzer Link eingefügt wurde
            val fileId = extractDriveFileId(driveFileId)
            
            val url = URI("https://www.googleapis.com/drive/v3/files/$fileId?alt=media").toURL()
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
            }

            if (connection.responseCode == 200) {
                val zipData = connection.inputStream.readBytes()
                uploadProject(name, ownerId, zipData)
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP ${connection.responseCode}"
                Result.failure(Exception("Download von Google Drive fehlgeschlagen: $errorMsg"))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Fetches all the Projects for a User.
     *
     * @param userId A valid userId.
     * @return A [Result] containing a list of [Project]
     */
    override suspend fun getProjectsForUser(userId: String): Result<List<Project>> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User id must not be blank"))
        return projektVerwaltung.getByUser(userId)
    }

    /**
     * Deletes a Project from the database
     * Note: Firebase considers the delete to be successful not just if the object
     * is deleted but also if the object did not exist.
     *
     * @param project a valid [Project]
     * @return A [Result] which shows whether or not deleting the [Project] was successful
     */
    override suspend fun deleteProject(project: Project): Result<Unit> =
        projektVerwaltung.deleteProject(project)

    /**
     * Updates a project in the database overwriting any fields that are different from that in the
     * database
     * Note: If the project did not previously exist it considers the update successful and creates a new object
     *
     * @param project a valid [Project]
     * @return A [Result] which shows whether or not updating the [Project] was successful
     */
    override suspend fun updateProject(project: Project): Result<Unit> {
        return projektVerwaltung.updateProject(project)
    }

    /**
     * Lists ZIP files from the user's Google Drive.
     *
     * @param accessToken A valid OAuth2 access token.
     * @return A [Result] containing a list of [DriveFile] metadata.
     */
    override suspend fun getDriveFiles(accessToken: String): Result<List<DriveFile>> = try {
        val query = "mimeType = 'application/zip' or name contains '.zip'"
        val url = URI("https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }

        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val files = mutableListOf<DriveFile>()
            
            // Simple JSON parsing for files list
            val fileRegex = "\"kind\"\\s*:\\s*\"drive#file\"\\s*,\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"mimeType\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            fileRegex.findAll(response).forEach { match ->
                files.add(DriveFile(
                    id = match.groupValues[1],
                    name = match.groupValues[2],
                    mimeType = match.groupValues[3]
                ))
            }
            Result.success(files)
        } else {
            val errorMsg = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP ${connection.responseCode}"
            Result.failure(Exception("Abrufen der Drive-Dateien fehlgeschlagen: $errorMsg"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Extracts a Google Drive file ID from a potentially long sharing URL or parameter string.
     *
     * @param input The input string containing the ID.
     * @return The extracted file ID.
     */
    private fun extractDriveFileId(input: String): String {
        return if (input.contains("/d/")) {
            input.substringAfter("/d/").substringBefore("/")
        } else if (input.contains("id=")) {
            input.substringAfter("id=").substringBefore("&")
        } else {
            input
        }
    }

    /**
     * Validates a project's directory structure by counting its files.
     *
     * @param path The local filesystem path to the project directory.
     * @return The number of files found in the directory tree, or 0 if invalid.
     */
    override fun validateProjectStructure(path: String): Int {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) return 0
        return directory.walkTopDown().filter { it.isFile }.count()
    }

    /**
     * Recursively archives a directory into a ZIP file.
     *
     * @param sourceDir The directory to archive.
     * @param zipFile The destination ZIP file.
     */
    private fun zipDirectory(sourceDir: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(sourceDir).path.replace('\\', '/')
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    /**
     * Generates a unique project identifier.
     * Format: "PRJ-<10-digit-number>"
     *
     * @return A unique string ID.
     */
    private fun generiereProjektId(): String {
        val zufall = Random.nextLong(1_000_000_000L, 9_999_999_999L)
        return "PRJ-$zufall"
    }

    /**
     * Fetches a specific project by it's projectId
     *
     * @param projectId a String that references an existing project in the database
     * @return a [Project] matching the given id
     */
    override suspend fun getProjektById(projectId: String): Result<Project> =
        projektVerwaltung.findById(projectId)
}
