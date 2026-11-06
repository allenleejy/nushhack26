package com.nushhack26.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * A QR code for [text], drawn crisp at any size with a white quiet zone. While it's on
 * screen the phone stays awake at full brightness, so the other phone scans it quickly.
 */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier, contentDescription: String = "Handover code") {
    val image = remember(text) { renderQrCode(text) }
    KeepScreenBright()
    Image(
        bitmap = image,
        contentDescription = contentDescription,
        filterQuality = FilterQuality.None,
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White)
            .padding(16.dp),
    )
}

/** One pixel per module, error correction L (docs/sync.md A.3). */
fun renderQrCode(text: String): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 0,
    )
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    val width = matrix.width
    val pixels = IntArray(width * matrix.height) { i ->
        if (matrix.get(i % width, i / width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, width, matrix.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Keeps the screen on at full brightness while in composition. */
@Composable
fun KeepScreenBright() {
    if (LocalInspectionMode.current) return
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply { screenBrightness = previous }
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
