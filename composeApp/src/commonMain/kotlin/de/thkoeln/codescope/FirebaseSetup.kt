package de.thkoeln.codescope

/**
 * Expects a function to initialize Firebase.
 * Each platform (android, jvm) must implement this function with ‘actual’.
 */
expect fun initializeFirebase()
