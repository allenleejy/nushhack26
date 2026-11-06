package com.nushhack26.app.auth

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.nushhack26.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app is locked (docs/sync.md A.7). It starts locked on every cold start and
 * locks again whenever the screen turns off. Only [Unlocker] (via the Lock screen) and a
 * successful enrolment unlock it. AppScaffold draws the Lock screen over the app while locked.
 */
class AppLock(context: Context) {
    private val appContext = context.applicationContext

    /**
     * Debug builds on a phone with no screen lock never lock: they sign with the labelled
     * software key, so there's nothing to authenticate with. Release builds always lock.
     */
    val lockApplies: Boolean
        get() = !BuildConfig.DEBUG || appContext.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    private val _locked = MutableStateFlow(lockApplies)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    fun lock() {
        if (lockApplies) _locked.value = true
    }

    /** Call only after [Unlocker.verify] passed, or right after a successful enrolment. */
    fun markUnlocked() {
        _locked.value = false
    }

    fun start() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) lock()
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
