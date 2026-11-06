package com.nushhack26.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetAngles
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One boxed cell on the parcel label: "FROM" → "Ravi Kumar". */
data class ParcelField(val label: String, val value: String)

private val StampAreaHeight = 140.dp
private const val DROP_DELAY_MS = 200L
private const val DROP_MS = 260
private const val DROP_FROM = 2.2f
private const val SQUASH = 0.94f
private const val SQUASH_MS = 40
private val WaveDepth = 6.dp
private const val WAVE_DOWN_MS = 60
private const val WAVE_MS_PER_DP = 0.25f

/**
 * The Done screen on both phones (docs/spec.md section 3, "Done: the parcel label"): warning
 * tape, the camera's name and code, an empty cell for the stamp, the field cells two per row,
 * and tape again. The stamp ([stampWord] / [stampTime] / [stampLine], for example
 * RECEIVED / 13:02 / FROM RAVI) drops in once per label with a thud, and a ripple runs out
 * through the cells from where it lands. Reduce motion: the stamp is simply there, and the
 * thud still fires once. Inspection mode (screenshots): the final state.
 */
@Composable
fun ParcelLabel(
    itemName: String,
    itemCode: String,
    fields: List<ParcelField>,
    stampWord: String,
    stampTime: String,
    stampLine: String,
    modifier: Modifier = Modifier,
) {
    val colors = Tenet.colors
    val haptics = rememberHaptics()
    val reduceMotion = LocalReduceMotion.current
    val inspection = LocalInspectionMode.current
    val density = LocalDensity.current

    var played by rememberSaveable(itemCode, stampWord, stampTime) { mutableStateOf(false) }
    val animate = remember { !played && !reduceMotion && !inspection }
    var stampShown by remember { mutableStateOf(!animate) }
    val stampScale = remember { Animatable(if (animate) DROP_FROM else 1f) }
    val wave = remember { Wave() }

    LaunchedEffect(Unit) {
        if (played || inspection) return@LaunchedEffect
        if (!animate) {
            played = true
            haptics.thud()
            return@LaunchedEffect
        }
        delay(DROP_DELAY_MS)
        stampShown = true
        stampScale.animateTo(1f, tween(DROP_MS, easing = FastOutLinearInEasing))
        played = true
        haptics.thud()
        coroutineScope {
            launch {
                stampScale.animateTo(SQUASH, tween(SQUASH_MS))
                stampScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
            }
            wave.play(this, with(density) { WaveDepth.toPx() }, density.density)
        }
    }

    val title = itemName.ifBlank { itemCode }
    val description = buildString {
        append("$stampWord $title, camera $itemCode, at $stampTime, ${stampLine.replaceFirstChar { it.lowercase() }}.")
        fields.forEach { append(" ${it.label.lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.value}.") }
    }

    Column(
        modifier
            .fillMaxWidth()
            .onPlaced { wave.root = it }
            .border(TenetDimens.strokeControl, colors.lineStrong)
            .background(colors.bg100)
            .padding(TenetDimens.strokeControl)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Tape(Modifier.waveCell(wave, "tape-top"))

        Column(
            Modifier
                .waveCell(wave, "header")
                .rule(colors.lineStrong, top = true)
                .fillMaxWidth()
                .padding(TenetDimens.space4),
        ) {
            Text(title.uppercase(), style = TenetType.headline, color = colors.ink)
            Text("CAMERA $itemCode", style = TenetType.record, color = colors.inkMuted)
        }

        Box(
            Modifier
                .zIndex(1f)
                .waveCell(wave, STAMP_KEY)
                .rule(colors.lineStrong, top = true)
                .fillMaxWidth()
                .heightIn(min = StampAreaHeight)
                .padding(TenetDimens.space4),
            contentAlignment = Alignment.Center,
        ) {
            Stamp(
                stampWord = stampWord,
                stampTime = stampTime,
                stampLine = stampLine,
                color = colors.ok,
                modifier = Modifier.graphicsLayer {
                    alpha = if (stampShown) 1f else 0f
                    scaleX = stampScale.value
                    scaleY = stampScale.value
                    rotationZ = TenetAngles.angleStamp
                },
            )
        }

        fields.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                pair.forEachIndexed { col, field ->
                    FieldCell(
                        field,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .waveCell(wave, "field-$row-$col")
                            .rule(colors.lineStrong, top = true, start = col == 1),
                    )
                }
            }
        }

        Tape(Modifier.waveCell(wave, "tape-bottom").rule(colors.lineStrong, top = true))
    }
}

