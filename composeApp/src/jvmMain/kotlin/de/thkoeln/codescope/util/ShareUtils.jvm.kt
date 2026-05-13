package de.thkoeln.codescope.util

import de.thkoeln.codescope.domain.analysis.AnalysisResult

actual fun shareAnalysisResult(projectName: String, result: AnalysisResult) {
    // Auf Desktop könnten wir das Ergebnis in die Zwischenablage kopieren oder eine Datei speichern.
    // Da der User explizit nach dem "Teilen"-Button auf Mobile gefragt hat, lassen wir das hier erstmal als Info.
    println("Teilen auf Desktop nicht implementiert. Projekt: $projectName")
}
