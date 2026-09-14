package com.pkmobile.keyboard.engine

import android.view.inputmethod.InputConnection
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Engine logika Auto-Text / Shortcut Expansion (seperti pada Perfect Keyboard).
 * Mengelola pelacakan kata kunci trigger (termasuk karakter awalan /, //, ., @, _, -, #, $)
 * dan melakukan ekspansi teks otomatis secara case-insensitive saat tombol SPASI atau ENTER ditekan.
 */
class AutoTextEngine(
    private val repository: ShortcutRepository,
    private val scope: CoroutineScope
) {
    // Buffer penyimpan kata yang sedang diketik
    private val currentWordBuffer = StringBuilder()

    // Cache in-memory untuk pencarian instan O(1) case-insensitive (lowercase key -> expansion)
    private val shortcutCache = ConcurrentHashMap<String, String>()

    // Listener untuk memperbarui tampilan suggestion bar di UI keyboard
    var onCandidateUpdateListener: ((shortcut: String?, expansion: String?) -> Unit)? = null

    init {
        // Sinkronisasi data Room Database ke cache memori secara reaktif
        scope.launch(Dispatchers.IO) {
            repository.allShortcutsFlow.collectLatest { list ->
                val newCache = HashMap<String, String>()
                for (item in list) {
                    val key = item.shortcut.trim().lowercase()
                    if (key.isNotEmpty()) {
                        newCache[key] = item.expansion
                    }
                }
                shortcutCache.clear()
                shortcutCache.putAll(newCache)

                // Pastikan callback listener dieksekusi di Main UI Thread
                withContext(Dispatchers.Main) {
                    checkCandidateMatch(null)
                }
            }
        }
    }

    /**
     * Memeriksa apakah karakter diizinkan masuk ke dalam kata trigger shortcut.
     * Mengizinkan huruf, angka, serta simbol awalan trigger (/, \, ., @, _, -, #, $, ~, !, ?, :, ;).
     */
    fun isAllowedWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() ||
                c == '/' || c == '\\' || c == '.' || c == '@' ||
                c == '_' || c == '-' || c == '#' || c == '$' ||
                c == '~' || c == '!' || c == '?' || c == ':' || c == ';'
    }

    /**
     * Menambahkan karakter teks ke buffer kata yang sedang diketik.
     */
    fun appendChar(c: Char, ic: InputConnection? = null) {
        if (isAllowedWordChar(c)) {
            currentWordBuffer.append(c)
        } else {
            // Reset buffer kata jika menerima spasi, newline, atau pemisah lainnya
            currentWordBuffer.setLength(0)
        }
        checkCandidateMatch(ic)
    }

    /**
     * Menangani penekanan tombol backspace pada buffer.
     */
    fun handleBackspace(ic: InputConnection? = null) {
        if (currentWordBuffer.isNotEmpty()) {
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
        }
        checkCandidateMatch(ic)
    }

    /**
     * Membersihkan buffer kata aktif.
     */
    fun resetBuffer() {
        currentWordBuffer.setLength(0)
        onCandidateUpdateListener?.invoke(null, null)
    }

    /**
     * Mengambil kata terakhir tepat sebelum kursor dari InputConnection
     * sebagai verifikasi agar sinkronisasi tidak pernah meleset.
     */
    fun getLastWordFromInputConnection(ic: InputConnection?): String {
        if (ic == null) return ""
        try {
            val charsBefore = ic.getTextBeforeCursor(64, 0)?.toString() ?: return ""
            if (charsBefore.isEmpty()) return ""

            var i = charsBefore.length - 1
            while (i >= 0 && !charsBefore[i].isWhitespace() && charsBefore[i] != ',' && charsBefore[i] != '(' && charsBefore[i] != ')') {
                i--
            }
            return charsBefore.substring(i + 1)
        } catch (_: Exception) {
            return ""
        }
    }

    /**
     * Mencari kecocokan shortcut secara case-insensitive baik dari buffer memori
     * maupun dari kata sebelum kursor di InputConnection.
     */
    private fun findMatchingWord(ic: InputConnection?): String? {
        // 1. Cek dari buffer internal
        val bufferWord = currentWordBuffer.toString().trim()
        if (bufferWord.isNotEmpty() && shortcutCache.containsKey(bufferWord.lowercase())) {
            return bufferWord
        }

        // 2. Cek langsung dari teks sebelum kursor di InputConnection
        if (ic != null) {
            val icWord = getLastWordFromInputConnection(ic).trim()
            if (icWord.isNotEmpty() && shortcutCache.containsKey(icWord.lowercase())) {
                return icWord
            }
        }

        return null
    }

    /**
     * Memperbarui tampilan preview chip pada Candidate Suggestion Bar.
     */
    fun checkCandidateMatch(ic: InputConnection?) {
        val word = findMatchingWord(ic)
        if (word != null) {
            val expansion = shortcutCache[word.lowercase()]
            onCandidateUpdateListener?.invoke(word, formatExpansion(expansion ?: ""))
        } else {
            val currentWord = currentWordBuffer.toString()
            if (currentWord.isNotEmpty()) {
                onCandidateUpdateListener?.invoke(currentWord, null)
            } else {
                onCandidateUpdateListener?.invoke(null, null)
            }
        }
    }

    /**
     * Menangani eksekusi tombol SPASI.
     * Jika kata yang baru diketik cocok dengan shortcut (case-insensitive),
     * hapus trigger dan gantikan dengan teks ekspansi + spasi.
     *
     * @return true jika auto-expansion berhasil dieksekusi, false jika spasi biasa.
     */
    fun handleSpace(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null) return false

        val matchedWord = findMatchingWord(inputConnection)
        val expansion = if (matchedWord != null) shortcutCache[matchedWord.lowercase()] else null

        return if (matchedWord != null && expansion != null) {
            val cleanExpansion = formatExpansion(expansion)
            // Hapus sejumlah karakter kata kunci trigger yang tertulis di layar
            inputConnection.deleteSurroundingText(matchedWord.length, 0)
            // Masukkan teks ekspansi diikuti spasi
            inputConnection.commitText("$cleanExpansion ", 1)
            resetBuffer()
            true
        } else {
            // Bukan shortcut, commit spasi biasa
            inputConnection.commitText(" ", 1)
            resetBuffer()
            false
        }
    }

    /**
     * Menangani eksekusi tombol ENTER.
     * Jika kata yang baru diketik cocok dengan shortcut (case-insensitive),
     * hapus trigger dan gantikan dengan teks ekspansi.
     *
     * @return true jika auto-expansion berhasil dieksekusi, false jika enter biasa.
     */
    fun handleEnter(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null) return false

        val matchedWord = findMatchingWord(inputConnection)
        val expansion = if (matchedWord != null) shortcutCache[matchedWord.lowercase()] else null

        return if (matchedWord != null && expansion != null) {
            val cleanExpansion = formatExpansion(expansion)
            // Hapus sejumlah karakter kata kunci trigger
            inputConnection.deleteSurroundingText(matchedWord.length, 0)
            // Masukkan teks ekspansi
            inputConnection.commitText(cleanExpansion, 1)
            resetBuffer()
            true
        } else {
            resetBuffer()
            false
        }
    }

    /**
     * Dipanggil ketika pengguna menyentuh langsung chip di Candidate Suggestion Bar.
     */
    fun applyCandidateExpansion(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null) return false

        val matchedWord = findMatchingWord(inputConnection)
        val expansion = if (matchedWord != null) shortcutCache[matchedWord.lowercase()] else null

        return if (matchedWord != null && expansion != null) {
            val cleanExpansion = formatExpansion(expansion)
            inputConnection.deleteSurroundingText(matchedWord.length, 0)
            inputConnection.commitText("$cleanExpansion ", 1)
            resetBuffer()
            true
        } else {
            false
        }
    }

    /**
     * Memastikan karakter newline \n dan tag enter terformat dengan benar saat di-commit.
     */
    private fun formatExpansion(raw: String): String {
        return raw
            .replace(Regex("(?i)<ent__>"), "\n")
            .replace(Regex("(?i)<enter>"), "\n")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)<tab__>"), "\t")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
    }

    /**
     * Mengambil teks kata yang sedang berada di dalam buffer.
     */
    fun getCurrentWord(): String = currentWordBuffer.toString()
}
