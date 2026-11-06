package com.nushhack26.app.nearby

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class TapPayload(val offer: String, val key: ByteArray) {
    companion object {
        private fun keyText(key: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(key)

        fun newKey(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

        fun encode(offer: String, key: ByteArray): String {
            require(key.size == 16)
            return ("CS1T:" + buildJsonObject {
                put("o", offer)
                put("k", keyText(key))
            }).also { require(it.length <= 1_400) }
        }

        fun parse(text: String): TapPayload? = runCatching {
            require(text.startsWith("CS1T:") && text.length <= 1_400)
            val json = Json.parseToJsonElement(text.removePrefix("CS1T:")).jsonObject
            val offer = json.getValue("o").jsonPrimitive.also { require(it.isString) }.content
            val encoded = json.getValue("k").jsonPrimitive.also { require(it.isString) }.content
            val key = Base64.getUrlDecoder().decode(encoded)
            require(key.size == 16 && keyText(key) == encoded)
            TapPayload(offer, key)
        }.getOrNull()

        private fun name(role: String, key: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest((role + keyText(key)).toByteArray(Charsets.UTF_8))
            return digest.take(4).joinToString("") { "%02x".format(it) }
        }

        fun giverName(key: ByteArray): String = "G-" + name("giver", key)
        fun receiverName(key: ByteArray): String = "R-" + name("receiver", key)
    }
}
