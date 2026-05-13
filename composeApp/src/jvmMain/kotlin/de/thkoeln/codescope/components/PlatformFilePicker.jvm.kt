package de.thkoeln.codescope.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.thkoeln.codescope.util.PlatformContext
import de.thkoeln.codescope.util.getFileNameFromUri
import de.thkoeln.codescope.util.readFileBytesFromUri

import java.awt.FileDialog
import java.awt.Frame

@Composable
actual fun PlatformFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (fileBytes: ByteArray, fileName: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        // LaunchedEffect stellt sicher, dass der Dialog nur einmal pro "Anzeige" gestartet wird
        LaunchedEffect(Unit) {
            val dialog = FileDialog(null as Frame?, "Datei wählen", FileDialog.LOAD).apply {
                // Filter setzen
                if (fileExtensions.isNotEmpty()) {
                    file = fileExtensions.joinToString(";") { "*.$it" }
                }

                filenameFilter = java.io.FilenameFilter { _, name ->
                    val nameLower = name.lowercase()
                    fileExtensions.any { extension -> 
                        nameLower.endsWith(".${extension.lowercase()}") 
                    }
                }
            }
            
            // Blockiert den Thread, bis der Nutzer auswählt oder abbricht
            dialog.isVisible = true

            val file = dialog.file
            val directory = dialog.directory

            if (file != null && directory != null) {
                val filePath = directory + file
                val platformContext = PlatformContext(Unit)
                val fileBytes = readFileBytesFromUri(platformContext, filePath)
                val fileName = getFileNameFromUri(platformContext, filePath)

                if (fileBytes != null && fileName != null) {
                    onFileSelected(fileBytes, fileName)
                } else {
                    onDismiss()
                }
            } else {
                // Nutzer hat abgebrochen
                onDismiss()
            }
            dialog.dispose()
        }
    }
}
