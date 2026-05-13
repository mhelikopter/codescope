package de.thkoeln.codescope.util

import android.content.Intent
import de.thkoeln.codescope.domain.analysis.AnalysisResult
import org.koin.core.context.GlobalContext

actual fun shareAnalysisResult(projectName: String, result: AnalysisResult) {
    val context = GlobalContext.get().get<android.content.Context>()
    
    val shareText = buildString {
        appendLine("CodeScope Analyseergebnis für Projekt: $projectName")
        appendLine("Datum: ${formatTimestamp(result.createdAt)}")
        appendLine("Score: ${result.score ?: 0}/100")
        appendLine("Modell: ${result.model}")
        appendLine()
        appendLine("Feedback:")
        result.feedback?.forEach { item ->
            appendLine("- ${item.criterion}: ${item.rating}/100")
            appendLine("  ${item.comment}")
        }
        if (!result.instructorComment.isNullOrBlank()) {
            appendLine()
            appendLine("Dozenten-Feedback:")
            appendLine(result.instructorComment)
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "CodeScope Analyse: $projectName")
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(Intent.createChooser(intent, "Teilen via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
