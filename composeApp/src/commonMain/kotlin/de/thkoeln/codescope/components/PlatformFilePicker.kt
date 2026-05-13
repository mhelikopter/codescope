package de.thkoeln.codescope.components

import androidx.compose.runtime.Composable

/**
 * Expects a platform-specific composable function that displays a file picker.
 * This function is responsible for opening the picker and, if successful,
 * returning the raw data of the file.
 *
 * @param show Whether to display the file picker.
 * @param onFileSelected Callback that is called when a file has been successfully
 * selected and read. Returns the file bytes and the file name.
 * @param onDismiss Callback that is called when the picker is closed without a selection.
 */
@Composable
expect fun PlatformFilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (fileBytes: ByteArray, fileName: String) -> Unit,
    onDismiss: () -> Unit
)
