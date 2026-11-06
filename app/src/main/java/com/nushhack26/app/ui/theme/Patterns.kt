// Generated from design/tokens.json (Tenet design system). Edit the tokens, not this file.
package com.nushhack26.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import kotlin.math.sqrt

/**
 * 45° diagonal bands behind the content. [band] is the width of one band measured across it,
 * [pitch] the distance from one band to the next (also measured across).
 */
fun Modifier.diagonalBands(ink: Color, ground: Color, band: Dp, pitch: Dp): Modifier = drawBehind {
    drawRect(ground)
    val k = sqrt(2f)
    val w = band.toPx() * k
    val p = pitch.toPx() * k
    val h = size.height
    clipRect {
        var x = -h
        while (x < size.width) {
            val path = Path().apply {
                moveTo(x, h)
                lineTo(x + w, h)
                lineTo(x + w + h, 0f)
                lineTo(x + h, 0f)
                close()
            }
            drawPath(path, ink)
            x += p
        }
    }
}

/** Hazard stripes: attention. Use onSignal on signal, alert on alertBg, or ink on bg000. */
fun Modifier.hazardStripes(ink: Color, ground: Color): Modifier =
    diagonalBands(ink, ground, band = TenetDimens.stripeWidth, pitch = TenetDimens.stripeWidth * 2)

/** Hatching: not available. Disabled buttons and voided records. Pass Tenet.colors.line and bg200. */
fun Modifier.hatching(line: Color, ground: Color): Modifier =
    diagonalBands(line, ground, band = TenetDimens.hatchLine, pitch = TenetDimens.hatchPitch)

/**
 * Text that reads bottom to top on a side tab (IN CUSTODY, VOID, the QR caption).
 * Swaps width and height so the tab measures correctly, then rotates by -90°.
 */
fun Modifier.verticalText(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth = constraints.maxHeight,
                maxHeight = constraints.maxWidth,
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2),
            )
        }
    }
    .rotate(TenetAngles.angleText)
