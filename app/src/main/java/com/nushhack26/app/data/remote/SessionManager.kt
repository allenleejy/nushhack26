package com.nushhack26.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Signs in anonymously once, the first time the phone is online with no session.
 * The session is kept on the phone, so the app opens and works offline after that.
 */
class SessionManager(
    private val client: SupabaseClient?,
    private val isOnline: StateFlow<Boolean>,
    private val scope: CoroutineScope,
) {
    private val _userId = MutableStateFlow<String?>(null)

    /** The signed-in user's id, or null until the first sign-in. */
    val userId: StateFlow<String?> = _userId.asStateFlow()

    fun start() {
        val auth = client?.auth ?: return
        scope.launch {
            auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) _userId.value = status.session.user?.id
                else if (status is SessionStatus.NotAuthenticated) _userId.value = null
            }
        }
        scope.launch {
            combine(auth.sessionStatus, isOnline) { status, online -> status is SessionStatus.NotAuthenticated && online }
                .filter { it }
                .collect {
                    // A failure here just waits for the next status or network change.
                    runCatching { auth.signInAnonymously() }
                }
        }
    }
}
