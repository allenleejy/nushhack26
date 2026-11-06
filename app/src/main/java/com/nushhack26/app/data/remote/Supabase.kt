package com.nushhack26.app.data.remote

import com.nushhack26.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseConfig {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()
}

/** The Supabase client, or null when the build has no server config. Only the sync engine and [SessionManager] use it. */
fun createSupabase(): SupabaseClient? {
    if (!SupabaseConfig.isConfigured) return null
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    ) {
        httpEngine = OkHttp.create()
        install(Auth)
        install(Postgrest)
    }
}
