package com.nushhack26.app.ui.enrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nushhack26.app.AppContainer
import com.nushhack26.app.BuildConfig
import com.nushhack26.app.auth.AccessRules
import com.nushhack26.app.crypto.KeyInfo
import com.nushhack26.app.crypto.NoSecureLockException
import com.nushhack26.app.data.repo.EnrolOutcome
import com.nushhack26.app.ui.components.SignResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EnrolUiState(
    /** Raw staff number: one letter plus up to 4 digits, no hyphen (`R2210`). The field draws the hyphen. */
    val staffNo: String = "",
    val code: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    /** The code didn't match: the code field shows the error too. */
    val wrongCode: Boolean = false,
    /** "Enrolled as Ravi Kumar · radiographer · key 3F:A9:12:7C" once the server accepted the phone. */
    val enrolled: String? = null,
    /** "This phone was withdrawn by your safety officer…" after a withdrawal wiped it (docs/sync.md A.12). */
    val withdrawnNotice: String? = null,
) {
    val canSubmit: Boolean get() = staffNo.length == STAFF_NO_LENGTH && code.length == CODE_LENGTH && enrolled == null
}

const val CODE_LENGTH = 6

/** Raw staff number length: a letter plus 4 digits (`R2210`, drawn as `R-2210`). */
const val STAFF_NO_LENGTH = 5

/** Keeps a staff number as a letter plus up to 4 digits: `r2210`, `R-2210`, `r 2210` and `R22105` all give `R2210`. */
internal fun normalizeStaffNo(input: String): String {
    val cleaned = input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
    val start = cleaned.indexOfFirst { it in 'A'..'Z' }
    if (start < 0) return ""
    val rest = cleaned.substring(start)
    return rest.first() + rest.drop(1).filter { it in '0'..'9' }.take(4)
}

/** The server's stored shape (`R-2210`) for a complete raw staff number (`R2210`). */
internal fun formatStaffNo(raw: String): String =
    if (raw.length > 1) raw.first() + "-" + raw.drop(1) else raw

sealed interface EnrolAction {
    data class StaffNoChanged(val value: String) : EnrolAction
    data class CodeChanged(val value: String) : EnrolAction
    data object CreateKey : EnrolAction
}

/** E1 Enrol (docs/spec.md section 3, docs/sync.md D.4): make the key, prove presence, then enrol online once. */
class EnrolViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(EnrolUiState())
    val state: StateFlow<EnrolUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // A new notice means this phone was just wiped: drop any earlier enrolment from this process.
            container.revocation.withdrawnNotice.distinctUntilChanged().collect { notice ->
                _state.update { if (notice != null) EnrolUiState(withdrawnNotice = notice) else it.copy(withdrawnNotice = null) }
            }
        }
    }

    fun onStaffNo(value: String) = _state.update {
        it.copy(staffNo = normalizeStaffNo(value), error = null, wrongCode = false)
    }

    fun onCode(value: String) = _state.update {
        it.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), error = null, wrongCode = false)
    }

    /** [sign] shows the system fingerprint or PIN prompt (SignPrompt). */
    fun createKeyAndEnrol(sign: suspend (title: String, subtitle: String) -> SignResult) {
        val current = _state.value
        if (current.busy || !current.canSubmit) return
        val staffNo = formatStaffNo(current.staffNo)
        val code = current.code
        if (!container.serverConfigured || !container.isOnline.value) {
            fail(NO_CONNECTION)
            return
        }
        _state.update { it.copy(busy = true, error = null, wrongCode = false) }
        viewModelScope.launch {
            val key: KeyInfo = try {
                withContext(Dispatchers.IO) { container.deviceKeys.createKey() }
            } catch (e: NoSecureLockException) {
                fail(e.message ?: NO_SECURE_LOCK)
                return@launch
            } catch (e: Exception) {
                fail("Couldn't create a signing key on this phone: ${e.message ?: e.javaClass.simpleName}")
                return@launch
            }
            if (sign("Create your signing key", "Enrol as $staffNo") is SignResult.Cancelled) {
                fail("Fingerprint cancelled. Nothing was created.")
                return@launch
            }
            when (val outcome = container.enrolRepository.enrol(staffNo, code, key, "r${BuildConfig.VERSION_CODE}")) {
                is EnrolOutcome.Enrolled -> {
                    container.revocation.clearWithdrawnNotice()
                    _state.update {
                        it.copy(
                            busy = false,
                            enrolled = enrolledLine(outcome.identity.name, outcome.identity.role, key.keyFp),
                        )
                    }
                }
                EnrolOutcome.WrongCode -> _state.update {
                    it.copy(busy = false, wrongCode = true, error = wrongCodeText(staffNo))
                }
                EnrolOutcome.Locked -> fail(LOCKED)
                is EnrolOutcome.NoConnection -> fail(NO_CONNECTION)
                is EnrolOutcome.Failed -> fail("The server didn't enrol this phone: ${outcome.reason}.")
            }
        }
    }

    private fun fail(message: String) = _state.update { it.copy(busy = false, error = message) }

    companion object {
        const val NO_CONNECTION = "No connection. Enrolment needs the network once."
        const val LOCKED = "Too many wrong codes. Try again in 10 minutes."
        const val NO_SECURE_LOCK = "Set a fingerprint or PIN on this phone to sign handovers."

        fun wrongCodeText(staffNo: String) = "That code doesn't match $staffNo. Ask your safety officer."

        fun enrolledLine(name: String, role: String, keyFp: String) =
            "Enrolled as $name · ${AccessRules.roleName(role)} · key $keyFp"
    }
}
