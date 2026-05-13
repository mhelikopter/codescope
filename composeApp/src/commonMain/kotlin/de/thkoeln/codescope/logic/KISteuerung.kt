package de.thkoeln.codescope.logic

import de.thkoeln.codescope.domain.ai.AiModel

/**
 * Controller responsible for handling AI model selection within CodeScope.
 *
 * This controller provides the available AI models and validates the user's
 * selected model. It does not execute any AI analysis itself. Instead, the
 * selected model is passed to other use cases such as LF30 (start analysis).
 *
 * Linked LF: **LF50 – KI-Modell auswählen**
 */
class KISteuerung : IKISteuerung{

    /**
     * Returns the list of available AI models that the user can choose from.
     *
     * **LF50 – KI-Modell auswählen**
     *
     * The models originate from the domain enumeration [AiModel] and represent
     * selectable AI configurations (e.g., different Gemini model variants).
     *
     * @return A list of all available [AiModel] entries.
     */
    override fun listAvailableModels(): List<AiModel> {
        return AiModel.entries
    }

    /**
     * Validates and selects an AI model based on a given model identifier.
     *
     * **LF50 – KI-Modell auswählen**
     *
     * The method checks if the provided model ID corresponds to a known
     * [AiModel]. If the model exists, it is returned. Otherwise, an error
     * is produced.
     *
     * @param modelId The string identifier of the AI model the user selected.
     *
     * @return A [Result] wrapping the selected [AiModel] on success,
     *         or a failure if the ID does not match any available model.
     *
     * @throws IllegalArgumentException If the model ID is unknown.
     */
    override fun selectModel(modelId: String): Result<AiModel> {
        val model = AiModel.entries.find { it.id == modelId }
            ?: return Result.failure(IllegalArgumentException("Unknown AI model: $modelId"))

        return Result.success(model)
    }
}