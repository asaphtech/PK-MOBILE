package com.pkmobile.keyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pkmobile.keyboard.BuildConfig
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import com.pkmobile.keyboard.data.supabase.AuthService
import com.pkmobile.keyboard.data.supabase.SupabaseConfig
import com.pkmobile.keyboard.data.supabase.SyncRepository
import com.pkmobile.keyboard.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity Pengaturan Aplikasi Keyboard PK MOBILE.
 * Menyediakan konfigurasi:
 * 1. Supabase Cloud Sync & Autentikasi Pengguna (Login/Register/Logout)
 * 2. Sembunyikan Keyboard setelah Kirim (pref_hide_on_send)
 * 3. Mode Ekspansi Auto-Text Default (pref_expansion_mode)
 * 4. Dialog Panduan Format Tombol PC / Fn
 * 5. Tampilan Versi Aplikasi dinamis
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var authService: AuthService
    private lateinit var syncRepository: SyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(this)
        val shortcutRepository = ShortcutRepository(database.shortcutDao())
        authService = AuthService(this)
        syncRepository = SyncRepository(this, shortcutRepository, authService)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Tombol Kembali
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Langkah Pengaktifan IME
        binding.btnEnableIme.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        binding.btnSelectIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }

        // Setup Supabase Cloud & Auth
        setupAuthViews()
        setupSyncViews()

        // 1. Switch "Sembunyikan Keyboard setelah Kirim"
        val isHideOnSend = prefs.getBoolean(PREF_HIDE_ON_SEND, false)
        binding.switchHideOnSend.isChecked = isHideOnSend
        binding.switchHideOnSend.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_HIDE_ON_SEND, isChecked).apply()
        }

        // 2. RadioGroup "Mode Ekspansi Auto-Text Default"
        val expansionMode = prefs.getString(PREF_EXPANSION_MODE, MODE_MANUAL) ?: MODE_MANUAL
        if (expansionMode == MODE_INSTANT) {
            binding.rbModeInstant.isChecked = true
        } else {
            binding.rbModeManual.isChecked = true
        }
        binding.rgExpansionMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.rb_mode_instant) MODE_INSTANT else MODE_MANUAL
            prefs.edit().putString(PREF_EXPANSION_MODE, mode).apply()
        }

        // 3. Card Petunjuk Tombol PC / Fn
        binding.cardFnGuide.setOnClickListener {
            showFnGuideDialog()
        }

        // 4. Versi Aplikasi
        binding.tvAppVersion.text = "v${BuildConfig.VERSION_NAME}"
    }

    private fun setupAuthViews() {
        refreshAuthState()

        // Tombol Masuk (Login)
        binding.btnAuthLogin.setOnClickListener {
            val email = binding.etAuthEmail.text?.toString()?.trim() ?: ""
            val password = binding.etAuthPassword.text?.toString() ?: ""

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Email dan kata sandi wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnAuthLogin.isEnabled = false
            lifecycleScope.launch {
                val result = authService.signIn(email, password)
                binding.btnAuthLogin.isEnabled = true
                if (result.isSuccess) {
                    Toast.makeText(this@SettingsActivity, "Berhasil masuk sebagai $email", Toast.LENGTH_SHORT).show()
                    refreshAuthState()
                    // Otomatisasi Sinkronisasi setelah login berhasil
                    triggerSync()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Gagal masuk ke akun"
                    Toast.makeText(this@SettingsActivity, "Gagal Masuk: $err", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Tombol Daftar (Register)
        binding.btnAuthRegister.setOnClickListener {
            val email = binding.etAuthEmail.text?.toString()?.trim() ?: ""
            val password = binding.etAuthPassword.text?.toString() ?: ""

            if (email.isBlank() || password.length < 6) {
                Toast.makeText(this, "Email valid dan kata sandi minimal 6 karakter diperlukan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnAuthRegister.isEnabled = false
            lifecycleScope.launch {
                val result = authService.signUp(email, password)
                binding.btnAuthRegister.isEnabled = true
                if (result.isSuccess) {
                    Toast.makeText(this@SettingsActivity, "Pendaftaran berhasil! Silakan cek email jika konfirmasi diaktifkan.", Toast.LENGTH_LONG).show()
                    refreshAuthState()
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Gagal mendaftar"
                    Toast.makeText(this@SettingsActivity, "Gagal Daftar: $err", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Tombol Keluar (Logout)
        binding.btnAuthLogout.setOnClickListener {
            lifecycleScope.launch {
                authService.signOut()
                Toast.makeText(this@SettingsActivity, "Berhasil keluar.", Toast.LENGTH_SHORT).show()
                refreshAuthState()
            }
        }

        // Konfigurasi Anon Key & URL
        binding.btnConfigAnonKey.setOnClickListener {
            showConfigKeyDialog()
        }
    }

    private fun refreshAuthState() {
        val isLoggedIn = authService.isLoggedIn()
        val email = authService.getCurrentUserEmail()

        if (isLoggedIn && !email.isNullOrEmpty()) {
            binding.tvAuthStatusBadge.text = "Terhubung"
            binding.tvAuthStatusBadge.setTextColor(getColor(R.color.accent))
            binding.tvAuthUserEmail.text = "Masuk sebagai: $email"
            binding.layoutAuthForm.visibility = View.GONE
            binding.btnAuthLogout.visibility = View.VISIBLE
        } else {
            binding.tvAuthStatusBadge.text = "Belum Masuk"
            binding.tvAuthStatusBadge.setTextColor(getColor(R.color.text_secondary))
            binding.tvAuthUserEmail.text = "Masuk dengan akun CS JFN Type Master untuk mengunduh template shortcut secara otomatis."
            binding.layoutAuthForm.visibility = View.VISIBLE
            binding.btnAuthLogout.visibility = View.GONE
        }
    }

    private fun setupSyncViews() {
        binding.btnSyncNow.setOnClickListener {
            triggerSync()
        }
    }

    /**
     * Menjalankan sinkronisasi data shortcut dua arah secara lokal Room dan Cloud Supabase.
     */
    private fun triggerSync() {
        binding.btnSyncNow.isEnabled = false
        binding.progressSync.visibility = View.VISIBLE
        binding.tvSyncStatus.text = "Sedang menyinkronkan data shortcut..."

        lifecycleScope.launch {
            val result = syncRepository.sync()
            binding.btnSyncNow.isEnabled = true
            binding.progressSync.visibility = View.GONE
            binding.tvSyncStatus.text = result.message

            Toast.makeText(
                this@SettingsActivity,
                result.message,
                if (result.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showConfigKeyDialog() {
        val currentKey = SupabaseConfig.getAnonKey(this)
        val currentUrl = SupabaseConfig.getUrl(this)

        // Gunakan form vertikal dinamis
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 12)
        }

        val tvLabelUrl = android.widget.TextView(this).apply {
            text = "Supabase Project URL:"
            setTextColor(getColor(R.color.candidate_text_highlight))
            textSize = 12f
        }
        val etUrl = EditText(this).apply {
            setText(currentUrl)
            setTextColor(getColor(R.color.text_primary))
            textSize = 13f
        }

        val tvLabelKey = android.widget.TextView(this).apply {
            text = "Supabase Anon Key:"
            setTextColor(getColor(R.color.candidate_text_highlight))
            textSize = 12f
            setPadding(0, 24, 0, 0)
        }
        val etKey = EditText(this).apply {
            setText(currentKey)
            setTextColor(getColor(R.color.text_primary))
            textSize = 12f
        }

        container.addView(tvLabelUrl)
        container.addView(etUrl)
        container.addView(tvLabelKey)
        container.addView(etKey)

        MaterialAlertDialogBuilder(this)
            .setTitle("Konfigurasi Supabase")
            .setView(container)
            .setPositiveButton("Simpan") { dialog, _ ->
                val newUrl = etUrl.text.toString().trim()
                val newKey = etKey.text.toString().trim()
                if (newUrl.isNotBlank()) SupabaseConfig.setUrl(this, newUrl)
                if (newKey.isNotBlank()) SupabaseConfig.setAnonKey(this, newKey)
                Toast.makeText(this, "Konfigurasi Supabase berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Menampilkan dialog panduan format kata kunci pemicu tombol PC / Fn.
     */
    private fun showFnGuideDialog() {
        val guideMessage = """
            Cara Menggunakan Tombol PC / Layer Fn:

            Setiap tombol khusus pada layer Fn memicu kata kunci tertentu di AutoTextEngine. Daftarkan kata kunci berikut pada daftar shortcut untuk mengeksekusi ekspansi otomatis:

            • F1 s/d F12 : [F1], [F2], ... [F12] (atau F1..F12)
            • Esc : [ESC]
            • Home / End : [HOME] / [END]
            • PgUp / PgDn : [PGUP] / [PGDN]
            • Ins / Del : [INS] / [DEL]
            • PrtScn : [PRTSCN]
            • Ctrl / Alt : [CTRL] / [ALT]
            • Caps / Tab / Break : [CAPS] / [TAB] / [BREAK]

            Keunggulan:
            Tombol ini aman dari gangguan sistem Android (tidak memicu refresh browser pada F5 atau keluar aplikasi pada Esc).
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Panduan Tombol PC / Layer Fn")
            .setMessage(guideMessage)
            .setPositiveButton("Mengerti") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        const val PREFS_NAME = "pk_keyboard_prefs"
        const val PREF_HIDE_ON_SEND = "pref_hide_on_send"
        const val PREF_EXPANSION_MODE = "pref_expansion_mode"
        const val MODE_MANUAL = "manual"
        const val MODE_INSTANT = "instant"
    }
}
