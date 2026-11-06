package com.nushhack26.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.Tenet

/**
 * The Junction mark (design/logo/tenet-icon-*.svg), drawn from the theme so it's right in Field
 * and Daylight: a crossbar outline in ink, a stem tilted 9°, amber where the two meet. In Field
 * the amber covers both crossbar borders; in Daylight the crossbar's bottom border stays visible.
 */
@Composable
fun TenetMark(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val colors = Tenet.colors
    val field = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Canvas(modifier.size(size).semantics { contentDescription = "Tenet" }) {
        scale(this.size.width / 96f, pivot = Offset.Zero) {
            drawRect(colors.ink, Offset(10f, 14f), Size(76f, 26f), style = Stroke(5f))
            clipRect(7.5f, 11.5f, 88.5f, if (field) 42.5f else 37.5f) {
                rotate(-9f, pivot = Offset(49.5f, 50.5f)) {
                    drawRect(colors.signal, Offset(36.5f, 12.5f), Size(26f, 72f))
                }
            }
            rotate(-9f, pivot = Offset(49.5f, 50.5f)) {
                drawRect(colors.signalEdge, Offset(36.5f, 12.5f), Size(26f, 72f), style = Stroke(5f))
            }
        }
    }
}
