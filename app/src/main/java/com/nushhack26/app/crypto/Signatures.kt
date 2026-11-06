package com.nushhack26.app.crypto

/** Checks signatures against public keys from the roster. Works offline. Card 01. */
object Signatures {
    /** True if [signature] (base64url DER) is [publicKey]'s SHA256withECDSA signature of [text]. */
    fun verify(publicKey: String, text: String, signature: String): Boolean = TODO("card 01")

    /** The first 4 bytes of SHA-256 of the key's DER, as 3F:A9:12:7C. */
    fun keyFp(publicKey: String): String = TODO("card 01")
}
