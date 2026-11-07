package com.nushhack26.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.SpaceMono
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

object ReturnTimeDefaults {
    /** Where the wheels start the first time: two hours from now, rounded up to the next quarter hour. */
    fun initial(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalTime {
        val t = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime().plusHours(2).withSecond(0).withNano(0)
        val up = (15 - t.minute % 15) % 15
        return t.plusMinutes(up.toLong())
    }
}

/**
 * The return time picker (G1, issue only): a bottom sheet with two drum wheels, hours and
 * minutes, 24-hour. [onConfirm] gets the chosen clock time; a time already passed today, or
 * this very minute, means tomorrow (`DueChoice.At`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnTimePicker(
    initial: LocalTime,
    nowMs: Long,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RectangleShape,
        containerColor = Tenet.colors.bg100,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        ReturnTimeSheet(initial, nowMs, onConfirm, onDismiss, Modifier.navigationBarsPadding())
    }
}

private const val ROW_COUNT = 5
private const val LOOPS = 1000
private val RowHeight = 56.dp
private val WheelWidth = 96.dp
private val ColonWidth = 32.dp
private val FadeHeight = 40.dp
private val QuickJumps = listOf(30 to "+30 min", 60 to "+1 h", 120 to "+2 h", 240 to "+4 h")

/** The sheet's own drawing, also used by the screenshot test (dialogs don't render there). */
@Composable
internal fun ReturnTimeSheet(
    initial: LocalTime,
    nowMs: Long,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val colors = Tenet.colors
    val reduceMotion = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val hours = rememberWheelState(initial.hour, 24)
    val minutes = rememberWheelState(initial.minute, 60)
    val hour by remember { derivedStateOf { hours.centreIndex() % 24 } }
    val minute by remember { derivedStateOf { minutes.centreIndex() % 60 } }
    val chosen = LocalTime.of(hour, minute)

    Column(modifier.fillMaxWidth().background(colors.bg100)) {
        Box(Modifier.fillMaxWidth().height(TenetDimens.strokeControl).background(colors.lineStrong))
        Box(Modifier.fillMaxWidth().height(TenetDimens.stripeWidth).hazardStripes(colors.onSignal, colors.signal))
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TenetDimens.space4, vertical = TenetDimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("RETURN BY", style = TenetType.label, color = colors.inkMuted, modifier = Modifier.fillMaxWidth())
            Column(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "%02d:%02d".format(hour, minute),
                    style = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp),
                    color = colors.ink,
                )
                Text(readout(chosen, nowMs, zone), style = TenetType.body, color = colors.inkMuted, textAlign = TextAlign.Center)
            }
            Box(Modifier.padding(vertical = TenetDimens.space3)) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .background(colors.signalBg),
                    ) {
                        Box(Modifier.fillMaxWidth().height(TenetDimens.strokeControl).background(colors.signal).align(Alignment.TopCenter))
                        Box(Modifier.fillMaxWidth().height(TenetDimens.strokeControl).background(colors.signal).align(Alignment.BottomCenter))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Wheel("Hour", hours, hour, 24, reduceMotion)
                    Text(
                        ":",
                        style = TextStyle(fontFamily = SpaceMono, fontWeight = FontWeight.Bold, fontSize = 32.sp),
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(ColonWidth),
                    )
                    Wheel("Minute", minutes, minute, 60, reduceMotion)
                }
                Box(
                    Modifier.matchParentSize(),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(FadeHeight).align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(listOf(colors.bg100, Color.Transparent))),
                    )
                    Box(
                        Modifier.fillMaxWidth().height(FadeHeight).align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bg100))),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TenetDimens.space2)) {
                QuickJumps.forEach { (plus, label) ->
                    QuickJumpChip(label, Modifier.weight(1f).fillMaxHeight()) {
                        val target = quickJump(nowMs, plus, zone)
                        scope.launch { hours.moveTo(target.hour, reduceMotion) }
                        scope.launch { minutes.moveTo(target.minute, reduceMotion) }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(top = TenetDimens.space6),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TenetDimens.space2),
            ) {
                PrimaryButton("Set return time", onClick = { onConfirm(chosen) })
                TextLinkButton("Cancel", onClick = onDismiss)
            }
        }
    }
}

/** One looping wheel: a long list of [size] values repeated, started in the middle. */
private class WheelState(val list: LazyListState, val size: Int, val rowPx: Float) {
    /** The list index nearest the centre row. */
    fun centreIndex(): Int =
        list.firstVisibleItemIndex + (list.firstVisibleItemScrollOffset / rowPx).roundToInt() + ROW_COUNT / 2

    suspend fun moveTo(value: Int, reduceMotion: Boolean) {
        val target = wheelIndexFor(value, size, centreIndex()) - ROW_COUNT / 2
        if (reduceMotion) list.scrollToItem(target) else list.animateScrollToItem(target)
    }

