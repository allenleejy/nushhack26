package com.nushhack26.app.ui.fork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.crypto.Canonical
import com.nushhack26.app.crypto.DeviceKeys
import com.nushhack26.app.data.local.PersonEntity
import com.nushhack26.app.data.local.ResolutionEntity
import com.nushhack26.app.data.repo.IdentityRepository
import com.nushhack26.app.data.repo.SafetyRepository
import com.nushhack26.app.ui.components.SignResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ForkViewModel(
    private val alertId: String,
    private val repository: SafetyRepository,
    private val identity: IdentityRepository,
    private val deviceKeys: () -> DeviceKeys,
    private val syncNow: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val input = MutableStateFlow(ForkInput())

    val state: StateFlow<ForkUiState> = combine(
        repository.observeAlert(alertId),
        repository.observeSafetyData(),
        identity.identity,
        identity.lease,
        input,
    ) { alert, data, me, lease, input ->
        val person = me?.let { id ->
            data.roster.person(id.personId) ?: PersonEntity(id.personId, id.name, id.role, staffNo = "")
        }
        buildForkState(alertId, alert, data, person, lease, input)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForkUiState())

    fun select(handoverId: String) = input.update { it.copy(selectedId = handoverId, error = null) }

    fun setNote(note: String) = input.update { it.copy(note = note.take(500)) }

    fun openConfirm() = input.update { it.copy(confirming = it.selectedId != null, error = null) }

    fun dismissConfirm() = input.update { it.copy(confirming = false) }

    /** Fingerprint, then sign CS1-RESOLVE with this phone's key, save with its outbox entry, and sync. */
    fun decide(sign: suspend (title: String, subtitle: String) -> SignResult, onDone: () -> Unit) {
        val current = state.value
        val chosen = current.selected ?: return
        if (current.signing || !current.canDecide || current.decision != null) return
        viewModelScope.launch {
            if (identity.leaseNow(clock()).expired) {
                input.update { it.copy(confirming = false, error = AccessRules.LEASE_EXPIRED_TEXT) }
                return@launch
            }
            val me = identity.current() ?: run {
                input.update { it.copy(confirming = false, error = "Enrol this phone before signing.") }
                return@launch
            }
            input.update { it.copy(signing = true, error = null) }
            when (val result = sign("Sign decision", "${current.itemName} is with ${chosen.receiverName}")) {
                is SignResult.Cancelled -> input.update { it.copy(signing = false, error = result.message) }
                SignResult.Approved -> runCatching {
                    val draft = ResolutionEntity(
                        id = UUID.randomUUID().toString(),
                        alertId = current.alertId,
                        itemId = current.itemId,
                        chosenHandoverId = chosen.handoverId,
                        decidedByPersonId = me.personId,
                        decidedByDeviceId = me.deviceId,
                        note = current.note.trim(),
                        decidedMs = clock(),
                        sig = "",
                    )
                    val sig = withContext(Dispatchers.Default) { deviceKeys().sign(Canonical.resolutionString(draft)) }
                    repository.saveResolution(draft.copy(sig = sig))
                }.onSuccess {
                    syncNow()
                    input.update { ForkInput() }
                    onDone()
                }.onFailure { e ->
                    input.update { it.copy(signing = false, error = e.message ?: "Couldn't sign the decision.") }
                }
            }
        }
    }
}
