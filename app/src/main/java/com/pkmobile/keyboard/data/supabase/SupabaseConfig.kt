package com.pkmobile.keyboard.data.supabase

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Konfigurasi dan inisialisasi singleton SupabaseClient.
 * Menggunakan URL proyek Supabase dan Anon Key.
 * Mendukung penyimpanan Anon Key fleksibel di SharedPreferences agar pengguna
 * dapat memasukkan atau memperbarui API key langsung dari UI Settings.
 */
object SupabaseConfig {
    const val DEFAULT_SUPABASE_URL = "https://qeacrkiuiwxqbkapmyrc.supabase.co"
    const val DEFAULT_ANON_KEY = "sb_anon_key_placeholder"

    private const val PREFS_NAME = "pk_keyboard_prefs"
    const val PREF_ANON_KEY = "pref_supabase_anon_key"
    const val PREF_URL = "pref_supabase_url"

    @Volatile
    private var client: SupabaseClient? = null

    fun getAnonKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_ANON_KEY, "") ?: ""
        return if (saved.isNotBlank()) saved else DEFAULT_ANON_KEY
    }

    fun setAnonKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_ANON_KEY, key.trim()).apply()
        client = null
    }

    fun getUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_URL, "") ?: ""
        return if (saved.isNotBlank()) saved else DEFAULT_SUPABASE_URL
    }

    fun setUrl(context: Context, url: String) {
        val cleanUrl = if (url.endsWith("/rest/v1/")) {
            url.removeSuffix("/rest/v1/")
        } else if (url.endsWith("/rest/v1")) {
            url.removeSuffix("/rest/v1")
        } else {
            url.trimEnd('/')
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_URL, cleanUrl).apply()
        client = null
    }

    @Synchronized
    fun getClient(context: Context): SupabaseClient {
        val existing = client
        if (existing != null) return existing

        val key = getAnonKey(context)
        val url = getUrl(context)

        val newClient = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Auth)
        }
        client = newClient
        return newClient
    }
}
