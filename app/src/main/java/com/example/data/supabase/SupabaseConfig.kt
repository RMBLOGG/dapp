package com.example.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseConfig {
    const val SUPABASE_URL = "https://vmemwzggocpimnizbukg.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZtZW13emdnb2NwaW1uaXpidWtnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4OTg3MzgsImV4cCI6MjEwMjQ3NDczOH0.JRwuAIpnEtAnhLy_tcNeH9tkuHqqjSfUe7PDl8vcup4"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Auth) {
            // Cocok dengan intent-filter "dapp://login-callback" di AndroidManifest.xml
            // dan Site URL yang di-set di Supabase Dashboard (Authentication > URL Configuration)
            host = "login-callback"
            scheme = "dapp"
        }
        install(Realtime)
        install(Storage)
    }

    val postgrest: Postgrest
        get() = client.postgrest

    val auth: Auth
        get() = client.auth

    val realtime: Realtime
        get() = client.realtime

    val storage: Storage
        get() = client.storage
}
