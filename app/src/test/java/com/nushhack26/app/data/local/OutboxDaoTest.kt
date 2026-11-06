package com.nushhack26.app.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class OutboxDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savedEntryReadsBack() = runTest {
        val entry = OutboxEntry(
            opId = "op-1",
            tableName = "handovers",
            recordId = "rec-1",
            kind = OutboxKind.INSERT,
            payload = """{"id":"rec-1"}""",
            baseVersion = null,
            createdAt = 1_000L,
        )
        db.outboxDao().insert(entry)

        assertEquals(listOf(entry), db.outboxDao().nextBatch(10))
        assertEquals(1, db.outboxDao().observeCount().first())
    }

    @Test
    fun failedEntryCountsAttempts() = runTest {
        val dao = db.outboxDao()
        dao.insert(OutboxEntry("op-2", "handovers", "rec-2", OutboxKind.UPDATE, "{}", 3L, 2_000L))
        dao.markFailed("op-2", "timeout")

        val saved = dao.nextBatch(10).single()
        assertEquals(1, saved.attempts)
        assertEquals("timeout", saved.lastError)

        dao.delete(listOf("op-2"))
        assertEquals(0, dao.observeCount().first())
    }
}
