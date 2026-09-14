package com.pkmobile.keyboard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pkmobile.keyboard.R
import com.pkmobile.keyboard.data.db.AppDatabase
import com.pkmobile.keyboard.data.db.ShortcutEntity
import com.pkmobile.keyboard.data.importer.ShortcutImporter
import com.pkmobile.keyboard.data.repository.ShortcutRepository
import com.pkmobile.keyboard.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ShortcutRepository
    private lateinit var adapter: ShortcutAdapter

    // Callback untuk menerima file dari File Picker (XML, CSV, TXT, JSON dari Perfect Keyboard)
    private var pendingFileImportCallback: ((String) -> Unit)? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFileUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(this)
        repository = ShortcutRepository(database.shortcutDao())

        setupViews()
        setupRecyclerView()
        observeShortcuts()

        // Pembersihan otomatis database dari tag XML lama (seperti <tscut>)
        lifecycleScope.launch(Dispatchers.IO) {
            val cleaned = repository.sanitizeExistingDatabase()
            if (cleaned > 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Otomatis membersihkan $cleaned data shortcut dari tag XML.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupViews() {
        // Tombol 1: Buka pengaturan sistem untuk menyalakan Keyboard
        binding.btnEnableIme.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        // Tombol 2: Buka dialog pemilih input method aktif
        binding.btnSelectIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }

        // Tombol Bulk Input Teks (Notepad)
        binding.btnBulkInput.setOnClickListener {
            showBulkInputDialog()
        }

        // Tombol Tambah Shortcut Manual
        binding.btnAddShortcut.setOnClickListener {
            showAddShortcutDialog()
        }

        // Tombol Migrasi / Import dari Perfect Keyboard
        binding.btnImportShortcut.setOnClickListener {
            showImportDialog()
        }

        // Tombol Ekspor Backup Shortcut (Pilihan JSON / CSV)
        binding.btnExportShortcut.setOnClickListener {
            showExportChoiceDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ShortcutAdapter(
            onItemClick = { shortcutEntity ->
                showEditShortcutDialog(shortcutEntity)
            },
            onDeleteClick = { shortcutEntity ->
                // Hapus shortcut
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.deleteShortcut(shortcutEntity)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, R.string.toast_deleted, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvShortcuts.layoutManager = LinearLayoutManager(this)
        binding.rvShortcuts.adapter = adapter
    }

    /**
     * Dialog Edit Trigger dan Teks Ekspansi:
     * Menampilkan data trigger dan teks ekspansi saat ini,
     * membersihkan trigger via ShortcutImporter.cleanTrigger(),
     * dan memperbarui entity ke Room Database.
     */
    private fun showEditShortcutDialog(shortcut: ShortcutEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_shortcut, null)
        val etEditTrigger = dialogView.findViewById<TextInputEditText>(R.id.etEditTrigger)
        val etEditExpansion = dialogView.findViewById<TextInputEditText>(R.id.etEditExpansion)
        val btnEditCancel = dialogView.findViewById<MaterialButton>(R.id.btnEditCancel)
        val btnEditSave = dialogView.findViewById<MaterialButton>(R.id.btnEditSave)

        // Isi form dengan data yang sedang diedit
        etEditTrigger.setText(shortcut.shortcut)
        etEditExpansion.setText(shortcut.expansion)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnEditCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnEditSave.setOnClickListener {
            val rawTrigger = etEditTrigger.text?.toString() ?: ""
            val rawExpansion = etEditExpansion.text?.toString() ?: ""

            // Bersihkan trigger input menggunakan ShortcutImporter.cleanTrigger()
            val cleanKey = ShortcutImporter.cleanTrigger(rawTrigger)
            val cleanExp = rawExpansion.trim()

            // Validasi agar trigger dan expansion tidak kosong
            if (cleanKey.isBlank() || cleanExp.isBlank()) {
                Toast.makeText(this, "Trigger dan teks ekspansi tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnEditSave.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val updatedEntity = shortcut.copy(
                        shortcut = cleanKey.lowercase(),
                        expansion = cleanExp
                    )
                    repository.update(updatedEntity)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Shortcut berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        if (dialog.isShowing && !isFinishing && !isDestroyed) {
                            dialog.dismiss()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnEditSave.isEnabled = true
                        Toast.makeText(
                            this@MainActivity,
                            "Gagal memperbarui shortcut: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun observeShortcuts() {
        lifecycleScope.launch {
            repository.allShortcutsFlow.collectLatest { list ->
                adapter.submitList(list)
                if (list.isEmpty()) {
                    binding.tvEmptyShortcuts.visibility = View.VISIBLE
                    binding.rvShortcuts.visibility = View.GONE
                } else {
                    binding.tvEmptyShortcuts.visibility = View.GONE
                    binding.rvShortcuts.visibility = View.VISIBLE
                }
                binding.tvShortcutHeader.text = "${getString(R.string.section_shortcuts)} (${list.size})"
            }
        }
    }

    private fun showAddShortcutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_shortcut, null)
        val etShortcut = dialogView.findViewById<TextInputEditText>(R.id.et_input_shortcut)
        val etExpansion = dialogView.findViewById<TextInputEditText>(R.id.et_input_expansion)

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { dialog, _ ->
                val shortcutText = etShortcut.text?.toString()?.trim() ?: ""
                val expansionText = etExpansion.text?.toString()?.trim() ?: ""

                if (shortcutText.isNotEmpty() && expansionText.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.insertShortcut(shortcutText, expansionText)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Kata kunci dan pengganti tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Dialog untuk migrasi/impor shortcut dari Perfect Keyboard.
     * Pengguna bisa memilih berkas ekspor (.xml, .csv, .txt, .backup) atau menempelkan teks langsung.
     */
    private fun showImportDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_shortcut, null)
        val btnChooseFile = dialogView.findViewById<MaterialButton>(R.id.btn_choose_file)
        val tvFileStatus = dialogView.findViewById<TextView>(R.id.tv_file_status)
        val etRawImport = dialogView.findViewById<TextInputEditText>(R.id.et_raw_import)
        val tvPreviewCount = dialogView.findViewById<TextView>(R.id.tv_preview_count)

        var detectedPairs = emptyList<Pair<String, String>>()
        var parseJob: kotlinx.coroutines.Job? = null

        fun updatePreview(text: String) {
            parseJob?.cancel()
            tvPreviewCount.text = "Menganalisis berkas..."
            tvPreviewCount.setTextColor(getColor(R.color.candidate_text_highlight))

            parseJob = lifecycleScope.launch(Dispatchers.Default) {
                val pairs = ShortcutImporter.parse(text)
                withContext(Dispatchers.Main) {
                    detectedPairs = pairs
                    tvPreviewCount.text = "Pratinjau: ${pairs.size} shortcut terdeteksi"
                    if (pairs.isNotEmpty()) {
                        tvPreviewCount.setTextColor(getColor(R.color.accent))
                    } else {
                        tvPreviewCount.setTextColor(getColor(R.color.text_secondary))
                    }
                }
            }
        }

        // Live text change listener pada text field
        etRawImport.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: ""
            if (text.isNotBlank()) {
                updatePreview(text)
            }
        }

        // Callback saat user memilih file dari memory
        pendingFileImportCallback = { fileContent ->
            tvFileStatus.visibility = View.VISIBLE
            val sizeKb = (fileContent.length / 1024).coerceAtLeast(1)
            tvFileStatus.text = "✓ Berkas berhasil dimuat ($sizeKb KB)"

            if (fileContent.length < 5000) {
                etRawImport.setText(fileContent)
            } else {
                etRawImport.setText("")
                etRawImport.hint = "Berkas ($sizeKb KB) siap diimpor. Atau tempel teks manual."
            }
            updatePreview(fileContent)
        }

        btnChooseFile.setOnClickListener {
            // Luncurkan pemilih file berkas
            filePickerLauncher.launch("*/*")
        }

        val cbClearBeforeImport = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.cb_clear_before_import)

        val importDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Mulai Import") { dialog, _ ->
                if (detectedPairs.isNotEmpty()) {
                    val shouldClear = cbClearBeforeImport?.isChecked ?: false
                    lifecycleScope.launch(Dispatchers.IO) {
                        val count = repository.importShortcuts(detectedPairs, clearExisting = shouldClear)
                        withContext(Dispatchers.Main) {
                            val msg = if (shouldClear) {
                                "Database diperbarui! Berhasil mengimpor $count shortcut bersih."
                            } else {
                                getString(R.string.toast_imported, count)
                            }
                            Toast.makeText(
                                this@MainActivity,
                                msg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.toast_no_shortcut_found, Toast.LENGTH_SHORT).show()
                }
                pendingFileImportCallback = null
                dialog.dismiss()
            }
            .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                pendingFileImportCallback = null
                dialog.dismiss()
            }
            .setOnDismissListener {
                pendingFileImportCallback = null
            }
            .create()

        val btnOpenBulkInput = dialogView.findViewById<MaterialButton>(R.id.btn_open_bulk_input)
        btnOpenBulkInput?.setOnClickListener {
            pendingFileImportCallback = null
            importDialog.dismiss()
            showBulkInputDialog()
        }

        importDialog.show()
    }

    /**
     * Dialog Bulk Input Teks:
     * Pengguna dapat menempelkan seluruh daftar shortcut sekaligus dari Notepad / Aplikasi Teks.
     * Mendukung format per baris:
     * - TRIGGER = TEKS
     * - TRIGGER -> TEKS
     * - TRIGGER [TAB] TEKS
     */
    private fun showBulkInputDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bulk_input, null)
        val etBulkInput = dialogView.findViewById<TextInputEditText>(R.id.et_bulk_input)
        val tvBulkPreviewCount = dialogView.findViewById<TextView>(R.id.tv_bulk_preview_count)
        val btnBulkCancel = dialogView.findViewById<MaterialButton>(R.id.btn_bulk_cancel)
        val btnBulkProcess = dialogView.findViewById<MaterialButton>(R.id.btn_bulk_process)

        var detectedPairs = emptyList<Pair<String, String>>()
        var parseJob: kotlinx.coroutines.Job? = null

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // Deteksi langsung saat pengguna mengetik atau menempelkan teks
        etBulkInput.doAfterTextChanged { editable ->
            val text = editable?.toString() ?: ""
            parseJob?.cancel()
            if (text.isBlank()) {
                detectedPairs = emptyList()
                tvBulkPreviewCount.text = "Pratinjau: 0 shortcut terdeteksi"
                tvBulkPreviewCount.setTextColor(getColor(R.color.text_secondary))
            } else {
                tvBulkPreviewCount.text = "Menganalisis teks..."
                tvBulkPreviewCount.setTextColor(getColor(R.color.candidate_text_highlight))
                parseJob = lifecycleScope.launch(Dispatchers.Default) {
                    val pairs = ShortcutImporter.parseBulkText(text)
                    withContext(Dispatchers.Main) {
                        detectedPairs = pairs
                        tvBulkPreviewCount.text = "Pratinjau: ${pairs.size} shortcut terdeteksi"
                        if (pairs.isNotEmpty()) {
                            tvBulkPreviewCount.setTextColor(getColor(R.color.accent))
                        } else {
                            tvBulkPreviewCount.setTextColor(getColor(R.color.text_secondary))
                        }
                    }
                }
            }
        }

        btnBulkCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnBulkProcess.setOnClickListener {
            val text = etBulkInput.text?.toString() ?: ""
            if (text.isBlank()) {
                Toast.makeText(this, "Silakan tempel teks shortcut terlebih dahulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnBulkProcess.isEnabled = false
            btnBulkProcess.text = "Menyimpan ke Database..."

            lifecycleScope.launch(Dispatchers.IO) {
                val pairs = ShortcutImporter.parseBulkText(text)
                if (pairs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        btnBulkProcess.isEnabled = true
                        btnBulkProcess.text = "Proses & Simpan Ke Database"
                        Toast.makeText(this@MainActivity, "Tidak ditemukan format shortcut yang valid.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val entities = pairs.map { (key, value) ->
                    ShortcutEntity(
                        shortcut = key.trim().lowercase(),
                        expansion = value.trim()
                    )
                }
                repository.insertAll(entities)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Berhasil menyimpan ${entities.size} shortcut ke database!",
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun handleSelectedFileUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: ""

                withContext(Dispatchers.Main) {
                    if (content.isNotBlank()) {
                        pendingFileImportCallback?.invoke(content)
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal membaca berkas atau berkas kosong.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error membaca file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Menampilkan dialog pilihan format ekspor cadangan (JSON / CSV).
     */
    private fun showExportChoiceDialog() {
        val options = arrayOf(
            "📁 Simpan Cadangan sebagai JSON (shortcuts.json) [Direkomendasikan]",
            "📄 Simpan Cadangan sebagai CSV (shortcuts.csv)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Format Ekspor Cadangan")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportShortcuts(asJson = true)
                    1 -> exportShortcuts(asJson = false)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    /**
     * Membagikan / mengekspor seluruh shortcut ke format JSON atau CSV standar via Android Share Sheet
     * dan otomatis menyimpan salinan ke folder Download perangkat.
     */
    private fun exportShortcuts(asJson: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = repository.getAllList()
            if (list.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Tidak ada shortcut untuk diekspor.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val fileName = if (asJson) "shortcuts.json" else "shortcuts.csv"
            val mimeType = if (asJson) "application/json" else "text/csv"
            val fileContent = if (asJson) {
                ShortcutImporter.exportToJson(list)
            } else {
                ShortcutImporter.exportToCsv(list)
            }

            // Simpan salinan berkas ke folder Download internal jika memungkinkan
            try {
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir.exists() || downloadDir.mkdirs()) {
                    val exportFile = java.io.File(downloadDir, fileName)
                    exportFile.writeText(fileContent)
                }
            } catch (_: Exception) {
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Berhasil mengekspor ${list.size} shortcut ($fileName)",
                    Toast.LENGTH_SHORT
                ).show()

                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, fileContent)
                    putExtra(Intent.EXTRA_TITLE, fileName)
                    type = mimeType
                }
                val shareIntent = Intent.createChooser(sendIntent, "Ekspor / Backup Shortcut ($fileName)")
                startActivity(shareIntent)
            }
        }
    }
}
