package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.client.IRestClient
import de.thkoeln.codescope.data.repository.IAnalyseVerwaltung
import de.thkoeln.codescope.data.repository.IKursVerwaltung
import de.thkoeln.codescope.data.repository.IProjektVerwaltung
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.project.Project
import de.thkoeln.codescope.domain.project.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Unit tests for [AnalyseSteuerung] focusing on the validation rules and
 * orchestration of `startAnalysis`. Collaborators are replaced by hand-rolled
 * fakes so the test runs in `commonTest` on both JVM and Android.
 */
class AnalyseSteuerungTest {

    // ----- Fakes -----------------------------------------------------------

    private class FakeProjektVerwaltung(
        private val projects: MutableMap<String, Project> = mutableMapOf()
    ) : IProjektVerwaltung {

        var lastSaved: Project? = null
            private set

        fun put(project: Project) { projects[project.id] = project }

        override suspend fun save(project: Project): Result<Unit> {
            lastSaved = project
            projects[project.id] = project
            return Result.success(Unit)
        }

        override suspend fun findById(id: String): Result<Project> {
            val p = projects[id]
                ?: return Result.failure(NoSuchElementException("no project $id"))
            return Result.success(p)
        }

        override suspend fun getByUser(userId: String): Result<List<Project>> =
            Result.success(projects.values.filter { it.ownerId == userId })

        override suspend fun deleteProject(project: Project): Result<Unit> {
            projects.remove(project.id)
            return Result.success(Unit)
        }

        override suspend fun updateProject(project: Project): Result<Unit> {
            projects[project.id] = project
            return Result.success(Unit)
        }
    }

    private class FakeAnalyseVerwaltung : IAnalyseVerwaltung {
        override suspend fun saveAnalysis(result: AnalysisResult): Result<Unit> =
            Result.success(Unit)
        override suspend fun loadAnalysis(analysisId: String): Result<AnalysisResult> =
            Result.failure(NoSuchElementException("not used"))
        override suspend fun loadAnalysesForUser(userId: String): Result<List<AnalysisResult>> =
            Result.success(emptyList())
        override suspend fun loadAnalysesForCourse(courseId: String): Result<List<AnalysisResult>> =
            Result.success(emptyList())
        override suspend fun deleteAnalysis(analysisId: String): Result<Unit> =
            Result.success(Unit)
        override fun observeAnalysesForUser(userId: String): Flow<List<AnalysisResult>> =
            emptyFlow()
    }

    private class FakeRestClient(
        private val analysisIdToReturn: Result<String> = Result.success("analysis-42")
    ) : IRestClient {
        var lastRequest: Triple<String, String, String>? = null
            private set

        override suspend fun requestAnalysis(
            projectId: String, catalogId: String, modelId: String, courseId: String?
        ): Result<String> {
            lastRequest = Triple(projectId, catalogId, modelId)
            return analysisIdToReturn
        }

        override suspend fun generateCriteria(topic: String): Result<List<String>> =
            fail("not used")
    }

    private class FakeKursVerwaltung : IKursVerwaltung {
        override suspend fun findById(courseId: String): Result<Course> =
            Result.failure(NoSuchElementException("not used"))
        override suspend fun save(course: Course): Result<Unit> = Result.success(Unit)
        override suspend fun delete(courseId: String): Result<Unit> = Result.success(Unit)
        override suspend fun loadCoursesByLecturer(lecturerId: String): Result<List<Course>> =
            Result.success(emptyList())
        override suspend fun loadCoursesByMember(studentId: String): Result<List<Course>> =
            Result.success(emptyList())
        override fun getAllCourses(): Flow<List<Course>> = emptyFlow()
    }

    private class RecordingNotificationService : INotificationService {
        val notifications = mutableListOf<Pair<String, String>>()
        override fun showNotification(title: String, message: String) {
            notifications += title to message
        }
    }

    private fun newProject(
        id: String = "p1",
        ownerId: String = "u1",
        status: ProjectStatus = ProjectStatus.UPLOADED
    ) = Project(
        id = id, name = "P", ownerId = ownerId,
        sourceLocation = "loc://$id", status = status
    )

    private fun newSut(
        projekt: IProjektVerwaltung = FakeProjektVerwaltung(),
        analyse: IAnalyseVerwaltung = FakeAnalyseVerwaltung(),
        rest: IRestClient = FakeRestClient(),
        kurs: IKursVerwaltung = FakeKursVerwaltung(),
        notif: INotificationService = RecordingNotificationService()
    ) = AnalyseSteuerung(projekt, analyse, rest, kurs, notif)

    // ----- Tests -----------------------------------------------------------

    @Test
    fun startAnalysis_rejectsBlankProjectId() = runTest {
        val result = newSut().startAnalysis(projectId = "  ", catalogId = "c1", model = "m1")
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun startAnalysis_rejectsBlankCatalogId() = runTest {
        val result = newSut().startAnalysis(projectId = "p1", catalogId = "", model = "m1")
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun startAnalysis_rejectsBlankModel() = runTest {
        val result = newSut().startAnalysis(projectId = "p1", catalogId = "c1", model = " ")
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun startAnalysis_failsWhenProjectIsAlreadyRunning() = runTest {
        val projekt = FakeProjektVerwaltung().apply {
            put(newProject(status = ProjectStatus.ANALYSIS_RUNNING))
        }
        val rest = FakeRestClient()
        val sut = newSut(projekt = projekt, rest = rest)

        val result = sut.startAnalysis(projectId = "p1", catalogId = "c1", model = "m1")

        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
        // Backend must NOT have been contacted when project is already running.
        assertNull(rest.lastRequest)
        // Project must NOT have been mutated.
        assertNull(projekt.lastSaved)
    }

    @Test
    fun startAnalysis_happyPath_marksProjectRunning_andReturnsAnalysisId() = runTest {
        val projekt = FakeProjektVerwaltung().apply { put(newProject()) }
        val rest = FakeRestClient(Result.success("analysis-xyz"))
        val sut = newSut(projekt = projekt, rest = rest)

        val result = sut.startAnalysis(projectId = "p1", catalogId = "c1", model = "gemini")

        assertTrue(result.isSuccess)
        assertEquals("analysis-xyz", result.getOrNull())
        assertEquals(Triple("p1", "c1", "gemini"), rest.lastRequest)
        assertEquals(ProjectStatus.ANALYSIS_RUNNING, projekt.lastSaved?.status)
    }
}
