package com.nushhack26.app.ui.components

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FlashlightOff
import androidx.compose.material.icons.sharp.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nushhack26.app.BuildConfig
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetMotion
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A square camera viewfinder that reads QR codes on the phone, with no network (ML Kit's
 * bundled model). Calls [onCode] with each new code's text; the same code again only after
 * 2 seconds. Asks for the camera permission itself. Debug builds add "Paste code", which
 * feeds the clipboard to [onCode], so flows can be tested on an emulator.
 *
 * Card 21 draws it as an instrument: [title] names what it scans in the bezel ("SCANNER · SEAL"),
 * [onToggleTorch] puts the torch in the bezel (null hides it), and each increase of
 * [rejectCount] flashes the brackets orange once (the caller found the code wrong).
 */
@Composable
fun QrScanner(
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier,
    torchOn: Boolean = false,
    hint: String = "Point at the code",
    title: String = "Scanner",
    onToggleTorch: (() -> Unit)? = null,
    rejectCount: Int = 0,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val latestOnCode by rememberUpdatedState(onCode)

    var status by remember { mutableStateOf(ScanStatus.SEARCHING) }
    var lockSeq by remember { mutableIntStateOf(0) }
    var seenRejects by remember { mutableIntStateOf(rejectCount) }

    val decoded: (String) -> Unit = { code ->
        status = ScanStatus.LOCKED
        lockSeq++
        haptics.click()
        scope.launch {
            delay(LOCK_TO_CODE_MS)
            latestOnCode(code)
        }
    }
    LaunchedEffect(lockSeq) {
        if (lockSeq == 0) return@LaunchedEffect
        delay(LOCK_HOLD_MS)
        if (status == ScanStatus.LOCKED) status = ScanStatus.SEARCHING
    }
    LaunchedEffect(rejectCount) {
        if (rejectCount <= seenRejects) return@LaunchedEffect
        seenRejects = rejectCount
        status = ScanStatus.REJECTED
        haptics.reject()
        delay(REJECT_FLASH_MS)
        status = ScanStatus.SEARCHING
    }

    val paste: (() -> Unit)? = if (BuildConfig.DEBUG) {
        {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val text = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
            if (!text.isNullOrBlank()) decoded(text.trim())
        }
    } else {
        null
    }

    if (inPreview) {
        ScannerInstrument(title, status, hint, torchOn, onToggleTorch, paste, modifier) {}
        return
    }

    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LifecycleResumeEffect(Unit) {
        granted = hasCameraPermission(context)
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    ScannerInstrument(
        title = title,
        status = status,
        hint = hint,
        torchOn = torchOn,
        onToggleTorch = onToggleTorch.takeIf { granted },
        onPaste = paste,
        modifier = modifier,
        showSights = granted,
    ) {
        if (granted) {
            CameraPreview(decoded, torchOn, Modifier.fillMaxSize())
        } else {
            CameraPermissionWindow(
                refused = asked,
                onAllow = { launcher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
            )
        }
    }
}

internal enum class ScanStatus { SEARCHING, LOCKED, REJECTED }

/**
 * The scanner's bezel: header readout, a square window with the sights drawn over [window],
 * and the hazard foot. Stateless, so screenshot tests can draw each [status].
 */
@Composable
internal fun ScannerInstrument(
    title: String,
    status: ScanStatus,
    hint: String,
    torchOn: Boolean,
    onToggleTorch: (() -> Unit)?,
    onPaste: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showSights: Boolean = true,
    window: @Composable BoxScope.() -> Unit,
) {
    val colors = Tenet.colors
    val still = LocalInspectionMode.current
    val reduce = LocalReduceMotion.current

    val stateColor = when (status) {
        ScanStatus.SEARCHING -> colors.signal
        ScanStatus.LOCKED -> colors.ok
        ScanStatus.REJECTED -> colors.alert
    }
    val bracketColor by animateColorAsState(
        stateColor,
        if (reduce) snap() else tween(TenetMotion.SMALL_MS, easing = FastOutSlowInEasing),
        label = "bracketColor",
    )
    val closeIn by animateFloatAsState(
        if (status == ScanStatus.LOCKED && !reduce) LOCK_CLOSE_IN else 0f,
        tween(TenetMotion.SMALL_MS, easing = FastOutSlowInEasing),
        label = "bracketCloseIn",
    )

    val sweep = remember { Animatable(STILL_SWEEP) }
    val sweeping = status == ScanStatus.SEARCHING && !reduce && !still
    LaunchedEffect(sweeping) {
        if (!sweeping) return@LaunchedEffect
        sweep.snapTo(0f)
        while (true) {
            sweep.animateTo(1f, tween(SWEEP_MS, easing = LinearEasing))
            sweep.snapTo(0f)
        }
    }

    var lightOn by remember { mutableStateOf(true) }
    val blinking = status == ScanStatus.SEARCHING && !reduce && !still
    LaunchedEffect(blinking) {
        lightOn = true
        while (blinking) {
            delay(BLINK_MS)
            lightOn = !lightOn
        }
    }
    val lightColor = if (status == ScanStatus.SEARCHING) colors.inkMuted else stateColor

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.bg100, RectangleShape)
                .border(TenetDimens.strokeControl, colors.lineStrong, RectangleShape)
                .padding(TenetDimens.strokeControl),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = TenetDimens.targetMin)
                    .background(colors.bg200)
                    .padding(start = 12.dp, end = if (onToggleTorch == null) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.uppercase(),
                    style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold),
                    color = colors.ink,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(8.dp)
                        .drawBehind { if (lightOn) drawRect(lightColor) },
                )
                Spacer(Modifier.width(6.dp))
                Text(status.name, style = TenetType.recordLabel, color = colors.ink)
                if (onToggleTorch != null) {
                    IconButton(onClick = onToggleTorch, modifier = Modifier.size(TenetDimens.targetMin)) {
                        Icon(
                            if (torchOn) Icons.Sharp.FlashlightOff else Icons.Sharp.FlashlightOn,
                            contentDescription = if (torchOn) "Torch off" else "Torch on",
                            tint = colors.ink,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(colors.bg200)
                    .clipToBounds()
                    .drawWithContent {
                        drawContent()
                        if (showSights) {
                            drawSights(
                                color = bracketColor,
                                closeIn = closeIn,
                                sweep = sweep.value,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
                content = window,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(TenetDimens.stripeWidth)
                    .hazardStripes(colors.onSignal, colors.signal),
            )
        }
        Text(hint, style = TenetType.body, color = colors.ink)
        if (onPaste != null) TextLinkButton("Paste code", onClick = onPaste)
    }
}

/** Corner brackets and the scan line, [sweep] of the way down. */
private fun DrawScope.drawSights(color: Color, closeIn: Float, sweep: Float) {
    val inset = SIGHT_INSET.toPx()
    val armLen = BRACKET_LENGTH.toPx()
    val thick = BRACKET_THICKNESS.toPx()
    val span = size.width - 2 * inset
    val left = inset + span * closeIn
    val top = inset + span * closeIn
    val right = size.width - left
    val bottom = size.height - top

    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawRect(color, Offset(if (dx > 0) x else x - armLen, if (dy > 0) y else y - thick), androidx.compose.ui.geometry.Size(armLen, thick))
        drawRect(color, Offset(if (dx > 0) x else x - thick, if (dy > 0) y else y - armLen), androidx.compose.ui.geometry.Size(thick, armLen))
    }
    corner(left, top, 1f, 1f)
    corner(right, top, -1f, 1f)
    corner(left, bottom, 1f, -1f)
    corner(right, bottom, -1f, -1f)

    val lineH = SCAN_LINE.toPx()
    val tail = SCAN_TAIL.toPx()
    val y = inset + (size.height - 2 * inset - lineH) * sweep
    val tailTop = (y - tail).coerceAtLeast(inset)
    if (y > tailTop) {
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.4f)), startY = y - tail, endY = y),
            Offset(inset, tailTop),
            androidx.compose.ui.geometry.Size(span, y - tailTop),
        )
    }
    drawRect(color, Offset(inset, y), androidx.compose.ui.geometry.Size(span, lineH))
}

