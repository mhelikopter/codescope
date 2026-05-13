package de.thkoeln.codescope.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun DirectoryPicker(
    show: Boolean,
    onPathSelected: (String?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            onPathSelected(uri?.toString())
        }
    )

    LaunchedEffect(show) {
        if (show) {
            launcher.launch(null)
        }
    }
}