    suspend fun step(by: Int, reduceMotion: Boolean) {
        val target = centreIndex() + by - ROW_COUNT / 2
        if (reduceMotion) list.scrollToItem(target) else list.animateScrollToItem(target)
    }
}

@Composable
private fun rememberWheelState(value: Int, size: Int): WheelState {
    val middle = size * LOOPS / 2
    val list = rememberLazyListState(initialFirstVisibleItemIndex = middle - middle % size + value - ROW_COUNT / 2)
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }
    return remember(list, size, rowPx) { WheelState(list, size, rowPx) }
}

@Composable
private fun Wheel(name: String, wheel: WheelState, value: Int, size: Int, reduceMotion: Boolean) {
    val colors = Tenet.colors
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val rowPx = wheel.rowPx
    val centre by remember(wheel) { derivedStateOf { wheel.centreIndex() } }

    LaunchedEffect(wheel) {
        snapshotFlow { wheel.centreIndex() }.distinctUntilChanged().drop(1).collect { haptics.tick() }
    }

    fun step(by: Int) = scope.launch { wheel.step(by, reduceMotion) }

    LazyColumn(
        state = wheel.list,
        flingBehavior = rememberSnapFlingBehavior(wheel.list, SnapPosition.Center),
        modifier = Modifier
            .width(WheelWidth)
            .height(RowHeight * ROW_COUNT)
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = "%02d".format(value)
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..(size - 1).toFloat(), steps = size - 2)
                setProgress { target ->
                    val to = target.roundToInt().coerceIn(0, size - 1)
                    scope.launch { wheel.moveTo(to, reduceMotion) }
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction("Increase") { step(1); true },
                    CustomAccessibilityAction("Decrease") { step(-1); true },
                )
            },
    ) {
        items(size * LOOPS) { index ->
            val isCentre = index == centre
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(RowHeight)
                    .graphicsLayer {
                        val list = wheel.list
                        val centrePos = list.firstVisibleItemIndex + list.firstVisibleItemScrollOffset / rowPx + ROW_COUNT / 2
                        val d = index - centrePos
                        val s = (1f - 0.12f * abs(d)).coerceAtLeast(0.7f)
                        scaleX = s
                        scaleY = s
                        alpha = (1f - 0.3f * abs(d)).coerceAtLeast(0.25f)
                        if (!reduceMotion) {
                            rotationX = -18f * d
                            cameraDistance = 8f * density
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%02d".format(index % size),
                    style = TextStyle(
                        fontFamily = SpaceMono,
                        fontWeight = if (isCentre) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 32.sp,
                    ),
                    color = if (isCentre) colors.ink else colors.inkMuted,
                )
            }
        }
    }
}

@Composable
private fun QuickJumpChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = Tenet.colors
    Box(
        modifier
            .heightIn(min = TenetDimens.targetMin)
            .background(colors.bg100, RectangleShape)
            .border(TenetDimens.strokeControl, colors.lineStrong, RectangleShape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = TenetDimens.space1, vertical = TenetDimens.space2),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), style = TenetType.label, color = colors.ink, textAlign = TextAlign.Center)
    }
}

private val DayMonthYear = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** "Today · in 3 h 20 min", or "Tomorrow 08.11.2026 · in 21 h 5 min" when [time] has passed today or is this minute. */
internal fun readout(time: LocalTime, nowMs: Long, zone: ZoneId): String {
    val now = Instant.ofEpochMilli(nowMs).atZone(zone).truncatedTo(ChronoUnit.MINUTES)
    var at = now.with(time.withSecond(0).withNano(0))
    val tomorrow = !at.isAfter(now)
    if (tomorrow) at = at.plusDays(1)
    val total = Duration.between(now, at).toMinutes()
    val h = total / 60
    val m = total % 60
    val inText = when {
        h == 0L -> "in $m min"
        m == 0L -> "in $h h"
        else -> "in $h h $m min"
    }
    val day = if (tomorrow) "Tomorrow ${at.format(DayMonthYear)}" else "Today"
    return "$day · $inText"
}

/** Now plus [plusMinutes], rounded up to the next 5 minutes. */
internal fun quickJump(nowMs: Long, plusMinutes: Int, zone: ZoneId): LocalTime {
    val t = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime().truncatedTo(ChronoUnit.MINUTES).plusMinutes(plusMinutes.toLong())
    return t.plusMinutes(((5 - t.minute % 5) % 5).toLong())
}

/** The list index nearest [current] that shows [value], on a wheel of [size] values repeated. */
internal fun wheelIndexFor(value: Int, size: Int, current: Int): Int {
    val base = current - Math.floorMod(current, size) + value
    return listOf(base - size, base, base + size).minBy { abs(it - current) }
}