@Composable
private fun Tape(modifier: Modifier) {
    val colors = Tenet.colors
    Spacer(modifier.fillMaxWidth().height(TenetDimens.stripeBand).hazardStripes(colors.onSignal, colors.signal))
}

@Composable
private fun FieldCell(field: ParcelField, modifier: Modifier) {
    val colors = Tenet.colors
    Column(modifier.padding(horizontal = TenetDimens.space3, vertical = TenetDimens.space2)) {
        Text(field.label.uppercase(), style = TenetType.recordLabel, color = colors.inkMuted)
        Text(field.value, style = TenetType.record.copy(fontWeight = FontWeight.Bold), color = colors.ink)
    }
}

@Composable
private fun Stamp(stampWord: String, stampTime: String, stampLine: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .border(TenetDimens.strokeStamp, color)
            .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bold = FontWeight.Bold
        Text(stampWord.uppercase(), style = TenetType.title.copy(fontWeight = bold), color = color, textAlign = TextAlign.Center)
        Text(stampTime.uppercase(), style = TenetType.record.copy(fontWeight = bold), color = color, textAlign = TextAlign.Center)
        Text(stampLine.uppercase(), style = TenetType.recordLabel.copy(fontWeight = bold), color = color, textAlign = TextAlign.Center)
    }
}

/**
 * A 2 dp rule along the cell's top (and start) edge, drawn inside the cell so it moves with it
 * in the wave. Pads the content clear of the rule.
 */
private fun Modifier.rule(color: Color, top: Boolean = false, start: Boolean = false): Modifier {
    val w: Dp = TenetDimens.strokeControl
    return drawBehind {
        val px = w.toPx()
        if (top) drawRect(color, size = size.copy(height = px))
        if (start) drawRect(color, size = size.copy(width = px))
    }.padding(top = if (top) w else 0.dp, start = if (start) w else 0.dp)
}

private const val STAMP_KEY = "stamp"

/** The ripple: every cell and tape band dips [WaveDepth] and springs back, later the farther it is from the stamp. */
private class Wave {
    class Cell {
        var coords: LayoutCoordinates? = null
        val dy: Animatable<Float, AnimationVector1D> = Animatable(0f)
    }

    var root: LayoutCoordinates? = null
    val cells = mutableMapOf<String, Cell>()

    fun cell(key: String) = cells.getOrPut(key) { Cell() }

    fun play(scope: kotlinx.coroutines.CoroutineScope, depthPx: Float, density: Float) {
        val root = root ?: return
        fun centre(c: Cell): Offset? {
            val coords = c.coords?.takeIf { it.isAttached && root.isAttached } ?: return null
            val size = coords.size
            return root.localPositionOf(coords, Offset(size.width / 2f, size.height / 2f))
        }
        val origin = cells[STAMP_KEY]?.let(::centre) ?: return
        cells.values.forEach { cell ->
            val at = centre(cell) ?: return@forEach
            val delayMs = ((at - origin).getDistance() / density * WAVE_MS_PER_DP).toLong()
            scope.launch {
                delay(delayMs)
                cell.dy.animateTo(depthPx, tween(WAVE_DOWN_MS))
                cell.dy.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 600f))
            }
        }
    }
}

/** Records the cell's place (layout only, so the wave reads where it rests) and moves it by its wave offset. */
private fun Modifier.waveCell(wave: Wave, key: String): Modifier {
    val cell = wave.cell(key)
    return onPlaced { cell.coords = it }.graphicsLayer { translationY = cell.dy.value }
}
