package com.nushhack26.app.auth

import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime
import java.util.TimeZone

/** docs/sync.md A.8 and A.9, with the seed's people (docs/spec.md section 8). */
class AccessRulesTest {
    private lateinit var savedZone: TimeZone

    @Before fun singaporeTime() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Singapore"))
    }

    @After fun restoreZone() = TimeZone.setDefault(savedZone)

    private fun endOfDay(date: String) = ZonedDateTime.parse("${date}T23:59:59+08:00").toInstant().toEpochMilli()

    private val now = ZonedDateTime.parse("2026-11-07T14:30:00+08:00").toInstant().toEpochMilli()
    private val siti = PersonEntity("p1", "Siti Aminah", Roles.STORE_OFFICER, "S-1021")
    private val ravi = PersonEntity("p2", "Ravi Kumar", Roles.RADIOGRAPHER, "R-2210", authorisedUntilMs = endOfDay("2027-06-30"))
    private val weiMing = PersonEntity("p3", "Tan Wei Ming", Roles.RADIOGRAPHER, "R-2234", authorisedUntilMs = endOfDay("2027-06-30"))
    private val jun = PersonEntity("p4", "Jun Lim", Roles.RADIOGRAPHER, "R-2251", authorisedUntilMs = endOfDay("2026-11-03"))
    private val drTan = PersonEntity("p5", "Dr Tan Mei Ling", Roles.RSO, "O-3001")

    @Test fun kindsFollowTheRoles() {
        assertEquals(HandoverKind.ISSUE, AccessRules.kindFor(Roles.STORE_OFFICER, Roles.RADIOGRAPHER))
        assertEquals(HandoverKind.TRANSFER, AccessRules.kindFor(Roles.RADIOGRAPHER, Roles.RADIOGRAPHER))
        assertEquals(HandoverKind.RETURN, AccessRules.kindFor(Roles.RADIOGRAPHER, Roles.STORE_OFFICER))
        assertNull(AccessRules.kindFor(Roles.STORE_OFFICER, Roles.STORE_OFFICER))
        assertNull(AccessRules.kindFor(Roles.RSO, Roles.RADIOGRAPHER))
        assertNull(AccessRules.kindFor(Roles.RADIOGRAPHER, Roles.RSO))
    }

    @Test fun anAuthorisedReceiverPasses() {
        val check = AccessRules.checkReceiver(giver = ravi, me = weiMing, atMs = now)
        assertTrue(check.ok)
        assertEquals("Authorised · radiographer · valid to 30.06.2027", check.text)
    }

    @Test fun aStoreOfficerReceivingAReturnPasses() {
        val check = AccessRules.checkReceiver(giver = ravi, me = siti, atMs = now)
        assertTrue(check.ok)
        assertEquals("Authorised · store officer", check.text)
    }

    @Test fun aLapsedReceiverIsRefusedOnTheirOwnPhone() {
        val check = AccessRules.checkReceiver(giver = ravi, me = jun, atMs = now)
        assertFalse(check.ok)
        assertEquals("You aren't authorised to hold sealed sources (lapsed 03.11.2026)", check.text)
    }

    @Test fun aLapsedReceiverIsGreyedOutInThePicker() {
        val check = AccessRules.checkPick(giver = ravi, receiver = jun, atMs = now)
        assertFalse(check.ok)
        assertEquals("Jun Lim · authorisation lapsed 03.11.2026", check.text)
        assertTrue(AccessRules.checkPick(giver = siti, receiver = ravi, atMs = now).ok)
    }

    @Test fun authorisationCountsUntilTheEndOfItsLastDay() {
        assertTrue(AccessRules.checkReceiver(ravi, jun, atMs = endOfDay("2026-11-03")).ok)
        assertFalse(AccessRules.checkReceiver(ravi, jun, atMs = endOfDay("2026-11-03") + 1).ok)
    }

    @Test fun aLapsedRadiographerMayStillReturnACamera() {
        assertTrue(AccessRules.checkPick(giver = jun, receiver = siti, atMs = now).ok)
    }

    @Test fun anInactivePersonIsRefused() {
        val gone = weiMing.copy(active = false)
        val check = AccessRules.checkReceiver(ravi, gone, now)
        assertFalse(check.ok)
        assertEquals("Tan Wei Ming is no longer on the active roster", check.text)
    }

    @Test fun wrongRolesAreRefused() {
        assertFalse(AccessRules.checkReceiver(giver = siti, me = siti.copy(id = "p9"), atMs = now).ok)
        assertFalse(AccessRules.checkReceiver(giver = drTan, me = ravi, atMs = now).ok)
        assertFalse(AccessRules.checkReceiver(giver = null, me = ravi, atMs = now).ok)
    }

    @Test fun onlyASafetyOfficerSignsDecisions() {
        assertTrue(AccessRules.maySignDecision(drTan))
        assertFalse(AccessRules.maySignDecision(ravi))
        assertFalse(AccessRules.maySignDecision(drTan.copy(active = false)))
    }

    @Test fun theLeaseLasts72HoursFromTheLaterOfSyncAndEnrolment() {
        val hour = 60 * 60 * 1000L
        val lease = AccessRules.lease(lastSyncAt = now - 10 * hour, enrolledAt = now - 50 * hour, now = now)
        assertEquals(now - 10 * hour, lease.confirmedAt)
        assertEquals(now + 62 * hour, lease.validUntil)
        assertFalse(lease.expired)

        assertTrue(AccessRules.lease(lastSyncAt = now - 73 * hour, enrolledAt = null, now = now).expired)
        assertFalse(AccessRules.lease(lastSyncAt = null, enrolledAt = now - hour, now = now).expired)
        assertTrue(AccessRules.lease(lastSyncAt = null, enrolledAt = null, now = now).expired)
    }

    @Test fun theUnlockChallengeHasTheCanonicalForm() {
        assertEquals(
            "CS1-UNLOCK|dev-1|person-1|AAECAwQFBgcICQoLDA0ODw|1794466800000",
            Unlocker.challenge("dev-1", "person-1", "AAECAwQFBgcICQoLDA0ODw", 1794466800000),
        )
        assertEquals(22, Unlocker.newNonce().length)
    }
}
