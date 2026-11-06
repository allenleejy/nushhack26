package com.nushhack26.app.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * The app's haptics, the same on every screen. Haptics still fire when animations are off.
 * - [click]: a tap is felt (the phones touch, a QR code locks)
 * - [confirm]: something succeeded
 * - [reject]: a wrong code or a refusal
 * - [tick]: a small step (a check line passing)
 * - [thud]: a heavy landing (the tap flood, the stamp)
 */
class Haptics(private val view: View, private val vibrator: Vibrator?) {
    fun click() = perform(HapticFeedbackConstants.CONTEXT_CLICK)
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)
    fun confirm() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    fun reject() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)

    /** The thud primitive where the vibrator has it (many Xiaomi phones don't), else a heavy click. */
    fun thud() {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return perform(HapticFeedbackConstants.LONG_PRESS)
        val effect = if (Build.VERSION.SDK_INT >= 31 && v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)) {
            VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f).compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        v.vibrate(effect)
    }

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        Haptics(view, vibrator)
    }
}
