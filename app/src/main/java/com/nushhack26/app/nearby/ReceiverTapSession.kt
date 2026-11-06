package com.nushhack26.app.nearby

import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class ReceiverTapSession internal constructor(private val client: ConnectionsClient, scope: CoroutineScope, payload: TapPayload) {
    private val connected = MutableStateFlow(false)
    val linked: StateFlow<Boolean> = connected
    private val job = Job(scope.coroutineContext[Job])
    private val sessionScope = CoroutineScope(scope.coroutineContext + job + Dispatchers.Main.immediate)
    @Volatile private var endpoint: String? = null
    private val transfers = ConcurrentHashMap<Long, CompletableDeferred<Boolean>>()
    private val receiverName = TapPayload.receiverName(payload.key)
    private val giverName = TapPayload.giverName(payload.key)
    private val callback = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, payload: Payload) = Unit
        override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {
            if (id != endpoint) return
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> transfers.remove(update.payloadId)?.complete(true)
                PayloadTransferUpdate.Status.FAILURE, PayloadTransferUpdate.Status.CANCELED -> transfers.remove(update.payloadId)?.complete(false)
            }
        }
    }
    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            sessionScope.launch {
                try {
                    if (id == endpoint && info.endpointName == giverName) client.acceptConnection(id, callback).await()
                    else client.rejectConnection(id).await()
                } catch (e: Exception) { if (e is CancellationException) throw e; close() }
            }
        }
        override fun onConnectionResult(id: String, result: ConnectionResolution) {
            if (id == endpoint) {
                connected.value = result.status.isSuccess
                if (result.status.isSuccess) client.stopDiscovery() else close()
            }
        }
        override fun onDisconnected(id: String) { if (id == endpoint) close() }
    }

    init {
        sessionScope.launch {
            try {
                client.startDiscovery(TapLink.SERVICE_ID, object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
                        if (info.endpointName != giverName || endpoint != null) return
                        endpoint = id
                        sessionScope.launch {
                            try { client.requestConnection(receiverName, id, lifecycle).await() }
                            catch (e: Exception) { if (e is CancellationException) throw e; close() }
                        }
                    }
                    override fun onEndpointLost(id: String) = Unit
                }, DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()).await()
            } catch (e: Exception) { if (e is CancellationException) throw e; close() }
        }
    }

    suspend fun send(text: String, timeoutMs: Long = 10_000): Boolean = withContext(Dispatchers.Main.immediate) {
        withTimeoutOrNull(timeoutMs) {
        if (!job.isActive) return@withTimeoutOrNull false
        linked.first { it }
        val id = endpoint ?: return@withTimeoutOrNull false
        val payload = Payload.fromBytes(text.toByteArray(Charsets.UTF_8))
        val result = CompletableDeferred<Boolean>()
        transfers[payload.id] = result
        try {
            client.sendPayload(id, payload).await()
            result.await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        } finally { transfers.remove(payload.id) }
        } ?: false
    }

    fun close() {
        connected.value = false
        transfers.values.forEach { it.complete(false) }
        transfers.clear()
        job.cancel()
        client.stopDiscovery()
        endpoint?.let { client.disconnectFromEndpoint(it) }
        endpoint = null
    }
}
