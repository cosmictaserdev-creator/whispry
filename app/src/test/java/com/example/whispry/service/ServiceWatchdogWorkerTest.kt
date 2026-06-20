package com.example.whispry.service

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ServiceWatchdogWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)

    private lateinit var worker: ServiceWatchdogWorker

    @Before
    fun setup() {
        worker = spyk(ServiceWatchdogWorker(context, workerParams), recordPrivateCalls = true)
        
        // Mock private methods to avoid accessing Android system settings/services in unit tests
        every { worker["isAccessibilityEnabled"]() } returns true
        every { worker["isServiceRunning"](any<Class<*>>()) } returns true
    }

    @Test
    fun `test worker execution returns success`() = runBlocking {
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
