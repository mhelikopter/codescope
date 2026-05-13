package de.thkoeln.codescope.util

import androidx.compose.runtime.Composable

@Composable
expect fun calculateProjectSize(path: String): Long
