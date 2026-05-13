package de.thkoeln.codescope.util

import de.thkoeln.codescope.AppContext
import android.net.Uri
import android.annotation.SuppressLint
import android.content.Context
import android.provider.OpenableColumns
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast

actual fun readFileBytes(context: AppContext, path: String): ByteArray {
    val uri = Uri.parse(path)
    return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
}

actual class PlatformContext actual constructor(context: Any) {
    val androidContext = context as Context
}

@SuppressLint("Range")
actual fun getFileNameFromUri(context: PlatformContext, uri: String): String? {
    val androidUri = Uri.parse(uri)
    var fileName: String? = null
    context.androidContext.contentResolver.query(androidUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

actual fun readFileBytesFromUri(context: PlatformContext, uri: String): ByteArray? {
    val androidUri = Uri.parse(uri)
    return try {
        context.androidContext.contentResolver.openInputStream(androidUri)?.use { inputStream ->
            inputStream.readBytes()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

actual fun saveFileToDisk(context: PlatformContext, fileName: String, content: ByteArray) {
    val resolver = context.androidContext.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content)
            }
            Toast.makeText(context.androidContext, "Datei in Downloads gespeichert: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context.androidContext, "Fehler beim Speichern: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context.androidContext, "Konnte Datei nicht im System registrieren", Toast.LENGTH_LONG).show()
    }
}
