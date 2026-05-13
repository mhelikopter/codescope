package de.thkoeln.codescope.util

import androidx.compose.runtime.Composable
import java.io.File

@Composable
actual fun calculateProjectSize(path: String): Long {
    val file = File(path)
    if (!file.exists()) {
        return 0L
    }
    return file.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
}
