package de.thkoeln.codescope.util

import de.thkoeln.codescope.domain.analysis.AnalysisResult

expect fun shareAnalysisResult(projectName: String, result: AnalysisResult)
