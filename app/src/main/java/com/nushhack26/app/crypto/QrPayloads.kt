package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.HandoverEntity
import com.nushhack26.app.data.local.HandoverKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.nio.ByteBuffer
import java.util.UUID

/** What a scanned QR code turned out to be. */
sealed interface ScannedCode {
    /** A signed offer, plus the record that made the giver the holder (null if that's a register record). */
    data class Offer(val offer: HandoverEntity, val proof: HandoverEntity?) : ScannedCode

    data class Acceptance(
        val id: String,
        val toDeviceId: String,
        val sealNo: String,
        val acceptedMs: Long,
        val receiverSig: String,
    ) : ScannedCode

    data class Seal(val itemCode: String, val sealNo: String) : ScannedCode

    data class Unknown(val raw: String) : ScannedCode
}

/** The CS1O:, CS1A: and CS1S: codes (docs/sync.md A.3). Card 01 fills in the bodies. */
object QrPayloads {
    /** At most 1,100 characters with a proof record. */
    fun offer(offer: HandoverEntity, proof: HandoverEntity?): String {
        val prevId = requireNotNull(offer.prevId)
        val prevHash = if (proof == null) {
            Canonical.registerHash(prevId, offer.itemId, requireNotNull(offer.fromPersonId))
        } else {
            require(proof.id == prevId && proof.itemId == offer.itemId && proof.toPersonId == offer.fromPersonId)
            require(proof.recordHash == Canonical.recordHash(proof))
            proof.recordHash
        }
        require(offer.prevHash == prevHash) { "The offer must link to its proof" }
        val json = buildJsonObject {
            put("d", uuid(offer.id))
            put("i", uuid(offer.itemId))
            put("t", uuid(offer.toPersonId))
            signedOfferFields(offer).forEach { (key, value) -> put(key, value) }
            put("p", proof?.let { h ->
                buildJsonObject {
                    // Its id, item and receiver are already in the enclosing offer.
                    signedOfferFields(h).forEach { (key, value) -> put(key, value) }
                    put("h", hash(requireNotNull(h.prevHash)))
                    put("v", uuid(requireNotNull(h.toDeviceId)))
                    put("z", requireNotNull(h.sealNo))
                    put("a", requireNotNull(h.acceptedMs))
                    put("r", requireNotNull(h.receiverSig))
                }
            } ?: JsonNull)
        }
        return ("CS1O:" + json).also { require(it.length <= 1_100) { "Offer QR exceeds 1,100 characters" } }
    }

    fun acceptance(record: HandoverEntity): String = "CS1A:" + buildJsonObject {
        put("d", uuid(record.id))
        put("v", uuid(requireNotNull(record.toDeviceId)))
        put("z", requireNotNull(record.sealNo))
        put("a", requireNotNull(record.acceptedMs))
        put("r", requireNotNull(record.receiverSig))
    }

    fun seal(itemCode: String, sealNo: String): String = "CS1S:$itemCode:$sealNo"

    fun parse(text: String): ScannedCode = runCatching {
        require(text.length <= 20_000)
        when {
            text.startsWith("CS1O:") -> {
                require(text.length <= 1_100)
                val json = Json.parseToJsonElement(text.substring(5)).jsonObject
                val offer = readOffer(json, unuuid(json.string("d")), unuuid(json.string("i")), unuuid(json.string("t")))
                val proof = when (val p = json.getValue("p")) {
                    JsonNull -> null
                    else -> {
                        val fields = p.jsonObject
                        val h = readOffer(fields, requireNotNull(offer.prevId), offer.itemId, requireNotNull(offer.fromPersonId))
                            .copy(
                                prevHash = unhash(fields.string("h")), toDeviceId = unuuid(fields.string("v")),
                                sealNo = fields.string("z"), acceptedMs = fields.time("a"), receiverSig = fields.string("r"),
                            )
                        h.copy(recordHash = Canonical.recordHash(h))
                    }
                }
                ScannedCode.Offer(
                    offer.copy(prevHash = proof?.recordHash ?: Canonical.registerHash(
                        requireNotNull(offer.prevId), offer.itemId, requireNotNull(offer.fromPersonId),
                    )), proof,
                )
            }
            text.startsWith("CS1A:") -> {
                val json = Json.parseToJsonElement(text.substring(5)).jsonObject
                ScannedCode.Acceptance(
                    unuuid(json.string("d")), unuuid(json.string("v")), json.string("z"), json.time("a"), json.string("r"),
                )
            }
            text.startsWith("CS1S:") -> {
                val parts = text.split(':')
                require(parts.size == 3 && parts[1].isNotBlank() && parts[2].isNotBlank() && '|' !in text)
                ScannedCode.Seal(parts[1], parts[2])
            }
            else -> ScannedCode.Unknown(text)
        }
    }.getOrElse { ScannedCode.Unknown(text) }

    // d/i/t = id/item/receiver; b/f/g = previous/giver/person's device;
    // u/n/o/x/s = due/nonce/offered/expires/giver signature; p = proof.
    private fun signedOfferFields(h: HandoverEntity): JsonObject = buildJsonObject {
        put("k", when (h.kind) {
            HandoverKind.ISSUE -> "i"
            HandoverKind.TRANSFER -> "t"
            HandoverKind.RETURN -> "r"
            else -> error("Not a signed handover")
        })
        put("b", uuid(requireNotNull(h.prevId)))
        put("f", uuid(requireNotNull(h.fromPersonId)))
        put("g", uuid(requireNotNull(h.fromDeviceId)))
        if (h.dueMs != null) put("u", h.dueMs)
        put("n", requireNotNull(h.nonce))
        put("o", requireNotNull(h.offeredMs))
        put("x", requireNotNull(h.expiresMs))
        put("s", requireNotNull(h.giverSig))
    }

    private fun readOffer(json: JsonObject, id: String, item: String, to: String): HandoverEntity {
        val nonce = json.string("n")
        require(nonce.length == 22 && decode(nonce).size == 16)
        return HandoverEntity(
            id = id, itemId = item, toPersonId = to,
            kind = when (json.string("k")) {
                "i" -> HandoverKind.ISSUE
                "t" -> HandoverKind.TRANSFER
                "r" -> HandoverKind.RETURN
                else -> error("Unknown handover kind")
            },
            prevId = unuuid(json.string("b")), fromPersonId = unuuid(json.string("f")),
            fromDeviceId = unuuid(json.string("g")), dueMs = json["u"]?.let { json.time("u") },
            nonce = nonce, offeredMs = json.time("o"), expiresMs = json.time("x"), giverSig = json.string("s"),
        )
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.let {
        require(it.isString && it.content.isNotEmpty() && '|' !in it.content)
        it.content
    }

    private fun JsonObject.time(key: String): Long = getValue(key).jsonPrimitive.let {
        require(!it.isString)
        it.long.also { value -> require(value >= 0) }
    }

    private fun uuid(id: String): String = UUID.fromString(id).let {
        require(it.toString() == id)
        encode(ByteBuffer.allocate(16).putLong(it.mostSignificantBits).putLong(it.leastSignificantBits).array())
    }

    private fun unuuid(id: String): String {
        require(id.length == 22)
        val bytes = decode(id)
        require(bytes.size == 16)
        return ByteBuffer.wrap(bytes).let { UUID(it.long, it.long).toString() }
    }

    private fun hash(hex: String): String {
        require(hex.matches(Regex("[0-9a-f]{64}")))
        return encode(hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
    }

    private fun unhash(value: String): String = decode(value).also { require(it.size == 32) }
        .joinToString("") { "%02x".format(java.util.Locale.ROOT, it.toInt() and 0xff) }
}
