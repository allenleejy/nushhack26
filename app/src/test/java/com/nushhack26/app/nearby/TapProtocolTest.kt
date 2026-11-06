package com.nushhack26.app.nearby

import org.junit.Assert.*
import org.junit.Test

class TapProtocolTest {
    private val key = ByteArray(16) { it.toByte() }

    @Test fun encodeParse() {
        val offer = "CS1O:quotes\"\\\n雪"
        val parsed = TapPayload.parse(TapPayload.encode(offer, key))!!
        assertEquals(offer, parsed.offer)
        assertArrayEquals(key, parsed.key)
        assertEquals("", TapPayload.parse(TapPayload.encode("", key))!!.offer)
        assertEquals(16, TapPayload.newKey().size)
    }

    @Test fun invalidPayloads() {
        listOf("", "CS1T:", "CS1T:{}", "CS1T:{\"o\":1,\"k\":\"bad\"}", "CS1T:{\"o\":\"x\",\"k\":\"bad\"}").forEach {
            assertNull(TapPayload.parse(it))
        }
        assertNull(TapPayload.parse("CS1T:" + "x".repeat(1_400)))
    }

    @Test fun namesAreDeterministic() {
        assertEquals("G-15ac2830", TapPayload.giverName(key))
        assertEquals("R-b2ea362a", TapPayload.receiverName(key))
        assertEquals(TapPayload.giverName(key), TapPayload.giverName(key.copyOf()))
        assertEquals(TapPayload.receiverName(key), TapPayload.receiverName(key.copyOf()))
        assertTrue(TapPayload.giverName(key).matches(Regex("G-[0-9a-f]{8}")))
        assertTrue(TapPayload.receiverName(key).matches(Regex("R-[0-9a-f]{8}")))
        assertNotEquals(TapPayload.giverName(key), TapPayload.giverName(ByteArray(16)))
    }

    @Test fun fullLengthReaderLoop() {
        val overhead = TapPayload.encode("", key).length
        val text = TapPayload.encode("x".repeat(1_400 - overhead), key)
        assertEquals(1_400, text.length)
        val bytes = text.toByteArray()
        var reads = 0
        var finished = false
        assertEquals(text, TapApdu.read {
            if (it[1] == 0xB0.toByte()) reads++
            finished = finished || TapApdu.isLastRead(it, bytes)
            TapApdu.answer(it, bytes)
        })
        assertEquals(7, reads)
        assertTrue(finished)
        assertEquals("x".repeat(1_400 - overhead), TapPayload.parse(text)!!.offer)
    }

    @Test fun wrongAidAndUnsupported() {
        val wrong = TapApdu.select.also { it[it.lastIndex] = 2 }
        assertArrayEquals(byteArrayOf(0x6A, 0x82.toByte()), TapApdu.answer(wrong, byteArrayOf(1)))
        assertArrayEquals(byteArrayOf(0x6A, 0x82.toByte()), TapApdu.answer(TapApdu.select, null))
        assertArrayEquals(byteArrayOf(0x6D, 0), TapApdu.answer(byteArrayOf(), byteArrayOf(1)))
        assertArrayEquals(byteArrayOf(0x6D, 0), TapApdu.answer(byteArrayOf(0, 0xB0.toByte(), 0, 1, 0xE0.toByte()), byteArrayOf(1)))
    }

    @Test(expected = IllegalArgumentException::class) fun readerRejectsFailure() { TapApdu.read { byteArrayOf(0x6A, 0x82.toByte()) } }
    @Test(expected = IllegalArgumentException::class) fun readerRejectsEmptyChunk() {
        TapApdu.read { if (it[1] == 0xA4.toByte()) byteArrayOf(0, 1, 0x90.toByte(), 0) else byteArrayOf(0x90.toByte(), 0) }
    }
}
