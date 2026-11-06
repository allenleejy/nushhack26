package com.nushhack26.app.auth

import com.nushhack26.app.data.local.HandoverKind
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.Roles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Who may do what (docs/sync.md A.8) and the offline access lease (A.9). Pure, so it works
// offline and in tests. The server's sync_push checks the same rules (C.3.4 and C.3.4b).

/** One authorisation check: passed or not, and the line to show. */
data class AccessCheck(val ok: Boolean, val text: String)

/** How long this phone may keep working offline before it must sync once (A.9). */
data class AccessLease(
    /** The last time the server confirmed this phone's access: the later of the last sync and enrolment. */
    val confirmedAt: Long?,
    val validUntil: Long?,
    val expired: Boolean,
)

object AccessRules {
    const val LEASE_MS = 72L * 60 * 60 * 1000

    /** The kind of handover from a giver with [giverRole] to a receiver with [receiverRole], or null if none is allowed. */
    fun kindFor(giverRole: String, receiverRole: String): String? = when {
        giverRole == Roles.STORE_OFFICER && receiverRole == Roles.RADIOGRAPHER -> HandoverKind.ISSUE
        giverRole == Roles.RADIOGRAPHER && receiverRole == Roles.RADIOGRAPHER -> HandoverKind.TRANSFER
        giverRole == Roles.RADIOGRAPHER && receiverRole == Roles.STORE_OFFICER -> HandoverKind.RETURN
        else -> null
    }

    /** Whether [receiver] may take a camera from [giver] at [atMs], as G1 shows it ("Jun Lim · authorisation lapsed 3 Nov"). */
    fun checkPick(giver: PersonEntity, receiver: PersonEntity, atMs: Long): AccessCheck {
        val failure = failure(giver, receiver, atMs) ?: return AccessCheck(true, receiver.name)
        return AccessCheck(false, "${receiver.name} · ${failure.pickText}")
    }

    /** R2 line 6: whether [me], the receiver, may take this camera from [giver] at [atMs]. */
    fun checkReceiver(giver: PersonEntity?, me: PersonEntity, atMs: Long): AccessCheck {
        if (giver == null) return AccessCheck(false, "Unknown signer. Sync this phone before accepting.")
        val failure = failure(giver, me, atMs)
        if (failure != null) return AccessCheck(false, failure.receiverText)
        val until = me.authorisedUntilMs
        val text = when {
            me.role == Roles.STORE_OFFICER -> "Authorised · store officer"
            until == null -> "Authorised · ${roleName(me.role)}"
            else -> "Authorised · ${roleName(me.role)} · valid to ${longDate(until)}"
        }
        return AccessCheck(true, text)
    }

    /** Whether this person may sign a safety officer's decision. */
    fun maySignDecision(person: PersonEntity): Boolean = person.active && person.role == Roles.RSO

    fun lease(lastSyncAt: Long?, enrolledAt: Long?, now: Long): AccessLease {
        val confirmedAt = listOfNotNull(lastSyncAt, enrolledAt).maxOrNull()
            ?: return AccessLease(confirmedAt = null, validUntil = null, expired = true)
        val validUntil = confirmedAt + LEASE_MS
        return AccessLease(confirmedAt, validUntil, expired = now > validUntil)
    }

    const val LEASE_EXPIRED_TEXT = "Connect once to confirm your access. Until then you can look but not sign."

    private class Failure(val pickText: String, val receiverText: String)

    private fun failure(giver: PersonEntity, receiver: PersonEntity, atMs: Long): Failure? {
        val kind = kindFor(giver.role, receiver.role)
            ?: return Failure("can't receive from ${roleName(giver.role)}", wrongRole(giver.role, receiver.role))
        if (!receiver.active) {
            return Failure("no longer on the active roster", "${receiver.name} is no longer on the active roster")
        }
        val until = receiver.authorisedUntilMs
        if (kind != HandoverKind.RETURN && until != null && until < atMs) {
            return Failure(
                "authorisation lapsed ${shortDate(until)}",
                "You aren't authorised to hold sealed sources (lapsed ${shortDate(until)})",
            )
        }
        return null
    }

    private fun wrongRole(giverRole: String, receiverRole: String): String = when {
        giverRole == Roles.RSO || receiverRole == Roles.RSO -> "A safety officer doesn't hold cameras"
        giverRole == Roles.RADIOGRAPHER -> "A radiographer can only hand to a radiographer or return to store"
        receiverRole == Roles.STORE_OFFICER -> "A store officer can't issue to another store officer"
        else -> "A ${roleName(giverRole)} can't hand cameras to a ${roleName(receiverRole)}"
    }

    fun roleName(role: String): String = when (role) {
        Roles.STORE_OFFICER -> "store officer"
        Roles.RADIOGRAPHER -> "radiographer"
        Roles.RSO -> "safety officer"
        else -> role
    }

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val shortFormat = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val longFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

    /** "3 Nov" */
    fun shortDate(ms: Long): String = shortFormat.format(Instant.ofEpochMilli(ms).atZone(zone))

    /** "30 Jun 2027" */
    fun longDate(ms: Long): String = longFormat.format(Instant.ofEpochMilli(ms).atZone(zone))
}
