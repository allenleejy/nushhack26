package com.nushhack26.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Motion for the whole app (docs/spec.md section 3, "Motion"). Screens slide, nothing fades.
 * Read [LocalReduceMotion] before any animation: when it's true, jump to the end state, but
 * still fire the haptics.
 */
object TenetMotion {
    /** Material's emphasized curve: tab scrolls, pushes and step changes. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** Speeding up as it goes: the tap flood, a stamp falling. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** Slowing down as it arrives: the flood shrinking into the hazard band. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** A tap on the bottom bar scrolls the tab pager. */
    const val TAB_MS = 280
    /** A new screen slides in from the right; the old one moves a quarter to the left. */
    const val PUSH_MS = 260
    /** Back: the screen leaves to the right. */
    const val POP_MS = 220
    /** One step to the next inside Give and Receive. */
    const val STEP_MS = 220
    /** Small things: chips, stepper boxes, scanner brackets. */
    const val SMALL_MS = 150
}

/** True when the phone's "Remove animations" is on (animator duration scale 0). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Reads the system animator scale, again each time the app resumes. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduce by remember { mutableStateOf(read()) }
    LifecycleResumeEffect(resolver) {
        reduce = read()
        onPauseOrDispose { }
    }
    return reduce
}
