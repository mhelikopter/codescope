package de.thkoeln.codescope.components

import androidx.compose.runtime.Composable

@Composable
expect fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (String?) -> Unit
)
