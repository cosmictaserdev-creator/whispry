package com.example.whispry.domain.usecase

import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.domain.model.OutputPreset
import com.example.whispry.domain.repository.GroqFormatterRepository
import com.example.whispry.domain.util.Result
import com.example.whispry.features.tone.domain.repository.AppToneRepository
import com.example.whispry.service.ServiceLocator
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FormatTranscriptUseCase @Inject constructor(
    private val groqFormatterRepository: GroqFormatterRepository,
    private val settingsProvider: SettingsProvider,
    private val appToneRepository: AppToneRepository,
    private val getActiveMemoriesUseCase: GetActiveMemoriesUseCase
) {
    suspend operator fun invoke(
        rawText: String,
        preset: OutputPreset,
        skipAppAware: Boolean = false
    ): Result<String> {
        // Guard: never ship empty/near-empty input to the model — it hallucinates content
        // to fill the void. Return the raw text untouched instead.
        if (rawText.isBlank()) return Result.Success(rawText)

        val appAwareEnabled = !skipAppAware && settingsProvider.appAwareToneEnabled.first()
        val pkg = ServiceLocator.lastForegroundPackage
        
        val mapping = if (appAwareEnabled && pkg != null) {
            appToneRepository.getAppToneByPackage(pkg)
        } else {
            null
        }

        val finalPreset = if (mapping != null) {
            OutputPreset.values().find { it.name == mapping.presetName } ?: preset
        } else {
            preset
        }

        if (finalPreset == OutputPreset.NONE) {
            return Result.Success(rawText)
        }

        val basePrompt = if (finalPreset == OutputPreset.CUSTOM) {
            val customPrompt = if (mapping != null && mapping.presetName == "CUSTOM" && mapping.customPromptOverride.isNotBlank()) {
                mapping.customPromptOverride
            } else {
                settingsProvider.customAiInstructions.first()
            }
            if (customPrompt.isBlank()) return Result.Success(rawText)
            customPrompt
        } else {
            // Inject the user's chosen output language into the Translate preset prompt.
            if (finalPreset == OutputPreset.TRANSLATE_AUTO) {
                val targetLanguage = settingsProvider.translateTargetLanguage.first().ifBlank { "English" }
                finalPreset.systemPrompt.replace(OutputPreset.TARGET_LANGUAGE_PLACEHOLDER, targetLanguage)
            } else {
                finalPreset.systemPrompt
            }
        }

        if (basePrompt.isEmpty()) {
            return Result.Success(rawText)
        }

        // --- Memory Bank Injection ---
        val memories = getActiveMemoriesUseCase()
        val prompt = if (memories.isNotEmpty()) {
            val contextString = memories.joinToString("") { "- ${it.key}: ${it.value}" }
            """
            $basePrompt
            
            # PERSONAL CONTEXT
            Use the following personal context about the user to make the transcription more accurate and personalized. 
            Only use this information if relevant to the transcript content.
            
            $contextString
            """.trimIndent()
        } else {
            basePrompt
        }

        return groqFormatterRepository.formatText(rawText, prompt)
    }
}
