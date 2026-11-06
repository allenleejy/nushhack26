package com.nushhack26.app.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.RevocationRepository
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.give.GiveSnack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class DevicesLocal(val confirmingId: String? = null, val withdrawing: Boolean = false, val snack: GiveSnack? = null)

/** O3 Devices: the roster's phones from Room, and Withdraw through [RevocationRepository]. */
class DevicesViewModel(
    private val revocation: RevocationRepository,
    identity: Flow<Identity?>,
    private val syncStatus: StateFlow<SyncStatus>,
) : ViewModel() {
    private val local = MutableStateFlow(DevicesLocal())

    val state: StateFlow<DevicesUiState> = combine(revocation.devices, identity, syncStatus, local) { listings, me, status, ui ->
        val rows = deviceRows(listings, me?.deviceId)
        DevicesUiState(
            loading = false,
            rows = rows,
            status = status,
            confirming = rows.firstOrNull { it.deviceId == ui.confirmingId && it.canWithdraw },
            withdrawing = ui.withdrawing,
            snack = ui.snack,
        )
    }.catch { e ->
        emit(DevicesUiState(loading = false, error = "Couldn't read the phones on this device: ${e.message ?: e.javaClass.simpleName}"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    fun onAction(action: DevicesAction) {
        when (action) {
            is DevicesAction.Withdraw -> if (syncStatus.value.isOnline) local.update { it.copy(confirmingId = action.deviceId, snack = null) }
            DevicesAction.Cancel -> local.update { if (it.withdrawing) it else it.copy(confirmingId = null) }
            DevicesAction.SnackShown -> local.update { it.copy(snack = null) }
            DevicesAction.ConfirmWithdraw -> confirm()
        }
    }

    private fun confirm() {
        val row = state.value.confirming ?: return
        if (local.value.withdrawing) return
        local.update { it.copy(withdrawing = true) }
        viewModelScope.launch {
            val outcome = revocation.revoke(row.deviceId)
            val snack = withdrawSnack(outcome, row, System.currentTimeMillis(), syncStatus.value.isOnline)
            local.update { DevicesLocal(snack = snack) }
        }
    }
}
