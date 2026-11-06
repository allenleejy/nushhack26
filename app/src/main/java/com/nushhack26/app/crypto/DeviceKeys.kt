package com.nushhack26.app.crypto

import android.content.Context

// This phone's signing key (docs/sync.md A.1). Card 01 fills in the bodies; the
// signatures here are what the other cards call.

/** The public half of this phone's key, as it goes on the roster. */
data class KeyInfo(
    /** X.509 SubjectPublicKeyInfo, base64url without padding. */
    val publicKey: String,
    /** For example 3F:A9:12:7C. */
    val keyFp: String,
    /** strongbox, tee or software. */
    val security: String,
)

interface DeviceKeys {
    /** The key, or null if this phone hasn't made one yet. */
    fun keyInfo(): KeyInfo?

    /**
     * Creates the key, replacing any old one. Release builds throw [NoSecureLockException]
     * on a phone without a fingerprint or PIN.
     */
    fun createKey(): KeyInfo

    /**
     * Signs [text] (its UTF-8 bytes) with SHA256withECDSA and returns base64url DER.
     * Call it within 10 seconds of a successful SignPrompt, or it throws.
     */
    fun sign(text: String): String
}

class NoSecureLockException : Exception("Set a fingerprint or PIN on this phone to sign handovers.")

/** The Keystore key, or in debug builds on a phone with no screen lock, a software key. */
fun deviceKeys(context: Context): DeviceKeys = TODO("card 01")
