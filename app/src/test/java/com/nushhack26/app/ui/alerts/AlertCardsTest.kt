package com.nushhack26.app.ui.alerts

import com.nushhack26.app.screenshots.SampleSafety
import com.nushhack26.app.ui.fork.DECISION_WAITING_TEXT
import com.nushhack26.app.ui.fork.ForkInput
import com.nushhack26.app.ui.fork.buildForkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertCardsTest {
    private val s = SampleSafety
    private val now = s.at(14, 35)

    @Test
    fun ordersForkOverdueSealRefused() {
        val cards = buildAlertCards(s.data(listOf(s.refusedLapsed, s.sealAlert, s.fork)), now)
        assertEquals(
            listOf(AlertCard.Fork::class, AlertCard.Overdue::class, AlertCard.SealMismatch::class, AlertCard.Refused::class),
            cards.map { it::class },
        )
    }

    @Test
    fun forkNamesGiverAndChecksSignatures() {
        val fork = buildAlertCards(s.data(listOf(s.fork)), now).first() as AlertCard.Fork
        assertTrue(fork.sentence.startsWith("Ravi Kumar signed two handovers of C-12 after H1"))
        assertTrue(fork.sentence.endsWith(SIGNATURES_VALID_TEXT))

        val tampered = buildAlertCards(s.data(listOf(s.fork), tamperH2b = true), now).first() as AlertCard.Fork
        assertFalse(tampered.signaturesValid)
        assertFalse(tampered.sentence.contains(SIGNATURES_VALID_TEXT))
    }

    @Test
    fun overdueUsesServerViewAndSaysSo() {
        val overdue = buildAlertCards(s.data(listOf(s.fork)), now).filterIsInstance<AlertCard.Overdue>().single()
        assertTrue(overdue.sentence.contains("Tan Wei Ming"))
        assertTrue(overdue.sentence.endsWith(OVERDUE_CAVEAT))
    }

    @Test
    fun refusedReasonsInWords() {
        val refused = buildAlertCards(s.data(listOf(s.refusedLapsed, s.refusedDevice)), now).filterIsInstance<AlertCard.Refused>()
        assertEquals("Refused: C-12 to Jun Lim, whose authorisation had lapsed", refused[0].sentence)
        assertTrue(refused[1].sentence.endsWith("devices are added by enrol_device only"))
    }

    @Test
    fun resolvedForkCollapses() {
        val cards = buildAlertCards(s.data(listOf(s.fork), listOf(s.resolution(synced = true))), now)
        val resolved = cards.filterIsInstance<AlertCard.Resolved>().single()
        assertTrue(resolved.text.startsWith("Resolved by Dr Tan"))
        assertTrue(cards.none { it is AlertCard.Fork })
    }

    @Test
    fun onlySafetyOfficerMayDecide() {
        val data = s.data(listOf(s.fork))
        assertTrue(buildForkState(s.fork.id, s.fork, data, s.drTan, null, ForkInput()).canDecide)
        assertFalse(buildForkState(s.fork.id, s.fork, data, s.ravi, null, ForkInput()).canDecide)
        val waiting = buildForkState(s.fork.id, s.fork, s.data(listOf(s.fork), listOf(s.resolution(synced = false))), s.drTan, null, ForkInput())
        assertEquals(DECISION_WAITING_TEXT, waiting.decision)
    }

    @Test
    fun forkBranchesNameDeliveringPhone() {
        val state = buildForkState(s.fork.id, s.fork, s.data(listOf(s.fork)), s.drTan, null, ForkInput())
        assertEquals(listOf("H2", "H2b"), state.branches.map { it.label })
        assertTrue(state.branches[0].deliveredText.startsWith("Delivered by Wei Ming's phone"))
        assertTrue(state.branches.all { it.giver.ok && it.receiver.ok })
    }
}
