package com.nushhack26.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CloudOff
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nushhack26.app.crypto.Roster
import com.nushhack26.app.ui.components.formatTime
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType

// Wording shared by the Alerts, Fork and Outbox screens (card 06).

/** The name colleagues use: "Dr Tan" for Dr Tan Mei Ling, "Wei Ming" for Tan Wei Ming, "Ravi" for Ravi Kumar. */
fun shortName(full: String): String {
    val words = full.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> full
        words.first().trimEnd('.').equals("Dr", ignoreCase = true) && words.size >= 2 -> "Dr ${words[1]}"
        words.size >= 3 -> words.drop(1).joinToString(" ")
        else -> words.first()
    }
}

fun Roster.nameOf(personId: String?): String = person(personId)?.name ?: "Someone not on the roster"

fun Roster.shortNameOf(personId: String?): String = person(personId)?.name?.let(::shortName) ?: "Unknown"

/** "4 min", "1 h 12 min", "2 days". */
fun formatDuration(ms: Long): String {
    val minutes = (ms / 60_000).coerceAtLeast(1)
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 24 * 60 -> if (minutes % 60 == 0L) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"
        else -> (minutes / (24 * 60)).let { if (it == 1L) "1 day" else "$it days" }
    }
}

fun lastSyncText(lastSyncAt: Long?): String = lastSyncAt?.let { "Last sync ${formatTime(it)}" } ?: "Not synced yet"

/** A full-width notice under the screen header: the offline message or a sync problem, in words. */
@Composable
fun NoticeBanner(text: String, modifier: Modifier = Modifier, problem: Boolean = false) {
    val colors = Tenet.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (problem) colors.alertBg else colors.bg200)
            .border(TenetDimens.strokeControl, if (problem) colors.alert else colors.lineStrong)
            .padding(TenetDimens.space3),
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (problem) Icons.Sharp.Warning else Icons.Sharp.CloudOff,
            contentDescription = null,
            tint = if (problem) colors.alertText else colors.inkMuted,
            modifier = Modifier.size(TenetDimens.iconMd),
        )
        Text(text, style = TenetType.body, color = if (problem) colors.alertText else colors.ink, modifier = Modifier.weight(1f))
    }
}

/** A header line in uppercase mono with a meta line under it. */
@Composable
fun ScreenHeading(title: String, meta: String?, modifier: Modifier = Modifier) {
    val colors = Tenet.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TenetDimens.space1)) {
        Text(title.uppercase(), style = TenetType.headline, color = colors.ink)
        if (meta != null) Text(meta.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
    }
}
