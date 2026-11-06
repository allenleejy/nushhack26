package com.nushhack26.app.ui.devices

import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.data.repo.DeviceListing
import com.nushhack26.app.data.repo.RelayOutcome
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.formatTime
import com.nushhack26.app.ui.give.GiveSnack

const val WITHDRAW_OFFLINE_TEXT = "Withdrawing a phone needs the network."
const val DEVICES_EMPTY_TEXT = "Phones appear here once they enrol and sync to this phone."

/** One phone on the Devices list (docs/spec.md O3). */
data class DeviceRowUi(
    val deviceId: String,
    val name: String,
    val role: String,
    val keyLine: String,
    val enrolledLine: String,
    /** When the safety officer withdrew it, or null while it's in use. */
    val withdrawnAtMs: Long?,
    val isThisPhone: Boolean,
) {
    val canWithdraw: Boolean get() = !isThisPhone && withdrawnAtMs == null
}

data class DevicesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<DeviceRowUi> = emptyList(),
    val status: SyncStatus = SyncStatus(),
    /** The row the bottom sheet asks about, or null when it's closed. */
    val confirming: DeviceRowUi? = null,
    val withdrawing: Boolean = false,
    val snack: GiveSnack? = null,
)

sealed interface DevicesAction {
    data class Withdraw(val deviceId: String) : DevicesAction
    data object ConfirmWithdraw : DevicesAction
    data object Cancel : DevicesAction
    data object SnackShown : DevicesAction
}

/** "Key 3F:A9:12:7C · hardware-backed" */
fun deviceKeyLine(keyFp: String, security: String): String =
    "Key $keyFp · " + if (security == "software") "software test key" else "hardware-backed"

fun deviceRows(listings: List<DeviceListing>, myDeviceId: String?): List<DeviceRowUi> = listings.map { (device, person) ->
    DeviceRowUi(
        deviceId = device.id,
        name = person?.name ?: "Someone not on the roster",
        role = person?.role?.let(AccessRules::roleName)?.replaceFirstChar(Char::uppercase) ?: "Unknown role",
        keyLine = deviceKeyLine(device.keyFp, device.keySecurity),
        enrolledLine = if (device.performedAt > 0) "Enrolled ${AccessRules.shortDate(device.performedAt)} ${formatTime(device.performedAt)}" else "Enrolled",
        withdrawnAtMs = device.revokedAtMs,
        isThisPhone = device.id == myDeviceId,
    )
}

/** The bottom sheet's question. */
fun withdrawQuestion(name: String): String =
    "Withdraw $name's phone? It stops working at once, and other phones refuse anything it signs from now on."

/** What a withdrawal came to, as a snackbar. */
fun withdrawSnack(outcome: RelayOutcome, row: DeviceRowUi, nowMs: Long, online: Boolean): GiveSnack = when (outcome) {
    RelayOutcome.Sent -> GiveSnack.Confirmation("Phone withdrawn.", "${row.name} · ${formatTime(nowMs)}")
    is RelayOutcome.Refused -> GiveSnack.Problem("Not withdrawn", refusalText(outcome.reason))
    is RelayOutcome.Unavailable -> GiveSnack.Problem(
        "Not withdrawn",
        if (online) "Couldn't reach the server. Nothing changed. Try again." else WITHDRAW_OFFLINE_TEXT,
    )
}

/** The server's reason in words: "Only the safety officer can withdraw a phone." */
fun refusalText(reason: String): String = when {
    reason.startsWith("not enrolled") -> "This phone isn't enrolled any more, so it can't withdraw a phone."
    else -> reason.trim().replaceFirstChar(Char::uppercase).let { if (it.endsWith(".")) it else "$it." }
}
