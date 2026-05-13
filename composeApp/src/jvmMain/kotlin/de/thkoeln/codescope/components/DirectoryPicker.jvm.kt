package de.thkoeln.codescope.components

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame

@Composable
actual fun DirectoryPicker(
    show: Boolean,
    onPathSelected: (String?) -> Unit
) {
    if (show) {
        val fileDialog = FileDialog(null as Frame?, "Select a directory", FileDialog.LOAD)
        fileDialog.isVisible = true
        onPathSelected(fileDialog.directory?.let { it + fileDialog.file })
    }
}
