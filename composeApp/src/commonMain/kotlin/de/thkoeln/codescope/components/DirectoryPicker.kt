package de.thkoeln.codescope.components

import androidx.compose.runtime.Composable

@Composable
expect fun DirectoryPicker(
    show: Boolean,
    onPathSelected: (String?) -> Unit
)
