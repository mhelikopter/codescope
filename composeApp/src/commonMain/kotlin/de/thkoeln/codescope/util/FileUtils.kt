package de.thkoeln.codescope.util

import de.thkoeln.codescope.AppContext

expect fun readFileBytes(context: AppContext, path: String): ByteArray
expect class PlatformContext(context: Any)

expect fun getFileNameFromUri(context: PlatformContext, uri: String): String?

// Funktion, die den Dateiinhalt liest
expect fun readFileBytesFromUri(context: PlatformContext, uri: String): ByteArray?

/**
 * Öffnet einen Dialog zum Speichern einer Datei auf dem lokalen System.
 * Auf Android wird die Datei direkt in den Downloads-Ordner gespeichert.
 */
expect fun saveFileToDisk(context: PlatformContext, fileName: String, content: ByteArray)
