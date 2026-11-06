package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hatching

/** What sits at the start of a [ListRow]: a person's initials, a sealed source, or nothing. */
sealed interface RowLeading {
    data class Initials(val text: String) : RowLeading
    data object SealedSource : RowLeading
    data object None : RowLeading
}

/**
 * A tappable row in a square list (design/components/ListRow): people on the roster, items in
 * the store, records in the outbox. [blocked] rows stay visible, hatched and struck out, with
 * the reason as [supporting], and can't be picked.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: RowLeading = RowLeading.None,
    selected: Boolean = false,
    blocked: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Tenet.colors
    val ground = when {
        blocked -> Modifier.hatching(colors.line, colors.bg200)
        selected -> Modifier
            .background(colors.signalBg)
            .drawBehind { drawRect(colors.signal, size = Size(6.dp.toPx(), size.height)) }
        else -> Modifier.background(colors.bg100)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TenetDimens.targetRow)
            .then(ground)
            .then(if (onClick != null && !blocked) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TenetDimens.space3),
    ) {
        when (leading) {
            is RowLeading.Initials -> Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (selected) colors.signal else colors.bg100)
                    .border(TenetDimens.strokeControl, if (selected) colors.signalEdge else colors.lineStrong),
                contentAlignment = Alignment.Center,
            ) {
                Text(leading.text.uppercase(), style = TenetType.label, color = if (selected) colors.onSignal else colors.ink)
            }
            RowLeading.SealedSource -> HazardDiamond(size = 40.dp)
            RowLeading.None -> Unit
        }
        Column(Modifier.weight(1f)) {
            Text(
                title.uppercase(),
                style = TenetType.label.copy(textDecoration = if (blocked) TextDecoration.LineThrough else null),
                color = if (blocked) colors.inkMuted else colors.ink,
            )
            if (supporting != null) {
                Text(supporting, style = TenetType.body.copy(fontSize = 14.sp, lineHeight = 20.sp), color = colors.inkMuted)
            }
        }
        when {
            blocked -> Text("BLOCKED", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.inkMuted)
            selected -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SELECTED", style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold), color = colors.signalText)
                Icon(Icons.Sharp.Check, contentDescription = null, tint = colors.signalText, modifier = Modifier.size(20.dp))
            }
            trailing != null -> trailing()
        }
    }
}

/** The square list a [ListRow] sits in: bg-100, a 2dp line border, hairline dividers between rows (call [ListDivider]). */
@Composable
fun SquareList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(TenetDimens.strokeControl, Tenet.colors.line)
            .padding(TenetDimens.strokeControl),
        content = content,
    )
}

@Composable
fun ListDivider() = HorizontalDivider(thickness = TenetDimens.strokeHairline, color = Tenet.colors.line)
