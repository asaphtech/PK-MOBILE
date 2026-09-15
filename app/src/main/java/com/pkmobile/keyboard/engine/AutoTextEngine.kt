package com.pkmobile.keyboard.engine

import android.view.inputmethod.InputConnection
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class CachedShortcut(
    val expansion: String,
    val expansionMode: String = "SPACE"
)

/**
 * Engine logika Auto-Text / Shortcut Expansion (seperti pada Perfect Keyboard).
 * Mengelola pelacakan kata kunci trigger (termasuk karakter awalan /, //, ., @, _, -, #, $)
 * dan melakukan ekspansi teks otomatis secara case-insensitive:
 * - Mode INSTANT: langsung diekspansi begitu kata kunci cocok.
 * - Mode SPACE: ditahan hingga tombol SPASI atau ENTER ditekan.
 */
class AutoTextEngine(
    private val repository: ShortcutRepository,
    private val scope: CoroutineScope
) {
    // Buffer penyimpan kata yang sedang diketik
    private val currentWordBuffer = StringBuilder()

    // Cache in-memory untuk pencarian instan O(1) case-insensitive (lowercase key -> CachedShortcut)
    private val shortcutCache = ConcurrentHashMap<String, CachedShortcut>()

    // Listener untuk memperbarui tampilan suggestion bar di UI keyboard
    var onCandidateUpdateListener: ((shortcut: String?, expansion: String?) -> Unit)? = null

    // Mode Ekspansi Auto-Text Global (fallback): false = Manual (Spasi/Enter), true = Otomatis/Instan
    var isInstantMode: Boolean = false

    init {
        // Sinkronisasi data Room Database ke cache memori secara reaktif (hanya shortcut aktif/terpasang)
        scope.launch(Dispatchers.IO) {
            repository.activeShortcutsFlow.collectLatest { list ->
                updateCacheFromList(list)
            }
        }
    }

    private suspend fun updateCacheFromList(list: List<com.pkmobile.keyboard.data.db.ShortcutEntity>) {
        val newCache = HashMap<String, CachedShortcut>()
        for (item in list) {
            if (!item.isActive) continue
            val key = item.shortcut.trim().lowercase()
            if (key.isNotEmpty()) {
                newCache[key] = CachedShortcut(item.expansion, item.expansionMode)
            }
        }
        shortcutCache.clear()
        shortcutCache.putAll(newCache)

        // Pastikan callback listener dieksekusi di Main UI Thread
        withContext(Dispatchers.Main) {
            checkCandidateMatch(null)
        }
    }

    /**
     * Memaksa penyegaran cache shortcut secara real-time dari database Room.
     */
    suspend fun reloadCache() {
        withContext(Dispatchers.IO) {
            val list = repository.activeShortcutsFlow.firstOrNull() ?: emptyList()
            updateCacheFromList(list)
        }
    }

    /**
     * Mencari shortcut di cache memori secara case-insensitive tanpa memicu auto-expansion.
     * Mengembalikan daftar pasangan (trigger, expansion) yang cocok dengan kata kunci.
     */
    fun searchShortcuts(query: String): List<Pair<String, String>> {
        val q = query.trim().lowercase()
        val results = mutableListOf<Pair<String, String>>()
        for ((trigger, cached) in shortcutCache) {
            if (q.isEmpty() || trigger.contains(q) || cached.expansion.lowercase().contains(q)) {
                results.add(Pair(trigger, cached.expansion))
            }
        }
        return results.sortedWith(compareBy({ !it.first.startsWith(q) }, { it.first }))
    }


    /**
     * Memeriksa apakah karakter diizinkan masuk ke dalam kata trigger shortcut.
     * Mengizinkan huruf, angka, serta simbol awalan trigger (/, \, ., @, _, -, #, $, ~, !, ?, :, ;).
     */
    fun isAllowedWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() ||
                c == '/' || c == '\\' || c == '.' || c == '@' ||
                c == '_' || c == '-' || c == '#' || c == '$' ||
                c == '~' || c == '!' || c == '?' || c == ':' || c == ';' ||
                c == '[' || c == ']' || c == '<' || c == '>' || c == '{' || c == '}' ||
                c == '=' || c == '|' || c == '^' || c == '`'
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

        // Mode Ekspansi Per-Shortcut:
        // Jika shortcut.expansionMode == "INSTANT" (atau global isInstantMode true),
        // lakukan ekspansi teks secara langsung tanpa menunggu spasi/enter.
        if (ic != null) {
            val matchedWord = findMatchingWord(ic)
            val cached = if (matchedWord != null) shortcutCache[matchedWord.lowercase()] else null
            if (matchedWord != null && cached != null) {
                val shouldInstantExpand = cached.expansionMode.equals("INSTANT", ignoreCase = true) || isInstantMode
                if (shouldInstantExpand) {
                    val cleanExpansion = formatExpansion(cached.expansion)
                    ic.deleteSurroundingText(matchedWord.length, 0)
                    ic.commitText(cleanExpansion, 1)
                    resetBuffer()
                }
            }
        }
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
        // 1. Cek dari buffer internal (mendukung kata berawalan slash ganda //, tripel ///, angka //1)
        val bufferWord = currentWordBuffer.toString().trim()
        if (bufferWord.isNotEmpty()) {
            val lower = bufferWord.lowercase()
            if (shortcutCache.containsKey(lower)) {
                return bufferWord
            }
        }

        // 2. Cek langsung dari teks sebelum kursor di InputConnection
        if (ic != null) {
            val icWord = getLastWordFromInputConnection(ic).trim()
            if (icWord.isNotEmpty()) {
                val lower = icWord.lowercase()
                if (shortcutCache.containsKey(lower)) {
                    return icWord
                }
                // 3. Fallback pencocokan fleksibel untuk trigger ganda/tripel jika buffer kursor memuat slash berlebih
                for (key in shortcutCache.keys) {
                    if (lower.endsWith(key) && (lower.length == key.length || lower[lower.length - key.length - 1] == '/')) {
                        return icWord.substring(icWord.length - key.length)
                    }
                }
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
            val cached = shortcutCache[word.lowercase()]
            onCandidateUpdateListener?.invoke(word, formatExpansion(cached?.expansion ?: ""))
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
        val cached = if (matchedWord != null) shortcutCache[matchedWord.lowercase()] else null

        return if (matchedWord != null && cached != null) {
            val cleanExpansion = formatExpansion(cached.expansion)
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
        val cached = if (matchedWord != null) shortcutCache[matchedWord.lowercase()] else null

        return if (matchedWord != null && cached != null) {
            val cleanExpansion = formatExpansion(cached.expansion)
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
        val cached = if (matchedWord != null) shortcutCache[matchedWord.lowercase()] else null

        return if (matchedWord != null && cached != null) {
            val cleanExpansion = formatExpansion(cached.expansion)
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
     * Memproses penekanan tombol khusus Fn (misal: "[F5]", "[ESC]", "[DEL]", dll).
     * Mengecek kecocokan shortcut secara case-insensitive baik dalam format berbingkai "[F5]"
     * maupun tanpa bingkai "F5". Jika cocok, langsung diekspansi ke kolom teks target.
     * Jika tidak cocok, commit string trigger agar teks tetap terinput tanpa memicu fungsi native Android.
     *
     * @return true jika shortcut berhasil diekspansi, false jika commit teks biasa.
     */
    fun processKeyInput(triggerKey: String, inputConnection: InputConnection?): Boolean {
        if (inputConnection == null) return false

        val cleanKey = triggerKey.trim()
        val lowerKey = cleanKey.lowercase()
        val rawKeyWithoutBrackets = lowerKey.removeSurrounding("[", "]")

        val cached = shortcutCache[lowerKey]
            ?: shortcutCache[rawKeyWithoutBrackets]
            ?: shortcutCache["[$rawKeyWithoutBrackets]"]

        return if (cached != null) {
            val cleanExpansion = formatExpansion(cached.expansion)
            inputConnection.commitText(cleanExpansion, 1)
            resetBuffer()
            true
        } else {
            // Commit teks trigger langsung tanpa memicu fungsi native sistem Android
            inputConnection.commitText(cleanKey, 1)
            resetBuffer()
            false
        }
    }

    /**
     * Mengambil teks kata yang sedang berada di dalam buffer.
     */
    fun getCurrentWord(): String = currentWordBuffer.toString()
}
