package com.nushhack26.app.ui.enrol

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class StaffNoFormatTest {
    @Test
    fun `normalising keeps a letter plus four digits`() {
        assertEquals("R2210", normalizeStaffNo("r2210"))
        assertEquals("R2210", normalizeStaffNo("R-2210"))
        assertEquals("R2210", normalizeStaffNo("r 2210"))
        assertEquals("R2210", normalizeStaffNo("R22105"))
        assertEquals("R2210", normalizeStaffNo("r 2210 9"))
    }

    @Test
    fun `normalising waits for a letter and then takes digits only`() {
        assertEquals("", normalizeStaffNo(""))
        assertEquals("", normalizeStaffNo("2210"))
        assertEquals("R", normalizeStaffNo("R"))
        assertEquals("R", normalizeStaffNo("R-"))
        assertEquals("R2", normalizeStaffNo("R-2"))
        assertEquals("R22", normalizeStaffNo("Ra2b2"))
        assertEquals("S1021", normalizeStaffNo("9s-1021"))
    }

    @Test
    fun `transformed text adds the hyphen after the letter`() {
        assertEquals("", StaffNoTransformation.filter(AnnotatedString("")).text.text)
        assertEquals("R-", StaffNoTransformation.filter(AnnotatedString("R")).text.text)
        assertEquals("R-2210", StaffNoTransformation.filter(AnnotatedString("R2210")).text.text)
    }

    @Test
    fun `offset mapping puts the cursor after the hyphen`() {
        val mapping = StaffNoTransformation.filter(AnnotatedString("R2210")).offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(1))
        assertEquals(3, mapping.originalToTransformed(2))
        assertEquals(6, mapping.originalToTransformed(5))
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(1, mapping.transformedToOriginal(2))
        assertEquals(2, mapping.transformedToOriginal(3))
        assertEquals(5, mapping.transformedToOriginal(6))
    }

    @Test
    fun `canSubmit needs a complete staff number and the six digit code`() {
        assertEquals(false, EnrolUiState(staffNo = "R221", code = "552917").canSubmit)
        assertEquals(false, EnrolUiState(staffNo = "R2210", code = "55291").canSubmit)
        assertEquals(true, EnrolUiState(staffNo = "R2210", code = "552917").canSubmit)
    }

    @Test
    fun `enrolling sends the formatted staff number`() {
        assertEquals("R-2210", formatStaffNo("R2210"))
        assertEquals("R", formatStaffNo("R"))
    }
}
