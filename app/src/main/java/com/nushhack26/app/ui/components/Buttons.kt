package com.nushhack26.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.PrimaryButtonShape
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hatching

// The buttons from design/components/Button. Labels are a verb and its object, shown in
// uppercase mono: SIGN AND SHOW QR, SCAN ACCEPTANCE, RETURN C-12. One primary per screen.

/**
 * The one primary action: amber, 72dp, full width, cut top-right corner. When [enabled] is
 * false it's hatched out and says why, for example label "Sign", reason "Scan seal first".
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    disabledReason: String? = null,
) {
    if (!enabled) {
        DisabledButton(label, disabledReason, modifier)
        return
    }
    val colors = Tenet.colors
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(TenetDimens.targetPrimary),
        shape = PrimaryButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.signal, contentColor = colors.onSignal),
        border = BorderStroke(TenetDimens.strokeControl, colors.signalEdge),
    ) {
        ButtonContent(label, icon)
    }
}

/** The alternative path on the same screen: outlined, square, 64dp. */
@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = Tenet.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(TenetDimens.targetRow),
        shape = RectangleShape,
        border = BorderStroke(TenetDimens.strokeControl, colors.lineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
    ) {
        ButtonContent(label, icon)
    }
}

/** Only for acts with consequences: REPORT MISSING, REFUSE HANDOVER. Never the primary. */
@Composable
fun DangerButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val colors = Tenet.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(TenetDimens.targetRow),
        shape = RectangleShape,
        border = BorderStroke(TenetDimens.strokeControl, colors.alert),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.alertText),
    ) {
        ButtonContent(label, icon)
    }
}

/** Navigation and extras: VIEW CHAIN, NOT NOW, CANCEL OFFER. */
@Composable
fun TextLinkButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = TenetDimens.targetMin), shape = RectangleShape) {
        Text(label.uppercase(), style = TenetType.label.copy(textDecoration = TextDecoration.Underline), color = Tenet.colors.ink)
    }
}

/** Struck out with hatching and a dashed border, with the reason on a solid patch. */
@Composable
private fun DisabledButton(label: String, reason: String?, modifier: Modifier) {
    val colors = Tenet.colors
    val text = listOfNotNull(label, reason).joinToString(" · ").uppercase()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TenetDimens.targetPrimary)
            .hatching(colors.line, colors.bg200)
            .dashedBorder(colors.lineStrong)
            .semantics {
                disabled()
                contentDescription = text
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TenetType.label,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.background(colors.bg000).padding(horizontal = TenetDimens.space2, vertical = TenetDimens.space1),
        )
    }
}

@Composable
private fun ButtonContent(label: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(TenetDimens.iconMd))
        Spacer(Modifier.width(TenetDimens.space2))
    }
    Text(label.uppercase(), style = TenetType.label, textAlign = TextAlign.Center)
}

/** A dashed square border: the look of "waiting" and of a disabled button. */
fun Modifier.dashedBorder(color: Color, width: Dp = TenetDimens.strokeControl): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}
