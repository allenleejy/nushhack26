package com.nushhack26.app.nearby

import android.content.Context
import kotlinx.coroutines.CoroutineScope

/**
 * Tap to hand over offline (docs/sync.md A.11): NFC carries the offer and a session key, then
 * Nearby Connections carries the acceptance back. Placeholder: card 16 builds the API that
 * card 16's notes describe; card 17 uses it in the give and receive screens.
 */
class TapLink(private val context: Context, private val scope: CoroutineScope)
