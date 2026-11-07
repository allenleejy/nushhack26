package com.nushhack26.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReturnTimeMathTest {
    private val zone = ZoneId.of("Asia/Singapore")
    private fun at(h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 11, 7, h, m, s, 0, zone).toInstant().toEpochMilli()

    @Test fun readoutToday() = assertEquals("Today · in 4 h 15 min", readout(LocalTime.of(18, 45), at(14, 30), zone))

    @Test fun readoutTodayIgnoresSeconds() = assertEquals("Today · in 4 h 15 min", readout(LocalTime.of(18, 45), at(14, 30, 40), zone))

    @Test fun readoutWholeHours() = assertEquals("Today · in 2 h", readout(LocalTime.of(16, 30), at(14, 30), zone))

    @Test fun readoutUnderAnHour() = assertEquals("Today · in 45 min", readout(LocalTime.of(15, 15), at(14, 30), zone))

    @Test fun readoutTomorrow() =
        assertEquals("Tomorrow 08.11.2026 · in 21 h 5 min", readout(LocalTime.of(11, 35), at(14, 30), zone))

    @Test fun readoutSameMinuteIsTomorrow() =
        assertEquals("Tomorrow 08.11.2026 · in 24 h", readout(LocalTime.of(14, 30), at(14, 30, 20), zone))

    @Test fun quickJumpRoundsUpToFive() {
        assertEquals(LocalTime.of(15, 35), quickJump(at(14, 32), 60, zone))
        assertEquals(LocalTime.of(15, 0), quickJump(at(14, 30, 50), 30, zone))
        assertEquals(LocalTime.of(18, 35), quickJump(at(14, 31), 240, zone))
    }

    @Test fun quickJumpAcrossMidnight() {
        assertEquals(LocalTime.of(0, 0), quickJump(at(23, 56), 0, zone))
        assertEquals(LocalTime.of(1, 5), quickJump(at(23, 3), 120, zone))
        assertEquals(LocalTime.of(0, 30), quickJump(at(23, 58), 30, zone))
    }

    @Test fun wheelIndexForSameLoop() {
        assertEquals(1205, wheelIndexFor(5, 60, 1200))
        assertEquals(1200, wheelIndexFor(0, 60, 1200))
    }

    @Test fun wheelIndexForWrapsForward() {
        // 59 is centred; 2 is three rows on, past 00.
        assertEquals(1202 + 60, wheelIndexFor(2, 60, 1259))
        assertEquals(48, wheelIndexFor(0, 24, 47))
    }

    @Test fun wheelIndexForWrapsBackward() {
        // 00 is centred; 58 is two rows back.
        assertEquals(1198, wheelIndexFor(58, 60, 1200))
        assertEquals(23, wheelIndexFor(23, 24, 24))
    }
}
