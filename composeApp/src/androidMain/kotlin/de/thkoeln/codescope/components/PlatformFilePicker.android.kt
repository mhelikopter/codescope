package de.thkoeln.codescope.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import de.thkoeln.codescope.util.PlatformContext
import de.thkoeln.codescope.util.getFileNameFromUri
import de.thkoeln.codescope.util.readFileBytesFromUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun PlatformFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (fileBytes: ByteArray, fileName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    if (show) {
        // Verwenden des bestehenden `FilePicker`, der eine Uri zurückgibt.
        FilePicker(true, fileExtensions = fileExtensions) { fileUriString ->
            if (fileUriString != null) {
                // Das Einlesen von Dateien kann blockieren, daher in einer Coroutine ausführen.
                CoroutineScope(Dispatchers.IO).launch {
                    val platformContext = PlatformContext(context)
                    val fileBytes = readFileBytesFromUri(platformContext, fileUriString)
                    val fileName = getFileNameFromUri(platformContext, fileUriString)

                    if (fileBytes != null && fileName != null) {
                        // Rufe den Callback mit den gelesenen Daten auf.
                        onFileSelected(fileBytes, fileName)
                    } else {
                        onDismiss() // Fehler beim Lesen, behandeln wie Abbruch
                    }
                }
            } else {
                // Nutzer hat den Picker abgebrochen
                onDismiss()
            }
        }
    }
}
