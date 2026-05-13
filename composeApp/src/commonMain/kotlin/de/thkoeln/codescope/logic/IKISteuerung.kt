package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.ai.AiModel

/**
 * Interface for selecting an AI model.
 *
 * Linked LF: LF50 – KI-Modell auswählen
 */
interface IKISteuerung {

    /**
     * Returns all available AI models.
     */
    fun listAvailableModels(): List<AiModel>

    /**
     * Validates and selects a specific AI model based on its identifier.
     */
    fun selectModel(modelId: String): Result<AiModel>
}