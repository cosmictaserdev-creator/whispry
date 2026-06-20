package com.example.whispry.features.tone.domain.usecase

import com.example.whispry.features.tone.domain.repository.AppToneRepository
import javax.inject.Inject

class DeleteAppToneUseCase @Inject constructor(
    private val repository: AppToneRepository
) {
    suspend operator fun invoke(packageName: String) = repository.deleteAppTone(packageName)
}
