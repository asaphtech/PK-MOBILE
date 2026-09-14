package com.pkmobile.keyboard.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.ui.SettingsActivity
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import com.pkmobile.keyboard.engine.AutoTextEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Service Utama Input Method Editor (IME) untuk Custom Keyboard PK MOBILE.
 * Menangani pembuatan tampilan keyboard dengan dukungan:
 * 1. Layer Alfabet (QWERTY)
 * 2. Layer Simbol 1/2 (Angka & Simbol Umum)
 * 3. Layer Simbol 2/2 (Simbol Lengkap PC)
 * 4. Layer Fn (Tombol Fungsi PC CS yang terintegrasi langsung ke AutoTextEngine)
 * 5. Tombol ENTER teks (Aksi Kirim/Submit) & tombol Alinea Baru (↵ baris baru murni)
 */
class CustomKeyboardService : InputMethodService() {

    enum class KeyboardLayer {
        ALPHA,
        SYMBOL_1,
        SYMBOL_2,
        FN
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var autoTextEngine: AutoTextEngine
    private lateinit var repository: ShortcutRepository

    private var currentLayer = KeyboardLayer.ALPHA
    private var isShiftActive = false

    // Referensi global PopupWindow menu slash/fn untuk mencegah re-creation & flickering
    private var fnPopupWindow: PopupWindow? = null

    // Layout Containers
    private var keyboardRoot: View? = null
    private var layoutAlpha: LinearLayout? = null
    private var layoutSymbol1: LinearLayout? = null
    private var layoutSymbol2: LinearLayout? = null
    private var layoutFn: LinearLayout? = null

    // Suggestion Bar Views
    private var tvCandidatePrefix: TextView? = null
    private var tvCandidateText: TextView? = null
    private var btnCandidateChip: LinearLayout? = null
    private var keyShiftButton: ImageButton? = null

    // Daftar tombol alfabet untuk refresh uppercase/lowercase
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
        setupSymbol1Keys(view)
        setupSymbol2Keys(view)
        setupFnKeys(view)
        setupSpecialKeys(view)

        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadPreferences()
        // Reset state setiap kali keyboard terbuka
        autoTextEngine.resetBuffer()
        isShiftActive = false
        showLayer(KeyboardLayer.ALPHA)
        updateCandidateUI(null, null)
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val expansionMode = prefs.getString(SettingsActivity.PREF_EXPANSION_MODE, SettingsActivity.MODE_MANUAL)
        autoTextEngine.isInstantMode = (expansionMode == SettingsActivity.MODE_INSTANT)
    }

    private fun initViews(root: View) {
        layoutAlpha = root.findViewById(R.id.layout_alpha)
        layoutSymbol1 = root.findViewById(R.id.layout_symbol)
        layoutSymbol2 = root.findViewById(R.id.layout_symbol2)
        layoutFn = root.findViewById(R.id.layout_fn)

        tvCandidatePrefix = root.findViewById(R.id.tv_candidate_prefix)
        tvCandidateText = root.findViewById(R.id.tv_candidate_text)
        btnCandidateChip = root.findViewById(R.id.btn_candidate_chip)
        keyShiftButton = root.findViewById(R.id.key_shift)

        // Klik chip candidate suggestion bar untuk mengekspansi shortcut
        btnCandidateChip?.setOnClickListener {
            currentInputConnection?.let { ic ->
                autoTextEngine.applyCandidateExpansion(ic)
            }
        }
    }

