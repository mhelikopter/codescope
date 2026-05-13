package de.thkoeln.codescope.util

/**
 * Formats a timestamp (Long) into a readable German date string.
 * Example: "22.05.2024, 14:30 Uhr"
 */
expect fun formatTimestamp(timestamp: Long): String
