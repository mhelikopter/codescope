package de.thkoeln.codescope.viewmodel

import de.thkoeln.codescope.domain.analysis.AnalysisResult
import de.thkoeln.codescope.domain.analysis.AnalysisStatus
import de.thkoeln.codescope.domain.analysis.FeedbackItem
import de.thkoeln.codescope.domain.criteria.CriteriaCatalog
import de.thkoeln.codescope.logic.IAnalyseSteuerung
import de.thkoeln.codescope.logic.IKriterienkatalogSteuerung
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Unit tests for [AnalysisConfigViewModel.onFileSelected].
 *
 * The method is synchronous (no coroutine launch), which makes it cleanly
 * testable in `commonTest` without a Main dispatcher. We focus on the
 * file-extension validation rules added during development.
 */
class AnalysisConfigViewModelTest {

    private class NoopAnalyseSteuerung : IAnalyseSteuerung {
        override suspend fun startAnalysis(
            projectId: String, catalogId: String, model: String, courseId: String?
        ): Result<String> = fail("not used")
        override suspend fun getAnalysisResult(analysisId: String): Result<AnalysisResult> =
            fail("not used")
        override suspend fun getAnalysisHistory(userId: String): Result<List<AnalysisResult>> =
            fail("not used")
        override suspend fun getAnalysesForCourse(courseId: String): Result<List<AnalysisResult>> =
            fail("not used")
        override suspend fun getAnalysesForAllInstructorCourses(instructorId: String): Result<List<AnalysisResult>> =
            fail("not used")
        override suspend fun deleteAnalysisResult(analysisId: String, requesterId: String): Result<Unit> =
            fail("not used")
        override suspend fun startBatchAnalysis(
            projectIds: List<String>, catalogId: String, model: String,
            resetFeedback: Boolean, notifyStudents: Boolean, courseId: String?
        ): Result<List<String>> = fail("not used")
        override suspend fun updateAnalysisResult(
            analysisId: String, instructorId: String, newStatus: AnalysisStatus,
            newScore: Int?, newFeedback: List<FeedbackItem>?, instructorComment: String?
        ): Result<AnalysisResult> = fail("not used")
        override suspend fun linkAnalysisToCourse(
            analysisId: String, userId: String, courseId: String
        ): Result<Unit> = fail("not used")
        override fun observeAnalysesForUser(userId: String): Flow<List<AnalysisResult>> = emptyFlow()
    }

    private class NoopKatalogSteuerung : IKriterienkatalogSteuerung {
        override suspend fun uploadCriteriaCatalog(
            name: String, ownerId: String, fileData: ByteArray, fileName: String
        ): Result<Unit> = fail("not used")
        override suspend fun updateCriteriaCatalog(
            catalogId: String, name: String, fileData: ByteArray
        ): Result<Unit> = fail("not used")
        override suspend fun listCriteriaCatalogs(userId: String): Result<List<CriteriaCatalog>> =
            fail("not used")
        override suspend fun shareCatalogWithCourse(
            catalogId: String, courseId: String, lecturerId: String
        ): Result<Unit> = fail("not used")
        override suspend fun unshareCatalogFromCourse(
            catalogId: String, courseId: String, lecturerId: String
        ): Result<Unit> = fail("not used")
        override suspend fun deleteCriteriaCatalog(catalogId: String, requesterId: String): Result<Unit> =
            fail("not used")
        override suspend fun exportCriteriaCatalog(catalogId: String): Result<String> = fail("not used")
        override suspend fun downloadCriteriaCatalog(catalogId: String): Result<ByteArray> = fail("not used")
        override suspend fun generateCriteriaFromAi(topic: String): Result<List<String>> = fail("not used")
        override suspend fun getCriteriaCatalog(catalogId: String): Result<CriteriaCatalog> = fail("not used")
    }

    private fun newViewModel() =
        AnalysisConfigViewModel(NoopAnalyseSteuerung(), NoopKatalogSteuerung())

    @Test
    fun onFileSelected_rejectsUnsupportedExtension_andDoesNotOpenDialog() {
        val vm = newViewModel()

        vm.onFileSelected(bytes = byteArrayOf(1, 2, 3), name = "criteria.pdf")

        assertNotNull(vm.errorMessage)
        assertTrue(
            vm.errorMessage!!.contains("Ungültiges Format"),
            "Expected German invalid-format message, got: ${vm.errorMessage}"
        )
        assertFalse(vm.showNewCatalogDialog)
        assertFalse(vm.showFilePicker)
        // newCatalogName must remain empty when the file is rejected.
        assertEquals("", vm.newCatalogName)
    }

    @Test
    fun onFileSelected_acceptsTxtFile_andPrefillsCatalogName() {
        val vm = newViewModel()

        vm.onFileSelected(bytes = byteArrayOf(7, 8, 9), name = "My-Catalog.txt")

        assertNull(vm.errorMessage)
        assertTrue(vm.showNewCatalogDialog, "Dialog must open after a valid file is selected")
        assertFalse(vm.showFilePicker)
        // Name is derived from the file name without the extension.
        assertEquals("My-Catalog", vm.newCatalogName)
    }

    @Test
    fun onFileSelected_acceptsJsonFile_caseInsensitive() {
        val vm = newViewModel()

        vm.onFileSelected(bytes = byteArrayOf(0), name = "Stuff.JSON")

        assertNull(vm.errorMessage)
        assertTrue(vm.showNewCatalogDialog)
        assertEquals("Stuff", vm.newCatalogName)
    }
}