    /**
     * Berpindah antar layer keyboard (ALPHA, SYMBOL_1, SYMBOL_2, FN).
     */
    private fun showLayer(layer: KeyboardLayer) {
        currentLayer = layer
        layoutAlpha?.visibility = if (layer == KeyboardLayer.ALPHA) View.VISIBLE else View.GONE
        layoutSymbol1?.visibility = if (layer == KeyboardLayer.SYMBOL_1) View.VISIBLE else View.GONE
        layoutSymbol2?.visibility = if (layer == KeyboardLayer.SYMBOL_2) View.VISIBLE else View.GONE
        layoutFn?.visibility = if (layer == KeyboardLayer.FN) View.VISIBLE else View.GONE
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

                    if (isShiftActive) {
                        isShiftActive = false
                        refreshAlphaKeyLabels()
                    }
                }
            }
        }
    }

    /**
     * Mengatur tombol simbol Halaman 1 (1/2).
     */
    private fun setupSymbol1Keys(root: View) {
        val symbol1KeyIds = intArrayOf(
            R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5,
            R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_0,
            R.id.sym_at, R.id.sym_hash, R.id.sym_dollar, R.id.sym_percent,
            R.id.sym_amp, R.id.sym_minus, R.id.sym_plus, R.id.sym_paren_open,
            R.id.sym_paren_close, R.id.sym_slash, R.id.sym_star, R.id.sym_quote,
            R.id.sym_singlequote, R.id.sym_colon, R.id.sym_semicolon,
            R.id.sym_exclamation, R.id.sym_question, R.id.sym_comma, R.id.sym_period
        )

        for (id in symbol1KeyIds) {
            val btn = root.findViewById<Button>(id)
            btn?.setOnClickListener {
                commitCharacter(btn.text.toString())
            }
        }

        // Navigasi ke Halaman Simbol 2 (2/2)
        root.findViewById<Button>(R.id.sym_page_switch)?.setOnClickListener {
            showLayer(KeyboardLayer.SYMBOL_2)
        }

        // Kembali ke Huruf ABC
        root.findViewById<Button>(R.id.sym_switch_abc)?.setOnClickListener {
            showLayer(KeyboardLayer.ALPHA)
        }

        // Beralih ke Halaman Fn
        root.findViewById<Button>(R.id.sym_switch_fn)?.setOnClickListener {
            showLayer(KeyboardLayer.FN)
        }

        // Spasi
        root.findViewById<Button>(R.id.sym_space)?.setOnClickListener {
            handleSpaceKey()
        }

        // Backspace dengan auto-repeat
        attachRepeatBackspaceListener(root.findViewById(R.id.sym_key_backspace))

        // Tombol Alinea Baru murni (\n)
        root.findViewById<Button>(R.id.sym_new_line)?.setOnClickListener {
            handleNewLineKey()
        }

        // Tombol ENTER (Aksi Kirim/Submit)
        root.findViewById<Button>(R.id.sym_enter)?.setOnClickListener {
            handleEnterKey()
        }
    }

    /**
     * Mengatur tombol simbol Halaman 2 (2/2) - Simbol Tambahan PC Lengkap.
     * Termasuk: ~ ` ^ = _ { } [ ] \ | < > dan simbol komputasi lainnya.
     */
    private fun setupSymbol2Keys(root: View) {
        val symbol2KeyIds = intArrayOf(
            R.id.sym2_tilde, R.id.sym2_backtick, R.id.sym2_caret, R.id.sym2_equal,
            R.id.sym2_underscore, R.id.sym2_brace_open, R.id.sym2_brace_close,
            R.id.sym2_bracket_open, R.id.sym2_bracket_close, R.id.sym2_backslash,
            R.id.sym2_pipe, R.id.sym2_less, R.id.sym2_greater, R.id.sym2_euro,
            R.id.sym2_pound, R.id.sym2_yen, R.id.sym2_cent, R.id.sym2_degree,
            R.id.sym2_bullet, R.id.sym2_ellipsis, R.id.sym2_guillemet_left,
            R.id.sym2_guillemet_right, R.id.sym2_section, R.id.sym2_copy,
            R.id.sym2_reg, R.id.sym2_plusminus, R.id.sym2_notequal,
            R.id.sym2_comma, R.id.sym2_period
        )

        for (id in symbol2KeyIds) {
            val btn = root.findViewById<Button>(id)
            btn?.setOnClickListener {
                commitCharacter(btn.text.toString())
            }
        }

        // Navigasi kembali ke Halaman Simbol 1 (1/2)
        root.findViewById<Button>(R.id.sym2_page_switch)?.setOnClickListener {
            showLayer(KeyboardLayer.SYMBOL_1)
        }

        // Kembali ke Huruf ABC
        root.findViewById<Button>(R.id.sym2_switch_abc)?.setOnClickListener {
            showLayer(KeyboardLayer.ALPHA)
        }

        // Beralih ke Halaman Fn
        root.findViewById<Button>(R.id.sym2_switch_fn)?.setOnClickListener {
            showLayer(KeyboardLayer.FN)
        }

        // Spasi
        root.findViewById<Button>(R.id.sym2_space)?.setOnClickListener {
            handleSpaceKey()
        }

        // Backspace dengan auto-repeat
        attachRepeatBackspaceListener(root.findViewById(R.id.sym2_key_backspace))

        // Tombol Alinea Baru murni (\n)
        root.findViewById<Button>(R.id.sym2_new_line)?.setOnClickListener {
            handleNewLineKey()
        }

        // Tombol ENTER (Aksi Kirim/Submit)
        root.findViewById<Button>(R.id.sym2_enter)?.setOnClickListener {
            handleEnterKey()
        }
    }

    /**
     * Mengatur tombol fungsi PC pada Layer "Fn".
     * Ketentuan Khusus:
     * - TIDAK memicu aksi native Android (tidak memicu refresh/back).
     * - Mengirim nilai string trigger (misal: "[F5]", "[ESC]", "[DEL]") ke AutoTextEngine.processKeyInput().
     * - Menjalankan ekspansi auto-text jika kata/kode trigger cocok dengan database Room.
     */
    private fun setupFnKeys(root: View) {
        val fnKeyMap = mapOf(
            R.id.fn_f1 to "[F1]",
            R.id.fn_f2 to "[F2]",
            R.id.fn_f3 to "[F3]",
            R.id.fn_f4 to "[F4]",
            R.id.fn_f5 to "[F5]",
            R.id.fn_f6 to "[F6]",
            R.id.fn_f7 to "[F7]",
            R.id.fn_f8 to "[F8]",
            R.id.fn_f9 to "[F9]",
            R.id.fn_f10 to "[F10]",
            R.id.fn_f11 to "[F11]",
            R.id.fn_f12 to "[F12]",
            R.id.fn_esc to "[ESC]",
            R.id.fn_home to "[HOME]",
            R.id.fn_end to "[END]",
            R.id.fn_pgup to "[PGUP]",
            R.id.fn_pgdn to "[PGDN]",
            R.id.fn_ins to "[INS]",
            R.id.fn_del to "[DEL]",
            R.id.fn_prtscn to "[PRTSCN]",
            R.id.fn_ctrl to "[CTRL]",
            R.id.fn_alt to "[ALT]",
            R.id.fn_caps to "[CAPS]",
            R.id.fn_tab to "[TAB]",
            R.id.fn_break to "[BREAK]"
        )

        for ((viewId, trigger) in fnKeyMap) {
            root.findViewById<Button>(viewId)?.setOnClickListener {
                // Eksekusi langsung ke AutoTextEngine tanpa aksi native Android
                autoTextEngine.processKeyInput(trigger, currentInputConnection)
            }
        }

        // Navigasi ke Huruf ABC
        root.findViewById<Button>(R.id.fn_switch_abc)?.setOnClickListener {
            showLayer(KeyboardLayer.ALPHA)
        }

        // Navigasi ke Simbol ?123
        root.findViewById<Button>(R.id.fn_switch_sym)?.setOnClickListener {
            showLayer(KeyboardLayer.SYMBOL_1)
        }

        // Spasi
        root.findViewById<Button>(R.id.fn_space)?.setOnClickListener {
            handleSpaceKey()
        }

        // Backspace dengan auto-repeat
        attachRepeatBackspaceListener(root.findViewById(R.id.fn_key_backspace))

        // Tombol Alinea Baru murni (\n)
        root.findViewById<Button>(R.id.fn_new_line)?.setOnClickListener {
            handleNewLineKey()
        }

        // Tombol ENTER (Aksi Kirim/Submit)
        root.findViewById<Button>(R.id.fn_enter)?.setOnClickListener {
            handleEnterKey()
        }
    }

    /**
     * Mengatur tombol kontrol khusus pada Layer Alfabet (Shift, Space, Backspace, Enter, dll).
     */
    private fun setupSpecialKeys(root: View) {
        // Tombol Shift / Caps
        keyShiftButton?.setOnClickListener {
            isShiftActive = !isShiftActive
            refreshAlphaKeyLabels()
        }

        // Tombol Backspace dengan continuous repeat on hold
        attachRepeatBackspaceListener(root.findViewById(R.id.key_backspace))

        // Tombol Spasi (Pemicu Utama Auto-Text / Shortcut Expansion)
        root.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            handleSpaceKey()
        }

        // Tombol Alinea Baru (\n) murni
        root.findViewById<Button>(R.id.key_new_line)?.setOnClickListener {
            handleNewLineKey()
        }

        // Tombol ENTER (Aksi Kirim/Submit)
        root.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            handleEnterKey()
        }

        // Tombol Slash (/) dengan dukungan tap singkat & long-press menu popup
        val keySlashBtn = root.findViewById<Button>(R.id.key_slash)
        keySlashBtn?.setOnClickListener {
            commitCharacter("/")
        }
        keySlashBtn?.setOnLongClickListener { view ->
            // Cegah Re-creation & Looping Pop-up
            if (fnPopupWindow?.isShowing == true) {
                return@setOnLongClickListener true
            }
            // Haptic Feedback (getaran pendek)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showSlashMenuPopup(view)
            // Wajib kembalikan true agar Android System tidak memproses event lanjutan
            true
        }

        // Tanda Koma dan Titik
        root.findViewById<Button>(R.id.key_comma)?.setOnClickListener {
            commitCharacter(",")
        }
        root.findViewById<Button>(R.id.key_period)?.setOnClickListener {
            commitCharacter(".")
        }

        // Tombol Ganti Mode Simbol (?123)
        root.findViewById<Button>(R.id.key_symbol_switch)?.setOnClickListener {
            showLayer(KeyboardLayer.SYMBOL_1)
        }

        // Tombol Beralih ke Halaman Fn
        root.findViewById<Button>(R.id.key_fn_switch)?.setOnClickListener {
            showLayer(KeyboardLayer.FN)
        }
    }

    /**
     * Menampilkan menu popup melayang di atas tombol slash (/) saat ditekan lama (Long-Press).
     * Opsi Menu:
     * 1) Layer Fn (Tombol PC)
     * 2) Pengaturan Aplikasi
     */
    private fun showSlashMenuPopup(anchorView: View) {
        if (fnPopupWindow?.isShowing == true) {
            return
        }
        try {
            val themedContext = androidx.appcompat.view.ContextThemeWrapper(this, R.style.Theme_PKMobileKeyboard)
            val inflater = LayoutInflater.from(themedContext)
            val popupView = inflater.inflate(R.layout.popup_slash_menu, null)

            val popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                elevation = 16f
                isOutsideTouchable = true
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                setOnDismissListener {
                    if (fnPopupWindow == this) {
                        fnPopupWindow = null
                    }
                }
            }
            fnPopupWindow = popupWindow

            popupView.findViewById<View>(R.id.menu_layer_fn)?.setOnClickListener {
                dismissFnPopup()
                showLayer(KeyboardLayer.FN)
            }

            popupView.findViewById<View>(R.id.menu_settings)?.setOnClickListener {
                dismissFnPopup()
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }

            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val yOffset = -(anchorView.height + popupView.measuredHeight + 16)
            try {
                popupWindow.showAsDropDown(anchorView, 0, yOffset)
            } catch (_: Exception) {
                keyboardRoot?.let { root ->
                    popupWindow.showAtLocation(root, Gravity.CENTER, 0, 0)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("PKKeyboard", "Gagal menampilkan popup menu slash", e)
        }
    }

    /**
     * Mengirim karakter teks ke kolom input yang sedang aktif.
     */
    private fun commitCharacter(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)

        if (text.isNotEmpty()) {
            for (ch in text) {
                autoTextEngine.appendChar(ch, ic)
            }
        }
    }

    /**
     * Menangani penekanan tombol Spasi (mendeteksi shortcut auto-text atau spasi biasa).
     */
    private fun handleSpaceKey() {
        val ic = currentInputConnection ?: return
        autoTextEngine.handleSpace(ic)
    }

    /**
     * Menangani tombol Backspace.
     * Jika ada teks yang sedang diblok (selectedText tidak kosong):
     * - Hapus hanya teks yang sedang diblok menggunakan commitText("", 1)
     * - Reset buffer kata di autoTextEngine
     * - JANGAN jalankan deleteSurroundingText(1, 0) agar spasi/karakter sebelumnya tidak ikut terhapus!
     * Jika tidak ada teks yang diblok:
     * - Jalankan logika penghapusan karakter normal seperti sebelumnya.
     */
    private fun handleBackspaceKey() {
        val ic = currentInputConnection ?: return

        // 1. Periksa apakah ada teks yang sedang diblok / diseleksi
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            // Hapus hanya teks yang sedang diblok secara native
            ic.commitText("", 1)
            autoTextEngine.resetBuffer()
            return
        }

        // 2. Jika tidak ada teks yang diblok, jalankan penghapusan normal
        ic.deleteSurroundingText(1, 0)
        autoTextEngine.handleBackspace(ic)
    }

    /**
     * Menangani tombol Alinea Baru ("↵").
     * Memanggil currentInputConnection.commitText("\n", 1) untuk baris baru murni.
     */
    private fun handleNewLineKey() {
        val ic = currentInputConnection ?: return
        ic.commitText("\n", 1)
        autoTextEngine.resetBuffer()
    }

    /**
     * Menangani tombol "ENTER" (Aksi Kirim / Submit).
     * Memanggil sendDefaultEditorAction(true).
     * Jika aksi editor tidak di-handle oleh input target, kirim fallback KeyEvent ENTER.
     */
    private fun handleEnterKey() {
        val ic = currentInputConnection ?: return

        // 1. Cek apakah ada shortcut sebelum kursor untuk diekspansi
        val expanded = autoTextEngine.handleEnter(ic)
        if (expanded) {
            checkHideKeyboardOnSend()
            return
        }

        // 2. Eksekusi aksi kirim / submit editor (sendDefaultEditorAction)
        val actionHandled = sendDefaultEditorAction(true)
        if (!actionHandled) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }

        checkHideKeyboardOnSend()
    }

    /**
     * Memeriksa preferensi "Sembunyikan Keyboard setelah Kirim" (pref_hide_on_send).
     * Jika aktif, keyboard ditutup otomatis setiap tombol ENTER ditekan.
     */
    private fun checkHideKeyboardOnSend() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val isHideOnSend = prefs.getBoolean(SettingsActivity.PREF_HIDE_ON_SEND, false)
        if (isHideOnSend) {
            requestHideSelf(0)
        }
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

    /**
     * Memperbarui UI Suggestion Bar saat ada shortcut yang cocok.
     */
    private fun updateCandidateUI(shortcut: String?, expansion: String?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                updateCandidateUI(shortcut, expansion)
            }
            return
        }

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

    // Handler untuk auto-repeat tombol backspace saat ditahan
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var isBackspaceRepeating = false

    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceRepeating) {
                handleBackspaceKey()
                backspaceHandler.postDelayed(this, 45)
            }
        }
    }

    /**
     * Memasang listener touch pada tombol backspace agar menghapus teks secara berkelanjutan saat ditahan.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachRepeatBackspaceListener(button: View?) {
        button?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    handleBackspaceKey()
                    isBackspaceRepeating = true
                    backspaceHandler.postDelayed(backspaceRepeatRunnable, 350)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
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

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dismissFnPopup()
    }

    /**
     * Membersihkan dan menutup popup Fn/Slash secara aman untuk mencegah memory leak.
     */
    private fun dismissFnPopup() {
        try {
            if (fnPopupWindow?.isShowing == true) {
                fnPopupWindow?.dismiss()
            }
        } catch (_: Exception) {}
        fnPopupWindow = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissFnPopup()
        isBackspaceRepeating = false
        backspaceHandler.removeCallbacks(backspaceRepeatRunnable)
        serviceScope.cancel()
    }
}
