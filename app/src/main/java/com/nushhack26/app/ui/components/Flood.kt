package com.nushhack26.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * The full-screen amber flood after a tap (docs/spec.md section 3, "The tap moment"). AppScaffold
 * wraps the whole app in [FloodHost], so the flood covers the top bar too; [TapPulse] starts it
 * from its core with `LocalFlood.current.play(...)`.
 */
interface FloodController {
    /**
     * Grows a square of `signal` from [origin] (root coordinates) until it covers the screen, then
     * calls [onCovered] (swap the content underneath there), then shrinks it up into the top bar's
     * hazard band. With reduce motion on: [onCovered] at once, no overlay.
     */
    fun play(origin: Offset, onCovered: () -> Unit)
}

val LocalFlood = staticCompositionLocalOf<FloodController> { ImmediateFlood }

/** No overlay: what screenshot tests and anything outside [FloodHost] get. */
object ImmediateFlood : FloodController {
    override fun play(origin: Offset, onCovered: () -> Unit) = onCovered()
}

private const val FLOOD_OUT_MS = 380
private const val FLOOD_HOLD_MS = 100L
private const val FLOOD_IN_MS = 240

/** Top bar (64 dp) plus its 6 dp hazard band, under the status bar. */
private val BandBottom = 64.dp + 6.dp

/** Half the tap core (160 dp), where the flood square starts. */
private val StartHalf = 80.dp

private class AnimatedFlood(
    private val scope: CoroutineScope,
    private val reduceMotion: () -> Boolean,
    private val thud: () -> Unit,
) : FloodController {
    /** The square growing out of the origin: 0 at the core's edge, 1 covering the screen. */
    val grow = Animatable(0f)
    /** The bottom edge rising: 0 at the screen's foot, 1 at the hazard band's foot. */
    val rise = Animatable(0f)
    var origin = Offset.Zero
        private set
    /** Where [FloodHost] sits in root coordinates, so [origin] can be drawn in its own space. */
    var hostOffset = Offset.Zero
    var running by mutableStateOf(false)
        private set
    var covering by mutableStateOf(false)
        private set

    override fun play(origin: Offset, onCovered: () -> Unit) {
        if (running) return onCovered()
        if (reduceMotion()) {
            thud()
            return onCovered()
        }
        this.origin = origin
        running = true
        covering = true
        scope.launch {
            try {
                grow.snapTo(0f)
                rise.snapTo(0f)
                grow.animateTo(1f, tween(FLOOD_OUT_MS, easing = TenetMotion.EmphasizedAccelerate))
                thud()
                onCovered()
                delay(FLOOD_HOLD_MS)
                covering = false
                rise.animateTo(1f, tween(FLOOD_IN_MS, easing = TenetMotion.EmphasizedDecelerate))
            } finally {
                covering = false
                running = false
            }
        }
    }
}

/**
 * Hosts the flood overlay over [content]: a `signal` square grows from the origin (380 ms), thuds,
 * holds 100 ms, then its bottom edge rises into the top bar's hazard band (240 ms). The overlay
 * only takes touches while it covers the screen.
 */
@Composable
fun FloodHost(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val reduce by rememberUpdatedState(LocalReduceMotion.current)
    val controller = remember(scope, haptics) { AnimatedFlood(scope, { reduce }, haptics::thud) }
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density)
    val color = Tenet.colors.signal

    CompositionLocalProvider(LocalFlood provides controller) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { controller.hostOffset = it.positionInRoot() }) {
            content()
            if (controller.running) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (controller.covering) Modifier.pointerInput(Unit) { swallowTouches() } else Modifier)
                        .drawBehind { drawFlood(controller, controller.origin - controller.hostOffset, statusTop + BandBottom.toPx(), color) },
                )
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.swallowTouches() = awaitEachGesture {
    while (true) {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) break
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlood(
    flood: AnimatedFlood,
    origin: Offset,
    bandBottom: Float,
    color: Color,
) {
    val w = size.width
    val h = size.height
    val rise = flood.rise.value
    if (rise > 0f) {
        val bottom = h + (bandBottom - h) * rise
        drawRect(color, size = Size(w, bottom.coerceAtLeast(0f)))
        return
    }
    val startHalf = StartHalf.toPx()
    val endHalf = max(max(origin.x, w - origin.x), max(origin.y, h - origin.y))
    val half = startHalf + (endHalf - startHalf) * flood.grow.value
    drawRect(color, topLeft = Offset(origin.x - half, origin.y - half), size = Size(half * 2, half * 2))
}
