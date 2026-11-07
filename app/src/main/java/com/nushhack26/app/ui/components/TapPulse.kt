package com.nushhack26.app.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetMotion
import com.nushhack26.app.ui.theme.TenetType
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which phone draws the tap moment: the giver's cube goes into an amber circle, the receiver's comes out of a cyan one (card 28). */
enum class TapRole { Giver, Receiver }

/** Where the tap stands, on either phone (card 20). */
enum class TapPhase {
    /** Waiting for the other phone: the circle glows (with the cube in it on the giver's phone). */
    Waiting,
    /** The phones touched: the cube goes into the giver's circle, or comes out of the receiver's, then `onFlooded`. */
    Contact,
    /** The giver after the tap: the cube's dashed outline with an amber line tracing it, while the receiver signs. */
    Linked,
}

private val StageHeight = 260.dp
private val PortalSize = 240.dp
private val CubeBox = 240.dp
private const val MAX_BLUR = 10f

private const val GLOW_LOW = 0.94f
private const val GLOW_HIGH = 1.05f
private const val GLOW_MS = 2000
private const val TURN_MS = 9000
private const val TRACE_MS = 2200
private const val OPEN = 1.3f
private const val SHRUNK = 0.04f
private const val SPIN = 900f

/** Roborazzi doesn't set inspection mode, and an endless animation never lets a capture settle. */
private val inRobolectric = Build.FINGERPRINT == "robolectric"

/** The traced outline (the cube's rest silhouette) in its 181 × 183 viewBox, offset (−1, +6) units from centre. */
private val TraceEdges = listOf(
    listOf(108.7f to 57.1f, 4.0f to 32.5f),
    listOf(108.7f to 57.1f, 107.6f to 178.8f),
    listOf(108.7f to 57.1f, 176.3f to 23.2f),
)
private val TraceOutline = listOf(4.0f to 32.5f, 78.0f to 4.0f, 176.3f to 23.2f, 171.5f to 132.9f, 107.6f to 178.8f, 9.1f to 145.5f)
private const val TRACE_CX = 181f / 2 + 1f
private const val TRACE_CY = 183f / 2 - 6f
private const val RUNNER_ON = 70f
private const val RUNNER_OFF = 515f

/** The animated values, held outside composition and read only while drawing. */
private class PortalMotion {
    val angle = Animatable(CUBE_REST_DEG)
    val cubeScale = Animatable(1f)
    /** The cube's blur radius in dp. */
    val blur = Animatable(0f)
    val circle = Animatable(1f)
    val glow = Animatable(1f)
    /** 0..1 over one lap of the trace runner. */
    val trace = Animatable(0f)
    val cubeAlpha = mutableFloatStateOf(1f)
    var contact: Job? = null
    /** True from the start of a Contact until the phase leaves it, so `onFlooded` fires once per Contact. */
    var inContact = false

    suspend fun rest(cubeShown: Boolean, circleTo: Float) {
        angle.snapTo(CUBE_REST_DEG)
        cubeScale.snapTo(1f)
        blur.snapTo(0f)
        glow.snapTo(1f)
        trace.snapTo(0f)
        circle.snapTo(circleTo)
        cubeAlpha.floatValue = if (cubeShown) 1f else 0f
    }

    suspend fun glowLoop() {
        glow.snapTo(GLOW_LOW)
        while (true) {
            glow.animateTo(GLOW_HIGH, tween(GLOW_MS, easing = FastOutSlowInEasing))
            glow.animateTo(GLOW_LOW, tween(GLOW_MS, easing = FastOutSlowInEasing))
        }
    }

    suspend fun turnLoop() {
        while (true) {
            if (angle.value >= CUBE_REST_DEG + 360f) angle.snapTo(angle.value - 360f)
            angle.animateTo(angle.value + 360f, tween(TURN_MS, easing = LinearEasing))
        }
    }

    suspend fun traceLoop() {
        while (true) {
            trace.snapTo(0f)
            trace.animateTo(1f, tween(TRACE_MS, easing = LinearEasing))
        }
    }

