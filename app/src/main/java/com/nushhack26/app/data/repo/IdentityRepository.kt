package com.nushhack26.app.data.repo

import com.nushhack26.app.data.local.SyncStateDao
import com.nushhack26.app.data.local.put
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Who this phone is: the person it enrolled as, and its device on the roster. */
data class Identity(
    val personId: String,
    val deviceId: String,
    val name: String,
    val role: String,
)

sealed interface IdentityState {
    data object Loading : IdentityState
    data object NotEnrolled : IdentityState
    data class Enrolled(val identity: Identity) : IdentityState
}

/**
 * Kept in sync_state, not in the people table, so the phone stays enrolled after
 * "Clear local data" and before the roster has synced again.
 */
class IdentityRepository(private val syncState: SyncStateDao) {
    val state: Flow<IdentityState> = combine(
        syncState.observe(PERSON_ID),
        syncState.observe(DEVICE_ID),
        syncState.observe(NAME),
        syncState.observe(ROLE),
    ) { personId, deviceId, name, role ->
        if (personId != null && deviceId != null && name != null && role != null) {
            IdentityState.Enrolled(Identity(personId, deviceId, name, role))
        } else {
            IdentityState.NotEnrolled
        }
    }

    val identity: Flow<Identity?> = state.map { (it as? IdentityState.Enrolled)?.identity }

    suspend fun current(): Identity? {
        val personId = syncState.get(PERSON_ID) ?: return null
        val deviceId = syncState.get(DEVICE_ID) ?: return null
        val name = syncState.get(NAME) ?: return null
        val role = syncState.get(ROLE) ?: return null
        return Identity(personId, deviceId, name, role)
    }

    suspend fun setIdentity(identity: Identity) {
        syncState.put(PERSON_ID, identity.personId)
        syncState.put(DEVICE_ID, identity.deviceId)
        syncState.put(NAME, identity.name)
        syncState.put(ROLE, identity.role)
    }

    /** Demo only: the tampered phone keeps an old copy of the token (docs/sync.md A.5). */
    val tampered: Flow<Boolean> = syncState.observe(TAMPERED).map { it == "true" }

    suspend fun isTampered(): Boolean = syncState.get(TAMPERED) == "true"

    suspend fun setTampered(on: Boolean) = syncState.put(TAMPERED, on.toString())

    companion object {
        const val PERSON_ID = "person_id"
        const val DEVICE_ID = "device_id"
        const val NAME = "person_name"
        const val ROLE = "person_role"
        const val TAMPERED = "tampered"

        /** Kept by "Clear local data". */
        val KEPT_KEYS = listOf(PERSON_ID, DEVICE_ID, NAME, ROLE, TAMPERED)
    }
}
