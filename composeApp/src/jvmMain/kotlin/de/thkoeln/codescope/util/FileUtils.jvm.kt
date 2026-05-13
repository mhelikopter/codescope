package de.thkoeln.codescope.util

import de.thkoeln.codescope.AppContext
import java.io.File
import java.awt.FileDialog
import java.awt.Frame

actual fun readFileBytes(context: AppContext, path: String): ByteArray {
    return File(path).readBytes()
}

actual class PlatformContext actual constructor(context: Any) {
}

actual fun getFileNameFromUri(context: PlatformContext, uri: String): String? {
    return try {
        File(uri).name
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

actual fun readFileBytesFromUri(context: PlatformContext, uri: String): ByteArray? {
    return try {
        File(uri).readBytes()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

actual fun saveFileToDisk(context: PlatformContext, fileName: String, content: ByteArray) {
    val fileDialog = FileDialog(null as Frame?, "Datei speichern", FileDialog.SAVE)
    fileDialog.file = fileName
    fileDialog.isVisible = true

    val directory = fileDialog.directory
    val file = fileDialog.file
    if (directory != null && file != null) {
        File(directory, file).writeBytes(content)
    }
}
