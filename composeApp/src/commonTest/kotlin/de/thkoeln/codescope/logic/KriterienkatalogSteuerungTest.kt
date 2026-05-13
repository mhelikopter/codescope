package de.thkoeln.codescope.logic

import de.thkoeln.codescope.data.client.IRestClient
import de.thkoeln.codescope.data.repository.IKriterienKatalogVerwaltung
import de.thkoeln.codescope.data.repository.IKursVerwaltung
import de.thkoeln.codescope.domain.course.Course
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Unit tests for [KriterienkatalogSteuerung] that focus on its authorization
 * rules and validation paths. Repository/REST collaborators are replaced by
 * hand-rolled fakes so the test stays fast and platform-neutral.
 */
class KriterienkatalogSteuerungTest {

    // ----- Fakes -----------------------------------------------------------

    private class FakeKatalogVerwaltung(
        private val catalogs: MutableMap<String, CriteriaCatalog> = mutableMapOf()
    ) : IKriterienKatalogVerwaltung {

        fun put(catalog: CriteriaCatalog) { catalogs[catalog.id] = catalog }

        override suspend fun saveCatalog(catalog: CriteriaCatalog): Result<Unit> {
            catalogs[catalog.id] = catalog
            return Result.success(Unit)
        }

        override suspend fun loadCatalog(catalogId: String): Result<CriteriaCatalog> {
            val c = catalogs[catalogId]
                ?: return Result.failure(NoSuchElementException("no catalog $catalogId"))
            return Result.success(c)
        }

        override suspend fun loadCatalogsByUser(userId: String): Result<List<CriteriaCatalog>> =
            Result.success(catalogs.values.filter { it.ownerId == userId })

        override suspend fun deleteCatalog(catalog: CriteriaCatalog): Result<Unit> {
            catalogs.remove(catalog.id)
            return Result.success(Unit)
        }

        override suspend fun uploadCatalog(
            byteArray: ByteArray,
            catalogName: String,
            ownerId: String
        ): Result<Unit> {
            val id = "cat-${catalogs.size + 1}"
            catalogs[id] = CriteriaCatalog(
                id = id,
                name = catalogName,
                ownerId = ownerId,
                sourceLocation = "fake://$id",
                lastUpdated = 0L
            )
            return Result.success(Unit)
        }

        override suspend fun updateCatalog(catalog: CriteriaCatalog, byteArray: ByteArray): Result<Unit> {
            catalogs[catalog.id] = catalog
            return Result.success(Unit)
        }

        override suspend fun getCatalogUrl(catalogId: String): Result<String> =
            Result.success("https://fake/$catalogId")

        override suspend fun getCatalogBytes(catalogId: String): Result<ByteArray> =
            Result.success(ByteArray(0))
    }

    private class FakeKursVerwaltung(
        private val courses: MutableMap<String, Course> = mutableMapOf()
    ) : IKursVerwaltung {

        var lastSaved: Course? = null
            private set

        fun put(course: Course) { courses[course.id] = course }

        override suspend fun findById(courseId: String): Result<Course> {
            val c = courses[courseId]
                ?: return Result.failure(NoSuchElementException("no course $courseId"))
            return Result.success(c)
        }

        override suspend fun save(course: Course): Result<Unit> {
            lastSaved = course
            courses[course.id] = course
            return Result.success(Unit)
        }

        override suspend fun delete(courseId: String): Result<Unit> {
            courses.remove(courseId)
            return Result.success(Unit)
        }

        override suspend fun loadCoursesByLecturer(lecturerId: String): Result<List<Course>> =
            Result.success(courses.values.filter { it.lecturerId == lecturerId })

        override suspend fun loadCoursesByMember(studentId: String): Result<List<Course>> =
            Result.success(courses.values.filter { studentId in it.memberIds })

        override fun getAllCourses(): Flow<List<Course>> = emptyFlow()
    }

    private class UnusedRestClient : IRestClient {
        override suspend fun requestAnalysis(
            projectId: String, catalogId: String, modelId: String, courseId: String?
        ): Result<String> = fail("REST client should not be invoked by these tests")

        override suspend fun generateCriteria(topic: String): Result<List<String>> =
            fail("REST client should not be invoked by these tests")
    }

    // ----- Helpers ---------------------------------------------------------

    private fun catalog(id: String, ownerId: String) = CriteriaCatalog(
        id = id, name = "Cat $id", ownerId = ownerId,
        sourceLocation = "x://$id", lastUpdated = 1L
    )

    private fun course(id: String, lecturerId: String, shared: List<String> = emptyList()) = Course(
        id = id, name = "Course $id", lecturerId = lecturerId,
        memberIds = emptyList(), sharedCatalogIds = shared
    )

    // ----- Tests -----------------------------------------------------------

    @Test
    fun shareCatalogWithCourse_succeeds_andPersistsCatalogIdOnCourse() = runTest {
        val katalog = FakeKatalogVerwaltung().apply { put(catalog("cat1", ownerId = "lect1")) }
        val kurs = FakeKursVerwaltung().apply { put(course("course1", lecturerId = "lect1")) }
        val sut = KriterienkatalogSteuerung(katalog, kurs, UnusedRestClient())

        val result = sut.shareCatalogWithCourse("cat1", "course1", "lect1")

        assertTrue(result.isSuccess, "Share should succeed for matching owner+lecturer")
        assertEquals(listOf("cat1"), kurs.lastSaved?.sharedCatalogIds)
    }

    @Test
    fun shareCatalogWithCourse_rejectsForeignCatalogOwner() = runTest {
        val katalog = FakeKatalogVerwaltung().apply { put(catalog("cat1", ownerId = "someoneElse")) }
        val kurs = FakeKursVerwaltung().apply { put(course("course1", lecturerId = "lect1")) }
        val sut = KriterienkatalogSteuerung(katalog, kurs, UnusedRestClient())

        val result = sut.shareCatalogWithCourse("cat1", "course1", "lect1")

        assertTrue(result.isFailure)
        assertIs<IllegalAccessException>(result.exceptionOrNull())
    }

    @Test
    fun shareCatalogWithCourse_isIdempotent_whenAlreadyShared() = runTest {
        val katalog = FakeKatalogVerwaltung().apply { put(catalog("cat1", ownerId = "lect1")) }
        val kurs = FakeKursVerwaltung().apply {
            put(course("course1", lecturerId = "lect1", shared = listOf("cat1")))
        }
        val sut = KriterienkatalogSteuerung(katalog, kurs, UnusedRestClient())

        val result = sut.shareCatalogWithCourse("cat1", "course1", "lect1")

        assertTrue(result.isSuccess)
        // No save expected when nothing changes.
        assertEquals(null, kurs.lastSaved)
    }

    @Test
    fun uploadCriteriaCatalog_rejectsBlankInput() = runTest {
        val sut = KriterienkatalogSteuerung(
            FakeKatalogVerwaltung(), FakeKursVerwaltung(), UnusedRestClient()
        )

        val result = sut.uploadCriteriaCatalog(
            name = "  ", ownerId = "u1", fileData = byteArrayOf(1, 2, 3), fileName = "x.txt"
        )

        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }
}
