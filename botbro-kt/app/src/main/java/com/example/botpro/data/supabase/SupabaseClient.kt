package com.example.botpro.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    const val SUPABASE_URL = "https://lsxoakzcxllxmcxeieyg.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxzeG9ha3pjeGxseG1jeGVpZXlnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyMDM4MjAsImV4cCI6MjEwNDc3OTgyMH0.TRM78TqKVgz6YwHKIaiE8VPIRi-iyJYu8LLeIArhbDk"
    const val DISPATCH_URL = "$SUPABASE_URL/functions/v1/telegram-api/dispatch-user-message"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Functions)
        }
    }
}
