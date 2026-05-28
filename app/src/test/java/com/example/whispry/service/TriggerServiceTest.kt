package com.example.whispry.service

import android.view.KeyEvent
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue

class TriggerServiceTest {

    private lateinit var serviceBridge: ServiceBridge

    @Before
    fun setUp() {
        serviceBridge = ServiceBridge()
    }

    @Test
    fun `emitting RecordingStarted is received`() = runTest {
        serviceBridge.triggerEvent.test {
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
            assertTrue(awaitItem() is ServiceBridge.TriggerEvent.RecordingStarted)
        }
    }

    @Test
    fun `emitting multiple events are received in order`() = runTest {
        serviceBridge.triggerEvent.test {
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted)
            serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped)
            
            assertTrue(awaitItem() is ServiceBridge.TriggerEvent.RecordingStarted)
            assertTrue(awaitItem() is ServiceBridge.TriggerEvent.RecordingStopped)
        }
    }

    @Test
    fun `reset to idle is received`() = runTest {
        serviceBridge.triggerEvent.test {
            serviceBridge.emit(ServiceBridge.TriggerEvent.Idle)
            assertTrue(awaitItem() is ServiceBridge.TriggerEvent.Idle)
        }
    }
}