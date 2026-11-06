package com.nushhack26.app.auth

import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.crypto.Signatures
import com.nushhack26.app.data.repo.IdentityRepository
import java.security.SecureRandom
import java.util.Base64

sealed interface UnlockResult {
    data object Unlocked : UnlockResult

    /** Not enrolled, no key, or the key doesn't match the enrolment. The app stays locked. */
    data class Refused(val message: String) : UnlockResult
}

/**
 * Unlocks the app offline (docs/sync.md A.7): the phone signs a fresh CS1-UNLOCK challenge
 * with its hardware key and checks the signature against the public key the server accepted
 * at enrolment. Call [verify] within 10 seconds of a successful SignPrompt.
 */
class Unlocker(
    private val keys: () -> DeviceKeys,
    private val identity: IdentityRepository,
    private val appLock: AppLock,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun verify(): UnlockResult {
        val me = identity.current() ?: return UnlockResult.Refused("This phone isn't enrolled.")
        val enrolledKey = identity.enrolledPublicKey() ?: return KEY_MISMATCH
        val keyInfo = runCatching { keys().keyInfo() }.getOrNull() ?: return KEY_MISMATCH
        if (keyInfo.publicKey != enrolledKey) return KEY_MISMATCH

        val challenge = challenge(me.deviceId, me.personId, newNonce(), now())
        val signature = try {
            keys().sign(challenge)
        } catch (e: Exception) {
            return UnlockResult.Refused("Fingerprint cancelled. Tenet stays locked.")
        }
        if (!Signatures.verify(enrolledKey, challenge, signature)) return KEY_MISMATCH

        appLock.markUnlocked()
        return UnlockResult.Unlocked
    }

    companion object {
        private val KEY_MISMATCH =
            UnlockResult.Refused("This phone's key doesn't match its enrolment. Clear the app's storage and enrol again.")

        /** `CS1-UNLOCK|device_id|person_id|nonce|unlock_ms` (docs/sync.md A.2). */
        fun challenge(deviceId: String, personId: String, nonce: String, unlockMs: Long): String =
            "CS1-UNLOCK|$deviceId|$personId|$nonce|$unlockMs"

        fun newNonce(): String {
            val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
