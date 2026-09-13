package com.pkmobile.keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import com.pkmobile.keyboard.engine.AutoTextEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Service Utama Input Method Editor (IME) untuk Custom Keyboard.
 * Menangani pembuatan tampilan keyboard, penekanan tombol karakter,
 * kontrol spasi, backspace, enter, shift, dan integrasi Auto-Text engine.
 */
class CustomKeyboardService : InputMethodService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var autoTextEngine: AutoTextEngine
    private lateinit var repository: ShortcutRepository

    private var isShiftActive = false
    private var isSymbolMode = false

    // Views
    private var keyboardRoot: View? = null
    private var layoutAlpha: LinearLayout? = null
    private var layoutSymbol: LinearLayout? = null
    private var tvCandidatePrefix: TextView? = null
    private var tvCandidateText: TextView? = null
    private var btnCandidateChip: LinearLayout? = null
    private var keyShiftButton: ImageButton? = null

    // Daftar tombol huruf alfabet untuk update caps/lowercase
    private val alphaButtons = mutableListOf<Button>()

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = ShortcutRepository(database.shortcutDao())
        autoTextEngine = AutoTextEngine(repository, serviceScope)

        // Callback saat engine mendeteksi kecocokan shortcut
        autoTextEngine.onCandidateUpdateListener = { shortcut, expansion ->
            updateCandidateUI(shortcut, expansion)
        }
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardRoot = view

        initViews(view)
        setupAlphaKeys(view)
        setupSymbolKeys(view)
        setupSpecialKeys(view)

        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Reset state setiap kali keyboard terbuka kembali
        autoTextEngine.resetBuffer()
        isSymbolMode = false
        showAlphabetKeyboard()
        updateCandidateUI(null, null)
    }

    private fun initViews(root: View) {
        layoutAlpha = root.findViewById(R.id.layout_alpha)
        layoutSymbol = root.findViewById(R.id.layout_symbol)
        tvCandidatePrefix = root.findViewById(R.id.tv_candidate_prefix)
        tvCandidateText = root.findViewById(R.id.tv_candidate_text)
        btnCandidateChip = root.findViewById(R.id.btn_candidate_chip)
        keyShiftButton = root.findViewById(R.id.key_shift)

        // Klik pada candidate chip langsung mengekspansi shortcut yang sedang cocok
        btnCandidateChip?.setOnClickListener {
            currentInputConnection?.let { ic ->
                autoTextEngine.applyCandidateExpansion(ic)
            }
        }
    }

    /**
     * Mengatur tombol huruf QWERTY (q..p, a..l, z..m).
     */
    private fun setupAlphaKeys(root: View) {
        alphaButtons.clear()
        val alphaKeyIds = intArrayOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )

        for (id in alphaKeyIds) {
            val btn = root.findViewById<Button>(id)
            if (btn != null) {
                alphaButtons.add(btn)
                btn.setOnClickListener {
                    val rawText = btn.text.toString()
                    val charToCommit = if (isShiftActive) rawText.uppercase() else rawText.lowercase()
                    commitCharacter(charToCommit)

                    // Jika bukan caps lock permanen, matikan shift setelah 1 huruf ditekan
                    if (isShiftActive) {
                        isShiftActive = false
                        refreshAlphaKeyLabels()
                    }
                }
            }
        }
    }

    /**
     * Mengatur tombol angka dan simbol (?123 layout).
     */
    private fun setupSymbolKeys(root: View) {
        val symbolKeyIds = intArrayOf(
            R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5,
            R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_0,
            R.id.sym_at, R.id.sym_hash, R.id.sym_dollar, R.id.sym_percent,
            R.id.sym_amp, R.id.sym_minus, R.id.sym_plus, R.id.sym_paren_open,
            R.id.sym_paren_close, R.id.sym_slash, R.id.sym_star, R.id.sym_quote,
            R.id.sym_singlequote, R.id.sym_colon, R.id.sym_semicolon,
            R.id.sym_exclamation, R.id.sym_question, R.id.sym_comma, R.id.sym_period
        )

        for (id in symbolKeyIds) {
            val btn = root.findViewById<Button>(id)
            btn?.setOnClickListener {
                commitCharacter(btn.text.toString())
            }
        }

        // Tombol switch kembali ke keyboard huruf
        root.findViewById<Button>(R.id.sym_switch_abc)?.setOnClickListener {
            showAlphabetKeyboard()
        }

        // Tombol spasi di mode simbol
        root.findViewById<Button>(R.id.sym_space)?.setOnClickListener {
            handleSpaceKey()
        }

        // Tombol backspace di mode simbol dengan repeat on hold
        attachRepeatBackspaceListener(root.findViewById(R.id.sym_key_backspace))

        // Tombol enter di mode simbol
        root.findViewById<ImageButton>(R.id.sym_enter)?.setOnClickListener {
            handleEnterKey()
        }
    }

    /**
     * Mengatur tombol kontrol khusus (Shift, Space, Backspace, Enter, Punctuation).
     */
    private fun setupSpecialKeys(root: View) {
        // Tombol Shift / Caps
        keyShiftButton?.setOnClickListener {
            isShiftActive = !isShiftActive
            refreshAlphaKeyLabels()
        }

        // Tombol Backspace dengan fitur tahan untuk menghapus terus-menerus (continuous repeat)
        attachRepeatBackspaceListener(root.findViewById(R.id.key_backspace))

        // Tombol Spasi (Pemicu Utama Auto-Text / Shortcut Expansion)
        root.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            handleSpaceKey()
        }

        // Tombol Enter / Kirim / Cari
        root.findViewById<ImageButton>(R.id.key_enter)?.setOnClickListener {
            handleEnterKey()
        }

        // Tombol Tanda Koma dan Titik
        root.findViewById<Button>(R.id.key_comma)?.setOnClickListener {
            commitCharacter(",")
        }
        root.findViewById<Button>(R.id.key_period)?.setOnClickListener {
            commitCharacter(".")
        }

        // Tombol Ganti Mode Simbol
        root.findViewById<Button>(R.id.key_symbol_switch)?.setOnClickListener {
            showSymbolKeyboard()
        }
    }

    /**
     * Mengirim karakter teks ke kolom input yang sedang aktif.
     */
    private fun commitCharacter(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)

        // Catat karakter ke buffer auto-text engine
        if (text.isNotEmpty()) {
            for (ch in text) {
                autoTextEngine.appendChar(ch)
            }
        }
    }

    /**
     * Menangani logika tombol Spasi (mendeteksi shortcut auto-text atau ketik spasi biasa).
     */
    private fun handleSpaceKey() {
        val ic = currentInputConnection ?: return
        // Delegasikan ke engine untuk auto-text replacement
        autoTextEngine.handleSpace(ic)
    }

    /**
     * Menangani tombol Backspace (menghapus 1 karakter teks di input dan di buffer).
     */
    private fun handleBackspaceKey() {
        val ic = currentInputConnection ?: return
        autoTextEngine.handleBackspace()
        ic.deleteSurroundingText(1, 0)
    }

    /**
     * Menangani tombol Enter (menjalankan editor action atau menyisipkan baris baru).
     */
    private fun handleEnterKey() {
        val ic = currentInputConnection ?: return
        autoTextEngine.resetBuffer()

        // Cek editor info apakah tombol aksi (Send, Search, Go, Next, Done)
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null && editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION != EditorInfo.IME_ACTION_NONE) {
            val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action)
                return
            }
        }

        // Default enter: baris baru
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    /**
     * Memperbarui label tombol alfabet berdasarkan status Shift/Caps.
     */
    private fun refreshAlphaKeyLabels() {
        for (btn in alphaButtons) {
            val current = btn.text.toString()
            btn.text = if (isShiftActive) current.uppercase() else current.lowercase()
        }
        val tintColor = if (isShiftActive) {
            ContextCompat.getColor(this, R.color.primary)
        } else {
            ContextCompat.getColor(this, R.color.key_text)
        }
        keyShiftButton?.setColorFilter(tintColor)
    }

    private fun showAlphabetKeyboard() {
        isSymbolMode = false
        layoutAlpha?.visibility = View.VISIBLE
        layoutSymbol?.visibility = View.GONE
    }

    private fun showSymbolKeyboard() {
        isSymbolMode = true
        layoutAlpha?.visibility = View.GONE
        layoutSymbol?.visibility = View.VISIBLE
    }

    /**
     * Memperbarui UI Suggestion Bar saat ada shortcut yang cocok.
     */
    private fun updateCandidateUI(shortcut: String?, expansion: String?) {
        if (shortcut != null && expansion != null) {
            tvCandidatePrefix?.visibility = View.VISIBLE
            tvCandidateText?.text = "$shortcut ➔ $expansion"
            tvCandidateText?.setTextColor(ContextCompat.getColor(this, R.color.candidate_text_highlight))
            btnCandidateChip?.setBackgroundResource(R.drawable.bg_candidate_chip)
        } else {
            val currentWord = autoTextEngine.getCurrentWord()
            if (currentWord.isNotEmpty()) {
                tvCandidatePrefix?.visibility = View.GONE
                tvCandidateText?.text = currentWord
                tvCandidateText?.setTextColor(ContextCompat.getColor(this, R.color.candidate_text_primary))
            } else {
                tvCandidatePrefix?.visibility = View.GONE
                tvCandidateText?.text = getString(R.string.keyboard_hint)
                tvCandidateText?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    // Handler untuk auto-repeat tombol backspace saat ditekan terus-menerus
    private val backspaceHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var isBackspaceRepeating = false

    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceRepeating) {
                handleBackspaceKey()
                // Interval penghapusan cepat (45 milidetik)
                backspaceHandler.postDelayed(this, 45)
            }
        }
    }

    /**
     * Memasang listener touch pada tombol backspace agar menghapus teks secara berkelanjutan saat ditahan.
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun attachRepeatBackspaceListener(button: View?) {
        button?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    // Hapus karakter pertama langsung
                    handleBackspaceKey()
                    isBackspaceRepeating = true
                    // Mulai pengulangan cepat setelah jeda awal 350 milidetik
                    backspaceHandler.postDelayed(backspaceRepeatRunnable, 350)
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    isBackspaceRepeating = false
                    backspaceHandler.removeCallbacks(backspaceRepeatRunnable)
                    v.performClick()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isBackspaceRepeating = false
        backspaceHandler.removeCallbacks(backspaceRepeatRunnable)
        serviceScope.cancel()
    }
}
