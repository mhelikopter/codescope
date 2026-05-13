package de.thkoeln.codescope.components

import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (String?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            onFileSelected(uri?.toString())
        }
    )

    LaunchedEffect(show) {
        if (show) {
            // 1. Dateiendung in MIME Typ konverten
            val mimeTypes = fileExtensions.mapNotNull { extension ->
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }.toTypedArray()

            // 2. Übergebe diese MIME-Typen an den Launcher.
            launcher.launch(mimeTypes.takeIf { it.isNotEmpty() } ?: arrayOf("*/*"))

        }
    }
}
