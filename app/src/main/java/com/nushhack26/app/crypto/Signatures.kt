package com.nushhack26.app.crypto

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Locale

/** Checks signatures against public keys from the roster. Works offline. Card 01. */
object Signatures {
    /** True if [signature] (base64url DER) is [publicKey]'s SHA256withECDSA signature of [text]. */
    fun verify(publicKey: String, text: String, signature: String): Boolean = runCatching {
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(decode(publicKey)))
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(key)
            update(text.toByteArray(Charsets.UTF_8))
            verify(decode(signature))
        }
    }.getOrDefault(false)

    /** The first 4 bytes of SHA-256 of the key's DER, as 3F:A9:12:7C. */
    fun keyFp(publicKey: String): String = MessageDigest.getInstance("SHA-256")
        .digest(decode(publicKey)).take(4)
        .joinToString(":") { "%02X".format(Locale.ROOT, it.toInt() and 0xff) }
}

internal fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

internal fun decode(text: String): ByteArray = Base64.getUrlDecoder().decode(text)
