package de.thkoeln.codescope.components

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (String?) -> Unit
) {
    if (show) {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("Zip files", "zip")
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onFileSelected(fileChooser.selectedFile.absolutePath)
        } else {
            onFileSelected(null)
        }
    }
}
