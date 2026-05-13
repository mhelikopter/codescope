package de.thkoeln.codescope

actual fun initializeFirebase() {
    // JVM/Desktop: Firebase wird nicht initialisiert (kein GitLive/Firebase im Desktop-Build)
}
