package com.pkmobile.keyboard.data.supabase

import android.content.Context
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service untuk menangani Autentikasi Pengguna (Login/Register/Logout)
 * menggunakan Supabase Auth SDK.
 */
class AuthService(private val context: Context) {

    private val client by lazy { SupabaseConfig.getClient(context) }

    suspend fun signUp(emailInput: String, passwordInput: String): Result<UserInfo?> = withContext(Dispatchers.IO) {
        try {
            client.auth.signUpWith(Email) {
                email = emailInput.trim()
                password = passwordInput
            }
            val user = client.auth.currentUserOrNull()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(emailInput: String, passwordInput: String): Result<UserInfo?> = withContext(Dispatchers.IO) {
        try {
            client.auth.signInWith(Email) {
                email = emailInput.trim()
                password = passwordInput
            }
            val user = client.auth.currentUserOrNull()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): UserInfo? {
        return try {
            client.auth.currentUserOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentUserId(): String? {
        return getCurrentUser()?.id
    }

    fun getCurrentUserEmail(): String? {
        return getCurrentUser()?.email
    }

    fun isLoggedIn(): Boolean {
        return try {
            client.auth.currentSessionOrNull() != null
        } catch (_: Exception) {
            false
        }
    }
}
