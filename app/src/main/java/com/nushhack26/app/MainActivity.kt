package com.nushhack26.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.nushhack26.app.ui.AppScaffold
import com.nushhack26.app.ui.theme.TenetTheme

/** A FragmentActivity only because BiometricPrompt needs one. The UI is all Compose. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        setContent {
            TenetTheme { AppScaffold(container) }
        }
    }

    /**
     * Sync each time Tenet comes on screen, including waking a locked phone. That's also how a
     * withdrawn phone finds out and wipes itself (docs/sync.md A.12), without waiting for the
     * 15-minute background run that some phones delay further.
     */
    override fun onStart() {
        super.onStart()
        (application as App).container.syncScheduler.syncNow()
    }
}
