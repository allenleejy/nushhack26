package com.nushhack26.app.screenshots

import com.nushhack26.app.auth.AccessRules
import java.time.LocalDateTime
import java.time.ZoneId

/** Fixed phone times for the Enrol, Lock and Me screenshots. */
internal fun localMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Fri 6 Nov 2026 14:02: access confirmed, valid offline until Mon 9 Nov 14:02. */
internal val CONFIRMED_AT = localMs(2026, 11, 6, 14, 2)
internal val NOW = localMs(2026, 11, 6, 16, 30)
internal val validLease = AccessRules.lease(lastSyncAt = CONFIRMED_AT, enrolledAt = CONFIRMED_AT, now = NOW)
internal val expiredLease = AccessRules.lease(lastSyncAt = CONFIRMED_AT, enrolledAt = CONFIRMED_AT, now = localMs(2026, 11, 10, 9, 0))
internal val END_OF_JUNE_2027 = localMs(2027, 6, 30, 23, 59)

/** At 360dp wide and 1.3x font size, to check nothing clips. */
const val NARROW = "w360dp-h800dp-xxhdpi"
