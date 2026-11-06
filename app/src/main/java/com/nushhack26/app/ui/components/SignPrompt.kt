package com.nushhack26.app.ui.components

import android.app.KeyguardManager
import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nushhack26.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface SignResult {
    /** The person authenticated: sign within 10 seconds (docs/sync.md A.1). */
    data object Approved : SignResult

    /** Cancelled, locked out, or no fingerprint or PIN. Nothing may be signed. */
    data class Cancelled(val message: String) : SignResult
}

/**
 * Shows the system fingerprint (or PIN) prompt. A wrong finger is refused by the system and
 * the prompt stays up. Debug builds on a phone with no screen lock skip the prompt, because
 * they sign with the labelled software key.
 */
suspend fun requestSignature(activity: FragmentActivity, title: String, subtitle: String): SignResult {
    val keyguard = activity.getSystemService(KeyguardManager::class.java)
    if (BuildConfig.DEBUG && keyguard?.isDeviceSecure != true) return SignResult.Approved

    return suspendCancellableCoroutine { continuation ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(SignResult.Approved)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(SignResult.Cancelled(errString.toString()))
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowedAuthenticators())
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(info)
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

/** Fingerprint or PIN. Below Android 11 the strong-biometric-or-PIN pair isn't supported, so weak is allowed. */
fun allowedAuthenticators(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** For screens: `val sign = rememberSignPrompt()`, then in a coroutine `sign("Sign this handover", "Siti → Ravi · C-12")`. */
@Composable
fun rememberSignPrompt(): suspend (title: String, subtitle: String) -> SignResult {
    val context = LocalContext.current
    return remember(context) {
        val prompt: suspend (String, String) -> SignResult = { title, subtitle ->
            val activity = context.findActivity() as? FragmentActivity
            if (activity == null) SignResult.Cancelled("Can't show the fingerprint prompt here.")
            else requestSignature(activity, title, subtitle)
        }
        prompt
    }
}
