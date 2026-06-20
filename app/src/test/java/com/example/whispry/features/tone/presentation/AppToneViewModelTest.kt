package com.example.whispry.features.tone.presentation

import android.content.Context
import android.content.pm.PackageManager
import com.example.whispry.features.tone.data.model.AppToneEntity
import com.example.whispry.features.tone.domain.usecase.DeleteAppToneUseCase
import com.example.whispry.features.tone.domain.usecase.GetAppTonesUseCase
import com.example.whispry.features.tone.domain.usecase.SaveAppToneUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppToneViewModelTest {

    private val context: Context = mockk(relaxed = true)
    private val pm: PackageManager = mockk()
    private val getAppTonesUseCase: GetAppTonesUseCase = mockk()
    private val saveAppToneUseCase: SaveAppToneUseCase = mockk()
    private val deleteAppToneUseCase: DeleteAppToneUseCase = mockk()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AppToneViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        io.mockk.mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        
        io.mockk.mockkConstructor(android.content.Intent::class)
        every { anyConstructed<android.content.Intent>().addCategory(any()) } returns mockk()
        
        every { context.packageName } returns "com.example.whispry"
        every { context.packageManager } returns pm
        every { pm.queryIntentActivities(any(), any<Int>()) } returns emptyList()
        every { getAppTonesUseCase() } returns flowOf(
            listOf(
                AppToneEntity("com.whatsapp", "WhatsApp", "CASUAL"),
                AppToneEntity("com.slack", "Slack", "PROFESSIONAL")
            )
        )
        
        viewModel = AppToneViewModel(
            context,
            getAppTonesUseCase,
            saveAppToneUseCase,
            deleteAppToneUseCase
        )
    }

    @After
    fun tearDown() {
        io.mockk.unmockkStatic(Dispatchers::class)
        io.mockk.unmockkConstructor(android.content.Intent::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads app tone mappings correctly`() = runTest {
        val collectJob = launch { viewModel.appTones.collect {} }
        advanceUntilIdle()
        val tones = viewModel.appTones.value
        assertEquals(2, tones.size)
        assertEquals("com.whatsapp", tones[0].packageName)
        assertEquals("com.slack", tones[1].packageName)
        collectJob.cancel()
    }

    @Test
    fun `saveAppTone calls saveAppToneUseCase`() = runTest {
        coEvery { saveAppToneUseCase(any()) } returns Unit
        
        viewModel.saveAppTone("com.whatsapp", "WhatsApp", "CASUAL", "Be cool")
        advanceUntilIdle()

        coVerify {
            saveAppToneUseCase(AppToneEntity("com.whatsapp", "WhatsApp", "CASUAL", "Be cool"))
        }
    }

    @Test
    fun `deleteAppTone calls deleteAppToneUseCase`() = runTest {
        coEvery { deleteAppToneUseCase(any()) } returns Unit
        
        viewModel.deleteAppTone("com.slack")
        advanceUntilIdle()

        coVerify {
            deleteAppToneUseCase("com.slack")
        }
    }
}
