package com.nushhack26.app.screenshots

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.SyncStatusBar
import com.nushhack26.app.ui.theme.TenetTheme

/** Device used by every screenshot test: a Pixel-sized phone. */
const val PHONE = "w412dp-h915dp-xxhdpi"

/** Renders [content] under the sync status bar, the way it sits in the app, to build/outputs/roborazzi/[name].png. */
fun captureScreen(name: String, status: SyncStatus = SyncStatus(), content: @Composable () -> Unit) {
    captureRoboImage("build/outputs/roborazzi/$name.png") {
        TenetTheme {
            Surface {
                Column {
                    SyncStatusBar(status)
                    content()
                }
            }
        }
    }
}
