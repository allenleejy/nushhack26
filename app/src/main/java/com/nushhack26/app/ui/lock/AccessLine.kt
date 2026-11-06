package com.nushhack26.app.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import com.nushhack26.app.auth.AccessLease
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Texts for the offline access lease (docs/sync.md A.9), shared by Lock and Me.

private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
private val dayTime = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.UK)
private val dateTime = DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.UK)

private fun format(format: DateTimeFormatter, ms: Long): String =
    format.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

private fun untilWord(lease: AccessLease) = if (lease.expired) "offline access ran out" else "valid offline until"

/** Lock: "Access confirmed with the server 14:02 · valid offline until Mon 9 Nov 14:02". */
fun lockAccessLine(lease: AccessLease?): String {
    val confirmedAt = lease?.confirmedAt ?: return "Access not confirmed with the server yet"
    val until = lease.validUntil?.let { " · ${untilWord(lease)} ${format(dateTime, it)}" }.orEmpty()
    return "Access confirmed with the server ${format(time, confirmedAt)}$until"
}

/** Me: "access confirmed 14:02 · valid offline until Mon 14:02". */
fun meAccessLine(lease: AccessLease?): String {
    val confirmedAt = lease?.confirmedAt ?: return "access not confirmed yet"
    val until = lease.validUntil?.let { " · ${untilWord(lease)} ${format(dayTime, it)}" }.orEmpty()
    return "access confirmed ${format(time, confirmedAt)}$until"
}

/** The red banner shown once the lease has run out: the phone can look but not sign. */
@Composable
fun LeaseExpiredBanner(modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.alertBg)
            .border(TenetDimens.strokeControl, colors.alert, RectangleShape)
            .padding(TenetDimens.space4)
            .testTag("lease_expired"),
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Sharp.Warning, contentDescription = null, tint = colors.alertText, modifier = Modifier.size(TenetDimens.iconLg))
        Text(AccessRules.LEASE_EXPIRED_TEXT, style = TenetType.body, color = colors.ink)
    }
}