    /** Giver, t = 0 at contact: punch, then spin and shrink into the circle (900 ms) while it opens and closes (1140 ms). */
    suspend fun giverContact(still: Boolean, haptics: Haptics, flooded: () -> Unit) = coroutineScope {
        haptics.click()
        if (still) {
            cubeAlpha.floatValue = 0f
            circle.snapTo(0f)
            haptics.thud()
            flooded()
            return@coroutineScope
        }
        launch { glow.animateTo(1f, tween(420)) }
        launch {
            circle.animateTo(OPEN, tween(420))
            circle.animateTo(0f, tween(720, easing = TenetMotion.EmphasizedAccelerate))
        }
        cubeScale.animateTo(1.15f, tween(90))
        cubeScale.animateTo(1f, tween(90))
        coroutineScope {
            val spin = tween<Float>(720, easing = TenetMotion.EmphasizedAccelerate)
            launch { cubeScale.animateTo(SHRUNK, spin) }
            launch { blur.animateTo(MAX_BLUR, spin) }
            angle.animateTo(angle.value + SPIN, spin)
        }
        cubeAlpha.floatValue = 0f
        haptics.thud()
        flooded()
    }

    /** Receiver, t = 0 at contact: the circle opens, the cube spins out of it and lands (1200 ms), the circle closes (1680 ms). */
    suspend fun receiverContact(still: Boolean, haptics: Haptics, flooded: () -> Unit) = coroutineScope {
        haptics.click()
        if (still) {
            rest(cubeShown = true, circleTo = 0f)
            haptics.thud()
            flooded()
            return@coroutineScope
        }
        launch { glow.animateTo(1f, tween(360)) }
        val circling = launch {
            circle.animateTo(OPEN, tween(360))
            circle.animateTo(1.2f, tween(720))
            circle.animateTo(0f, tween(600, easing = TenetMotion.EmphasizedAccelerate))
        }
        delay(360)
        angle.snapTo(CUBE_REST_DEG - SPIN)
        cubeScale.snapTo(SHRUNK)
        blur.snapTo(MAX_BLUR)
        cubeAlpha.floatValue = 1f
        coroutineScope {
            val land = tween<Float>(840, easing = TenetMotion.EmphasizedDecelerate)
            launch { cubeScale.animateTo(1f, land) }
            launch { blur.animateTo(0f, land) }
            angle.animateTo(CUBE_REST_DEG, land)
        }
        haptics.thud()
        cubeScale.animateTo(1.04f, tween(120))
        cubeScale.animateTo(1f, tween(240))
        launch { turnLoop() }
        circling.join()
        flooded()
    }
}

