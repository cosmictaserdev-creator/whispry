package com.example.whispry.features.tone.domain.usecase

import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.features.tone.domain.repository.AppToneRepository
import javax.inject.Inject

class SaveAppToneUseCase @Inject constructor(
    private val repository: AppToneRepository
) {
    suspend operator fun invoke(mapping: AppToneEntity) = repository.saveAppTone(mapping)
}
