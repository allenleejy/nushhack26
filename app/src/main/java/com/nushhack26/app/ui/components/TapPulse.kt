package com.nushhack26.app.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which phone draws the tap moment: the giver's cube goes into an amber circle, the receiver's comes out of a cyan one (card 28). */
enum class TapRole { Giver, Receiver }

/** Where the tap stands, on either phone (card 20). */
enum class TapPhase {
    /** Waiting for the other phone: the core breathes and square rings ripple out. */
    Waiting,
    /** The phones touched: punch and click, then flood the screen and call `onFlooded`. */
    Contact,
    /** The giver after the flood: a calm cyan pulse while the receiver scans the seal and signs. */
    Linked,
}

private val CoreSize = 160.dp
private val RingStroke = 2.dp
private const val WAIT_MS = 1400
private const val LINKED_MS = 2000
private const val FAST_RING_MS = 300
private const val TURN_MS = 150
private const val FLOOD_AFTER_MS = 250L
private const val RING_GROWTH = 0.8f
private val StillRings = listOf(1.3f to 0.6f, 1.6f to 0.3f)
private const val LINKING = "LINKING…"

/** Roborazzi doesn't set inspection mode, and an endless animation never lets a capture settle. */
private val inRobolectric = Build.FINGERPRINT == "robolectric"

/** The animated values, held outside composition and read only while drawing. */
private class PulseMotion {
    val breath = Animatable(1f)
    val punch = Animatable(1f)
    /** 0..1 over one ring period, repeating. */
    val ringClock = Animatable(0f)
    val fastRing = Animatable(0f)
    val turn = Animatable(0f)
    var coreCentre = Offset.Zero
}

/**
 * The tap target at the centre of G3 and R1 (docs/spec.md section 3, "The tap moment").
 * [title] is uppercase mono ("HOLD TO WEI MING'S PHONE"); [subtitle] is a sentence.
 *
 * Waiting breathes and ripples square rings; Contact clicks, punches, turns the rings to 45°,
 * shows LINKING… and 250 ms later floods the screen from the core ([LocalFlood]), calling
 * [onFlooded] once it covers. Linked breathes slowly in cyan. With reduce motion, in previews
 * and in tests, it draws still.
 */
@Composable
fun TapPulse(
    phase: TapPhase,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onFlooded: () -> Unit = {},
    role: TapRole = TapRole.Giver,
) {
    val colors = Tenet.colors
    val still = LocalInspectionMode.current || LocalReduceMotion.current || inRobolectric
    val haptics = rememberHaptics()
    val flood = LocalFlood.current
    val flooded = rememberUpdatedState(onFlooded)
    val motion = remember { PulseMotion() }
    val linked = phase == TapPhase.Linked
    val tint = if (linked) colors.ok else colors.signal
    val edge = if (linked) colors.ok else colors.signalEdge
    val iconTint = if (linked) colors.bg000 else colors.onSignal
    val shownTitle = if (phase == TapPhase.Contact) LINKING else title.uppercase()

    LaunchedEffect(phase) {
        if (phase != TapPhase.Contact) return@LaunchedEffect
        haptics.click()
        if (still) {
            motion.turn.snapTo(45f)
            flood.play(motion.coreCentre) { flooded.value() }
            return@LaunchedEffect
        }
        launch { motion.breath.animateTo(1f, tween(TURN_MS)) }
        launch {
            val punch = spring<Float>(dampingRatio = 0.45f, stiffness = 500f)
            motion.punch.animateTo(1.18f, punch)
            motion.punch.animateTo(1f, punch)
        }
        launch {
            motion.fastRing.snapTo(0f)
            motion.fastRing.animateTo(1f, tween(FAST_RING_MS, easing = LinearOutSlowInEasing))
        }
        launch { motion.turn.animateTo(45f, tween(TURN_MS)) }
        delay(FLOOD_AFTER_MS)
        flood.play(motion.coreCentre) { flooded.value() }
    }

    LaunchedEffect(phase, still) {
        if (phase == TapPhase.Contact) return@LaunchedEffect
        motion.turn.snapTo(0f)
        motion.fastRing.snapTo(0f)
        motion.punch.snapTo(1f)
        motion.breath.snapTo(1f)
        if (still) return@LaunchedEffect
        val period = if (linked) LINKED_MS else WAIT_MS
        coroutineScope {
            launch {
                while (true) {
                    motion.breath.animateTo(1.04f, tween(period, easing = FastOutSlowInEasing))
                    motion.breath.animateTo(1f, tween(period, easing = FastOutSlowInEasing))
                }
            }
            launch {
                while (true) {
                    motion.ringClock.snapTo(0f)
                    motion.ringClock.animateTo(1f, tween(period, easing = LinearEasing))
                }
            }
        }
    }

    Column(
        modifier.fillMaxWidth().padding(vertical = TenetDimens.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TenetDimens.space4),
    ) {
        Box(
            Modifier
                .size(CoreSize)
                .onGloballyPositioned { motion.coreCentre = it.boundsInRoot().center }
                .drawBehind { drawRings(motion, phase, still, tint) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(CoreSize)
                    .graphicsLayer {
                        val scale = motion.breath.value * motion.punch.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(tint)
                    .border(RingStroke, edge)
                    .semantics { contentDescription = shownTitle },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Sharp.Contactless, contentDescription = null, tint = iconTint, modifier = Modifier.size(48.dp))
            }
        }
        Box(
            Modifier.fillMaxWidth().clipToBounds().clearAndSetSemantics {
                contentDescription = shownTitle
                liveRegion = LiveRegionMode.Polite
            },
            contentAlignment = Alignment.Center,
        ) {
            if (still) {
                PulseTitle(shownTitle)
            } else {
                AnimatedContent(
                    targetState = shownTitle,
                    transitionSpec = {
                        slideInHorizontally(tween(TURN_MS)) { it } togetherWith slideOutHorizontally(tween(TURN_MS)) { -it }
                    },
                    label = "tapTitle",
                ) { PulseTitle(it) }
            }
        }
        subtitle?.let { Text(it, style = TenetType.body, color = colors.inkMuted, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun PulseTitle(text: String) {
    Text(text, Modifier.fillMaxWidth(), style = TenetType.title, color = Tenet.colors.ink, textAlign = TextAlign.Center)
}

private fun DrawScope.drawRings(motion: PulseMotion, phase: TapPhase, still: Boolean, color: Color) {
    val stroke = Stroke(RingStroke.toPx())
    val turn = motion.turn.value
    if (still) {
        StillRings.forEach { (scale, alpha) -> squareRing(scale, alpha, turn, color, stroke) }
        return
    }
    val slots = if (phase == TapPhase.Linked) 1 else 2
    val clock = motion.ringClock.value
    for (slot in 0 until slots) {
        val t = (clock + slot.toFloat() / slots) % 1f
        val e = LinearOutSlowInEasing.transform(t)
        squareRing(1f + RING_GROWTH * e, 1f - e, turn, color, stroke)
    }
    if (phase == TapPhase.Contact) {
        val e = motion.fastRing.value
        if (e > 0f && e < 1f) squareRing(1f + RING_GROWTH * e, 1f - e, turn, color, stroke)
    }
}

private fun DrawScope.squareRing(scale: Float, alpha: Float, degrees: Float, color: Color, stroke: Stroke) {
    if (alpha <= 0f) return
    val side = size.minDimension * scale
    rotate(degrees) {
        drawRect(
            color = color,
            topLeft = Offset(center.x - side / 2, center.y - side / 2),
            size = Size(side, side),
            alpha = alpha.coerceIn(0f, 1f),
            style = stroke,
        )
    }
}
