package com.pkmobile.keyboard.engine

import android.view.inputmethod.InputConnection
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Engine logika Auto-Text / Shortcut Expansion (mirip fitur Perfect Keyboard).
 * Mengelola buffer kata aktif dan melakukan penggantian teks secara otomatis saat tombol spasi ditekan.
 */
class AutoTextEngine(
    private val repository: ShortcutRepository,
    private val scope: CoroutineScope
) {
    // Buffer penyimpan kata yang sedang diketik sebelum spasi / tanda baca
    private val currentWordBuffer = java.lang.StringBuilder()

    // Cache in-memory untuk pencarian instan O(1) tanpa lag database di UI thread
    private val shortcutCache = ConcurrentHashMap<String, String>()

    // Listener untuk memperbarui tampilan suggestion strip di UI keyboard
    var onCandidateUpdateListener: ((shortcut: String?, expansion: String?) -> Unit)? = null

    init {
        // Sinkronisasi data Room Database ke cache memori secara reaktif
        scope.launch(Dispatchers.IO) {
            repository.allShortcutsFlow.collectLatest { list ->
                shortcutCache.clear()
                for (item in list) {
                    shortcutCache[item.shortcut.lowercase()] = item.expansion
                }
                // Update kembali status candidate jika ada perubahan data
                checkCandidateMatch()
            }
        }
    }

    /**
     * Menambahkan karakter teks ke buffer kata yang sedang diketik.
     */
    fun appendChar(c: Char) {
        if (c.isLetterOrDigit() || c == '_' || c == '-') {
            currentWordBuffer.append(c)
        } else {
            // Karakter pemisah / simbol selain huruf mereset buffer kata aktif
            currentWordBuffer.setLength(0)
        }
        checkCandidateMatch()
    }

    /**
     * Menangani penekanan tombol backspace pada buffer.
     */
    fun handleBackspace() {
        if (currentWordBuffer.isNotEmpty()) {
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
        }
        checkCandidateMatch()
    }

    /**
     * Membersihkan buffer (misal kursor dipindah atau enter ditekan).
     */
    fun resetBuffer() {
        currentWordBuffer.setLength(0)
        checkCandidateMatch()
    }

    /**
     * Mengecek apakah kata di buffer saat ini cocok dengan shortcut yang terdaftar.
     */
    private fun checkCandidateMatch() {
        val word = currentWordBuffer.toString().lowercase()
        if (word.isNotEmpty() && shortcutCache.containsKey(word)) {
            val expansion = shortcutCache[word]
            onCandidateUpdateListener?.invoke(word, expansion)
        } else {
            onCandidateUpdateListener?.invoke(null, null)
        }
    }

    /**
     * Dipanggil saat tombol Space ditekan.
     * Jika kata di buffer cocok dengan shortcut, lakukan auto-expansion:
     * 1. Hapus kata shortcut yang terketik via deleteSurroundingText(panjang_kata, 0)
     * 2. Tuliskan teks ekspansi + spasi via commitText(expansion + " ", 1)
     *
     * @return true jika terjadi ekspansi auto-text, false jika spasi biasa.
     */
    fun handleSpace(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null) return false

        val typedWord = currentWordBuffer.toString()
        val key = typedWord.lowercase()

        val expansion = shortcutCache[key]
        return if (expansion != null && typedWord.isNotEmpty()) {
            // Hapus kata kunci yang telah diketik
            inputConnection.deleteSurroundingText(typedWord.length, 0)
            // Masukkan teks ekspansi lengkap diikuti spasi
            inputConnection.commitText("$expansion ", 1)
            // Reset buffer kata
            resetBuffer()
            true
        } else {
            // Bukan shortcut, masukkan karakter spasi standar
            inputConnection.commitText(" ", 1)
            resetBuffer()
            false
        }
    }

    /**
     * Dipanggil ketika user menekan preview chip di suggestion candidate bar.
     */
    fun applyCandidateExpansion(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null) return false

        val typedWord = currentWordBuffer.toString()
        val key = typedWord.lowercase()
        val expansion = shortcutCache[key]

        return if (expansion != null && typedWord.isNotEmpty()) {
            inputConnection.deleteSurroundingText(typedWord.length, 0)
            inputConnection.commitText("$expansion ", 1)
            resetBuffer()
            true
        } else {
            false
        }
    }

    /**
     * Mengambil teks kata yang sedang berada di dalam buffer.
     */
    fun getCurrentWord(): String = currentWordBuffer.toString()
}
