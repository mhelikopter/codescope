package de.thkoeln.codescope

/**
 * Implements the ‘expect’ declaration from commonMain for the Android platform.
 * On Android, Firebase initialization usually happens automatically
 * through the google-services plugin and the google-services.json file.
 * Therefore, this function can remain empty.
 */
actual fun initializeFirebase() {
}
