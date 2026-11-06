package com.nushhack26.app.crypto

import com.nushhack26.app.data.local.DeviceEntity
import com.nushhack26.app.data.local.PersonEntity

/** Everyone on the team and their devices' public keys, as last synced to this phone. */
data class Roster(
    val people: List<PersonEntity> = emptyList(),
    val devices: List<DeviceEntity> = emptyList(),
) {
    fun person(id: String?): PersonEntity? = people.firstOrNull { it.id == id }

    fun device(id: String?): DeviceEntity? = devices.firstOrNull { it.id == id }

    /** The device, only if it belongs to [personId]. */
    fun deviceOf(personId: String?, deviceId: String?): DeviceEntity? =
        device(deviceId)?.takeIf { it.personId == personId }
}
