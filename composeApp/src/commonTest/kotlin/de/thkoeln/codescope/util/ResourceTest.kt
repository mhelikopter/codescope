package de.thkoeln.codescope.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [Resource], verifying that the sealed result type
 * correctly discriminates between its Success, Error and Loading variants.
 */
class ResourceTest {

    @Test
    fun successCarriesData() {
        val resource: Resource<String> = Resource.Success("hello")

        assertIs<Resource.Success<String>>(resource)
        assertEquals("hello", resource.data)
    }

    @Test
    fun errorCarriesMessageAndOptionalThrowable() {
        val cause = IllegalStateException("boom")
        val resource: Resource<Nothing> = Resource.Error("failed", cause)

        assertIs<Resource.Error>(resource)
        assertEquals("failed", resource.message)
        assertNotNull(resource.throwable)
        assertEquals("boom", resource.throwable?.message)
    }

    @Test
    fun errorThrowableDefaultsToNull() {
        val resource: Resource<Nothing> = Resource.Error("oops")
        assertIs<Resource.Error>(resource)
        assertNull(resource.throwable)
    }

    @Test
    fun loadingIsSingletonObject() {
        // Loading is an object, so all references must be identical regardless of T.
        val a: Resource<*> = Resource.Loading
        val b: Resource<*> = Resource.Loading
        assertSame(a, b)
    }

    @Test
    fun exhaustiveWhenCoversAllVariants() {
        // A small helper that proves the sealed hierarchy is fully matchable.
        fun describe(resource: Resource<Int>): String = when (resource) {
            is Resource.Success -> "success:${resource.data}"
            is Resource.Error -> "error:${resource.message}"
            is Resource.Loading -> "loading"
        }

        assertEquals("success:42", describe(Resource.Success(42)))
        assertEquals("error:nope", describe(Resource.Error("nope")))
        assertEquals("loading", describe(Resource.Loading))
        assertTrue(true) // sanity
    }
}