/** The window's content while the camera permission is missing. */
@Composable
internal fun CameraPermissionWindow(refused: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Allow the camera to scan codes.", style = TenetType.body, color = Tenet.colors.ink, textAlign = TextAlign.Center)
        if (refused) {
            SecondaryButton("Open settings", onClick = onOpenSettings)
        } else {
            SecondaryButton("Allow camera", onClick = onAllow)
        }
    }
}

private fun hasCameraPermission(context: android.content.Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCode: (String) -> Unit, torchOn: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnCode by rememberUpdatedState(onCode)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var lastValue: String? = null
        var lastAt = 0L

        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            // Dense offer codes need more pixels than the default analysis size.
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                        )
                        .build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { proxy ->
                val media = proxy.image
                if (media == null) {
                    proxy.close()
                    return@setAnalyzer
                }
                scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener(mainExecutor) { codes ->
                        val value = codes.firstOrNull()?.rawValue ?: return@addOnSuccessListener
                        val now = SystemClock.elapsedRealtime()
                        if (value != lastValue || now - lastAt > REPEAT_AFTER_MS) {
                            lastValue = value
                            lastAt = now
                            latestOnCode(value)
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }
            runCatching {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, mainExecutor)

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            scanner.close()
            analysisExecutor.shutdown()
            camera = null
        }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    AndroidView(factory = { previewView }, modifier = modifier.clipToBounds())
}

private const val REPEAT_AFTER_MS = 2_000L
private const val LOCK_TO_CODE_MS = 250L
private const val LOCK_HOLD_MS = 1_500L
private const val REJECT_FLASH_MS = 400L
private const val BLINK_MS = 600L
private const val SWEEP_MS = 1_800
private const val LOCK_CLOSE_IN = 0.12f
private const val STILL_SWEEP = 0.4f
private val SIGHT_INSET = 24.dp
private val BRACKET_LENGTH = 28.dp
private val BRACKET_THICKNESS = 4.dp
private val SCAN_LINE = 2.dp
private val SCAN_TAIL = 24.dp
