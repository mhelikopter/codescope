package de.thkoeln.codescope.domain.ai

import kotlinx.serialization.Serializable

/**
 * Enumeration of available AI models.
 * (Extendable in the future: Gemini, GPT, Local models, etc.)
 */
@Serializable
enum class AiModel(val id: String) {
    GEMINI_2_FLASH("gemini-2.0-flash"),
    GEMINI_25_FLASH("gemini-2.5-flash"),
    GEMINI_25_PRO("gemini-2.5-pro");
}