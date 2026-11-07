package com.nushhack26.app.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LivePullTest {
    @Test
    fun `syncs every tick while online and enrolled, and stops when the screen goes`() = runTest {
        var syncs = 0
        val pull = LivePull({ syncs++ }, MutableStateFlow(true), { true }, {})
        val job = launch { pull.run() }
        runCurrent()
        advanceTimeBy(LivePull.POLL_MS * 3)
        runCurrent()
        assertEquals(4, syncs)
        job.cancel()
        advanceTimeBy(LivePull.POLL_MS * 3)
        assertEquals(4, syncs)
    }

    @Test
    fun `waits while offline or not enrolled`() = runTest {
        var syncs = 0
        val online = MutableStateFlow(false)
        var enrolled = true
        val job = launch { LivePull({ syncs++ }, online, { enrolled }, {}).run() }
        advanceTimeBy(LivePull.POLL_MS * 2)
        assertEquals(0, syncs)
        online.value = true
        enrolled = false
        advanceTimeBy(LivePull.POLL_MS * 2)
        assertEquals(0, syncs)
        job.cancel()
    }

    @Test
    fun `not enrolled runs the withdrawal check, other failures keep polling`() = runTest {
        var checks = 0
        var syncs = 0
        val errors = ArrayDeque(listOf("not enrolled", "timeout"))
        val pull = LivePull({ syncs++; errors.removeFirstOrNull()?.let { throw IllegalStateException(it) } },
            MutableStateFlow(true), { true }, { checks++ })
        val job = launch { pull.run() }
        runCurrent()
        advanceTimeBy(LivePull.POLL_MS * 2)
        runCurrent()
        assertEquals(3, syncs)
        assertEquals(1, checks)
        job.cancel()
    }
}
