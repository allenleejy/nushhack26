package com.nushhack26.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.nushhack26.app.ui.AppScaffold
import com.nushhack26.app.ui.theme.AppTheme

/** A FragmentActivity only because BiometricPrompt needs one. The UI is all Compose. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        setContent {
            AppTheme { AppScaffold(container) }
        }
    }
}
