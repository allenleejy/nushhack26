package com.nushhack26.app.ui.components

import com.nushhack26.app.data.local.HandoverEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptNumbersTest {
    private fun h(id: String, prev: String?) = HandoverEntity(id = id, itemId = "c12", kind = "HANDOVER", prevId = prev, toPersonId = "p")

    private val register = h("r", null)
    private val second = h("a", "r")
    private val third = h("b", "a")
    private val chain = listOf(third, register, second)

    @Test fun registerIsRecordOne() = assertEquals(1, ReceiptNumbers.recordNo(register, chain))

    @Test fun countsAlongPrevLinks() = assertEquals(3, ReceiptNumbers.recordNo(third, chain))

    @Test fun missingLinkGivesNull() = assertNull(ReceiptNumbers.recordNo(third, listOf(third, register)))

    @Test fun loopGivesNull() = assertNull(ReceiptNumbers.recordNo(h("x", "y"), listOf(h("x", "y"), h("y", "x"))))

    @Test fun shortHashInFours() =
        assertEquals("SHA 4F2A 91C0 77E3 0B1D", ReceiptNumbers.shortHash("4f2a91c077e30b1d" + "0".repeat(48)))

    @Test fun verifyCodeInThrees() = assertEquals("482 107", ReceiptNumbers.verifyCode("48 21 07"))
}