/**
 * The tap target at the centre of G3 and R1 (docs/spec.md section 3, "The tap moment: the cube and
 * the portal"). [title] is uppercase mono ("HOLD TO WEI MING'S PHONE"); [subtitle] is a sentence.
 *
 * The giver's sealed cube turns in a glowing amber circle and, on Contact, spins and shrinks into
 * it; Linked then traces the cube's outline. The receiver's cyan circle waits empty and, on
 * Contact, the cube spins out of it and lands. [onFlooded] fires once per Contact. With reduce
 * motion, in previews and in tests, it jumps straight to the end states.
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
    val flooded = rememberUpdatedState(onFlooded)
    val motion = remember { PortalMotion() }
    val scope = rememberCoroutineScope()
    val giver = role == TapRole.Giver
    val tracing = giver && phase == TapPhase.Linked

    LaunchedEffect(phase, role, still) {
        when (phase) {
            TapPhase.Contact -> {
                if (motion.inContact) return@LaunchedEffect
                motion.inContact = true
                motion.contact?.cancel()
                motion.contact = scope.launch {
                    if (giver) motion.giverContact(still, haptics) { flooded.value() }
                    else motion.receiverContact(still, haptics) { flooded.value() }
                }
            }
            TapPhase.Waiting -> {
                motion.inContact = false
                motion.contact?.cancel()
                motion.rest(cubeShown = giver, circleTo = 1f)
                if (still) return@LaunchedEffect
                coroutineScope {
                    launch { motion.glowLoop() }
                    if (giver) launch { motion.turnLoop() }
                }
            }
            TapPhase.Linked -> {
                motion.inContact = false
                if (giver) {
                    // The circle may still be closing from the contact: let it finish.
                    if (motion.contact?.isActive != true) motion.circle.snapTo(0f)
                    motion.cubeAlpha.floatValue = 0f
                    motion.trace.snapTo(0f)
                    if (!still) motion.traceLoop()
                } else {
                    motion.contact?.cancel()
                    motion.rest(cubeShown = true, circleTo = 0f)
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
            Modifier.fillMaxWidth().height(StageHeight).portal(motion, if (giver) colors.signal else colors.ok),
            contentAlignment = Alignment.Center,
        ) {
            if (tracing) {
                TracedCube(motion, Modifier.size(CubeBox))
            } else {
                Box(Modifier.size(CubeBox).cubeLayer(motion)) {
                    SealedCube(angleY = { motion.angle.value }, scale = { motion.cubeScale.value }, modifier = Modifier.fillMaxSize())
                }
            }
        }
        Text(
            title.uppercase(),
            Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = TenetType.label,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        subtitle?.let { Text(it, style = TenetType.body, color = colors.inkMuted, textAlign = TextAlign.Center) }
    }
}

/** The glowing circle, behind the cube: the colour at 62 % alpha in the middle, 40 % at 60 % out, gone by 93 %. */
private fun Modifier.portal(motion: PortalMotion, color: Color): Modifier = drawWithCache {
    val radius = PortalSize.toPx() / 2
    val brush = Brush.radialGradient(
        0f to color.copy(alpha = 0.62f),
        0.6f to color.copy(alpha = 0.40f),
        0.93f to color.copy(alpha = 0f),
        1f to color.copy(alpha = 0f),
        center = size.center,
        radius = radius,
    )
    onDrawBehind {
        val s = motion.glow.value * motion.circle.value
        if (s <= 0.001f) return@onDrawBehind
        scale(s, pivot = center) { drawCircle(brush, radius, center) }
    }
}

/** Blur on the cube's layer only (API 31 and up); older phones fade it to half instead. */
private fun Modifier.cubeLayer(motion: PortalMotion): Modifier = graphicsLayer {
    val blurDp = motion.blur.value
    val shown = motion.cubeAlpha.floatValue
    clip = false
    if (Build.VERSION.SDK_INT >= 31) {
        val r = blurDp.dp.toPx()
        renderEffect = if (r > 0.01f) BlurEffect(r, r, TileMode.Decal) else null
        alpha = shown
    } else {
        alpha = shown * (1f - 0.5f * blurDp / MAX_BLUR)
    }
}

/** The giver's Linked phase: the cube's dashed outline, with one amber segment lapping it every 2.2 s. */
@Composable
private fun TracedCube(motion: PortalMotion, modifier: Modifier) {
    val colors = Tenet.colors
    Box(
        modifier.drawWithCache {
            val k = CubeEdge.toPx() / 120f
            val mid = size.center
            fun at(p: Pair<Float, Float>) = Offset(mid.x + (p.first - TRACE_CX) * k, mid.y + (p.second - TRACE_CY) * k)
            val outline = Path().apply {
                TraceOutline.forEachIndexed { i, p -> at(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
                close()
            }
            val base = Path().apply {
                addPath(outline)
                TraceEdges.forEach { (a, b) -> at(a).let { moveTo(it.x, it.y) }; at(b).let { lineTo(it.x, it.y) } }
            }
            val dashed = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * k, 5f * k)))
            val lap = (RUNNER_ON + RUNNER_OFF) * k
            val runnerWidth = 4.dp.toPx()
            onDrawBehind {
                drawPath(base, colors.pending, style = dashed)
                // CSS dashoffset 0 → −585: the same as phase lap → 0.
                val phase = (1f - motion.trace.value) * lap % lap
                val runner = Stroke(
                    runnerWidth,
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Miter,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(RUNNER_ON * k, RUNNER_OFF * k), phase),
                )
                drawPath(outline, colors.signal, style = runner)
            }
        },
    )
}
