package com.nushhack26.app.ui.give

import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class GiveLogicTest {
    private val zone = ZoneId.of("Asia/Singapore")
    private fun at(hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 11, 6, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun dueChipsAddTimeOrPickTheNextClockTime() {
        val now = at(14)
        assertEquals(now + 90_000, DueChip.Plus90s.dueMs(now, zone))
        assertEquals(now + 300_000, DueChip.Plus5Min.dueMs(now, zone))
        assertEquals(at(17), DueChip.At1700.dueMs(now, zone))
        assertEquals(at(19), DueChip.At1900.dueMs(now, zone))
    }

    @Test
    fun aPassedClockTimeMeansTomorrow() {
        val now = at(18, 30)
        assertEquals(at(17) + 86_400_000, DueChip.At1700.dueMs(now, zone))
        assertEquals(at(19), DueChip.At1900.dueMs(now, zone))
    }

    @Test
    fun radiographerSeesColleaguesAndStoreWithLapsedPeopleBlocked() {
        val now = at(14)
        val me = PersonEntity("p-ravi", "Ravi Kumar", Roles.RADIOGRAPHER, "S1")
        val people = listOf(
            me,
            PersonEntity("p-wei", "Tan Wei Ming", Roles.RADIOGRAPHER, "S2"),
            PersonEntity("p-jun", "Jun Lim", Roles.RADIOGRAPHER, "S3", authorisedUntilMs = at(9)),
            PersonEntity("p-ali", "Siti Aliyah", Roles.STORE_OFFICER, "S4"),
        )
        val options = GiveViewModel.receiversFor(me, people, now)
        assertEquals(listOf("p-wei", "p-jun", "p-ali"), options.map { it.id })
        assertEquals(listOf("Hand to a colleague", "Hand to a colleague", "Return to store"), options.map { it.group })
        assertFalse(options.first { it.id == "p-jun" }.selectable)
        assertTrue(options.first { it.id == "p-jun" }.text.startsWith("Jun Lim · authorisation lapsed"))
        assertTrue(options.first { it.id == "p-ali" }.selectable)
    }

    @Test
    fun storeOfficerIssuesOnlyToRadiographers() {
        val me = PersonEntity("p-ali", "Siti Aliyah", Roles.STORE_OFFICER, "S4")
        val people = listOf(me, PersonEntity("p-wei", "Tan Wei Ming", Roles.RADIOGRAPHER, "S2"), PersonEntity("p-o2", "Lee Hock", Roles.STORE_OFFICER, "S5"))
        val options = GiveViewModel.receiversFor(me, people, at(14))
        assertEquals(listOf("p-wei"), options.map { it.id })
        assertEquals("Issue to", options.single().group)
    }

    @Test
    fun callNames() {
        assertEquals("Wei Ming", callName("Tan Wei Ming"))
        assertEquals("Ravi", callName("Ravi Kumar"))
    }
}
