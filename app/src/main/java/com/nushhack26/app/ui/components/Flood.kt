package com.nushhack26.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

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

/**
 * Hosts the flood overlay over [content].
 *
 * STUB from the shared prep: card 20 draws the real flood (380 ms out, 100 ms hold, 240 ms into
 * the band) with the thud. This version swaps the content at once.
 */
@Composable
fun FloodHost(content: @Composable () -> Unit) {
    val controller = remember { ImmediateFlood }
    CompositionLocalProvider(LocalFlood provides controller) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}
