package com.nushhack26.app.nearby

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

enum class TapAvailability { Ready, NfcOff, NoNfc, BluetoothOff, NeedsPermission }

sealed interface GiverTapEvent {
    data object Tapped : GiverTapEvent
    data object Linked : GiverTapEvent
    data class Acceptance(val text: String) : GiverTapEvent
    data class Failed(val reason: String) : GiverTapEvent
}

@SuppressLint("MissingPermission")
class TapLink(private val context: Context, private val scope: CoroutineScope) {
    private val client = Nearby.getConnectionsClient(context)
    private val availability = MutableStateFlow(TapAvailability.NoNfc)
    val available: StateFlow<TapAvailability> = availability
    var lastTapStartedAt: Long = 0
        private set
    var lastTapReadAt: Long = 0
        private set
    init {
        refresh()
        // Re-check on every resume, so turning Bluetooth or NFC on or off shows on any screen.
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = refresh()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun requiredPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        } else add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }.toTypedArray()

    fun refresh() {
        val nfc = NfcAdapter.getDefaultAdapter(context)
        availability.value = when {
            nfc == null -> TapAvailability.NoNfc
            !nfc.isEnabled -> TapAvailability.NfcOff
            requiredPermissions().any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED } -> TapAvailability.NeedsPermission
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled != true -> TapAvailability.BluetoothOff
            else -> TapAvailability.Ready
        }
    }

    fun serve(offer: String): Flow<GiverTapEvent> = callbackFlow {
        val key = TapPayload.newKey()
        val serving = TapServing.Offer(TapPayload.encode(offer, key).toByteArray(Charsets.UTF_8)) { trySend(GiverTapEvent.Tapped) }
        TapServing.offer = serving
        val accepted = ConcurrentHashMap.newKeySet<String>()
        // Endpoints that already delivered their acceptance: hanging up after that is normal.
        val delivered = ConcurrentHashMap.newKeySet<String>()
        fun fail(e: Exception) { if (e is CancellationException) throw e; trySend(GiverTapEvent.Failed(e.message ?: "Nearby failed")) }
        val payloads = object : PayloadCallback() {
            override fun onPayloadReceived(id: String, payload: Payload) {
                if (id in accepted && payload.type == Payload.Type.BYTES) payload.asBytes()?.let {
                    delivered.add(id)
                    trySend(GiverTapEvent.Acceptance(it.toString(Charsets.UTF_8)))
                }
            }
            override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) = Unit
        }
        val lifecycle = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
                launch {
                    try {
                        if (info.endpointName == TapPayload.receiverName(key)) {
                            accepted.add(id)
                            client.acceptConnection(id, payloads).await()
                        } else client.rejectConnection(id).await()
                    } catch (e: Exception) { fail(e) }
                }
            }
            override fun onConnectionResult(id: String, result: ConnectionResolution) {
                if (id in accepted) {
                    if (result.status.isSuccess) trySend(GiverTapEvent.Linked)
                    else trySend(GiverTapEvent.Failed(result.status.toString()))
                }
            }
            override fun onDisconnected(id: String) {
                if (accepted.remove(id) && !delivered.remove(id)) {
                    trySend(GiverTapEvent.Failed("The other phone left before sending its acceptance"))
                }
            }
        }
        val advertising = launch {
            try {
                client.startAdvertising(TapPayload.giverName(key), SERVICE_ID, lifecycle,
                    AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()).await()
            } catch (e: Exception) { fail(e) }
        }
        awaitClose {
            advertising.cancel()
            if (TapServing.offer === serving) TapServing.offer = null
            client.stopAdvertising()
            client.stopAllEndpoints()
        }
    }

    fun join(payload: TapPayload): ReceiverTapSession = ReceiverTapSession(client, scope, payload)

    @Composable
    fun rememberTapReader(enabled: Boolean, onTap: (TapPayload) -> Unit) {
        val onTapNow = rememberUpdatedState(onTap)
        val local = LocalContext.current
        val owner = LocalLifecycleOwner.current
        DisposableEffect(local, owner, enabled) {
            var current: Context? = local
            while (current is ContextWrapper && current !is Activity) current = current.baseContext
            val activity = current as? Activity
            val adapter = NfcAdapter.getDefaultAdapter(local)
            var active = true
            fun enable() {
                refresh()
                if (enabled && activity != null && adapter?.isEnabled == true) {
                    adapter.enableReaderMode(activity, { tag ->
                        val started = SystemClock.elapsedRealtime()
                        runCatching {
                            IsoDep.get(tag)?.use { dep ->
                                dep.connect()
                                TapPayload.parse(TapApdu.read(dep::transceive))
                            }
                        }.getOrNull()?.let { payload ->
                            Handler(Looper.getMainLooper()).post {
                                if (active && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                    lastTapStartedAt = started
                                    lastTapReadAt = SystemClock.elapsedRealtime()
                                    onTapNow.value(payload)
                                }
                            }
                        }
                    }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) enable()
                if (event == Lifecycle.Event.ON_PAUSE && activity != null) adapter?.disableReaderMode(activity)
            }
            owner.lifecycle.addObserver(observer)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) enable()
            onDispose {
                active = false
                owner.lifecycle.removeObserver(observer)
                if (activity != null) adapter?.disableReaderMode(activity)
            }
        }
    }

    companion object { const val SERVICE_ID = "com.nushhack26.app.tap" }
}
