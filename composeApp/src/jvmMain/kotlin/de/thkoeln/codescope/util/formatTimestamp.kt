package de.thkoeln.codescope.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

actual fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Unbekannt"

    val formatter = DateTimeFormatter
        .ofPattern("dd.MM.yyyy, HH:mm 'Uhr'")
        .withZone(ZoneId.systemDefault())

    return formatter.format(Instant.ofEpochMilli(timestamp))
}
